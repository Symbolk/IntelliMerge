package edu.pku.intellimerge.evaluation;

import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.FilesManager;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible to evaluate the result and the performance of IntelliMerge Comparing with
 * git-merge & jFSTMerge
 */
public class Evaluator {
  private static final Logger logger = LoggerFactory.getLogger(Evaluator.class);

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

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");
    //      BasicConfigurator.configure();
    try {
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
//      String sourceDir = "D:\\github\\merges\\javaparser\\0ccca235068397ea4b045025034a488e78b83863";
      String sourceDir = "D:\\github\\test";
//            String sourceDir = "D:\\github\\test";
      String mergeResultDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
      String manualMergedDir = sourceDir + File.separator + Side.MANUAL.asString() + File.separator;
      apiClient.processDirectory(sourceDir, mergeResultDir);

//      String targetDir = "D:\\github\\test";
//      List<String> relativePaths = new ArrayList<>();
//      relativePaths.add("javaparser-core\\src\\main\\java\\com\\github\\javaparser\\ast\\body\\FieldDeclaration.java");
//      relativePaths.add("javaparser-core\\src\\main\\java\\com\\github\\javaparser\\ast\\type\\ClassOrInterfaceType.java");
//      //      FilesManager.formatManualMergedResults(manualMergedDir);
//      FilesManager.copyAllVersions(sourceDir, relativePaths, targetDir);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
