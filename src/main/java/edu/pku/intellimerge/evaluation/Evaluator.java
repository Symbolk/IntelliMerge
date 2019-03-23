package edu.pku.intellimerge.evaluation;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.model.SourceFile;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
import io.reflectoring.diffparser.api.DiffParser;
import io.reflectoring.diffparser.api.UnifiedDiffParser;
import io.reflectoring.diffparser.api.model.Diff;
import io.reflectoring.diffparser.api.model.Hunk;
import io.reflectoring.diffparser.api.model.Line;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
      String mergeCommit = "7fd7c83851fb87d727c220043bac0e4e81632182";
      String sourceDir = "D:\\github\\merges\\javaparser\\" + mergeCommit;
      //      String sourceDir = "D:\\github\\test";
      String mergeResultDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
      String manualMergedDir = sourceDir + File.separator + Side.MANUAL.asString() + File.separator;
      // 1. merge to get our results
      apiClient.processDirectory(sourceDir, mergeResultDir, true);
      // 2. format the manual results
      Utils.formatAllJavaFiles(manualMergedDir);

      // 3. compare merge results with manual results
      compareMergeResults(REPO_NAME, mergeCommit, collection, mergeResultDir, manualMergedDir);

      //      String targetDir = "D:\\github\\test";
      //      List<String> relativePaths = new ArrayList<>();
      //
      // relativePaths.add("javaparser-core\\src\\main\\java\\com\\github\\javaparser\\ast\\Modifier.java");
      //      Utils.copyAllVersions(sourceDir, relativePaths, targetDir);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Compare results merged by the tool with results merged manually
   *
   * @param repoName
   * @param mergeCommit
   * @param collection
   * @param mergeResultDir
   * @param manualMergedDir
   * @throws Exception
   */
  private static void compareMergeResults(
      String repoName,
      String mergeCommit,
      MongoCollection<Document> collection,
      String mergeResultDir,
      String manualMergedDir)
      throws Exception {

    // for each file in the manual results, find and diff with the corresponding intelli result
    ArrayList<SourceFile> temp = new ArrayList<>();
    ArrayList<SourceFile> manualMergedResults =
        Utils.scanJavaSourceFiles(manualMergedDir, temp, manualMergedDir);
    for (SourceFile manualMergedFile : manualMergedResults) {
      String manualMergedPath = manualMergedFile.getAbsolutePath();
      String relativePath = manualMergedFile.getRelativePath();
      String intelliMergedPath = Utils.formatPathSeparator(mergeResultDir + relativePath);

      int loc = 1; // the number of code lines in the manual merged file
      int same_loc =
          1; // the number of code lines same between the manual merged and intelli merged files
      String diffOutput =
          Utils.runSystemCommand(
              REPO_DIR,
              "git",
              "diff",
              "--ignore-cr-at-eol",
              "--ignore-all-space",
              "--ignore-blank-lines",
              "--no-index",
              "-U0",
              manualMergedPath,
              intelliMergedPath);
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
        List<Document> hunkDocuments = new ArrayList<>();

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
        }
        Document doc =
            new Document("repo_name", repoName)
                .append("merge_commit", mergeCommit)
                .append("file_relative_path", relativePath)
                .append("loc", 1)
                .append("same_loc", 1)
                .append("diff_hunks", hunkDocuments);
        collection.insertOne(doc);
      }
      break;
    }
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

    return ignoreEmptyChars ? Utils.getStringContentOneLine(content) : content;
  }
}
