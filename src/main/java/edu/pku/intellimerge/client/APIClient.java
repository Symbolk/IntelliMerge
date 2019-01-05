package edu.pku.intellimerge.client;

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
import org.jgrapht.Graphs;
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

      // 1. Get changed java files between parent commit and merge base commit

      String diffPath = DIFF_PATH + "/" + mergeCommitID + "/";
      MergeScenario mergeScenario =
          new MergeScenario(mergeCommitID, oursCommitID, baseCommitID, theirsCommitID);
      SourceFileCollector sourceFileCollector =
          new SourceFileCollector(
              repository, REPO_NAME, REPO_PATH, SRC_PATH, diffPath, mergeScenario);
      sourceFileCollector.collect();
      // 2.1 Build ours/theirs graphs among changed files & their imported files (one hop)
      Graph<SemanticNode, SemanticEdge> oursGraph =
          buildGraph(repository, oursCommitID, diffPath, Side.OURS);
      Graph<SemanticNode, SemanticEdge> theirsGraph =
          buildGraph(repository, theirsCommitID, diffPath, Side.THEIRS);

      printGraph(oursGraph);
      printGraph(theirsGraph);

      // 2.2 Build base/merge graphs among ours/theirs files

      Graph<SemanticNode, SemanticEdge> baseGraph =
          buildGraph(repository, baseCommitID, diffPath, Side.BASE);

      // 3. Match nodes and merge the 3-way graphs
      Graph<SemanticNode, SemanticEdge> mergedGraph = baseGraph;
      //      System.out.println(Graphs.addGraph(mergedGraph, oursGraph));
      System.out.println(Graphs.addGraph(mergedGraph, theirsGraph));
      printGraph(mergedGraph);
      // 4. Prettyprint the merged graph into code

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Print the graph fo debugging
   *
   * @param graph
   */
  private static void printGraph(Graph<SemanticNode, SemanticEdge> graph) {

    //    for (SemanticNode node : graph.vertexSet()) {
    //      System.out.println(node);
    //    }
    //    System.out.println("------------------------------");
    //    for (SemanticEdge edge : graph.edgeSet()) {
    //      SemanticNode source = graph.getEdgeSource(edge);
    //      SemanticNode target = graph.getEdgeTarget(edge);
    //      System.out.println(
    //          source.getDisplayName() + " " + edge.getEdgeType() + " " + target.getDisplayName());
    //    }
    //    System.out.println("------------------------------");
    System.out.println(SemanticGraphExporter.exportAsDot(graph));
  }
  /**
   * Build the SemanticGraph for one side
   *
   * @param repository
   * @param commitID
   * @param side
   * @return
   * @throws Exception
   */
  private static Graph<SemanticNode, SemanticEdge> buildGraph(
      Repository repository, String commitID, String diffPath, Side side) throws Exception {
    String sideDiffPath = diffPath + side.toString().toLowerCase() + "/";

    Graph<SemanticNode, SemanticEdge> graph =
        //        SemanticGraphBuilder.initGraph();
        SemanticGraphBuilder.buildForRepo(sideDiffPath, REPO_PATH + SRC_PATH);
    if (graph == null) {
      logger.error(side.toString() + " graph is null!");
    }
    return graph;
  }
}
