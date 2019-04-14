package edu.pku.intellimerge.client;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.pku.intellimerge.model.constant.Side;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final String REPO_NAME = "test";
  private static final String REPO_DIR = "D:\\github\\repos\\" + REPO_NAME;
  private static final String GIT_URL = "https://github.com/javaparser/javaparser.git"; // unused
  private static final String DIFF_DIR =
      "D:\\github\\ref_conflicts\\" + REPO_NAME; // the directory to temporarily save the diff files

  // unused
  private static final String MERGE_RESULT_DIR =
      "D:\\github\\merges\\"
          + REPO_NAME
          + File.separator
          + Side.INTELLI.asString(); // the directory to eventually save the merge results
  private static final String STATISTICS_FILE_PATH =
      "F:\\workspace\\dev\\refactoring-analysis-results\\stats\\merge_scenarios_involved_refactorings_"
          + REPO_NAME
          + ".csv";

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");
    //      BasicConfigurator.configure();
    try {
      MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
      MongoClient mongoClient = new MongoClient(connectionString);
      MongoDatabase intelliDB = mongoClient.getDatabase("IntelliVSManual");
      MongoCollection<Document> intelliDBCollection = intelliDB.getCollection(REPO_NAME);

      APIClient apiClient =
          new APIClient(REPO_NAME, REPO_DIR, GIT_URL, DIFF_DIR, MERGE_RESULT_DIR, true);

      // read merge scenario info from csv, merge and record runtime data in the database
      Set<String> processedMergeCommits = new HashSet<>();

      String mergeCommit = "802330034e7472e6835fa6251ce259a467704be7";
      String parent1 = "parent1";
      String parent2 = "parent2";
      String baseCommit = "merge_base";
      if (!processedMergeCommits.contains(mergeCommit)) {
        // source files to be merged
        String sourceDir = DIFF_DIR + File.separator + mergeCommit;

        // merged files by 3 tools
        String intelliMergedDir =
            sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
        String manualMergedDir =
            sourceDir + File.separator + Side.MANUAL.asString() + File.separator;
        String manualMergedFormattedDir =
            sourceDir + File.separator + Side.MANUAL.asString() + "_Formatted" + File.separator;

        List<Long> runtimes = apiClient.processDirectory(sourceDir, intelliMergedDir);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
