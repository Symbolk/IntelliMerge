package edu.pku.intellimerge.evaluation;

import br.ufpe.cin.app.JFSTMerge;
import com.google.common.base.Stopwatch;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import edu.pku.intellimerge.client.IntelliMerge;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Class responsible to evaluate the result and the performance of IntelliMerge Comparing with
 * git-merge & jFSTMerge
 */
public class Evaluator {
  private static final Logger logger = LoggerFactory.getLogger(Evaluator.class);

  private static final String REPO_NAME = "error-prone";
  private static final String REPO_DIR = "D:\\github\\repos\\" + REPO_NAME;
  private static final String DATA_DIR =
      "D:\\github\\ref_conflicts_diff2\\"
          + REPO_NAME; // the directory to temporarily save the diff files

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");
    //      BasicConfigurator.configure();
    try {
      MongoClientURI connectionString = new MongoClientURI("mongodb://localhost:27017");
      MongoClient mongoClient = new MongoClient(connectionString);
      MongoDatabase intelliDB = mongoClient.getDatabase("IntelliVSManual");
      MongoDatabase gitDB = mongoClient.getDatabase("GitVSManual");
      MongoDatabase jfstDB = mongoClient.getDatabase("JFSTVSManual");
      MongoCollection<Document> intelliDBCollection = intelliDB.getCollection(REPO_NAME);
      MongoCollection<Document> gitDBCollection = gitDB.getCollection(REPO_NAME);
      MongoCollection<Document> jfstDBCollection = jfstDB.getCollection(REPO_NAME);

      // replay the collected merge scenario with IntelliMerge and jFSTMerge, save the data on disk
      // and mongodb
      File file = new File(DATA_DIR);
      File[] files = file.listFiles();
      for (File f : files) {
        if (f.isDirectory()) {
          String sourceDir = f.getAbsolutePath();
          String mergeCommit = f.getName();

          // dirs to save merged files by 3 tools
          String intelliMergedDir =
              sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
          String jfstMergedDir = sourceDir + File.separator + Side.JFST.asString() + File.separator;
          String gitMergedDir = sourceDir + File.separator + Side.GIT.asString() + File.separator;
          String manualMergedDir =
              sourceDir + File.separator + Side.MANUAL.asString() + File.separator;

          // jump some cases where no base files collected, or no manual files collected
          String baseDir = sourceDir + File.separator + Side.BASE.asString() + File.separator;
          File baseDirFile = new File(baseDir);
          if (!baseDirFile.exists()) {
            continue;
          }
          File manualDirFile = new File(manualMergedDir);
          if (!manualDirFile.exists()) {
            continue;
          }

          // 1. replay the merge with the three tools
          // runtime for each phase (need to run multiple times and get average)
          IntelliMerge intelliMerge = new IntelliMerge();
          List<String> directories = new ArrayList<>();
          directories.add(sourceDir + File.separator + Side.OURS.asString());
          directories.add(sourceDir + File.separator + Side.BASE.asString());
          directories.add(sourceDir + File.separator + Side.THEIRS.asString());
          List<Long> runtimes = intelliMerge.mergeDirectories(directories, intelliMergedDir, true);
          logger.info("IntelliMerge done in {}ms", runtimes.size() == 4 ? runtimes.get(3) : -1);

          Stopwatch stopwatch = Stopwatch.createStarted();
          JFSTMerge jfstMerge = new JFSTMerge();
          jfstMerge.mergeDirectories(
              sourceDir + File.separator + Side.OURS.asString(),
              sourceDir + File.separator + Side.BASE.asString(),
              sourceDir + File.separator + Side.THEIRS.asString(),
              jfstMergedDir);
          stopwatch.stop();
          long jfstRuntime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
          logger.info("JFSTMerge done in {}ms.", jfstRuntime);

          // 2. remove all comments and format the manual results
          //          Utils.removeAllComments(intelliMergedDir);
          //          Utils.removeAllComments(gitMergedDir);
          //          Utils.removeAllComments(jfstMergedDir);
          //          Utils.removeAllComments(manualMergedDir);
          // in order to alleviate format caused diffs, compare git-merge and jfst with unformatted
          // manual
          // in order to alleviate format caused diffs, compare intelli with formatted manual
          //          Utils.copyDir(manualMergedDir, manualMergedFormattedDir);
          //          String manualMergedFormattedDir = sourceDir + File.separator +
          // Side.MANUAL.asString() + "_Formatted" + File.separator;
          //          Utils.formatAllJavaFiles(manualMergedFormattedDir);

          // 3. compare merge results with manual results
          ArrayList<SourceFile> temp = new ArrayList<>();
          ArrayList<SourceFile> manualMergedResults =
              Utils.scanJavaSourceFiles(manualMergedDir, temp, manualMergedDir);

          // 1. Compare IntelliMerge with Manual
          //          Document scenarioDoc;
          Document scenarioDoc =
              new Document("repo_name", REPO_NAME).append("merge_commit", mergeCommit);
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
          scenarioDoc.append("conflict_blocks", intelliMergeConflicts.getRight());
          // compare only auto merged part
          ComparisonResult intelliVSmanual =
              compareAutoMerged(
                  intelliMergedDir.replaceFirst("Merged", "Merged_auto"), manualMergedResults);
          scenarioDoc.append("auto_merge_loc", intelliVSmanual.getTotalAutoMergeLOC());
          scenarioDoc.append("manual_merge_loc", intelliVSmanual.getTotalManualMergeLOC());
          scenarioDoc.append(
              "correct_loc_in_auto_merged", intelliVSmanual.getTotalSameAutoMergeLOC());
          scenarioDoc.append("correct_loc_in_manual", intelliVSmanual.getTotalSameManualLOC());
          scenarioDoc.append("auto_merge_precision", intelliVSmanual.getAutoMergePrecision());
          scenarioDoc.append("auto_merge_recall", intelliVSmanual.getAutoMergeRecall());
          // diff hunks between auto-merged code and manually-merged code
          scenarioDoc.append("auto_merge_diffs", intelliVSmanual.getAutoMergedDiffDocs());
          intelliDBCollection.insertOne(scenarioDoc);

          // 2. Compare JFSTMerge with Manual
          scenarioDoc =
              new Document("repo_name", REPO_NAME)
                  .append("merge_commit", mergeCommit)
                  .append("time_overall", jfstRuntime);
          Pair<Integer, List<Document>> jfstMergeConflicts =
              extractMergeConflicts(jfstMergedDir, false);
          scenarioDoc.append("conflicts_num", jfstMergeConflicts.getLeft());
          scenarioDoc.append("conflict_blocks", jfstMergeConflicts.getRight());
          ComparisonResult jfstVSmanual =
              compareAutoMerged(
                  jfstMergedDir.replaceFirst("Merged", "Merged_auto"), manualMergedResults);
          scenarioDoc.append("auto_merge_loc", jfstVSmanual.getTotalAutoMergeLOC());
          scenarioDoc.append("manual_merge_loc", jfstVSmanual.getTotalManualMergeLOC());
          scenarioDoc.append("correct_loc_in_auto_merged", jfstVSmanual.getTotalSameAutoMergeLOC());
          scenarioDoc.append("correct_loc_in_manual", jfstVSmanual.getTotalSameManualLOC());
          scenarioDoc.append("auto_merge_precision", jfstVSmanual.getAutoMergePrecision());
          scenarioDoc.append("auto_merge_recall", jfstVSmanual.getAutoMergeRecall());
          // files that stills contains diff hunks in auto_merge parts
          scenarioDoc.append("auto_merge_diffs", jfstVSmanual.getAutoMergedDiffDocs());
          jfstDBCollection.insertOne(scenarioDoc);

          // 3. Compare GitMerge with Manual
          temp = new ArrayList<>();
          manualMergedResults = Utils.scanJavaSourceFiles(manualMergedDir, temp, manualMergedDir);
          scenarioDoc = new Document("repo_name", REPO_NAME).append("merge_commit", mergeCommit);
          Pair<Integer, List<Document>> gitMergeConflicts =
              extractMergeConflicts(gitMergedDir, false);
          scenarioDoc.append("conflicts_num", gitMergeConflicts.getLeft());
          scenarioDoc.append("conflict_blocks", gitMergeConflicts.getRight());
          ComparisonResult gitVSmanual =
              compareAutoMerged(
                  gitMergedDir.replaceFirst("Merged", "Merged_auto"), manualMergedResults);
          scenarioDoc.append("auto_merge_loc", gitVSmanual.getTotalAutoMergeLOC());
          scenarioDoc.append("manual_merge_loc", gitVSmanual.getTotalManualMergeLOC());
          scenarioDoc.append("correct_loc_in_auto_merged", gitVSmanual.getTotalSameAutoMergeLOC());
          scenarioDoc.append("correct_loc_in_manual", gitVSmanual.getTotalSameManualLOC());
          scenarioDoc.append("auto_merge_precision", gitVSmanual.getAutoMergePrecision());
          scenarioDoc.append("auto_merge_recall", gitVSmanual.getAutoMergeRecall());
          // files that stills contains diff hunks in auto_merge parts
          scenarioDoc.append("auto_merge_diffs", gitVSmanual.getAutoMergedDiffDocs());
          gitDBCollection.insertOne(scenarioDoc);

          logger.info("Done with {}:{}", REPO_NAME, mergeCommit);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Extract all merge conflicts and !remove them! from merged results
   *
   * @param mergeResultDir
   * @param diff3Style
   * @return
   */
  public static Pair<Integer, List<Document>> extractMergeConflicts(
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
          Utils.extractConflictBlocks(
              sourceFile.getAbsolutePath(),
              sourceFile.getAbsolutePath().replaceFirst("Merged", "Merged_auto"),
              diff3Style);
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
   * Compare auto-merged results by tools with manual merged results for each merge scenario
   *
   * <p>nesting level: project -> merge scenario -> file
   *
   * @param mergeResultDir
   * @throws Exception
   */
  public static ComparisonResult compareAutoMerged(
      String mergeResultDir, ArrayList<SourceFile> manualMergedResults) {

    int numberOfMergedFiles = manualMergedResults.size();
    int numberOfDiffFiles = 0;
    Double autoMergePrecision = 0.0;
    Double autoMergeRecall = 0.0;
    // total numbers of all files in this merge scenario
    Integer totalAutoMergedLOC = 0;
    Integer totalManualMergedLOC = 0;
    Integer totalSameLOCMerged = 0;
    Integer totalSameLOCManual = 0;

    List<Document> fileDocs = new ArrayList<>();

    // for each file in the merged results, compare with the manual merged file by `git diff`
    for (SourceFile manualMergedFile : manualMergedResults) {
      // from the auto merged to the manual merged
      String toFilePath = manualMergedFile.getAbsolutePath();
      String relativePath = manualMergedFile.getRelativePath();
      String fromFilePath = Utils.formatPathSeparator(mergeResultDir + relativePath);
      // TODO modify jFSTMerge to save files with hierarchy, currently it saves files flatly
      String fileName = manualMergedFile.getFileName();
      if (mergeResultDir.contains(Side.JFST.asString())) {
        fromFilePath = Utils.formatPathSeparator(mergeResultDir + File.separator + fileName);
      }
      double filePrecision = 0.0;
      double fileRecall = 0.0;

      int autoMergedLOC = Utils.readFileToLines(fromFilePath).size();
      int manualLOC = Utils.readFileToLines(toFilePath).size();
      //      int autoMergedLOC = Utils.computeFileLOC(fromFilePath);
      //      int manualLOC = Utils.computeFileLOC(toFilePath);

      totalAutoMergedLOC += autoMergedLOC;
      totalManualMergedLOC += manualLOC;

      int fromDiffLoc = 0;
      int toDiffLoc = 0;
      List<Document> diffHunkDocs = new ArrayList<>();

      String diffOutput =
          Utils.runSystemCommand(
              REPO_DIR,
              "git",
              "diff",
              "--ignore-cr-at-eol",
              "--ignore-all-space",
              "--ignore-space-change",
              "--no-index",
              "-U0",
              fromFilePath,
              toFilePath);
      // 4. parse diff output and save in the mongodb
      DiffParser parser = new UnifiedDiffParser();
      List<Diff> diffs = parser.parse(new ByteArrayInputStream(diffOutput.getBytes()));
      for (Diff diff : diffs) { // usually 1, since only two files are compared here
        // remove false negative diff hunks, like empty lines, moved code
        List<Hunk> visitedHunks = new ArrayList<>();
        for (Hunk hunk : diff.getHunks()) {
          // if there are two hunk with the same line count but opposite direction (+/-), compare
          //           the lines to counteract possibly moved hunks
          if (!removeMovingCausedHunks(hunk, visitedHunks)) {
            visitedHunks.add(hunk);
          }
        }
        // double check the diff hunks
        //        removeHunksInvolvingConflicts(visitedHunks);
        //        visitedHunks = removeFormatCausedHunks(visitedHunks);
        // if there is no `real` diff hunks, mark it as the same file
        numberOfDiffFiles += visitedHunks.size() > 0 ? 1 : 0;

        for (Hunk hunk : visitedHunks) {
          int fromStartLine = hunk.getFromFileRange().getLineStart();
          int toStartLine = hunk.getToFileRange().getLineStart();
          String fromContent = getHunkContent(hunk, Line.LineType.FROM, false);
          String toContent = getHunkContent(hunk, Line.LineType.TO, false);
          int fromLOC = fromContent.isEmpty() ? 1 : hunk.getFromFileRange().getLineCount();
          int toLOC = toContent.isEmpty() ? 1 : hunk.getToFileRange().getLineCount();
          Document diffHunkDoc =
              new Document("from_start_line", fromStartLine)
                  .append("from_loc", fromLOC)
                  .append("to_start_line", toStartLine)
                  .append("to_loc", toLOC)
                  .append("from_content", fromContent)
                  .append("to_content", toContent);
          diffHunkDocs.add(diffHunkDoc);
          fromDiffLoc += fromLOC;
          toDiffLoc += toLOC;
        }
      }
      // if there exists differences, create one document to save the diffs for the file
      if (diffHunkDocs.size() > 0) {
        // from file: auto-merged (--) to file: manual (++)
        int sameLOCMerged = autoMergedLOC > 0 ? autoMergedLOC - fromDiffLoc - toDiffLoc : 0;
        int sameLOCManual = autoMergedLOC > 0 ? manualLOC - toDiffLoc : 0;
        sameLOCMerged = sameLOCMerged > 0 ? sameLOCMerged : sameLOCManual;
        totalSameLOCMerged += sameLOCMerged;
        totalSameLOCManual += sameLOCManual;

        if (autoMergedLOC > 0) {
          filePrecision = sameLOCMerged / (double) autoMergedLOC;
        } else {
          filePrecision = 0.0;
        }
        if (manualLOC > 0) {
          fileRecall = sameLOCMerged / (double) manualLOC;
        } else {
          fileRecall = 0.0;
        }
        Document fileDocument =
            new Document("file_relative_path", relativePath)
                .append("manual_loc", manualLOC)
                .append("auto_merge_loc", autoMergedLOC)
                .append("correct_loc_in_auto_merged", sameLOCMerged)
                .append("correct_loc_in_manual", sameLOCManual)
                .append("auto_merge_precision", filePrecision)
                .append("auto_merge_recall", fileRecall)
                .append("from_diff_loc", fromDiffLoc)
                .append("to_diff_loc", toDiffLoc)
                .append("diff_hunks", diffHunkDocs);
        fileDocs.add(fileDocument);
      } else {
        // auto-merged lines are identical with manual merged lines
        totalSameLOCMerged += autoMergedLOC;
        totalSameLOCManual += manualLOC;
      }
    }
    //    logger.info(
    //        "#Merged files: {}, #Identical files: {}",
    //        numberOfMergedFiles,
    //        numberOfMergedFiles - numberOfDiffFiles);
    if (numberOfDiffFiles > 0) {
      autoMergePrecision = totalSameLOCMerged / totalAutoMergedLOC.doubleValue();
    } else {
      // if auto_merge parts in all files are identical with manual, precision is 1.0
      autoMergePrecision = 1.0;
    }
    if (numberOfDiffFiles > 0) {
      autoMergeRecall = totalSameLOCMerged / totalManualMergedLOC.doubleValue();
    } else {
      // if auto_merge parts in all files are identical with manual, precision is 1.0
      autoMergePrecision = 1.0;
    }
    ComparisonResult result =
        new ComparisonResult(
            totalAutoMergedLOC,
            totalManualMergedLOC,
            totalSameLOCMerged,
            totalSameLOCManual,
            autoMergePrecision,
            autoMergeRecall,
            fileDocs);
    return result;
  }

  /**
   * Check if one hunk is caused by moving (false positive diff)
   *
   * @param hunk
   * @param visitedHunks
   * @return true: the hunk if casued by moving, remove it as well as the other one caused by moving
   */
  private static boolean removeMovingCausedHunks(Hunk hunk, List<Hunk> visitedHunks) {
    for (Hunk visitedHunk : visitedHunks) {
      // P.S. since actually \n brings deviation to line ranges, so here we directly compare hunk
      // contents
      // check if line ranges are opposite, e.g. @@ -130,47 +132,0 @@ and @@ -330,0 +287,47 @@
      //      if (visitedHunk.getFromFileRange().getLineCount() ==
      // hunk.getToFileRange().getLineCount()
      //          && visitedHunk.getToFileRange().getLineCount()
      //              == hunk.getFromFileRange().getLineCount()) {
      // check if hunk contents are the same
      String hunkFromContent = getHunkContent(hunk, Line.LineType.FROM, true);
      String visitedHunkFromContent = getHunkContent(visitedHunk, Line.LineType.FROM, true);
      String hunkToContent = getHunkContent(hunk, Line.LineType.TO, true);
      String visitedHunkToContent = getHunkContent(visitedHunk, Line.LineType.TO, true);
      if (hunkFromContent.equals(visitedHunkToContent)
          && hunkToContent.equals(visitedHunkFromContent)) {
        visitedHunks.remove(visitedHunk);
        return true;
      }
      //      }
    }
    return false;
  }

  /**
   * Remove diff hunks that have the identical content ignoring empty chars
   *
   * @param hunks
   */
  private static List<Hunk> removeFormatCausedHunks(List<Hunk> hunks) {
    //    List<Hunk> hunksCopy = new ArrayList<>(hunks);
    //    for (Hunk hunk : hunksCopy) {
    //      String hunkFromContent = getHunkContent(hunk, Line.LineType.FROM, true);
    //      String hunkToContent = getHunkContent(hunk, Line.LineType.TO, true);
    //      if (hunkFromContent.equals(hunkToContent)) {
    //        hunks.remove(hunk);
    //      }
    //    }
    return hunks.stream()
        .filter(
            hunk ->
                getHunkContent(hunk, Line.LineType.FROM, true)
                    .equals(getHunkContent(hunk, Line.LineType.TO, true)))
        .collect(Collectors.toList());
  }

  //  private static List<Hunk> removeHunksInvolvingConflicts(List<Hunk> hunks) {
  //    return hunks.stream()
  //            .filter(
  //                    hunk ->
  //                            getHunkContent(hunk, Line.LineType.FROM, true).concat(
  //                            (getHunkContent(hunk, Line.LineType.TO, true))).contains("<<<<<<")
  //            .collect(Collectors.toList());
  //  }

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
        hunk.getLines().stream()
            .filter(line -> line.getLineType().equals(lineType))
            .map(Line::getContent)
            .collect(Collectors.joining("\n"));

    return ignoreEmptyChars ? Utils.flattenString(content).trim() : content.trim();
  }
}
