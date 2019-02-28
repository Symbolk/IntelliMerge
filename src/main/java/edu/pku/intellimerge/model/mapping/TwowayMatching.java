package edu.pku.intellimerge.model.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.MatchingType;
import edu.pku.intellimerge.model.constant.NodeType;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.*;

public class TwowayMatching {
  // 2 kinds of matching: match by unchanged signature & signature changed but match by their roles
  public BiMap<SemanticNode, SemanticNode> one2oneMatchings; // confidence: 1
  public Map<NodeType, List<SemanticNode>> unmatchedNodes1; // possibly deleted nodes
  public Map<NodeType, List<SemanticNode>> unmatchedNodes2; // possibly added nodes
  //  private DefaultUndirectedWeightedGraph<SemanticNode, MatchingEdge> biPartite;
  public Graph<SemanticNode, MatchingEdge> biPartite; // may contain refactoring matching
  // store the two way nodes matching relationships in a bipartite
  private Set<SemanticNode> partition1; // usually base
  private Set<SemanticNode> partition2; // usually others

  public TwowayMatching() {
    this.one2oneMatchings = HashBiMap.create();
    this.unmatchedNodes1 = new LinkedHashMap<>();
    this.unmatchedNodes2 = new LinkedHashMap<>();

    this.biPartite = initBipartite();
    //        this.biPartite = new DefaultDirectedWeightedGraph<>(MatchingEdge.class);
    this.partition1 = new HashSet<>();
    this.partition2 = new HashSet<>();
  }

  /**
   * Add the edge to save the matching detected
   * @param node1
   * @param node2
   * @param matchingType
   * @param confidence
   */
  public void addMatchingEdge(
          SemanticNode node1, SemanticNode node2, MatchingType matchingType, double confidence) {
    partition1.add(node1);
    partition2.add(node2);
    biPartite.addVertex(node1);
    biPartite.addVertex(node2);
    biPartite.addEdge(node1, node2);
    biPartite.setEdgeWeight(node1, node2, confidence);
    biPartite.getEdge(node1, node2).setMatchingType(matchingType);
  }

  /**
   * Collect one2one matchings from the bipartite, to be merged later
   */
  public void getRefactoredOne2OneMatching() {
    for (SemanticNode node1 : partition1) {
      if (biPartite.edgesOf(node1).size() == 1) {
        for (MatchingEdge edge : biPartite.edgesOf(node1)) {
          if (!one2oneMatchings.containsKey(node1)) {
            one2oneMatchings.put(biPartite.getEdgeSource(edge), biPartite.getEdgeTarget(edge));
          }
        }
      }
    }
  }

  /**
   * Add the given node to unmatched nodes list according to type
   * @param node
   * @param isInBase whether the unmatched node is in base(1)
   */
  public void addUnmatchedNodes(SemanticNode node, boolean isInBase){
      if(isInBase){
        if (unmatchedNodes1.containsKey(node.getNodeType())) {
          unmatchedNodes1.get(node.getNodeType()).add(node);
        } else {
          unmatchedNodes1.put(node.getNodeType(), new ArrayList<>());
          unmatchedNodes1.get(node.getNodeType()).add(node);
        }
      }else{
        if (unmatchedNodes2.containsKey(node.getNodeType())) {
          unmatchedNodes2.get(node.getNodeType()).add(node);
        } else {
          unmatchedNodes2.put(node.getNodeType(), new ArrayList<>());
          unmatchedNodes2.get(node.getNodeType()).add(node);
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
