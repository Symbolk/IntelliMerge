package edu.pku.intellimerge.evaluation;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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
      MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
      MongoClient mongoClient = new MongoClient(connectionString);
      MongoDatabase database = mongoClient.getDatabase("diffresult");
      MongoCollection<Document> collection = database.getCollection(REPO_NAME);

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
      String sourceDir = "D:\\github\\merges\\javaparser\\7fd7c83851fb87d727c220043bac0e4e81632182";
      //      String sourceDir = "D:\\github\\test";
      String mergeResultDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
      String manualMergedDir = sourceDir + File.separator + Side.MANUAL.asString() + File.separator;
      // 1. merge to get our results
      //      apiClient.processDirectory(sourceDir, mergeResultDir, true);
      // 2. format the manual results
      Utils.formatManualMergedResults(manualMergedDir);
      // 3. compute diff with git-diff
      // for each file in the manual results, find and diff with the corresponding intelli result

      // 4. parse diff output and save in the mongodb

      Document doc =
          new Document("repo_name", "javaparser")
              .append("file_path", "database")
              .append("loc", 1)
              .append("same_loc", 1)
              .append("diff_blocks", new Document("x", 203).append("y", 102));
      collection.insertOne(doc);

      //      String targetDir = "D:\\github\\test";
      //      List<String> relativePaths = new ArrayList<>();
      //
      // relativePaths.add("javaparser-core\\src\\main\\java\\com\\github\\javaparser\\ast\\Modifier.java");
      //      Utils.copyAllVersions(sourceDir, relativePaths, targetDir);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
