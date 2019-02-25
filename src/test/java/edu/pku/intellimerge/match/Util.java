package edu.pku.intellimerge.match;

import edu.pku.intellimerge.core.SemanticGraphBuilder2;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.core.TwowayGraphMatcher;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.util.FilesManager;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Util {
  private static Logger logger = LoggerFactory.getLogger(Util.class);

  /**
   * Build and match graphs in three ways
   *
   * @param targetDir
   * @param resultDir
   * @return
   * @throws Exception
   */
  public static ThreewayGraphMerger matchGraphsThreeway(String targetDir, String resultDir) {
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
    merger.threewayMap();
    logger.info("Matching done for {}", targetDir);

    return merger;
  }

  /**
   * Match and merge code in three way
   *
   * @param targetDir
   * @param resultDir
   */
  public static List<String> mergeGraphsThreeway(String targetDir, String resultDir) {
    ThreewayGraphMerger merger = matchGraphsThreeway(targetDir, resultDir);
    List<String> mergedFilePaths = merger.threewayMerge();
    logger.info("Merging done for {}", targetDir);
    return mergedFilePaths;
  }
  /**
   * Build and match graphs in two ways, usually base and ours/theirs
   *
   * @param targetDir
   * @return
   */
  public static TwowayMatching matchGraphsTwoway(String targetDir, Side side1, Side side2) {
    SemanticGraphBuilder2 builder1 = new SemanticGraphBuilder2(null, side1, targetDir, false);
    SemanticGraphBuilder2 builder2 = new SemanticGraphBuilder2(null, side2, targetDir, false);

    Graph<SemanticNode, SemanticEdge> graph1 = builder1.build();
    Graph<SemanticNode, SemanticEdge> graph2 = builder2.build();

    logger.info("Building graph done for {}", targetDir);
    TwowayGraphMatcher matcher = new TwowayGraphMatcher(graph1, graph2);
    matcher.topDownMatch();
    matcher.bottomUpMatch();
    logger.info("Matching done for {}", targetDir);

    return matcher.matching;
  }
}
