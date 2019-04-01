package edu.pku.intellimerge.evaluation;

import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Class responsbile to collect and debug with a single file */
public class SingleFileTester {
  private static final String REPO_NAME = "javaparser";
  private static final String REPO_DIR = "D:\\github\\repos\\" + REPO_NAME;
  private static final String GIT_URL = "https://github.com/javaparser/javaparser.git";
  private static final String SRC_DIR =
      "/javaparser-core/src/main/java/"; // java project source folder
  //  private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";
  private static final String DIFF_DIR = "D:\\github\\diffs\\" + REPO_NAME;
  private static final String MERGE_RESULT_DIR = "D:\\github\\merges\\" + REPO_NAME;

  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("log4j.properties");

//    String mergeCommit = "7fd7c83851fb87d727c220043bac0e4e81632182";
//    String sourceDir = "D:\\github\\ref_conflicts\\" + REPO_NAME + File.separator + mergeCommit;
//    String intelliMergedDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
//
//    String jfstMergedDir = sourceDir + File.separator + Side.JFST.asString() + File.separator;
//    String manualMergedDir = sourceDir + File.separator + Side.MANUAL.asString() + File.separator;
//
//    String targetDir = "D:\\github\\test2";
//    List<String> relativePaths = new ArrayList<>();
//
//    relativePaths.add(
//        "javaparser-core/src/main/java/com/github/javaparser/ast/body/ConstructorDeclaration.java");
//    Utils.copyAllVersions(sourceDir, relativePaths, targetDir);
//    Utils.removeAllComments(intelliMergedDir);
//    Utils.removeAllComments(manualMergedDir);
//    Utils.removeAllComments(jfstMergedDir);


        APIClient apiClient =
            new APIClient(REPO_NAME, REPO_DIR, GIT_URL, SRC_DIR, DIFF_DIR, MERGE_RESULT_DIR,
     false);
        String sourceDir = "D:\\github\\test2";
        String mergeResultDir = sourceDir + File.separator + Side.INTELLI.asString() +
     File.separator;
        String manualMergedDir = sourceDir + File.separator + Side.MANUAL.asString() +
     File.separator;
        List<Long> runtimes = apiClient.processDirectory(sourceDir, mergeResultDir);
//        Utils.removeAllComments(mergeResultDir);
    //    Utils.formatAllJavaFiles(manualMergedDir);
    //    String jfstMergedDir = sourceDir + File.separator + Side.JFST.asString() + File.separator;
    //    JFSTMerge jfstMerge = new JFSTMerge();
    //    jfstMerge.mergeDirectories(
    //        sourceDir + File.separator + Side.OURS.asString(),
    //        sourceDir + File.separator + Side.BASE.asString(),
    //        sourceDir + File.separator + Side.THEIRS.asString(),
    //        jfstMergedDir);
    //    String path = "D:\\github\\test\\gitMerged\\Context.java";
    //
    //    List<ConflictBlock> conflictBlocks = Utils.extractConflictBlocksDiff3(path, true);
    //
    //    for (ConflictBlock conflictBlock : conflictBlocks) {
    //      System.out.println(conflictBlock.getLeft());
    //    }
  }
}
