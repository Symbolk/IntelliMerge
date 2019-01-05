package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.Side;
import edu.pku.intellimerge.model.SimpleDiffEntry;
import edu.pku.intellimerge.model.SourceFile;
import edu.pku.intellimerge.util.FilesManager;
import edu.pku.intellimerge.util.GitService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SourceFileCollector {

  private static final Logger logger = LoggerFactory.getLogger(APIClient.class);

  private Repository repository;
  private String repoName;
  private String repoPath;
  private String srcPath;
  private String diffPath;
  private MergeScenario mergeScenario;

  public SourceFileCollector(
      Repository repository,
      String repoName,
      String repoPath,
      String srcPath,
      String diffPath,
      MergeScenario mergeScenario) {
    this.repository = repository;
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.srcPath = srcPath;
    this.diffPath = diffPath;
    this.mergeScenario = mergeScenario;
  }

  public void collect() {
    try {
      Triple<List<SimpleDiffEntry>, List<SimpleDiffEntry>, List<SimpleDiffEntry>>
          threewayDiffEntries = getDiffJavaFiles();
      collectForOneSide(Side.OURS, threewayDiffEntries.getLeft());
      collectForOneSide(Side.BASE, threewayDiffEntries.getMiddle());
      collectForOneSide(Side.THEIRS, threewayDiffEntries.getRight());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void collectForOneSide(Side side, List<SimpleDiffEntry> diffEntries) throws Exception {
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

      ArrayList<SourceFile> javaSourceFiles = scanJavaFiles(sideCommitID);
      String sideDiffPath = diffPath + side.toString().toLowerCase() + "/";
      if (diffEntries != null) {
        copyFilesToParse(javaSourceFiles, diffEntries, sideDiffPath);
      }
    }
  }

  public Triple<List<SimpleDiffEntry>, List<SimpleDiffEntry>, List<SimpleDiffEntry>>
      getDiffJavaFiles() throws Exception {
    List<SimpleDiffEntry> oursDiffEntries =
        GitService.listDiffFilesJava(
            repository, mergeScenario.baseCommitID, mergeScenario.oursCommitID);
    List<SimpleDiffEntry> theirsDiffEntries =
        GitService.listDiffFilesJava(
            repository, mergeScenario.baseCommitID, mergeScenario.theirsCommitID);
    // 2 ways to union the diff file list
    //      List<SimpleDiffEntry> baseDiffEntries = oursDiffEntries;
    //      theirsDiffEntries.removeAll(baseDiffEntries);
    //      baseDiffEntries.addAll(theirsDiffEntries);
    Set<SimpleDiffEntry> temp = new HashSet<>();
    temp.addAll(oursDiffEntries);
    temp.addAll(theirsDiffEntries);
    List<SimpleDiffEntry> baseDiffEntries = new ArrayList<>(temp);
    return Triple.of(oursDiffEntries, baseDiffEntries, theirsDiffEntries);
  }

  public ArrayList<SourceFile> scanJavaFiles(String commitID) throws Exception {
    GitService.checkout(repository, commitID);
    ArrayList<SourceFile> temp = new ArrayList<>();
    //        String targetFolder=repoPath.endsWith("/")? repoPath + srcPath:repoPath+"/"+srcPath;
    String targetFolder = repoPath + srcPath;
    ArrayList<SourceFile> javaSourceFiles =
        FilesManager.scanJavaSourceFiles(targetFolder, temp, repoPath);
    return javaSourceFiles;
  }
  /**
   * Copy diff java files and imported java files to the diff path, to parse later
   *
   * @param diffEntries
   * @throws Exception
   */
  private void copyFilesToParse(
      List<SourceFile> sourceFiles, List<SimpleDiffEntry> diffEntries, String diffPath)
      throws Exception {

    for (SimpleDiffEntry diffEntry : diffEntries) {
      if (diffEntry.getChangeType().equals(DiffEntry.ChangeType.MODIFY)) {
        // src/main/java/edu/pku/intellimerge/core/SemanticGraphBuilder.java
        String relativePath = diffEntry.getNewPath();
        logger.info(
            "{} : {} -> {}",
            diffEntry.getChangeType(),
            diffEntry.getOldPath(),
            diffEntry.getNewPath());
        File srcFile = new File(repoPath + "/" + relativePath);
        // copy the diff files
        if (srcFile.exists()) {
          File dstFile = new File(diffPath + relativePath);

          FileUtils.copyFile(srcFile, dstFile);
          logger.info("Copying diff file: {} ...", srcFile.getName());

          // copy the imported files
          CompilationUnit cu = JavaParser.parse(dstFile);
          for (ImportDeclaration importDeclaration : cu.getImports()) {
            String qualifiedName =
                importDeclaration.getNameAsString().trim().replace("import ", "").replace(";", "");
            for (SourceFile sourceFile : sourceFiles) {
              // Check if the file has been copied, to avoid duplicate IO
              if (sourceFile.getQualifiedName().equals(qualifiedName) && !sourceFile.isCopied) {
                srcFile = new File(sourceFile.getAbsolutePath());
                dstFile = new File(diffPath + sourceFile.getRelativePath());
                FileUtils.copyFile(srcFile, dstFile);
                sourceFile.isCopied = true;
                logger.info("Copying imported file: {} ...", srcFile.getName());
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
