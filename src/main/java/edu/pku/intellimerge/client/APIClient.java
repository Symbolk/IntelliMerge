package edu.pku.intellimerge.client;

import com.google.common.base.Stopwatch;
import edu.pku.intellimerge.core.SemanticGraphBuilder2;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.exception.RangeNullException;
import edu.pku.intellimerge.io.SemanticGraphExporter;
import edu.pku.intellimerge.io.SourceFileCollector;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.Refactoring;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.util.Utils;
import org.eclipse.jgit.lib.Repository;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for providing APIs for evaluation
 * */
public class APIClient {

  private Logger logger = LoggerFactory.getLogger(APIClient.class);

  private String REPO_NAME;
  private String REPO_DIR;
  private String GIT_URL;
  private String SRC_DIR;
  private String DIFF_DIR;
  private String RESULT_DIR;
  private boolean hasMultipleModules;

  public APIClient(
      String REPO_NAME,
      String REPO_DIR,
      String GIT_URL,
      String DIFF_DIR,
      String RESULT_DIR,
      boolean hasMultipleModules) {
    this.REPO_NAME = REPO_NAME;
    this.REPO_DIR = REPO_DIR;
    this.GIT_URL = GIT_URL;
    this.SRC_DIR = "";
    this.DIFF_DIR = DIFF_DIR;
    this.RESULT_DIR = RESULT_DIR;
    this.hasMultipleModules = hasMultipleModules;
  }

  public APIClient(
      String REPO_NAME,
      String REPO_DIR,
      String GIT_URL,
      String SRC_DIR,
      String DIFF_DIR,
      String RESULT_DIR,
      boolean hasMultipleModules) {
    this.REPO_NAME = REPO_NAME;
    this.REPO_DIR = REPO_DIR;
    this.GIT_URL = GIT_URL;
    this.SRC_DIR = SRC_DIR;
    this.DIFF_DIR = DIFF_DIR;
    this.RESULT_DIR = RESULT_DIR;
    this.hasMultipleModules = hasMultipleModules;
  }

  /**
   * Generate one single merge scenario (mainly for testing)
   *
   * @return
   */
  public MergeScenario generateSingleMergeSenario() {
    String mergeCommitID = "d9c990a94c725b8d112ba02897988b7400100ce3";
    String oursCommitID = "dee6b3f144f3d3bf0f0469cfb3a5c9176b57b9d5";
    String theirsCommitID = "4d3a53a47d34f7d93d2b1af76d0b2d7250028397";
    String baseCommitID = "52814c72f70239f212e13178c2d1ef01e0e25f47";

    MergeScenario mergeScenario =
        new MergeScenario(
            REPO_NAME,
            REPO_DIR,
            SRC_DIR,
            mergeCommitID,
            oursCommitID,
            baseCommitID,
            theirsCommitID);
    return mergeScenario;
  }

  /**
   * Read merge commits from csv file and create merge scenarios
   *
   * @param statisticsFilePath csv file that contains four commits to form merge scenarios
   * @return
   */
  public List<MergeScenario> generateMergeScenarios(String statisticsFilePath) {
    List<MergeScenario> mergeScenarios = new ArrayList<>();
    for (String[] items : Utils.readCSVAsString(statisticsFilePath, ";")) {

      String mergeCommitID = items[0];
      String oursCommitID = items[1];
      String theirsCommitID = items[2];
      String baseCommitID = items[3];

      MergeScenario mergeScenario =
          new MergeScenario(
              REPO_NAME,
              REPO_DIR,
              SRC_DIR,
              mergeCommitID,
              oursCommitID,
              baseCommitID,
              theirsCommitID);
      mergeScenarios.add(mergeScenario);
    }
    return mergeScenarios;
  }

  /**
   * Save built graph into a .dot file
   *
   * @param mergeScenario
   * @param graph
   * @param side
   */
  private void saveDotToFile(
      MergeScenario mergeScenario,
      Graph<SemanticNode, SemanticEdge> graph,
      Side side,
      String dotDir) {
    SemanticGraphExporter.saveAsDot(
        graph,
        dotDir
            + File.separator
            + side
            + File.separator
            + mergeScenario.mergeCommitID.substring(0, 7)
            + ".dot");
  }

