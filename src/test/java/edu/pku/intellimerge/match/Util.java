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
  public static ThreewayGraphMerger matchGraphs(String targetDir, String resultDir)
      throws Exception {
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
   * Build and match graphs in two ways, usually base and ours/theirs
   *
   * @param targetDir
   * @return
   */
  public static TwowayMatching matchTwoGraphs(String targetDir, Side side1, Side side2) {
    SemanticGraphBuilder2 builder1 = new SemanticGraphBuilder2(null, side1, targetDir, false);
    SemanticGraphBuilder2 builder2 = new SemanticGraphBuilder2(null, side2, targetDir, false);

    Graph<SemanticNode, SemanticEdge> otherGraph = builder1.build();
    Graph<SemanticNode, SemanticEdge> baseGraph = builder2.build();

    logger.info("Building graph done for {}", targetDir);
    TwowayGraphMatcher matcher = new TwowayGraphMatcher(baseGraph, otherGraph);
    matcher.topDownMatch();
    matcher.bottomUpMatch();
    logger.info("Matching done for {}", targetDir);

    return matcher.matching;
  }
}
