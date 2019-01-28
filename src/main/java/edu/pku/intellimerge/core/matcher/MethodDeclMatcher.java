package edu.pku.intellimerge.core.matcher;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import edu.pku.intellimerge.util.SimilarityAlg;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;
import java.util.stream.Collectors;

public class MethodDeclMatcher {
  // use bipartite to match methods
  private Set<SemanticNode> partition1 = new HashSet<>();
  private Set<SemanticNode> partition2 = new HashSet<>();
  private DefaultUndirectedWeightedGraph<SemanticNode, DefaultWeightedEdge> biPartite;

  public MethodDeclMatcher() {
    this.biPartite = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
  }

  // check field and method signature change first
  public void matchChangeMethodSignature(
      TwowayMatching matchings,
      List<SemanticNode> methodDeclNodes1,
      List<SemanticNode> methodDeclNodes2) {
    for (SemanticNode node1 : methodDeclNodes1) {
      for (SemanticNode node2 : methodDeclNodes2) {
        biPartite.addVertex(node1);
        partition1.add(node1);
        biPartite.addVertex(node2);
        partition2.add(node2);
        biPartite.addEdge(node1, node2);
        double similarity = SimilarityAlg.method((MethodDeclNode) node1, (MethodDeclNode) node2);
        biPartite.setEdgeWeight(node1, node2, similarity);
      }
    }
    // bipartite matchings to match most likely renamed methods
    // find the maximum matchings, one method cannot be renamed to two
    MaximumWeightBipartiteMatching matcher =
        new MaximumWeightBipartiteMatching(biPartite, partition1, partition2);
    Set<DefaultWeightedEdge> edges = matcher.getMatching().getEdges();
    // add one2oneMatchings found and remove from unmatched
    biPartite.edgeSet();
    for (DefaultWeightedEdge edge : edges) {
      SemanticNode sourceNode = biPartite.getEdgeSource(edge);
      SemanticNode targetNode = biPartite.getEdgeTarget(edge);
      double confidence = biPartite.getEdgeWeight(edge);
      methodDeclNodes1.remove(sourceNode);
      methodDeclNodes2.remove(targetNode);
      matchings.addMatchingEdge(sourceNode, targetNode, "change_signature", confidence);
    }
  }

  public void matchExtractMethod(
      TwowayMatching matchings,
      List<SemanticNode> methodDeclNodes1,
      List<SemanticNode> methodDeclNodes2) {
    Map<SemanticNode, SemanticNode> reversedMatchings =
        matchings
            .one2oneMatchings
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    // Rule: one of callers in the matchings && original caller's parent==current parent&&union
    // context confidence > confidence before
    Map<MethodDeclNode, List<MethodDeclNode>> alternates = new HashMap<>();
    for (SemanticNode possiblyAddedMethod : methodDeclNodes2) {
      List<SemanticNode> callers = possiblyAddedMethod.incomingEdges.get(EdgeType.CALL_METHOD);
      List<MethodDeclNode> possiblyExtractedFromMethods = new ArrayList<>();
      for (SemanticNode caller : callers) {
        if (reversedMatchings.containsKey(caller)
            && caller.getParent().equals(possiblyAddedMethod.getParent())
            && caller instanceof MethodDeclNode) {
          possiblyExtractedFromMethods.add((MethodDeclNode) caller);
        }
      }
      alternates.put((MethodDeclNode) possiblyAddedMethod, possiblyExtractedFromMethods);
    }
    // try to inline the new method to the caller
    // if the similarity improves, we think it is extracted from the caller
    for (Map.Entry<MethodDeclNode, List<MethodDeclNode>> alternate : alternates.entrySet()) {
      MethodDeclNode callee = alternate.getKey();
      List<MethodDeclNode> callers = alternate.getValue();
      for (MethodDeclNode caller : callers) {
        MethodDeclNode callerBase = (MethodDeclNode) reversedMatchings.get(caller);
        double similarityBefore = SimilarityAlg.method(caller, callerBase);

        // TODO inline method body, i.e. revert extract
        Map<EdgeType, List<SemanticNode>> inUnion = new HashMap<>();
        inUnion.putAll(callee.incomingEdges);
        caller
            .incomingEdges
            .entrySet()
            .stream()
            .forEach(entry -> inUnion.get(entry.getKey()).addAll(entry.getValue()));
        Map<EdgeType, List<SemanticNode>> outUnion = new HashMap<>();
        outUnion.putAll(callee.outgoingEdges);
        caller
            .outgoingEdges
            .entrySet()
            .stream()
            .forEach(entry -> outUnion.get(entry.getKey()).addAll(entry.getValue()));
        caller.incomingEdges = inUnion;
        caller.outgoingEdges = outUnion;
        double similarityAfter = SimilarityAlg.method(caller, callerBase);
        if (similarityAfter > similarityBefore) {
          matchings.addMatchingEdge(callerBase, callee, "extract_method", similarityAfter);
        }
      }
    }
  }
}
