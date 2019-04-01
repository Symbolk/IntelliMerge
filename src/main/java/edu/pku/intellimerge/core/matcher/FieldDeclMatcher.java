package edu.pku.intellimerge.core.matcher;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.RefactoringType;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.FieldDeclNode;
import edu.pku.intellimerge.util.SimilarityAlg;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FieldDeclMatcher {

  /**
   * Match fields that are unmatched for signature change
   *
   * @param matching
   * @param unmatchedFields1
   * @param unmatchedFields2
   */
  public void matchFields(
      TwowayMatching matching,
      List<SemanticNode> unmatchedFields1,
      List<SemanticNode> unmatchedFields2) {
    // use bipartite to match methods according to similarity
    Set<SemanticNode> partition1 = new HashSet<>();
    Set<SemanticNode> partition2 = new HashSet<>();
    DefaultUndirectedWeightedGraph<SemanticNode, DefaultWeightedEdge> biPartite =
        new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

    for (SemanticNode node1 : unmatchedFields1) {
      for (SemanticNode node2 : unmatchedFields2) {
        biPartite.addVertex(node1);
        partition1.add(node1);
        biPartite.addVertex(node2);
        partition2.add(node2);
        biPartite.addEdge(node1, node2);
        double similarity = SimilarityAlg.field((FieldDeclNode) node1, (FieldDeclNode) node2);
        biPartite.setEdgeWeight(node1, node2, similarity);
      }
    }
    // bipartite matching to match most likely renamed methods
    // find the maximum matching, one terminalNodeSimilarity cannot be renamed to two
    MaximumWeightBipartiteMatching matcher =
        new MaximumWeightBipartiteMatching(biPartite, partition1, partition2);
    Set<DefaultWeightedEdge> edges = matcher.getMatching().getEdges();
    // add one2oneMatchings found and remove from unmatched
    biPartite.edgeSet();
    for (DefaultWeightedEdge edge : edges) {
      SemanticNode sourceNode = biPartite.getEdgeSource(edge);
      SemanticNode targetNode = biPartite.getEdgeTarget(edge);
      double confidence = biPartite.getEdgeWeight(edge);
      if (confidence >= 0.618) {
        unmatchedFields1.remove(sourceNode);
        unmatchedFields2.remove(targetNode);
        matching.markRefactoring(
            sourceNode, targetNode, RefactoringType.CHANGE_FIELD_SIGNATURE, confidence);
      }
    }
  }
}
