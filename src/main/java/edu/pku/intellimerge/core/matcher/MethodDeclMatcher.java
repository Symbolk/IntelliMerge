package edu.pku.intellimerge.core.matcher;

import com.google.common.collect.BiMap;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.RefactoringType;
import edu.pku.intellimerge.model.mapping.NodeContext;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import edu.pku.intellimerge.util.SimilarityAlg;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;
import java.util.stream.Collectors;

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
        if (SimilarityAlg.string(n1.getQualifiedName(), n2.getQualifiedName()) > MIN_SIMI) {
          biPartite.addVertex(n1);
          partition1.add(n1);
          biPartite.addVertex(n2);
          partition2.add(n2);
          biPartite.addEdge(n1, n2);
          double similarity = SimilarityAlg.terminal((MethodDeclNode) n1, (MethodDeclNode) n2);
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
    BiMap<SemanticNode, SemanticNode> reversedMatching = matching.one2oneMatchings.inverse();
    // union the context of matched callers
    // if the union context confidence > confidence before, consider it as an extraction
    Map<SemanticNode, List<SemanticNode>> candidates = new HashMap<>();
    for (SemanticNode possiblyAddedMethod : unmatchedMethods) {
      List<SemanticNode> callers =
          possiblyAddedMethod.context.getIncomingEdges().stream()
              .filter(edge -> edge.getEdgeType().equals(EdgeType.CALL))
              .map(SemanticEdge::getSource)
              .collect(Collectors.toList());
      // methods or constructors
      List<SemanticNode> extractionSources = new ArrayList<>();
      for (SemanticNode caller : callers) {
        if (reversedMatching.containsKey(caller)
            && caller.getParent().equals(possiblyAddedMethod.getParent())) {
          extractionSources.add(caller);
        }
      }
      if (!extractionSources.isEmpty()) {
        candidates.put(possiblyAddedMethod, extractionSources);
      }
    }

    for (Map.Entry<SemanticNode, List<SemanticNode>> alternate : candidates.entrySet()) {
      SemanticNode callee = alternate.getKey();
      List<SemanticNode> callers = alternate.getValue();
      for (SemanticNode caller : callers) {
        SemanticNode callerBase = reversedMatching.get(caller);
        NodeContext callerContext = caller.context;
        NodeContext callerBaseContext = callerBase.context;
        double similarityBefore = SimilarityAlg.context(callerContext, callerBaseContext);

        // union the context of the caller and callee
        NodeContext callerUnionContext = callerContext.join(callee.context);
        double similarityAfter = SimilarityAlg.context(callerUnionContext, callerBaseContext);

        if (similarityAfter > similarityBefore) {
          matching.markRefactoring(
              callerBase, callee, RefactoringType.EXTRACT_METHOD, similarityAfter);
          //          matching.unmatchedNodes2.get(NodeType.METHOD).remove(callee);
        }
      }
    }
  }

  /**
   * Match inline methods from unmatched nodes
   *
   * @param matching
   * @param unmatchedMethods
   */
  public void matchInlineMethod(TwowayMatching matching, List<SemanticNode> unmatchedMethods) {
    BiMap<SemanticNode, SemanticNode> one2oneMatchings = matching.one2oneMatchings;
    Map<SemanticNode, List<SemanticNode>> candidates = new HashMap<>();
    for (SemanticNode possiblyAddedMethod : unmatchedMethods) {
      List<SemanticNode> callers =
          possiblyAddedMethod.context.getIncomingEdges().stream()
              .filter(edge -> edge.getEdgeType().equals(EdgeType.CALL))
              .map(SemanticEdge::getSource)
              .collect(Collectors.toList());
      // methods or constructors
      List<SemanticNode> inlineTargets = new ArrayList<>();
      for (SemanticNode caller : callers) {
        if (one2oneMatchings.containsKey(caller)
            && caller.getParent().equals(possiblyAddedMethod.getParent())) {
          inlineTargets.add(caller);
        }
      }
      if (!inlineTargets.isEmpty()) {
        candidates.put(possiblyAddedMethod, inlineTargets);
      }
    }

    for (Map.Entry<SemanticNode, List<SemanticNode>> alternate : candidates.entrySet()) {
      SemanticNode callee = alternate.getKey();
      List<SemanticNode> callers = alternate.getValue();
      for (SemanticNode caller : callers) {
        SemanticNode callerOthers = one2oneMatchings.get(caller);
        NodeContext contextBase = caller.context;
        NodeContext contextOthers = callerOthers.context;
        double similarityBefore = SimilarityAlg.context(contextBase, contextOthers);

        contextBase.setOutgoingEdges(updateOutgoingEdges(contextBase, callee));
        double similarityAfter = SimilarityAlg.context(contextBase, contextOthers);

        if (similarityAfter > similarityBefore) {
          matching.markRefactoring(
              callerOthers, callee, RefactoringType.INLINE_METHOD, similarityAfter);
        }
      }
    }
  }

  /**
   * @param contextBase
   * @param callee
   * @return
   */
  private Set<SemanticEdge> updateOutgoingEdges(NodeContext contextBase, SemanticNode callee) {
    Set<SemanticEdge> edges = new LinkedHashSet<>(contextBase.getOutgoingEdges());
    // remove the method call to the possible inlined method
    for (SemanticEdge edge : contextBase.getOutgoingEdges()) {
      if (edge.getEdgeType().equals(EdgeType.CALL)
          && edge.getTarget().getDisplayName().equals(callee.getDisplayName())) {
        edges.remove(edge);
      }
    }
    // union the outgoing edges of the possible inlined method
    edges.addAll(callee.context.getOutgoingEdges());
    return edges;
  }
}
