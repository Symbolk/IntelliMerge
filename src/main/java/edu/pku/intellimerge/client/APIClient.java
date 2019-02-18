package edu.pku.intellimerge.client;

import edu.pku.intellimerge.core.SemanticGraphBuilder;
import edu.pku.intellimerge.core.SingleFileGraphBuilder;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.io.SemanticGraphExporter;
import edu.pku.intellimerge.io.SourceFileCollector;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.FilesManager;
import edu.pku.intellimerge.util.GitService;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jgit.lib.Repository;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class APIClient {

  private static final Logger logger = LoggerFactory.getLogger(APIClient.class);

  private static final String REPO_NAME = "javaparser";
  private static final String REPO_PATH = "D:\\github\\repos\\" + REPO_NAME;
  private static final String GIT_URL = "https://github.com/javaparser/javaparser.git";
  private static final String SRC_PATH =
      "/javaparser-core/src/main/java/"; // java project source folder
  //  private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";
  private static final String DIFF_PATH = "D:\\github\\diffs\\" + REPO_NAME;
  private static final String RESULT_PATH = "D:\\github\\merges\\" + REPO_NAME;

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");
    //      BasicConfigurator.configure();

    String mergeCommitID = "d9c990a94c725b8d112ba02897988b7400100ce3";
    String oursCommitID = "dee6b3f144f3d3bf0f0469cfb3a5c9176b57b9d5";
    String theirsCommitID = "4d3a53a47d34f7d93d2b1af76d0b2d7250028397";
    String baseCommitID = "52814c72f70239f212e13178c2d1ef01e0e25f47";

    try {
      // process merge scenarios in repository
      Repository repository = GitService.cloneIfNotExists(REPO_PATH, GIT_URL);
      MergeScenario mergeScenario =
          new MergeScenario(
              REPO_NAME,
              REPO_PATH,
              SRC_PATH,
              mergeCommitID,
              oursCommitID,
              baseCommitID,
              theirsCommitID);
//      processOneMergeScenario(mergeScenario, repository);

      // process single file
      String folderPath = "src/test/resources/RenameMethod/renameinboth/";
      String fileRelativePath = "SourceRoot.java";
      processSingleFiles(folderPath, fileRelativePath);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Handle one single merge scenario
   *
   * @param mergeScenario
   * @param repository
   * @throws Exception
   */
  public static void processOneMergeScenario(MergeScenario mergeScenario, Repository repository)
      throws Exception {

    // 1. Collect diff java files and imported files between merge parent commit and base commit

    // source files collected to be parse later
    String collectedFilePath =
        DIFF_PATH + File.separator + mergeScenario.mergeCommitID + File.separator;

    SourceFileCollector collector =
        new SourceFileCollector(mergeScenario, repository, collectedFilePath);
    collector.setOnlyBothModified(true);
    collector.setCopyImportedFiles(true);
    collector.collectFilesForAllSides();

    // 2.1 Build ours/theirs graphs with collected files
    SemanticGraphBuilder oursBuilder =
        new SemanticGraphBuilder(mergeScenario, Side.OURS, collectedFilePath);
    SemanticGraphBuilder baseBuilder =
        new SemanticGraphBuilder(mergeScenario, Side.BASE, collectedFilePath);
    SemanticGraphBuilder theirsBuilder =
        new SemanticGraphBuilder(mergeScenario, Side.THEIRS, collectedFilePath);

    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.build();
    SemanticGraphExporter.printAsDot(oursGraph);

    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.build();

    //    SemanticGraphExporter.printAsDot(theirsGraph);

    // 2.2 Build base/merge graphs among ours/theirs files
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.build();

    // 3. Match node and merge the 3-way graphs
    String resultFolder =
        RESULT_PATH
            + File.separator
            + mergeScenario.mergeCommitID
            + File.separator
            + "intelliMerged";
    FilesManager.prepareResultFolder(resultFolder);
    ThreewayGraphMerger merger =
        new ThreewayGraphMerger(resultFolder, oursGraph, baseGraph, theirsGraph);
    merger.threewayMerge();
    // 4. Print the merged graph into code, keep the original format as possible

  }

  /** Given three versions of one single java file, convert and merge them with graph */
  private static void processSingleFiles(String folderPath, String fileRelativePath)
      throws Exception {
    SingleFileGraphBuilder builder = new SingleFileGraphBuilder(folderPath, fileRelativePath);
    //    Triple<
    //            Graph<SemanticNode, SemanticEdge>,
    //            Graph<SemanticNode, SemanticEdge>,
    //            Graph<SemanticNode, SemanticEdge>> graphs= builder.buildGraphsForAllSides();
    Graph<SemanticNode, SemanticEdge> oursGraph = builder.buildGraphForOneSide(Side.OURS);
    SemanticGraphExporter.printAsDot(oursGraph);
  }
}
