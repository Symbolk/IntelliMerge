package edu.pku.intellimerge.evaluation;

import br.ufpe.cin.app.JFSTMerge;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.univocity.parsers.common.record.Record;
import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.model.ConflictBlock;
import edu.pku.intellimerge.model.SourceFile;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
import io.reflectoring.diffparser.api.DiffParser;
import io.reflectoring.diffparser.api.UnifiedDiffParser;
import io.reflectoring.diffparser.api.model.Diff;
import io.reflectoring.diffparser.api.model.Hunk;
import io.reflectoring.diffparser.api.model.Line;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
      MongoDatabase intelliDB = mongoClient.getDatabase("IntelliVSManual");
      MongoDatabase gitDB = mongoClient.getDatabase("GitVSManual");
      MongoDatabase jfstDB = mongoClient.getDatabase("jFSTVSManual");
      MongoCollection<Document> intelliDBCollection = intelliDB.getCollection(REPO_NAME);
      MongoCollection<Document> gitDBCollection = gitDB.getCollection(REPO_NAME);
      MongoCollection<Document> jfstDBCollection = jfstDB.getCollection(REPO_NAME);

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

      // read merge scenario info from csv, merge and record runtime data in the database
      String csvFilePath =
          "F:\\workspace\\dev\\refactoring-analysis-results\\stats\\merge_scenarios_involved_refactorings_173.csv";
      List<Record> records = Utils.readCSVAsRecord(csvFilePath, ";");
      Set<String> processedMergeCommits = new HashSet<>();

      for (Record record : records) {
        String mergeCommit = record.getString("merge_commit");
        String parent1 = record.getString("parent1");
        String parent2 = record.getString("parent2");
        String baseCommit = record.getString("merge_base");
        if (!processedMergeCommits.contains(mergeCommit)) {
          // source files to be merged
          String sourceDir = "D:\\github\\merges\\" + REPO_NAME + File.separator + mergeCommit;

          // merged files by 3 tools
          String intelliMergedDir =
              sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
          String jfstMergedDir = sourceDir + File.separator + Side.JFST.asString() + File.separator;
          String gitMergedDir = sourceDir + File.separator + Side.GIT.asString() + File.separator;
          String manualMergedDir =
              sourceDir + File.separator + Side.MANUAL.asString() + File.separator;
          // 1. merge with IntelliMerge and jFSTMerge
          // runtime for each phase (need to run multiple times and get average)
          List<Long> runtimes = apiClient.processDirectory(sourceDir, intelliMergedDir, true);
          JFSTMerge jfstMerge = new JFSTMerge();
          jfstMerge.mergeDirectories(
              sourceDir + File.separator + Side.OURS.asString(),
              sourceDir + File.separator + Side.BASE.asString(),
              sourceDir + File.separator + Side.THEIRS.asString(),
              jfstMergedDir);

          // 2. remove all comments and format the manual results
          Utils.removeAllComments(intelliMergedDir);
          Utils.removeAllComments(gitMergedDir);
          Utils.removeAllComments(jfstMergedDir);
          Utils.removeAllComments(manualMergedDir);
          Utils.formatAllJavaFiles(manualMergedDir);

          // 3. compare merge results with manual results
          ArrayList<SourceFile> temp = new ArrayList<>();
          ArrayList<SourceFile> manualMergedResults =
              Utils.scanJavaSourceFiles(manualMergedDir, temp, manualMergedDir);

          // 1. Compare IntelliMerge with Manual
          Document scenarioDoc =
              new Document("repo_name", REPO_NAME)
                  .append("merge_commit", mergeCommit)
                  .append("parent_1", parent1)
                  .append("parent_2", parent2)
                  .append("base_commit", baseCommit)
                  .append("conflict_files_num", manualMergedResults.size());
          if (runtimes.size() == 4) {
            scenarioDoc
                .append("time_graph_building", runtimes.get(0))
                .append("time_graph_matching", runtimes.get(1))
                .append("time_graph_merging", runtimes.get(2))
                .append("time_overall", runtimes.get(3));
          }
          Pair<Integer, List<Document>> intelliMergeConflicts =
              extractMergeConflicts(intelliMergedDir, false);
          scenarioDoc.append("conflicts_num", intelliMergeConflicts.getLeft());
          scenarioDoc.append("merge_conflicts", intelliMergeConflicts.getRight());
          Pair<Double, List<Document>> intelliVSmanual =
              compareAutoMerged(intelliMergedDir, manualMergedResults);
          scenarioDoc.append("auto_merged_precision", intelliVSmanual.getLeft());
          scenarioDoc.append("auto_merged", intelliVSmanual.getRight());
          intelliDBCollection.insertOne(scenarioDoc);

          // 2. Compare GitMerge with Manual
          scenarioDoc =
              new Document("repo_name", REPO_NAME)
                  .append("merge_commit", mergeCommit)
                  .append("parent_1", parent1)
                  .append("parent_2", parent2)
                  .append("base_commit", baseCommit)
                  .append("conflict_files_num", manualMergedResults.size());
          Pair<Integer, List<Document>> gitMergeConflicts =
              extractMergeConflicts(gitMergedDir, true);
          scenarioDoc.append("conflicts_num", gitMergeConflicts.getLeft());
          scenarioDoc.append("merge_conflicts", gitMergeConflicts.getRight());
          Pair<Double, List<Document>> gitVSmanual =
              compareAutoMerged(gitMergedDir, manualMergedResults);
          scenarioDoc.append("auto_merged_precision", gitVSmanual.getLeft());
          scenarioDoc.append("auto_merged", gitVSmanual.getRight());
          gitDBCollection.insertOne(scenarioDoc);

          // 3. Compare JFSTMerge with Manual
          scenarioDoc =
              new Document("repo_name", REPO_NAME)
                  .append("merge_commit", mergeCommit)
                  .append("parent_1", parent1)
                  .append("parent_2", parent2)
                  .append("base_commit", baseCommit)
                  .append("conflict_files_num", manualMergedResults.size());
          Pair<Integer, List<Document>> jfstMergeConflicts =
              extractMergeConflicts(jfstMergedDir, false);
          scenarioDoc.append("conflicts_num", jfstMergeConflicts.getLeft());
          scenarioDoc.append("merge_conflicts", jfstMergeConflicts.getRight());
          Pair<Double, List<Document>> jfstVSmanual =
              compareAutoMerged(jfstMergedDir, manualMergedResults);
          scenarioDoc.append("auto_merged_precision", jfstVSmanual.getLeft());
          scenarioDoc.append("auto_merged", jfstVSmanual.getRight());
          jfstDBCollection.insertOne(scenarioDoc);

          logger.info("Done with {}:{}", REPO_NAME, mergeCommit);
          processedMergeCommits.add(mergeCommit);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Extract all merge conflicts from merged results
   *
   * @param mergeResultDir
   * @param diff3Style
   * @return
   */
  private static Pair<Integer, List<Document>> extractMergeConflicts(
      String mergeResultDir, boolean diff3Style) throws Exception {
    ArrayList<SourceFile> temp = new ArrayList<>();
    ArrayList<SourceFile> mergedResults =
        Utils.scanJavaSourceFiles(mergeResultDir, temp, mergeResultDir);
    List<Document> fileDocs = new ArrayList<>();
    Integer conflictNum = 0;

    for (SourceFile sourceFile : mergedResults) {
      int conflictLOC = 0;
      List<Document> conflictDocs = new ArrayList<>();
      List<ConflictBlock> conflictBlocks =
          Utils.extractConflictBlocks(sourceFile.getAbsolutePath(), diff3Style, true);
      conflictNum += conflictBlocks.size();
      for (ConflictBlock conflictBlock : conflictBlocks) {
        Document conflictDoc =
            new Document("start_line", conflictBlock.getStartLine())
                .append("end_line", conflictBlock.getEndLine())
                .append("left", conflictBlock.getLeft())
                .append("base", conflictBlock.getBase())
                .append("right", conflictBlock.getRight());
        conflictLOC += (conflictBlock.getEndLine() - conflictBlock.getStartLine());
        conflictDocs.add(conflictDoc);
      }
      if (conflictDocs.size() > 0) {
        Document fileDoc =
            new Document("file_path", sourceFile.getRelativePath())
                .append("conflicts_num", conflictBlocks.size())
                .append("conflicts_loc", conflictLOC)
                .append("conflict_blocks", conflictDocs);
        fileDocs.add(fileDoc);
      }
    }
    return Pair.of(conflictNum, fileDocs);
  }

  /**
   * Compare results auto-merged by the tool with results merged manually
   *
   * @param mergeResultDir
   * @throws Exception
   */
  private static Pair<Double, List<Document>> compareAutoMerged(
      String mergeResultDir, ArrayList<SourceFile> manualMergedResults) {

    int numberOfMergedFiles = manualMergedResults.size();
    int numberOfDiffFiles = 0;
    Double scenarioPrecision =
        0.0; // if auto_merged part is identical with manual, precision is 1.0

    List<Document> fileDocs = new ArrayList<>();

    // for each file in the manual results, find and diff with the corresponding intelli result
    for (SourceFile manualMergedFile : manualMergedResults) {
      String fromFilePath = manualMergedFile.getAbsolutePath();
      String relativePath = manualMergedFile.getRelativePath();
      String toFilePath = Utils.formatPathSeparator(mergeResultDir + relativePath);
      // TODO modify jFSTMerge to save as the relative path
      String fileName = manualMergedFile.getFileName();
      if (mergeResultDir.contains(Side.JFST.asString())) {
        toFilePath = Utils.formatPathSeparator(mergeResultDir + File.separator + fileName);
      }
      Double filePrecision = 0.0;

      int manualLOC =
          Utils.readFileToLines(fromFilePath)
              .size(); // the number of code lines in the manual merged file
      int autoMergedLOC = Utils.readFileToLines(toFilePath).size();
      int fromDiffLoc = 0;
      int toDiffLoc = 0;
      List<Document> hunkDocuments = new ArrayList<>();

      String diffOutput =
          Utils.runSystemCommand(
              REPO_DIR,
              "git",
              "diff",
              "--ignore-cr-at-eol",
              "--ignore-all-space",
              "--ignore-blank-lines",
              "--ignore-space-change",
              "--no-index",
              "-U0",
              fromFilePath,
              toFilePath);
      // 4. parse diff output and save in the mongodb
      DiffParser parser = new UnifiedDiffParser();
      List<Diff> diffs = parser.parse(new ByteArrayInputStream(diffOutput.getBytes()));
      for (Diff diff : diffs) { // usually 1, since only two files are compared here
        // keep the true positive hunks
        List<Hunk> visitedHunks = new ArrayList<>();
        for (Hunk hunk : diff.getHunks()) {
          // if there are two hunk with the same line count but opposite direction (+/-), compare
          // the lines to counteract possibly moved hunks
          if (!removePossiblyMovedHunks(hunk, visitedHunks)) {
            visitedHunks.add(hunk);
          }
        }
        // save the true positive hunks into mongodb
        numberOfDiffFiles += visitedHunks.size() > 0 ? 1 : 0;

        for (Hunk hunk : visitedHunks) {
          int fromStartLine = hunk.getFromFileRange().getLineStart();
          int toStartLine = hunk.getToFileRange().getLineStart();
          int fromLOC = hunk.getFromFileRange().getLineCount();
          int toLOC = hunk.getToFileRange().getLineCount();
          String fromContent = getHunkContent(hunk, Line.LineType.FROM, false);
          String toContent = getHunkContent(hunk, Line.LineType.TO, false);
          Document hunkDocument =
              new Document("from_start_line", fromStartLine)
                  .append("from_loc", fromLOC)
                  .append("to_start_line", toStartLine)
                  .append("to_loc", toLOC)
                  .append("from_content", fromContent)
                  .append("to_content", toContent);
          hunkDocuments.add(hunkDocument);
          fromDiffLoc += fromLOC;
          toDiffLoc += toLOC;
        }
      }
      if (hunkDocuments.size() > 0) {
        int sameLOC = autoMergedLOC - toDiffLoc;
        if (autoMergedLOC > 0) {
          filePrecision = sameLOC / (double) autoMergedLOC;
          scenarioPrecision += filePrecision;
        }
        Document fileDocument =
            new Document("file_relative_path", relativePath)
                .append("manual_loc", manualLOC)
                .append("auto_merged_loc", autoMergedLOC)
                .append("same_with_manual_loc", sameLOC)
                .append("auto_merged_precision", filePrecision)
                .append("from_diff_loc", fromDiffLoc)
                .append("to_diff_loc", toDiffLoc)
                .append("diff_hunks", hunkDocuments);
        fileDocs.add(fileDocument);
      }
    }
    logger.info(
        "#Merged files: {}, #Identical files: {}",
        numberOfMergedFiles,
        numberOfMergedFiles - numberOfDiffFiles);
    if (numberOfDiffFiles > 0) {
      scenarioPrecision /= (double) numberOfDiffFiles;
    } else {
      // if auto_merged part is identical with manual, precision is 1.0
      scenarioPrecision = 1.0;
    }
    return Pair.of(scenarioPrecision, fileDocs);
  }

  /**
   * Check if one hunk is caused by moving (false positive diff)
   *
   * @param hunk
   * @param visitedHunks
   * @return true: the hunk if casued by moving, remove it as well as the other one caused by moving
   */
  private static boolean removePossiblyMovedHunks(Hunk hunk, List<Hunk> visitedHunks) {
    for (Hunk visitedHunk : visitedHunks) {
      // check if line counts are opposite, e.g. @@ -130,47 +132,0 @@ and @@ -330,0 +287,47 @@
      if (visitedHunk.getFromFileRange().getLineCount() == hunk.getToFileRange().getLineCount()
          && visitedHunk.getToFileRange().getLineCount()
              == hunk.getFromFileRange().getLineCount()) {
        // check if line contents are the same
        String hunkFromContent = getHunkContent(hunk, Line.LineType.FROM, true);
        String visitedHunkFromContent = getHunkContent(visitedHunk, Line.LineType.FROM, true);
        String hunkToContent = getHunkContent(hunk, Line.LineType.TO, true);
        String visitedHunkToContent = getHunkContent(visitedHunk, Line.LineType.TO, true);
        if (hunkFromContent.equals(visitedHunkToContent)
            && hunkToContent.equals(visitedHunkFromContent)) {
          visitedHunks.remove(visitedHunk);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Get the corresponding content from hunk by line type (FROM/TO/NEUTRAL)
   *
   * @param hunk
   * @param lineType
   * @param ignoreEmptyChars
   * @return
   */
  private static String getHunkContent(
      Hunk hunk, Line.LineType lineType, boolean ignoreEmptyChars) {
    String content =
        hunk.getLines()
            .stream()
            .filter(line -> line.getLineType().equals(lineType))
            .map(Line::getContent)
            .collect(Collectors.joining("\n"));

    return ignoreEmptyChars ? Utils.getStringContentOneLine(content).trim() : content.trim();
  }
}
