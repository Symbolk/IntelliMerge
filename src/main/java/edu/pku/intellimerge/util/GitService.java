package edu.pku.intellimerge.util;

import edu.pku.intellimerge.model.SimpleDiffEntry;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class GitService {
  private static final Logger logger = LoggerFactory.getLogger(GitService.class);

  public static Repository cloneIfNotExists(String projectPath, String cloneUrl) throws Exception {
    File folder = new File(projectPath);
    Repository repository;
    if (folder.exists()) {
      RepositoryBuilder builder = new RepositoryBuilder();
      repository =
          builder.setGitDir(new File(folder, ".git")).readEnvironment().findGitDir().build();

      logger.info(
          "Project {} is already cloned, current branch is {}", cloneUrl, repository.getBranch());
    } else {
      logger.info("Cloning {} ...", cloneUrl);
      Git git =
          Git.cloneRepository()
              .setDirectory(folder)
              .setURI(cloneUrl)
              .setCloneAllBranches(true)
              .call();
      repository = git.getRepository();
      logger.info("Done cloning {}, current branch is {}", cloneUrl, repository.getBranch());
    }
    return repository;
  }

  public static Repository cloneIfNotExistsWithBranch(
      String projectPath, String cloneUrl, String branch) throws Exception {

    Repository repository = cloneIfNotExists(projectPath, cloneUrl);

    if (branch != null && !repository.getBranch().equals(branch)) {
      Git git = new Git(repository);

      String localBranch = "refs/heads/" + branch;
      List<Ref> refs = git.branchList().call();
      boolean branchExists = false;
      for (Ref ref : refs) {
        if (ref.getName().equals(localBranch)) {
          branchExists = true;
        }
      }

      if (branchExists) {
        git.checkout().setName(branch).call();
      } else {
        git.checkout()
            .setCreateBranch(true)
            .setName(branch)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint("origin/" + branch)
            .call();
      }

      logger.info("Project {} switched to {}", cloneUrl, repository.getBranch());
    }
    return repository;
  }

  public static void checkout(Repository repository, String commitID) throws Exception {
    try (Git git = new Git(repository)) {
      CheckoutCommand checkout = git.checkout().setName(commitID);
      checkout.call();
    }
    logger.info("Checking out {} {} ...", repository.getDirectory().getParent().toString(), commitID);
    //		File workingDir = repository.getDirectory().getParentFile();
    //		ExternalProcess.execute(workingDir, "git", "checkout", commitID);
  }

  /**
   * List all changed java files between old and new commit, other language will be supported in
   * future
   *
   * @param repository
   * @param oldCommit
   * @param newCommit
   * @return
   * @throws GitAPIException
   * @throws IOException
   */
  public static List<SimpleDiffEntry> listDiffFilesJava(
      Repository repository, String oldCommit, String newCommit)
      throws GitAPIException, IOException {
    try (Git git = new Git(repository)) {

      final List<DiffEntry> javaDiffEntries =
          git.diff()
              .setOldTree(prepareTreeParser(repository, oldCommit))
              .setNewTree(prepareTreeParser(repository, newCommit))
              .call()
              .stream()
              .filter(diffEntry -> isJavaFile(diffEntry.getNewPath()))
              .collect(Collectors.toList());

      List<SimpleDiffEntry> javaDiffs = new ArrayList<>(new HashSet<>());
      for (DiffEntry diffEntry : javaDiffEntries) {
        javaDiffs.add(
            new SimpleDiffEntry(
                diffEntry.getOldPath(), diffEntry.getNewPath(), diffEntry.getChangeType()));
      }
      return javaDiffs;
    }
  }

  private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId)
      throws IOException {
    // from the commit we can build the tree which allows us to construct the TreeParser
    //noinspection Duplicates
    try (RevWalk walk = new RevWalk(repository)) {
      RevCommit commit = walk.parseCommit(repository.resolve(objectId));
      RevTree tree = walk.parseTree(commit.getTree().getId());

      CanonicalTreeParser treeParser = new CanonicalTreeParser();
      try (ObjectReader reader = repository.newObjectReader()) {
        treeParser.reset(reader, tree.getId());
      }

      walk.dispose();

      return treeParser;
    }
  }

  private static boolean isJavaFile(String path) {
    return path.endsWith(".java");
  }

  public String getFileContentAtCommit(Repository repository, String commitID, String filePath)
      throws Exception {
    ObjectId commitObjectId = ObjectId.fromString(commitID);

    // a RevWalk allows to walk over commits based on some filtering that is defined
    try (RevWalk revWalk = new RevWalk(repository)) {
      RevCommit commit = revWalk.parseCommit(commitObjectId);
      // and using commit's tree find the path
      RevTree tree = commit.getTree();

      // now try to find a specific file
      try (TreeWalk treeWalk = new TreeWalk(repository)) {
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(filePath));
        if (!treeWalk.next()) {
          throw new IllegalStateException("Did not find expected file " + filePath);
        }

        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(objectId);
        return new String(loader.getBytes(), StandardCharsets.UTF_8);
      }
      //      revWalk.dispose();
    }
  }
}
