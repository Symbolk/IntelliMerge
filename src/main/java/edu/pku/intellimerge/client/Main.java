package edu.pku.intellimerge.client;

import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.util.GitService;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final String REPO_NAME = "javaparser";
  private static final String REPO_DIR = "D:\\github\\repos\\" + REPO_NAME;
  private static final String GIT_URL = "https://github.com/javaparser/javaparser.git";
  private static final String SRC_DIR =
      "/javaparser-core/src/main/java/"; // java project source folder
  //  private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";
  private static final String DIFF_DIR = "D:\\github\\diffs\\" + REPO_NAME;
  private static final String RESULT_DIR = "D:\\github\\merges\\" + REPO_NAME;
  private static final String STATISTICS_PATH = "D:\\github\\merges\\javaparser\\statistics.csv";
  private static final String DOT_DIR = "C:\\Users\\Name\\Desktop\\GraphData\\";

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");
    //      BasicConfigurator.configure();

    try {
      // process merge scenarios in repository
      Repository repository = GitService.cloneIfNotExists(REPO_DIR, GIT_URL);

      //            for (MergeScenario mergeScenario : generateMergeScenarios()) {
      //              processMergeScenario(mergeScenario, repository);
      //            }
      APIClient apiClient =
          new APIClient(
              REPO_NAME,
                  REPO_DIR,
              GIT_URL,
                  SRC_DIR,
                  DIFF_DIR,
                  RESULT_DIR,
              STATISTICS_PATH,
                  DOT_DIR);
      MergeScenario mergeScenario = apiClient.generateSingleMergeSenario();
      apiClient.processMergeScenario(mergeScenario, repository);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
