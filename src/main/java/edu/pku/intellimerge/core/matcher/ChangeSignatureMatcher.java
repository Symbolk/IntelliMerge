package edu.pku.intellimerge.core.matcher;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.node.FieldDeclNode;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import edu.pku.intellimerge.util.SimilarityAlg;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.*;

public class ChangeSignatureMatcher {
  // check field and method signature change first
  public static void matchChangeFieldSignature(List<FieldDeclNode> fieldDeclNodes) {}

  public static void matchChangeMethodSignature(
      List<Map<SemanticNode, SemanticNode>> mappings,
      List<MethodDeclNode> methodDeclNodes1,
      List<MethodDeclNode> methodDeclNodes2) {
    // build a bipartite to match
    Set<MethodDeclNode> partition1 = new HashSet<>();
    Set<MethodDeclNode> partition2 = new HashSet<>();
    DefaultUndirectedWeightedGraph<MethodDeclNode, DefaultWeightedEdge> biPartite =
        new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
    for (MethodDeclNode node1 : methodDeclNodes1) {
      for (MethodDeclNode node2 : methodDeclNodes2) {
        biPartite.addVertex(node1);
        partition1.add(node1);
        biPartite.addVertex(node2);
        partition2.add(node2);
        biPartite.addEdge(node1, node2);
        double similarity = SimilarityAlg.method(node1, node2);
        biPartite.setEdgeWeight(node1, node2, similarity);
      }
    }
    // bipartite matching to match most likely renamed methods
    // find the maximum matching, one method cannot be renamed to two
    MaximumWeightBipartiteMatching matcher =
        new MaximumWeightBipartiteMatching(biPartite, partition1, partition2);
    Set<DefaultWeightedEdge> edges = matcher.getMatching().getEdges();
    // add matchings found and remove from unmatched
    for (DefaultWeightedEdge edge : edges) {
      SemanticNode sourceNode = biPartite.getEdgeSource(edge);
      SemanticNode targetNode = biPartite.getEdgeTarget(edge);

      Map<SemanticNode, SemanticNode> mapping = new HashMap<>();
      methodDeclNodes1.remove(sourceNode);
      methodDeclNodes1.remove(targetNode);
      mapping.put(sourceNode, targetNode);
      mappings.add(mapping);
    }
  }

  private static Graph<SemanticNode, DefaultWeightedEdge> initBiGraph() {
    return GraphTypeBuilder.<SemanticNode, DefaultWeightedEdge>undirected()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false) // recursion
        .edgeClass(DefaultWeightedEdge.class)
        .weighted(true)
        .buildGraph();
  }
}
