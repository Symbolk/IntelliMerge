package edu.pku.intellimerge.model.mapping;

import edu.pku.intellimerge.model.SemanticNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TwowayMatching {
  // 2 kinds of matchings: match by unchanged signature & signature changed but match by their roles
  public Map<SemanticNode, SemanticNode> one2oneMatchings; // confidence: 1
  //  private DefaultUndirectedWeightedGraph<SemanticNode, MatchingEdge> biPartite;
  public Graph<SemanticNode, MatchingEdge> biPartite; // may contain refactoring matchings
  // store the two way nodes matchings relationships in a bipartite
  private Set<SemanticNode> partition1; // usually base
  private Set<SemanticNode> partition2; // usually others

  public TwowayMatching() {
    this.one2oneMatchings = new HashMap<>();
    this.biPartite = initBipartite();
    //        this.biPartite = new DefaultDirectedWeightedGraph<>(MatchingEdge.class);
    this.partition1 = new HashSet<>();
    this.partition2 = new HashSet<>();
  }

  public void addMatchingEdge(
      SemanticNode node1, SemanticNode node2, String label, double confidence) {
    partition1.add(node1);
    partition2.add(node2);
    biPartite.addVertex(node1);
    biPartite.addVertex(node2);
    biPartite.addEdge(node1, node2);
    biPartite.setEdgeWeight(node1, node2, confidence);
    biPartite.getEdge(node1, node2).setLabel(label);
  }

  public void getRefactoredOne2OneMatching() {
    for (SemanticNode node1 : partition1) {
      if (biPartite.edgesOf(node1).size() == 1) {
        for (MatchingEdge edge : biPartite.edgesOf(node1)) {
          one2oneMatchings.put(biPartite.getEdgeSource(edge), biPartite.getEdgeTarget(edge));
        }
      }
    }
  }
  /**
   * Init the biparitie graph, directed from partition1 to partition2
   *
   * @return
   */
  private Graph<SemanticNode, MatchingEdge> initBipartite() {
    return GraphTypeBuilder.<SemanticNode, MatchingEdge>directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false) // recursion
        .edgeClass(MatchingEdge.class)
        .weighted(true)
        .buildGraph();
  }
}
