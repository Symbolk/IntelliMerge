package edu.pku.intellimerge.core.matcher;

import com.google.common.collect.BiMap;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.MatchingType;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import edu.pku.intellimerge.util.SimilarityAlg;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

public class MethodDeclMatcher {
  public MethodDeclMatcher() {}

  /**
   * Match methods that are unmatched for signature change, including many kinds of refactorings
   *
   * @param matching
   * @param unmatchedMethods1
   * @param unmatchedMethods2
   */
  public void matchMethods(
      TwowayMatching matching,
      List<SemanticNode> unmatchedMethods1,
      List<SemanticNode> unmatchedMethods2) {
    // use bipartite to match methods according to similarity
    Set<SemanticNode> partition1 = new HashSet<>();
    Set<SemanticNode> partition2 = new HashSet<>();
    DefaultUndirectedWeightedGraph<SemanticNode, DefaultWeightedEdge> biPartite =
        new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

    for (SemanticNode node1 : unmatchedMethods1) {
      for (SemanticNode node2 : unmatchedMethods2) {
        biPartite.addVertex(node1);
        partition1.add(node1);
        biPartite.addVertex(node2);
        partition2.add(node2);
        biPartite.addEdge(node1, node2);
        double similarity = SimilarityAlg.method((MethodDeclNode) node1, (MethodDeclNode) node2);
        biPartite.setEdgeWeight(node1, node2, similarity);
      }
    }
    // bipartite / to match most likely renamed methods
    // find the maximum /, one method cannot be renamed to two
    MaximumWeightBipartiteMatching matcher =
        new MaximumWeightBipartiteMatching(biPartite, partition1, partition2);
    Set<DefaultWeightedEdge> edges = matcher.getMatching().getEdges();
    // add one2oneMatchings found and remove from unmatched
    for (DefaultWeightedEdge edge : edges) {
      SemanticNode sourceNode = biPartite.getEdgeSource(edge);
      SemanticNode targetNode = biPartite.getEdgeTarget(edge);
      double confidence = biPartite.getEdgeWeight(edge);
      matching.unmatchedNodes1.get(NodeType.METHOD).remove(sourceNode);
      matching.unmatchedNodes2.get(NodeType.METHOD).remove(targetNode);
      matching.addMatchingEdge(sourceNode, targetNode, MatchingType.MATCHED_METHOD, confidence);
    }
  }

  /**
   * Match possible extracted methods from unmatched methods
   *
   * @param matching
   * @param unmatchedMethods
   */
  public void matchExtractMethod(TwowayMatching matching, List<SemanticNode> unmatchedMethods) {
    BiMap<SemanticNode, SemanticNode> reversedMatching = matching.one2oneMatchings.inverse();
    // Rule: one of callers in the / && original caller's parent==current parent&&union
    // context confidence > confidence before
    // The added method is called by an existing method in the same class
    Map<MethodDeclNode, List<MethodDeclNode>> alternates = new HashMap<>();
    for (SemanticNode possiblyAddedMethod : unmatchedMethods) {
      List<SemanticNode> callers = possiblyAddedMethod.incomingEdges.get(EdgeType.CALL_METHOD);
      List<MethodDeclNode> possiblyExtractedFromMethods = new ArrayList<>();
      for (SemanticNode caller : callers) {
        if (reversedMatching.containsKey(caller)
            && caller.getParent().equals(possiblyAddedMethod.getParent())
            && caller instanceof MethodDeclNode) {
          possiblyExtractedFromMethods.add((MethodDeclNode) caller);
        }
      }
      if (!possiblyExtractedFromMethods.isEmpty()) {
        alternates.put((MethodDeclNode) possiblyAddedMethod, possiblyExtractedFromMethods);
      }
    }
    // try to inline the new method to the caller
    // if the similarity improves, consider it as extracted from the caller
    for (Map.Entry<MethodDeclNode, List<MethodDeclNode>> alternate : alternates.entrySet()) {
      MethodDeclNode callee = alternate.getKey();
      List<MethodDeclNode> callers = alternate.getValue();
      for (MethodDeclNode caller : callers) {
        MethodDeclNode callerBase = (MethodDeclNode) reversedMatching.get(caller);
        double similarityBefore =
            SimilarityAlg.methodContext(caller.incomingEdges, callerBase.incomingEdges)
                + SimilarityAlg.methodContext(caller.outgoingEdges, callerBase.outgoingEdges);

        // TODO detect inline methods
        // combine the context edges
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
        // combine the body
        double similarityAfter =
            SimilarityAlg.methodContext(caller.incomingEdges, callerBase.incomingEdges)
                + SimilarityAlg.methodContext(caller.outgoingEdges, callerBase.outgoingEdges);

        // TODO improve precision
        if (similarityAfter >= similarityBefore) {
          matching.addMatchingEdge(
              callerBase, caller, MatchingType.EXTRACT_FROM_METHOD, similarityAfter);
          matching.addMatchingEdge(
              callerBase, callee, MatchingType.EXTRACT_TO_METHOD, similarityAfter);
          matching.unmatchedNodes2.get(NodeType.METHOD).remove(callee);
        }
      }
    }
  }
}
