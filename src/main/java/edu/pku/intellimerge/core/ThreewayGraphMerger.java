package edu.pku.intellimerge.core;

import edu.pku.intellimerge.model.Mapping;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.node.CompilationUnitNode;
import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Only the diff file/cu needs to be merged */
public class ThreewayGraphMerger {
  private Graph<SemanticNode, SemanticEdge> oursGraph;
  private Graph<SemanticNode, SemanticEdge> baseGraph;
  private Graph<SemanticNode, SemanticEdge> theirsGraph;
  private List<Mapping> mappings;

  public ThreewayGraphMerger(
      Graph<SemanticNode, SemanticEdge> oursGraph,
      Graph<SemanticNode, SemanticEdge> baseGraph,
      Graph<SemanticNode, SemanticEdge> theirsGraph) {
    this.oursGraph = oursGraph;
    this.baseGraph = baseGraph;
    this.theirsGraph = theirsGraph;
    this.mappings = new ArrayList<>();
  }

  /** Threeway map the CUs that need to merge */
  public void threewayMap() {
    // two way matching to get three way mapping
    TwowayGraphMatcher b2oMatcher = new TwowayGraphMatcher(baseGraph, oursGraph);
    TwowayGraphMatcher b2tMatcher = new TwowayGraphMatcher(baseGraph, theirsGraph);
    b2oMatcher.topDownMatch();
    b2oMatcher.bottomUpMatch();
    b2tMatcher.topDownMatch();
    b2tMatcher.bottomUpMatch();
    Map<SemanticNode, SemanticNode> b2oMatchings = b2oMatcher.matchings;
    Map<SemanticNode, SemanticNode> b2tMatchings = b2tMatcher.matchings;

    // collect CU mappings that need to merge
    for (SemanticNode node : baseGraph.vertexSet()) {
      if (node instanceof CompilationUnitNode) {
        CompilationUnitNode cu = (CompilationUnitNode) node;
        if (cu.needToMerge == true) {
          Mapping mapping =
              new Mapping(
                  Optional.ofNullable(b2oMatchings.getOrDefault(node, null)),
                  Optional.of(node),
                  Optional.ofNullable(b2tMatchings.getOrDefault(node, null)));
          mappings.add(mapping);
        }
      }
    }
  }

  /** Merge CUs according to the mappings */
  public void threewayMerge() {
    threewayMap();
    for (Mapping mapping : mappings) {
      System.out.println(mapping);
    }
    Graph<SemanticNode, SemanticEdge> mergedGraph = baseGraph;
    //      System.out.println(Graphs.addGraph(mergedGraph, oursGraph));
    //    System.out.println(Graphs.addGraph(mergedGraph, theirsGraph));
    //    SemanticGraphExporter.printAsDot(mergedGraph);

  }
}
