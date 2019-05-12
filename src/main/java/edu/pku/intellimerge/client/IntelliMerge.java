package edu.pku.intellimerge.client;

import edu.pku.intellimerge.core.SemanticGraphBuilder2;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class IntelliMerge {
  private static final Logger logger = LoggerFactory.getLogger(IntelliMerge.class);

  private String folderPath;
  private List<String> fileRelativePaths;
  private String resultFolderPath;

  public IntelliMerge(String folderPath, List<String> fileRelativePaths, String resultFolderPath) {
    this.folderPath = folderPath;
    this.fileRelativePaths = fileRelativePaths;
    this.resultFolderPath = resultFolderPath;
  }

  public static void main(String[] args) {
    // config the logger
    PropertyConfigurator.configure("log4j.properties");

    String folderPath = "D:\\github\\test2";
    List<String> filePaths = new ArrayList<>();
//    filePaths.add("AbstractDateDeserializer.java");
    filePaths.add("SourceRoot.java");
    String resultPath = folderPath + File.separator + Side.INTELLI.asString();
    IntelliMerge intelliMerge = new IntelliMerge(folderPath, filePaths, resultPath);
    intelliMerge.merge();
  }

  /** Merge a given list of files */
  public void merge() {
    ExecutorService executorService = Executors.newFixedThreadPool(3);

    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.OURS, folderPath, fileRelativePaths));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.BASE, folderPath, fileRelativePaths));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(Side.THEIRS, folderPath, fileRelativePaths));

    try {
      Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
      Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
      Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

      logger.info("Graph building done.");
      executorService.shutdown();

      Utils.prepareDir(resultFolderPath);
      ThreewayGraphMerger merger =
          new ThreewayGraphMerger(resultFolderPath, oursGraph, baseGraph, theirsGraph);
      merger.threewayMap();
      logger.info("Graph mapping done.");

      merger.threewayMerge();
      logger.info("Graph merging done.");

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }
}
