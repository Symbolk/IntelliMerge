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
  private String collectedFilePath;
  private MergeScenario mergeScenario;

  public SourceFileCollector(
      MergeScenario mergeScenario, Repository repository, String collectedFilePath) {
    this.mergeScenario = mergeScenario;
    this.repository = repository;
    this.collectedFilePath = collectedFilePath;
  }

  /** Collect related source files to process together */
  public void collectFilesForAllSides() {
    try {
      Triple<List<SimpleDiffEntry>, List<SimpleDiffEntry>, List<SimpleDiffEntry>>
          threewayDiffEntries = getDiffJavaFiles();
      collectFilesForOneSide(Side.OURS, threewayDiffEntries.getLeft());
      collectFilesForOneSide(Side.BASE, threewayDiffEntries.getMiddle());
      collectFilesForOneSide(Side.THEIRS, threewayDiffEntries.getRight());
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

      ArrayList<SourceFile> javaSourceFiles = scanJavaFiles(sideCommitID);
      String sideCollectedFilePath = collectedFilePath + side.toString().toLowerCase() + "/";
      if (diffEntries != null) {
        collect(javaSourceFiles, diffEntries, sideCollectedFilePath);
      }
    }
  }

  /**
   * Get diff java files between base and ours/theirs commit
   *
   * @return
   * @throws Exception
   */
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

  /**
   * Scan the whole project to index all source files
   *
   * @param commitID
   * @return
   * @throws Exception
   */
  public ArrayList<SourceFile> scanJavaFiles(String commitID) throws Exception {
    GitService.checkout(repository, commitID);
    ArrayList<SourceFile> temp = new ArrayList<>();
    //        String targetFolder=repoPath.endsWith("/")? repoPath + srcPath:repoPath+"/"+srcPath;
    String targetFolder = mergeScenario.repoPath + mergeScenario.srcPath;
    ArrayList<SourceFile> javaSourceFiles =
        FilesManager.scanJavaSourceFiles(targetFolder, temp, mergeScenario.repoPath);
    return javaSourceFiles;
  }

  /**
   * Copy diff java files and imported java files to the given path, for later process
   *
   * @param diffEntries
   * @throws Exception
   */
  private void collect(
      List<SourceFile> sourceFiles, List<SimpleDiffEntry> diffEntries, String sideCollectedFilePath)
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
        File srcFile = new File(mergeScenario.repoPath + "/" + relativePath);
        // copy the diff files
        if (srcFile.exists()) {
          File dstFile = new File(sideCollectedFilePath + relativePath);

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
                dstFile = new File(sideCollectedFilePath + sourceFile.getRelativePath());
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
