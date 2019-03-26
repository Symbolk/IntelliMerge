package edu.pku.intellimerge.evaluation;

import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
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
  private static final String STATISTICS_PATH = "D:\\github\\merges\\javaparser\\statistics.csv";
  private static final String DOT_DIR = "C:\\Users\\Name\\Desktop\\GraphData\\";

  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("log4j.properties");

    //    String mergeCommit = "90415702d623180f30e52e3d9426d3ef10b98276";
    //    String sourceDir = "D:\\github\\merges\\" + REPO_NAME + File.separator + mergeCommit;
    //
    //    String targetDir = "D:\\github\\test2";
    //    List<String> relativePaths = new ArrayList<>();
    //
    //    relativePaths.add(
    //
    // "javaparser-core/src/main/java/com/github/javaparser/ast/body/MethodDeclaration.java");
    //    Utils.copyAllVersions(sourceDir, relativePaths, targetDir);

    APIClient apiClient =
        new APIClient(
            REPO_NAME,
            REPO_DIR,
            GIT_URL,
            SRC_DIR,
            DIFF_DIR,
            MERGE_RESULT_DIR,
            STATISTICS_PATH,
            DOT_DIR);
    String sourceDir = "D:\\github\\test2";
    String mergeResultDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
    List<Long> runtimes = apiClient.processDirectory(sourceDir, mergeResultDir, false);
    Utils.removeAllComments(mergeResultDir);
  }
}