  /**
   * Collect, analyze, match and merge java files collected in one merge scenario
   *
   * @param mergeScenario
   * @param repository
   * @throws Exception
   */
  public void processMergeScenario(MergeScenario mergeScenario, Repository repository)
      throws Exception {

    // 1. Collect diff java files and imported files between merge parent commit and base commit

    // source files collected to be parse later
    String collectedFileDir =
        DIFF_DIR + File.separator + mergeScenario.mergeCommitID + File.separator;

    SourceFileCollector collector =
        new SourceFileCollector(mergeScenario, repository, collectedFileDir);
    collector.setOnlyBothModified(true);
    collector.setCopyImportedFiles(false);
    collector.collectFilesForAllSides();
    logger.info("Collecting files done for {}", mergeScenario.mergeCommitID);

    // 2.1 Build ours/theirs graphs with collected files
    Stopwatch stopwatch = Stopwatch.createStarted();
    boolean hasMultipleModule = true;
    ExecutorService executorService = Executors.newFixedThreadPool(3);

    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(
                mergeScenario, Side.OURS, collectedFileDir, hasMultipleModule));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(
                mergeScenario, Side.BASE, collectedFileDir, hasMultipleModule));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(
                mergeScenario, Side.THEIRS, collectedFileDir, hasMultipleModule));
    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

    stopwatch.stop();
    logger.info(
        "Building graph done for {} within {}ms.",
        mergeScenario.mergeCommitID,
        stopwatch.elapsed(TimeUnit.MILLISECONDS));
    executorService.shutdown();
    //    stopwatch.reset().start();

    String mergeResultDir =
        RESULT_DIR
            + File.separator
            + mergeScenario.mergeCommitID
            + File.separator
            + Side.INTELLI.asString()
            + File.separator;
    Utils.prepareDir(mergeResultDir);
    ThreewayGraphMerger merger =
        new ThreewayGraphMerger(mergeResultDir, oursGraph, baseGraph, theirsGraph);
    // 3. Match node and merge the 3-way graphs

    merger.threewayMap();
    logger.info("Matching done for {}", mergeScenario.mergeCommitID);

    // 4. Print the merged graph into code, keep the original format as possible
    merger.threewayMerge();
    logger.info("Merging done for {}", mergeScenario.mergeCommitID);
  }

  /**
   * Analyze all java files under the target directory, as if they are collected from merge scenario
   *
   * @param targetDir
   * @return runtime at each phase in order
   * @throws Exception
   */
  public List<Long> processDirectory(String targetDir, String resultDir) throws Exception {
    String targetDirName = Utils.getDirSimpleName(targetDir);

    ExecutorService executorService = Executors.newFixedThreadPool(3);

    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.OURS, targetDir, hasMultipleModules));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.BASE, targetDir, hasMultipleModules));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(Side.THEIRS, targetDir, hasMultipleModules));

    Stopwatch stopwatch = Stopwatch.createStarted();
    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

    stopwatch.stop();
    executorService.shutdown();
    long buildingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Building graph done for {}.", buildingTime, targetDirName);

    Utils.prepareDir(resultDir);
    ThreewayGraphMerger merger =
        new ThreewayGraphMerger(resultDir, oursGraph, baseGraph, theirsGraph);
    // 3. Match node and merge the 3-way graphs.
    stopwatch.reset().start();
    merger.threewayMap();
    stopwatch.stop();
    long matchingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Matching done for {}.", matchingTime, targetDirName);

    // save the detected refactorings into csv for human validation and debugging
    String b2oCsvFilePath = resultDir + File.separator + "ours_refactorings.csv";
    String b2tCsvFilePath = resultDir + File.separator + "theirs_refactorings.csv";
    saveRefactorings(b2oCsvFilePath, merger.b2oMatching);
    saveRefactorings(b2tCsvFilePath, merger.b2tMatching);

    // 4. Print the merged graph into code, keep the original format as possible
    stopwatch.reset().start();
    List<String> mergedFilePaths = merger.threewayMerge();
    stopwatch.stop();
    long mergingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Merging done for {}.", matchingTime, targetDirName);

    long overall = buildingTime + matchingTime + mergingTime;
    logger.info("Overall time cost: {}ms.", overall);

    List<Long> runtimes = new ArrayList<>();
    runtimes.add(buildingTime);
    runtimes.add(matchingTime);
    runtimes.add(mergingTime);
    runtimes.add(overall);
    return runtimes;
  }

  private void saveRefactorings(String filePath, TwowayMatching matching) {
    Utils.writeContent(
        filePath,
        "refactoring_type;node_type;confidence;before_location;before_node;after_location;after_node\n",
        false);
    try {

      for (Refactoring refactoring : matching.refactorings) {
        StringBuilder builder = new StringBuilder();
        builder.append(refactoring.getRefactoringType().getLabel()).append(";");
        builder.append(refactoring.getNodeType().asString()).append(";");
        builder.append(String.valueOf(refactoring.getConfidence())).append(";");
        builder.append(refactoring.getBeforeRange().begin.line).append("-");
        builder.append(refactoring.getBeforeRange().end.line).append(";");
        builder.append(refactoring.getBefore().getOriginalSignature()).append(";");
        builder.append(refactoring.getAfterRange().begin.line).append("-");
        builder.append(refactoring.getAfterRange().end.line).append(";");
        builder.append(refactoring.getAfter().getOriginalSignature()).append("\n");

        Utils.writeContent(filePath, builder.toString(), true);
      }
    } catch (RangeNullException e) {
      e.printStackTrace();
    }
  }
}
