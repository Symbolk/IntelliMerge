package edu.pku.intellimerge.core.matcher;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.RefactoringType;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import edu.pku.intellimerge.model.node.TypeDeclNode;
import edu.pku.intellimerge.util.SimilarityAlg;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TypeDeclMatcher {
    /**
     * Match methods that are unmatched for signature change, including many kinds of refactorings
     *
     * @param matching
     * @param unmatched1
     * @param unmatched2
     */
    public void matchClass(
            TwowayMatching matching,
            List<SemanticNode> unmatched1,
            List<SemanticNode> unmatched2) {
        // use bipartite to match methods according to similarity
        Set<SemanticNode> partition1 = new HashSet<>();
        Set<SemanticNode> partition2 = new HashSet<>();
        // should be simple graph: no self-loops and no multiple edges
        DefaultUndirectedWeightedGraph<SemanticNode, DefaultWeightedEdge> biPartite =
                new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

        for (SemanticNode node1 : unmatched1) {
            for (SemanticNode node2 : unmatched2) {
                biPartite.addVertex(node1);
                partition1.add(node1);
                biPartite.addVertex(node2);
                partition2.add(node2);
                biPartite.addEdge(node1, node2);
                double similarity = SimilarityAlg.compositeNodeSimilarity((TypeDeclNode) node1, (TypeDeclNode) node2);
                biPartite.setEdgeWeight(node1, node2, similarity);
            }
        }
        // bipartite / to match most likely renamed methods
        // find the maximum /, one terminalNodeSimilarity cannot be renamed to two
        MaximumWeightBipartiteMatching matcher =
                new MaximumWeightBipartiteMatching(biPartite, partition1, partition2);
        Set<DefaultWeightedEdge> edges = matcher.getMatching().getEdges();
        // add one2oneMatchings found and remove from unmatched
        for (DefaultWeightedEdge edge : edges) {
            SemanticNode sourceNode = biPartite.getEdgeSource(edge);
            SemanticNode targetNode = biPartite.getEdgeTarget(edge);
            double confidence = biPartite.getEdgeWeight(edge);
            if(confidence >= 0.618){
                matching.unmatchedNodes1.get(NodeType.CLASS).remove(sourceNode);
                matching.unmatchedNodes2.get(NodeType.CLASS).remove(targetNode);
                matching.markRefactoring(sourceNode, targetNode, RefactoringType.CHANGE_TYPE_SIGNATURE, confidence);
            }
        }
        matching.unmatchedNodes1.get(NodeType.CLASS);
        matching.unmatchedNodes2.get(NodeType.CLASS);
    }

}
