package edu.pku.intellimerge.client;

import edu.pku.intellimerge.core.ThreewayGraphMapper;
import edu.pku.intellimerge.core.SemanticGraphBuilder;
import edu.pku.intellimerge.core.SemanticGraphExporter;
import edu.pku.intellimerge.core.SourceFileCollector;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.Side;
import edu.pku.intellimerge.util.GitService;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jgit.lib.Repository;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIClient {

  private static final Logger logger = LoggerFactory.getLogger(APIClient.class);

  private static final String REPO_NAME = "IntelliMerge";
  private static final String REPO_PATH = "D:\\github\\repos\\" + REPO_NAME;
  private static final String GIT_URL = "https://github.com/javaparser/javaparser.git";
  private static final String SRC_PATH = "/src/main/java/";
  //  private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";
  private static final String DIFF_PATH = "D:\\github\\diffs\\" + REPO_NAME;

  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");
    //      BasicConfigurator.configure();

    String mergeCommitID = "3ceb2c9453198631adf0f49afc10ece85ccfc295";
    String oursCommitID = "3ab30428c5c85039cafdf380627436a80386b353";
    String theirsCommitID = "3ae7bb49d9331107b941a72c97b84042eebf9c7e";
    String baseCommitID = "003eba5af74699132eb15343c9cb39cab51eb85c";

    try {
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
      handleOneMergeScenario(mergeScenario, repository);

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
  public static void handleOneMergeScenario(MergeScenario mergeScenario, Repository repository)
      throws Exception {

    // 1. Collect diff java files and imported files between merge parent commit and base commit

    // source files collected to be parse later
    String collectedFilePath = DIFF_PATH + "/" + mergeScenario.mergeCommitID + "/";

    SourceFileCollector collector =
        new SourceFileCollector(mergeScenario, repository, collectedFilePath);
    //      collector.collectFilesForAllSides();

    // 2.1 Build ours/theirs graphs with collected files
    SemanticGraphBuilder builder = new SemanticGraphBuilder(mergeScenario, collectedFilePath);

    Graph<SemanticNode, SemanticEdge> oursGraph = builder.buildGraphForOneSide(Side.OURS);
    Graph<SemanticNode, SemanticEdge> theirsGraph = builder.buildGraphForOneSide(Side.THEIRS);

    SemanticGraphExporter.printAsDot(oursGraph);
    SemanticGraphExporter.printAsDot(theirsGraph);

    // 2.2 Build base/merge graphs among ours/theirs files
    Graph<SemanticNode, SemanticEdge> baseGraph = builder.buildGraphForOneSide(Side.BASE);

    // 3. Match nodes and merge the 3-way graphs
    Graph<SemanticNode, SemanticEdge> mergedGraph = baseGraph;
    //      System.out.println(Graphs.addGraph(mergedGraph, oursGraph));
//    System.out.println(Graphs.addGraph(mergedGraph, theirsGraph));
//    SemanticGraphExporter.printAsDot(mergedGraph);
    ThreewayGraphMapper mapper = new ThreewayGraphMapper(oursGraph, baseGraph, theirsGraph);
    // 4. Print the merged graph into code, keep the original format as possible


  }
}
