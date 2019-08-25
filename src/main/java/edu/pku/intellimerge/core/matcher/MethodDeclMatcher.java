package edu.pku.intellimerge.core.matcher;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.RefactoringType;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import edu.pku.intellimerge.util.SimilarityAlg;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodDeclMatcher {
  private static final Double MIN_SIMI = 0.618D;
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
    // should be simple graph: no self-loops and no multiple edges
    DefaultUndirectedWeightedGraph<SemanticNode, DefaultWeightedEdge> biPartite =
        new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

    for (SemanticNode n1 : unmatchedMethods1) {
      for (SemanticNode n2 : unmatchedMethods2) {
        // filter improbable pairs by qualified name (including package, type and its name)
        if (SimilarityAlg.string(n1.getQualifiedName(), n2.getQualifiedName())
            > MIN_SIMI) {
          biPartite.addVertex(n1);
          partition1.add(n1);
          biPartite.addVertex(n2);
          partition2.add(n2);
          biPartite.addEdge(n1, n2);
          double similarity =
              SimilarityAlg.terminal((MethodDeclNode) n1, (MethodDeclNode) n2);
          biPartite.setEdgeWeight(n1, n2, similarity);
        }
      }
    }
    // bipartite / to match most likely renamed methods
    // find the maximum /, one terminal cannot be renamed to two
            biPartite.edgeSet();
    MaximumWeightBipartiteMatching matcher =
        new MaximumWeightBipartiteMatching(biPartite, partition1, partition2);
    Set<DefaultWeightedEdge> edges = matcher.getMatching().getEdges();
    // add one2oneMatchings found and remove from unmatched
    for (DefaultWeightedEdge edge : edges) {
      SemanticNode sourceNode = biPartite.getEdgeSource(edge);
      SemanticNode targetNode = biPartite.getEdgeTarget(edge);
      double confidence = biPartite.getEdgeWeight(edge);
      if (confidence >= MIN_SIMI) {
        matching.unmatchedNodes1.get(NodeType.METHOD).remove(sourceNode);
        matching.unmatchedNodes2.get(NodeType.METHOD).remove(targetNode);
        matching.markRefactoring(
            sourceNode, targetNode, RefactoringType.CHANGE_METHOD_SIGNATURE, confidence);
      }
    }
  }

  /**
   * Match possible extracted methods from unmatched methods
   *
   * @param matching * @param unmatchedMethods
   */
  public void matchExtractMethod(TwowayMatching matching, List<SemanticNode> unmatchedMethods) {
    //    BiMap<SemanticNode, SemanticNode> reversedMatching = matching.one2oneMatchings.inverse();
    //    // Rule: one of callers in the / && original caller's parent==current parent&&union
    //    // context confidence > confidence before
    //    // The added terminal is called by an existing terminal in the
    // same
    //    // class
    //    Map<MethodDeclNode, List<MethodDeclNode>> candidates = new HashMap<>();
    //    for (SemanticNode possiblyAddedMethod : unmatchedMethods) {
    //      List<SemanticNode> callers = possiblyAddedMethod.incomingEdges.get(EdgeType.CALL);
    //      List<MethodDeclNode> possiblyExtractedFromMethods = new ArrayList<>();
    //      for (SemanticNode caller : callers) {
    //        if (reversedMatching.containsKey(caller)
    //            && caller.getParent().equals(possiblyAddedMethod.getParent())
    //            && caller instanceof MethodDeclNode) {
    //          possiblyExtractedFromMethods.add((MethodDeclNode) caller);
    //        }
    //      }
    //      if (!possiblyExtractedFromMethods.isEmpty()) {
    //        candidates.put((MethodDeclNode) possiblyAddedMethod, possiblyExtractedFromMethods);
    //      }
    //    }
    //    // try to inline the new terminal to the caller
    //    // if the similarity improves, consider it as extracted from the caller
    //    for (Map.Entry<MethodDeclNode, List<MethodDeclNode>> alternate : candidates.entrySet()) {
    //      MethodDeclNode callee = alternate.getKey();
    //      List<MethodDeclNode> callers = alternate.getValue();
    //      for (MethodDeclNode caller : callers) {
    //        MethodDeclNode callerBase = (MethodDeclNode) reversedMatching.get(caller);
    //        double similarityBefore =
    //            SimilarityAlg.context(caller.incomingEdges, callerBase.incomingEdges)
    //                + SimilarityAlg.context(caller.outgoingEdges,
    // callerBase.outgoingEdges);
    //
    //        // TODO detect inline methods
    //        // combine the context edges
    //        Map<EdgeType, List<SemanticNode>> inUnion = new HashMap<>();
    //        inUnion.putAll(callee.incomingEdges);
    //        caller.incomingEdges.entrySet().stream()
    //            .forEach(entry -> inUnion.get(entry.getKey()).addAll(entry.getValue()));
    //        Map<EdgeType, List<SemanticNode>> outUnion = new HashMap<>();
    //        outUnion.putAll(callee.outgoingEdges);
    //        caller.outgoingEdges.entrySet().stream()
    //            .forEach(entry -> outUnion.get(entry.getKey()).addAll(entry.getValue()));
    //        caller.incomingEdges = inUnion;
    //        caller.outgoingEdges = outUnion;
    //        // combine the body
    //        double similarityAfter =
    //            SimilarityAlg.context(caller.incomingEdges, callerBase.incomingEdges)
    //                + SimilarityAlg.context(caller.outgoingEdges,
    // callerBase.outgoingEdges);
    //
    //        if (similarityAfter > similarityBefore) {
    //          matching.markRefactoring(
    //              callerBase, caller, RefactoringType.EXTRACT_FROM_METHOD, similarityAfter);
    //          matching.markRefactoring(
    //              callerBase, callee, RefactoringType.EXTRACT_TO_METHOD, similarityAfter);
    //          // also need to be added
    //          //          matching.unmatchedNodes2.get(NodeType.METHOD).remove(callee);
    //        }
    //      }
    //    }
  }
}
