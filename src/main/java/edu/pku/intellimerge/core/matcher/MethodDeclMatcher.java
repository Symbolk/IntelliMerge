package edu.pku.intellimerge.core.matcher;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import edu.pku.intellimerge.util.SimilarityAlg;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class MethodDeclMatcher {
  // build a bipartite to match
  private Set<MethodDeclNode> partition1 = new HashSet<>();
  private Set<MethodDeclNode> partition2 = new HashSet<>();
  private DefaultUndirectedWeightedGraph<MethodDeclNode, DefaultWeightedEdge> biPartite;

  public MethodDeclMatcher() {
    this.biPartite = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
  }

  // check field and method signature change first
  public void matchChangeMethodSignature(
      Map<SemanticNode, SemanticNode> matchings,
      List<MethodDeclNode> methodDeclNodes1,
      List<MethodDeclNode> methodDeclNodes2) {
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

      methodDeclNodes1.remove(sourceNode);
      methodDeclNodes2.remove(targetNode);
      matchings.put(sourceNode, targetNode);
    }
  }

  private Graph<SemanticNode, DefaultWeightedEdge> initBiGraph() {
    return GraphTypeBuilder.<SemanticNode, DefaultWeightedEdge>undirected()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false) // recursion
        .edgeClass(DefaultWeightedEdge.class)
        .weighted(true)
        .buildGraph();
  }

  public void matchExtractMethod(
      Map<SemanticNode, SemanticNode> matchings,
      ArrayList<MethodDeclNode> methodDeclNodes1,
      ArrayList<MethodDeclNode> methodDeclNodes2) {
    Map<SemanticNode, SemanticNode> reversedMatchings =
        matchings
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    // Rule: one of callers in the matching && original caller's parent==current parent&&union
    // context confidence > confidence before
    Map<MethodDeclNode, List<MethodDeclNode>> alternates = new HashMap<>();
    for (MethodDeclNode possiblyAddedMethod : methodDeclNodes2) {
      List<SemanticNode> callers = possiblyAddedMethod.incomingEdges.get(EdgeType.CALL_METHOD);
      List<MethodDeclNode> possiblyExtractedFromMethods = new ArrayList<>();
      for (SemanticNode caller : callers) {
        if (reversedMatchings.containsKey(caller)
            && caller.getParent().equals(possiblyAddedMethod.getParent())
            && caller instanceof MethodDeclNode) {
          possiblyExtractedFromMethods.add((MethodDeclNode) caller);
        }
      }
      alternates.put(possiblyAddedMethod, possiblyExtractedFromMethods);
    }
    alternates.size();
    // try union the context of added method to the caller
    for (Map.Entry<MethodDeclNode, List<MethodDeclNode>> entry : alternates.entrySet()) {
      MethodDeclNode callee = entry.getKey();
      List<MethodDeclNode> callers = entry.getValue();
      for (MethodDeclNode caller : callers) {
        Map<EdgeType, List<SemanticNode>> inUnion = new HashMap<>();
        inUnion.putAll(callee.incomingEdges);
        inUnion.putAll(caller.incomingEdges);
        Map<EdgeType, List<SemanticNode>> outUnion = new HashMap<>();
        outUnion.putAll(callee.outgoingEdges);
        outUnion.putAll(caller.outgoingEdges);
      }
    }
    // if the context similarity improves, it is extracted from the caller
  }
}
