package edu.pku.intellimerge.match;

import com.google.common.base.Stopwatch;
import edu.pku.intellimerge.core.SemanticGraphBuilder2;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.core.TwowayGraphMatcher;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.util.Utils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    String targetDirName = Utils.getDirSimpleName(targetDir);
    try {
      Stopwatch stopwatch = Stopwatch.createStarted();
      boolean hasMultipleModule = false;
      ExecutorService executorService = Executors.newFixedThreadPool(3);

      Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
          executorService.submit(
              new SemanticGraphBuilder2(null, Side.OURS, targetDir, hasMultipleModule));
      Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
          executorService.submit(
              new SemanticGraphBuilder2(null, Side.BASE, targetDir, hasMultipleModule));
      Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
          executorService.submit(
              new SemanticGraphBuilder2(null, Side.THEIRS, targetDir, hasMultipleModule));
      Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
      Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
      Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

      stopwatch.stop();
      executorService.shutdown();
      logger.info(
          "Building graph done for {} within {}ms.",
          targetDirName,
          stopwatch.elapsed(TimeUnit.MILLISECONDS));

      logger.info("Building graph done for {}", targetDir);

      Utils.prepareDir(resultDir);
      ThreewayGraphMerger merger =
          new ThreewayGraphMerger(resultDir, oursGraph, baseGraph, theirsGraph);
      merger.threewayMap();
      logger.info("Matching done for {}", targetDir);
      return merger;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
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
    String targetDirName = Utils.getDirSimpleName(targetDir);

    try {
      Stopwatch stopwatch = Stopwatch.createStarted();
      boolean hasMultipleModule = false;
      ExecutorService executorService = Executors.newFixedThreadPool(2);

      Future<Graph<SemanticNode, SemanticEdge>> builder1 =
          executorService.submit(
              new SemanticGraphBuilder2(null, side1, targetDir, hasMultipleModule));
      Future<Graph<SemanticNode, SemanticEdge>> builder2 =
          executorService.submit(
              new SemanticGraphBuilder2(null, side2, targetDir, hasMultipleModule));
      Graph<SemanticNode, SemanticEdge> graph1 = builder1.get();
      Graph<SemanticNode, SemanticEdge> graph2 = builder2.get();

      stopwatch.stop();
      executorService.shutdown();

      logger.info(
          "Building graph done for {} within {}ms.",
          targetDirName,
          stopwatch.elapsed(TimeUnit.MILLISECONDS));

      logger.info("Building graph done for {}", targetDir);
      TwowayGraphMatcher matcher = new TwowayGraphMatcher(graph1, graph2);
      matcher.topDownMatch();
      matcher.bottomUpMatch();
      logger.info("Matching done for {}", targetDir);

      return matcher.matching;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
