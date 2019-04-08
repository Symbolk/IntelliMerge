package edu.pku.intellimerge.evaluation;

import br.ufpe.cin.app.JFSTMerge;
import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Class responsbile to collect and debug with a single file */
public class SingleFileTester {
  private static final String REPO_NAME = "junit4";
  private static final String REPO_DIR = "D:\\github\\repos\\" + REPO_NAME;
  private static final String GIT_URL = "https://github.com/javaparser/javaparser.git";
  private static final String SRC_DIR =
      "/javaparser-core/src/main/java/"; // java project source folder
  //  private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";
  private static final String DIFF_DIR = "D:\\github\\diffs\\" + REPO_NAME;
  private static final String MERGE_RESULT_DIR = "D:\\github\\merges\\" + REPO_NAME;

  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("log4j.properties");

    String mergeCommit = "f4682ce2558cdca60d12fbef39e9ca0370eba592";
    String sourceDir = "D:\\github\\ref_conflicts\\" + REPO_NAME + File.separator + mergeCommit;
    String intelliMergedDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;

    String jfstMergedDir = sourceDir + File.separator + Side.JFST.asString() + File.separator;
    String manualMergedDir = sourceDir + File.separator + Side.MANUAL.asString() + "_Formatted" + File.separator;

    String targetDir = "D:\\github\\test";
    List<String> relativePaths = new ArrayList<>();

    // Test Copying
    relativePaths.add(
        "src/test/java/org/junit/tests/running/classes/TestClassTest.java");
//                Utils.copyAllVersions(sourceDir, relativePaths, targetDir);
    //    Utils.removeAllComments(intelliMergedDir);
    //    Utils.removeAllComments(manualMergedDir);
    //    Utils.removeAllComments(jfstMergedDir);

    // Test Merging
    String dirToMerge = "D:\\github\\test";
    mergeWithIntelli(dirToMerge);
//    mergeWithJFST(dirToMerge);

    // Test Extracting
    //    String dirToExtractConflicts =
    //
    // "D:\\github\\ref_conflicts\\error-prone\\1a87c33bd18648f484133794840e94bd8d1d4a64\\gitMerged\\core\\src\\test\\resources\\com\\google\\errorprone\\bugpatterns";
    //    Pair<Integer, List<Document>> mergeConflicts =
    //        Evaluator.extractMergeConflicts(dirToExtractConflicts, true);
    //    System.out.println(mergeConflicts.getLeft());
    //    for (Document document : mergeConflicts.getRight()) {
    //      System.out.println(document);
    //    }
  }

  private static void mergeWithIntelli(String sourceDir) throws Exception {
    APIClient apiClient =
        new APIClient(REPO_NAME, REPO_DIR, GIT_URL, SRC_DIR, DIFF_DIR, MERGE_RESULT_DIR, false);

    String mergeResultDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
    String manualMergedDir = sourceDir + File.separator + Side.MANUAL.asString() + File.separator;
    List<Long> runtimes = apiClient.processDirectory(sourceDir, mergeResultDir);
//    Utils.removeAllComments(mergeResultDir);
//    Utils.formatAllJavaFiles(manualMergedDir);
  }

  private static void mergeWithJFST(String sourceDir) {
    String jfstMergedDir = sourceDir + File.separator + Side.JFST.asString() + File.separator;
    JFSTMerge jfstMerge = new JFSTMerge();
    jfstMerge.mergeDirectories(
        sourceDir + File.separator + Side.OURS.asString(),
        sourceDir + File.separator + Side.BASE.asString(),
        sourceDir + File.separator + Side.THEIRS.asString(),
        jfstMergedDir);
  }
}
