package edu.pku.intellimerge.io;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SimpleDiffEntry;
import edu.pku.intellimerge.model.SourceFile;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.GitService;
import edu.pku.intellimerge.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SourceFileCollector {

  private static final Logger logger = LoggerFactory.getLogger(SourceFileCollector.class);

  private Repository repository;
  private String collectedFilePath;
  private MergeScenario mergeScenario;
  // care only BothSides modified (possibly textually conflict files)
  private boolean onlyBothModified = false;
  // copy imported files or not
  private boolean copyImportedFiles = true;
  // save the collected file content to disk or not (true for development, false for production)
  //  private boolean collectToFiles = true;

  public SourceFileCollector(
      MergeScenario mergeScenario, Repository repository, String collectedFilePath) {
    this.mergeScenario = mergeScenario;
    this.repository = repository;
    this.collectedFilePath = collectedFilePath;
  }

  public SourceFileCollector(String repoPath, List<String> branchNames, String collectedDir) {
    try {
      this.repository = GitService.createRepository(repoPath);
      this.mergeScenario = generateMergeScenarioFromBranches(branchNames);
    } catch (Exception e) {
      e.printStackTrace();
    }
    this.collectedFilePath = collectedDir;
    this.copyImportedFiles = false;
  }

  /**
   * Get commit ids at the HEAD of the parent branches, and their base commit id
   *
   * @param branchNames
   * @return
   */
  private MergeScenario generateMergeScenarioFromBranches(List<String> branchNames)
      throws Exception {
    if (repository != null) {
      String oursCommitID = GitService.getHEADCommit(repository, branchNames.get(0));
      String theirsCommitID = GitService.getHEADCommit(repository, branchNames.get(1));
      String baseCommitID = GitService.getBASECommit(repository, oursCommitID, theirsCommitID);

      MergeScenario mergeScenario = new MergeScenario(oursCommitID, baseCommitID, theirsCommitID);
      return mergeScenario;
    } else {
      return null;
    }
  }

  public MergeScenario getMergeScenario() {
    return mergeScenario;
  }

  public void setOnlyBothModified(boolean onlyBothModified) {
    this.onlyBothModified = onlyBothModified;
  }

  public void setCopyImportedFiles(boolean copyImportedFiles) {
    this.copyImportedFiles = copyImportedFiles;
  }

  /** Collect related source files to process together */
  public void collectFilesForAllSides() {
    try {
      computeDiffEntries();
      if (this.onlyBothModified) {
        // collect only both modified files in two sides
        collectFilesForOneSide(Side.OURS, mergeScenario.bothModifiedEntries);
        collectFilesForOneSide(Side.BASE, mergeScenario.bothModifiedEntries);
        collectFilesForOneSide(Side.THEIRS, mergeScenario.bothModifiedEntries);
      } else {
        // collect both-sides modified and one-side modified files
        collectFilesForOneSide(Side.OURS, mergeScenario.oursDiffEntries);
        collectFilesForOneSide(Side.BASE, mergeScenario.baseDiffEntries);
        collectFilesForOneSide(Side.THEIRS, mergeScenario.theirsDiffEntries);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Collect related source files to process once for one side
   *
   * @param side
   * @param diffEntries
   * @throws Exception
   */
  public void collectFilesForOneSide(Side side, List<SimpleDiffEntry> diffEntries)
      throws Exception {
    String sideCommitID = null;
    switch (side) {
      case OURS:
        sideCommitID = mergeScenario.oursCommitID;
        break;
      case BASE:
        sideCommitID = mergeScenario.baseCommitID;
        break;
      case THEIRS:
        sideCommitID = mergeScenario.theirsCommitID;
        break;
    }
    if (sideCommitID != null) {
      //      ArrayList<SourceFile> javaSourceFiles = scanJavaFiles(sideCommitID);
      String sideCollectedFilePath =
          collectedFilePath + side.toString().toLowerCase() + File.separator;
      if (diffEntries != null) {
        //        collect(javaSourceFiles, diffEntries, sideCollectedFilePath);
        collect2(diffEntries, sideCollectedFilePath, sideCommitID);
      }
    }
  }

  /**
   * Get diff java files between base and ours/theirs commit
   *
   * @return
   * @throws Exception
   */
  public void computeDiffEntries() throws Exception {
    Set<SimpleDiffEntry> oursDiffEntries =
        new HashSet<>(
            GitService.listDiffFilesJava(
                repository, mergeScenario.baseCommitID, mergeScenario.oursCommitID));
    Set<SimpleDiffEntry> theirsDiffEntries =
        new HashSet<>(
            GitService.listDiffFilesJava(
                repository, mergeScenario.baseCommitID, mergeScenario.theirsCommitID));
    // add one-side modified entries to the other side's entries
    Set<SimpleDiffEntry> modifiedOurs =
        oursDiffEntries.stream()
            .filter(
                simpleDiffEntry ->
                    simpleDiffEntry.getChangeType().equals(DiffEntry.ChangeType.MODIFY))
            .collect(Collectors.toSet());
    Set<SimpleDiffEntry> modifiedTheirs =
        theirsDiffEntries.stream()
            .filter(
                simpleDiffEntry ->
                    simpleDiffEntry.getChangeType().equals(DiffEntry.ChangeType.MODIFY))
            .collect(Collectors.toSet());
    //      List<SimpleDiffEntry> baseDiffEntries = modifiedOurs;
    //      theirsDiffEntries.removeAll(baseDiffEntries);
    //      baseDiffEntries.addAll(theirsDiffEntries);
    oursDiffEntries.addAll(modifiedTheirs);
    theirsDiffEntries.addAll(modifiedOurs);
    mergeScenario.oursDiffEntries = new ArrayList<>(oursDiffEntries);
    mergeScenario.theirsDiffEntries = new ArrayList<>(theirsDiffEntries);

    Set<SimpleDiffEntry> union = new HashSet<>();
    union.addAll(mergeScenario.oursDiffEntries);
    union.addAll(mergeScenario.theirsDiffEntries);
    mergeScenario.baseDiffEntries = new ArrayList<>(union);

    Set<SimpleDiffEntry> intersection = new HashSet<>();
    intersection.addAll(mergeScenario.oursDiffEntries);
    intersection.retainAll(mergeScenario.theirsDiffEntries);
    mergeScenario.bothModifiedEntries = new ArrayList<>(intersection);
  }

  /**
   * Scan the whole project to index all source files at a specific commit
   *
   * @param commitID
   * @return
   * @throws Exception
   * @deprecated the checkout way is replaced by ObjectLoader
   */
  public ArrayList<SourceFile> scanJavaFiles(String targetDir, String commitID) throws Exception {
    GitService.checkout(repository, commitID);
    ArrayList<SourceFile> temp = new ArrayList<>();
    ArrayList<SourceFile> javaSourceFiles =
        Utils.scanJavaSourceFiles(targetDir, temp, mergeScenario.repoPath);
    return javaSourceFiles;
  }

  /**
   * Copy diff java files and imported java files to the given path, for later process
   *
   * <p>Without * checking out the repo (faster)</>
   *
   * @param diffEntries
   * @param sideCollectedFilePath
   * @param sideCommitID
   * @throws Exception
   */
  private void collect2(
      List<SimpleDiffEntry> diffEntries, String sideCollectedFilePath, String sideCommitID)
      throws Exception {
    // generate file relative paths
    List<String> diffFilePaths = new ArrayList<>();
    List<String> importedFilePaths = new ArrayList<>();
    for (SimpleDiffEntry diffEntry : diffEntries) {
      // relative path known to git
      String relativePath = "";
      if (diffEntry.getChangeType().equals(DiffEntry.ChangeType.ADD)) {
        relativePath = diffEntry.getNewPath();
      } else {
        // e.g. src/main/java/edu/pku/intellimerge/core/GraphBuilder.java
        relativePath = diffEntry.getOldPath();
      }
      diffFilePaths.add(relativePath);
    }
    // get file content with ObjectLoader
    try (RevWalk revWalk = new RevWalk(repository)) {
      ObjectId commitObj = repository.resolve(sideCommitID);
      RevCommit commit = revWalk.parseCommit(commitObj);
      // get the content of changed file before and after changing, return file paths imported by
      // them
      importedFilePaths = generateFiles(diffFilePaths, commit, sideCollectedFilePath);
      if (copyImportedFiles && !importedFilePaths.isEmpty()) {
        generateFiles(importedFilePaths, commit, sideCollectedFilePath);
      }
    }
  }

  /**
   * Generate files to be analyzed
   *
   * @param filePaths
   * @param commit
   * @param sideCollectedFilePath
   * @return
   * @throws Exception
   */
  private List<String> generateFiles(
      List<String> filePaths, RevCommit commit, String sideCollectedFilePath) throws Exception {
    List<String> importedFilePaths = new ArrayList<>();
    if (!filePaths.isEmpty()) {
      try (TreeWalk treeWalk = new TreeWalk(repository)) {
        RevTree parentTree = commit.getTree();
        treeWalk.addTree(parentTree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
          String pathString = treeWalk.getPathString();
          for (String path : filePaths) {
            // to tolerate import file paths, which is relative to source folder instead of repo
            // root
            if (pathString.endsWith(Utils.formatPathSeparator(path))) {
              ObjectId objectId = treeWalk.getObjectId(0);
              ObjectLoader loader = repository.open(objectId);
              // write content to file
              StringWriter writer = new StringWriter();
              IOUtils.copy(loader.openStream(), writer, Charset.defaultCharset());
              Utils.writeContent(
                  sideCollectedFilePath + File.separator + pathString, writer.toString(), false);
              // collect imported files
              if (copyImportedFiles) {
                String[] lines = writer.toString().split("\n");
                for (String line : lines) {
                  if (line.startsWith("import")) {
                    String importedFileRelativePath =
                        line.replace("import ", "")
                            .replace("static", "")
                            .replace(".", File.separator)
                            .replace(";", ".java")
                            .trim();
                    importedFilePaths.add(importedFileRelativePath);
                  }
                }
              }
              break;
            }
          }
        }
      }
    }
    return importedFilePaths;
  }

  /**
   * Copy diff java files and imported java files to the given path, for later process By checking
   * out the repo and copy files
   *
   * @param diffEntries
   * @throws Exception
   * @deprecated
   */
  private void collect(
      List<SourceFile> sourceFiles, List<SimpleDiffEntry> diffEntries, String sideCollectedFilePath)
      throws Exception {

    for (SimpleDiffEntry diffEntry : diffEntries) {
      // only care about BothSides modified files now
      if (diffEntry.getChangeType().equals(DiffEntry.ChangeType.MODIFY)) {
        // src/main/java/edu/pku/intellimerge/core/GraphBuilder.java
        String relativePath = diffEntry.getOldPath();
        //        logger.info(
        //            "{} : {} -> {}",
        //            diffEntry.getChangeType(),
        //            diffEntry.getOldPath(),
        //            diffEntry.getNewPath());
        File srcFile = new File(mergeScenario.repoPath + File.separator + relativePath);
        // copy the diff files
        if (srcFile.exists()) {
          File dstFile = new File(sideCollectedFilePath + relativePath);

          FileUtils.copyFile(srcFile, dstFile);
          //          logger.info("Copying diff file: {} ...", srcFile.getName());

          // copy the imported files
          if (copyImportedFiles) {
            CompilationUnit cu = JavaParser.parse(dstFile);
            for (ImportDeclaration importDeclaration : cu.getImports()) {
              String importedFileRelativePath =
                  importDeclaration
                      .getNameAsString()
                      .replace("import ", "")
                      .replace(";", "")
                      .trim();
              for (SourceFile sourceFile : sourceFiles) {
                // Check if the file has been copied, to avoid duplicate IO
                if (sourceFile.getQualifiedName().equals(importedFileRelativePath)
                    && !sourceFile.isCopied) {
                  srcFile = new File(sourceFile.getAbsolutePath());
                  dstFile = new File(sideCollectedFilePath + sourceFile.getRelativePath());
                  FileUtils.copyFile(srcFile, dstFile);
                  sourceFile.isCopied = true;
                  //                  logger.info("Copying imported file: {} ...",
                  // srcFile.getName());
                }
              }
            }
          }
        } else {
          logger.error("{} not exists", relativePath);
        }
      }
    }
  }
}
