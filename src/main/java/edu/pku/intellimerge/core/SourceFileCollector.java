package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SimpleDiffEntry;
import edu.pku.intellimerge.model.SourceFile;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.FilesManager;
import edu.pku.intellimerge.util.GitService;
import org.apache.commons.io.FileUtils;
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

  private static final Logger logger = LoggerFactory.getLogger(SourceFileCollector.class);

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
      getDiffJavaFiles();
//      collectFilesForOneSide(Side.OURS, mergeScenario.oursDiffEntries);
//      collectFilesForOneSide(Side.BASE, mergeScenario.baseDiffEntries);
//      collectFilesForOneSide(Side.THEIRS, mergeScenario.theirsDiffEntries);
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
      String sideCollectedFilePath =
          collectedFilePath + side.toString().toLowerCase() + File.separator;
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
  public void getDiffJavaFiles() throws Exception {
    mergeScenario.oursDiffEntries =
        GitService.listDiffFilesJava(
            repository, mergeScenario.baseCommitID, mergeScenario.oursCommitID);
    mergeScenario.theirsDiffEntries =
        GitService.listDiffFilesJava(
            repository, mergeScenario.baseCommitID, mergeScenario.theirsCommitID);
    // 2 ways to union the diff file list
    //      List<SimpleDiffEntry> baseDiffEntries = oursDiffEntries;
    //      theirsDiffEntries.removeAll(baseDiffEntries);
    //      baseDiffEntries.addAll(theirsDiffEntries);
    Set<SimpleDiffEntry> temp = new HashSet<>();
    temp.addAll(mergeScenario.oursDiffEntries);
    temp.addAll(mergeScenario.theirsDiffEntries);
    mergeScenario.baseDiffEntries = new ArrayList<>(temp);
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
      // TODO change type may should be ignored, since they may be imprecise
      if (diffEntry.getChangeType().equals(DiffEntry.ChangeType.MODIFY)) {
        // src/main/java/edu/pku/intellimerge/core/SemanticGraphBuilder.java
        String relativePath = diffEntry.getNewPath();
        logger.info(
            "{} : {} -> {}",
            diffEntry.getChangeType(),
            diffEntry.getOldPath(),
            diffEntry.getNewPath());
        File srcFile = new File(mergeScenario.repoPath + File.separator + relativePath);
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
