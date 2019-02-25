package edu.pku.intellimerge.client;

import edu.pku.intellimerge.core.SemanticGraphBuilder2;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.io.SemanticGraphExporter;
import edu.pku.intellimerge.io.SourceFileCollector;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.FilesManager;
import org.eclipse.jgit.lib.Repository;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class APIClient {

  private Logger logger = LoggerFactory.getLogger(APIClient.class);

  private String REPO_NAME;
  private String REPO_DIR;
  private String GIT_URL;
  private String SRC_DIR;
  private String DIFF_DIR;
  private String RESULT_DIR;
  private String STATISTICS_PATH;
  // to export graph as dot file
  private String DOT_DIR;

  public APIClient() {}

  public APIClient(
      String REPO_NAME,
      String REPO_DIR,
      String GIT_URL,
      String SRC_DIR,
      String DIFF_DIR,
      String RESULT_DIR,
      String STATISTICS_PATH,
      String DOT_DIR) {
    this.REPO_NAME = REPO_NAME;
    this.REPO_DIR = REPO_DIR;
    this.GIT_URL = GIT_URL;
    this.SRC_DIR = SRC_DIR;
    this.DIFF_DIR = DIFF_DIR;
    this.RESULT_DIR = RESULT_DIR;
    this.STATISTICS_PATH = STATISTICS_PATH;
    this.DOT_DIR = DOT_DIR;
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
   * Read merge commits from csv file, and create merge scenarios
   *
   * @return
   */
  public List<MergeScenario> generateMergeScenarios() {
    List<MergeScenario> mergeScenarios = new ArrayList<>();
    for (String[] items : FilesManager.readCSV(STATISTICS_PATH, ";")) {

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
      MergeScenario mergeScenario, Graph<SemanticNode, SemanticEdge> graph, Side side) {
    SemanticGraphExporter.saveAsDot(
        graph,
        DOT_DIR
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
    String collectedFilePath =
        DIFF_DIR + File.separator + mergeScenario.mergeCommitID + File.separator;

    SourceFileCollector collector =
        new SourceFileCollector(mergeScenario, repository, collectedFilePath);
    collector.setOnlyBothModified(true);
    collector.setCopyImportedFiles(false);
    collector.collectFilesForAllSides();
    logger.info("Collecting files done for {}", mergeScenario.mergeCommitID);

    // 2.1 Build ours/theirs graphs with collected files
    SemanticGraphBuilder2 oursBuilder =
        new SemanticGraphBuilder2(mergeScenario, Side.OURS, collectedFilePath, true);
    SemanticGraphBuilder2 baseBuilder =
        new SemanticGraphBuilder2(mergeScenario, Side.BASE, collectedFilePath, true);
    SemanticGraphBuilder2 theirsBuilder =
        new SemanticGraphBuilder2(mergeScenario, Side.THEIRS, collectedFilePath, true);

    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.build();

    //    saveDotToFile(mergeScenario, oursGraph, Side.OURS);
    //    SemanticGraphExporter.printAsDot(oursGraph);

    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.build();

    //    saveDotToFile(mergeScenario, theirsGraph, Side.THEIRS);

    // 2.2 Build base/merge graphs among ours/theirs files
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.build();
    //    saveDotToFile(mergeScenario, baseGraph, Side.BASE);

    logger.info("Building graph done for {}", mergeScenario.mergeCommitID);

    String resultDir =
        RESULT_DIR
            + File.separator
            + mergeScenario.mergeCommitID
            + File.separator
            + "intelliMerged";
    FilesManager.clearResultDir(resultDir);
    ThreewayGraphMerger merger =
        new ThreewayGraphMerger(resultDir, oursGraph, baseGraph, theirsGraph);
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
   * @throws Exception
   */
  public void processDirectory(String targetDir, String resultDir) throws Exception {
    SemanticGraphBuilder2 oursBuilder =
        new SemanticGraphBuilder2(null, Side.OURS, targetDir, false);
    SemanticGraphBuilder2 baseBuilder =
        new SemanticGraphBuilder2(null, Side.BASE, targetDir, false);
    SemanticGraphBuilder2 theirsBuilder =
        new SemanticGraphBuilder2(null, Side.THEIRS, targetDir, false);

    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.build();
    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.build();
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.build();

    logger.info("Building graph done for {}", targetDir);

    FilesManager.clearResultDir(resultDir);
    ThreewayGraphMerger merger =
        new ThreewayGraphMerger(resultDir, oursGraph, baseGraph, theirsGraph);
    // 3. Match node and merge the 3-way graphs
    merger.threewayMap();
    logger.info("Matching done for {}", targetDir);

    // 4. Print the merged graph into code, keep the original format as possible
    List<String> mergedFilePaths = merger.threewayMerge();
    for(String mergedFilePath : mergedFilePaths){
      logger.info(mergedFilePath);
    }
    logger.info("Merging done for {}", targetDir);
  }
}
