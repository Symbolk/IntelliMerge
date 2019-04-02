package edu.pku.intellimerge.evaluation;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import edu.pku.intellimerge.util.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Class responsible to calculate statistics from the database */
public class Statistics {
  private static final Logger logger = LoggerFactory.getLogger(Statistics.class);

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");
    MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
    MongoClient mongoClient = new MongoClient(connectionString);
    MongoDatabase intelliDB = mongoClient.getDatabase("IntelliVSManual");
    MongoDatabase gitDB = mongoClient.getDatabase("GitVSManual");
    MongoDatabase jfstDB = mongoClient.getDatabase("JFSTVSManual");

    String runtimeCSVPath = "F:\\workspace\\dev\\refactoring-analysis-results\\stats\\runtimes.csv";
    File file = new File(runtimeCSVPath);
    if (file.exists()) {
      file.delete();
      Utils.writeContent(runtimeCSVPath, "merge_tool;repo_name;merge_commit;runtime\n", false);
    }

    List<String> repoNames = new ArrayList<>();
//    repoNames.add("javaparser");
//    repoNames.add("gradle");
//    repoNames.add("junit4");
    repoNames.add("fastjson");
    repoNames.add("error-prone");
    repoNames.add("antlr4");
    repoNames.add("deeplearning4j");
    for (String repoName : repoNames) {
      // one collection stands for one examined repo
      MongoCollection<Document> intelliDBCollection = intelliDB.getCollection(repoName);
      MongoCollection<Document> gitDBCollection = gitDB.getCollection(repoName);
      MongoCollection<Document> jfstDBCollection = jfstDB.getCollection(repoName);

      // calculate average precision for the three tools
      Pair<Double, Double> pAndR = calculatePAndRForRepo(intelliDBCollection);
      Long runtime = calculateRuntimeForRepo(intelliDBCollection);
      Integer numConflicts = calculateNumConflictsForRepo(intelliDBCollection);
      collectRuntimeIntoCSV(runtimeCSVPath, intelliDBCollection, "IntelliMerge");
      System.out.println(repoName);
      System.out.println(
          String.format(
              "%-20s %-40s %-40s %-40s %s",
              "IntelliMerge",
              "Precision: " + pAndR.getLeft() + "%",
              "Recall: " + pAndR.getRight() + "%",
              "Conflicts Num: " + numConflicts,
              "Runtime: " + runtime + "ms"));

      pAndR = calculatePAndRForRepo(jfstDBCollection);
      runtime = calculateRuntimeForRepo(jfstDBCollection);
      numConflicts = calculateNumConflictsForRepo(jfstDBCollection);
      collectRuntimeIntoCSV(runtimeCSVPath, jfstDBCollection, "JFSTMerge");

      System.out.println(
          String.format(
              "%-20s %-40s %-40s %-40s %s",
              "JFSTMerge",
              "Precision: " + pAndR.getLeft() + "%",
              "Recall: " + pAndR.getRight() + "%",
              "Conflicts Num: " + numConflicts,
              "Runtime: " + runtime + "ms"));

      pAndR = calculatePAndRForRepo(gitDBCollection);
      numConflicts = calculateNumConflictsForRepo(gitDBCollection);
      System.out.println(
          String.format(
              "%-20s %-40s %-40s %s",
              "GitMerge",
              "Precision: " + pAndR.getLeft() + "%",
              "Recall: " + pAndR.getRight() + "%",
              "Conflicts Num: " + numConflicts));
      System.out.println();
    }
  }

  /**
   * Calculate the precision and the recall for one repo
   *
   * @param collection
   * @return
   */
  private static Pair<Double, Double> calculatePAndRForRepo(MongoCollection<Document> collection) {
    MongoCursor<Document> cursor = collection.find().iterator();

    Double repoPrecision = 0.0;
    Double repoRecall = 0.0;
    Integer autoMergeLOC = 0;
    Integer manualMergeLOC = 0;
    Integer sameLOC1 = 0;
    Integer sameLOC2 = 0;
    try {
      while (cursor.hasNext()) {
        Document doc = cursor.next();
        autoMergeLOC += (Integer) doc.get("auto_merge_loc");
        manualMergeLOC += (Integer) doc.get("manual_merge_loc");
        sameLOC1 += (Integer) doc.get("correct_loc_in_auto_merged");
        sameLOC2 += (Integer) doc.get("correct_loc_in_manual");
      }
    } finally {
      cursor.close();
    }
    if (autoMergeLOC > 0) {
      repoPrecision = sameLOC1 / autoMergeLOC.doubleValue();
    } else {
      repoPrecision = 0.0;
    }
    if (manualMergeLOC > 0) {
      repoRecall = sameLOC2 / manualMergeLOC.doubleValue();
    } else {
      repoRecall = 0.0;
    }
    return Pair.of(repoPrecision * 100, repoRecall * 100);
  }

  /**
   * Calculate the mean runtime for all merge scenarios in the repo
   *
   * @param collection
   * @return
   */
  private static Long calculateRuntimeForRepo(MongoCollection<Document> collection) {
    MongoCursor<Document> cursor = collection.find().iterator();
    Long runtime = 0L;
    int count = 0;
    try {
      while (cursor.hasNext()) {
        Document doc = cursor.next();
        runtime += (Long) doc.get("time_overall");
        count++;
      }
    } finally {
      cursor.close();
    }
    return count > 0 ? runtime / count : 0;
  }

  /**
   * Calculate the sum number of conflicts for all merge scenarios in the repo
   *
   * @param collection
   * @return
   */
  private static Integer calculateNumConflictsForRepo(MongoCollection<Document> collection) {
    MongoCursor<Document> cursor = collection.find().iterator();
    Integer sum = 0;
    try {
      while (cursor.hasNext()) {
        Document doc = cursor.next();
        sum += (Integer) doc.get("conflicts_num");
      }
    } finally {
      cursor.close();
    }
    return sum;
  }

  /** Collect runtimes for all merge scenarios in all projects into one csv file */
  private static void collectRuntimeIntoCSV(
      String filePath, MongoCollection<Document> collection, String mergeTool) {
    MongoCursor<Document> cursor = collection.find().iterator();
    // repo_name;merge_commit;time_overall;merge_tool
    List<String> lines = new ArrayList<>();
    try {
      while (cursor.hasNext()) {
        List<String> line = new ArrayList<>();
        Document doc = cursor.next();
        line.add(mergeTool);
        line.add(doc.getString("repo_name"));
        line.add(doc.getString("merge_commit"));
        //        Double timeInSeconds = doc.getLong("time_overall")/1000.0;
        Long timeInMiliSeconds = doc.getLong("time_overall");
        line.add(String.valueOf(timeInMiliSeconds));
        lines.add(line.stream().collect(Collectors.joining(";")));
      }
    } finally {
      cursor.close();
    }
    String lineString = lines.stream().collect(Collectors.joining("\n"));
    Utils.appendContent(filePath, lineString);
  }
}
