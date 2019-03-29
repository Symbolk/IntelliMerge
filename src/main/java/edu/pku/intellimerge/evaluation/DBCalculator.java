package edu.pku.intellimerge.evaluation;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class responsible to calculate statistics from the database */
public class DBCalculator {
  private static final Logger logger = LoggerFactory.getLogger(DBCalculator.class);

  private static final String REPO_NAME = "javaparser";

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");

    MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
    MongoClient mongoClient = new MongoClient(connectionString);
    MongoDatabase intelliDB = mongoClient.getDatabase("IntelliVSManual");
    MongoDatabase gitDB = mongoClient.getDatabase("GitVSManual");
    MongoDatabase jfstDB = mongoClient.getDatabase("JFSTVSManual");
    MongoCollection<Document> intelliDBCollection = intelliDB.getCollection(REPO_NAME);
    MongoCollection<Document> gitDBCollection = gitDB.getCollection(REPO_NAME);
    MongoCollection<Document> jfstDBCollection = jfstDB.getCollection(REPO_NAME);
    // calculate average precision for the three tools
    Pair<Double, Double> pAndR = calculateForRepo(intelliDBCollection);
    System.out.println(
        String.format(
            "%-20s %-40s %s",
            "IntelliMerge",
            "Precision: " + pAndR.getLeft() + "%",
            "Recall: " + pAndR.getRight() + "%"));
    pAndR = calculateForRepo(gitDBCollection);
    System.out.println(
        String.format(
            "%-20s %-40s %s",
            "GitMerge",
            "Precision: " + pAndR.getLeft() + "%",
            "Recall: " + pAndR.getRight() + "%"));
    pAndR = calculateForRepo(jfstDBCollection);
    System.out.println(
        String.format(
            "%-20s %-40s %s",
            "JFSTMerge",
            "Precision: " + pAndR.getLeft() + "%",
            "Recall: " + pAndR.getRight() + "%"));
  }

  /**
   * Calculate the precision and the recall for one repo
   *
   * @param collection
   * @return
   */
  private static Pair<Double, Double> calculateForRepo(MongoCollection<Document> collection) {
    MongoCursor<Document> cursor = collection.find().iterator();

    Double repoPrecision = 0.0;
    Double repoRecall = 0.0;
    Integer autoMergeLOC = 0;
    Integer manualMergeLOC = 0;
    Integer sameLOC = 0;
    try {
      while (cursor.hasNext()) {
        Document doc = cursor.next();
        autoMergeLOC += (Integer) doc.get("auto_merge_loc");
        manualMergeLOC += (Integer) doc.get("manual_merge_loc");
        sameLOC += (Integer) doc.get("same_with_manual_loc");
      }
    } finally {
      cursor.close();
    }
    if (autoMergeLOC > 0) {
      repoPrecision = sameLOC / autoMergeLOC.doubleValue();
    } else {
      repoPrecision = 1.0;
    }
    if (manualMergeLOC > 0) {
      repoRecall = sameLOC / manualMergeLOC.doubleValue();
    } else {
      repoRecall = 1.0;
    }
    return Pair.of(repoPrecision * 100, repoRecall * 100);
  }
}
