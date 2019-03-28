package edu.pku.intellimerge.evaluation;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBCalculator {
  private static final Logger logger = LoggerFactory.getLogger(DBCalculator.class);

  private static final String REPO_NAME = "javaparser";

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");

    MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
    MongoClient mongoClient = new MongoClient(connectionString);
    MongoDatabase intelliDB = mongoClient.getDatabase("IntelliVSManual");
    MongoDatabase gitDB = mongoClient.getDatabase("GitVSManual");
    MongoDatabase jfstDB = mongoClient.getDatabase("jFSTVSManual");
    MongoCollection<Document> intelliDBCollection = intelliDB.getCollection(REPO_NAME);
    MongoCollection<Document> gitDBCollection = gitDB.getCollection(REPO_NAME);
    MongoCollection<Document> jfstDBCollection = jfstDB.getCollection(REPO_NAME);
    // calculate average precision for the three tools
    System.out.println("IntelliMerge:\t" + calculateMeanPrecision(intelliDBCollection) + "%");
    System.out.println("GitMerge:\t" + calculateMeanPrecision(gitDBCollection) + "%");
    System.out.println("JFSTMergeg:\t" + calculateMeanPrecision(jfstDBCollection) + "%");
  }

  /**
   * Calculate mean precision of all merge scenarios
   *
   * @param collection
   * @return
   */
  private static double calculateMeanPrecision(MongoCollection<Document> collection) {
    MongoCursor<Document> cursor = collection.find().iterator();

    double avg_precision = 0.0;
    int count = 0;
    try {
      while (cursor.hasNext()) {
        avg_precision += (double) cursor.next().get("auto_merged_precision");
        count += 1;
      }
    } finally {
      cursor.close();
    }
    avg_precision = (avg_precision / count) * 100;
    return avg_precision;
  }
}
