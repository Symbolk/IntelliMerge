package edu.pku.intellimerge.core;

import edu.pku.intellimerge.core.matcher.MethodDeclMatcher;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import org.jgrapht.Graph;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TwowayGraphMatcher {
  public TwowayMatching matching;

  private Graph<SemanticNode, SemanticEdge> graph1; // old graph(base)
  private Graph<SemanticNode, SemanticEdge> graph2; // new graph(ours/theirs)

  public TwowayGraphMatcher(
      Graph<SemanticNode, SemanticEdge> graph1, Graph<SemanticNode, SemanticEdge> graph2) {
    this.graph1 = graph1;
    this.graph2 = graph2;
    this.matching = new TwowayMatching();
  }

  /**
   * Map all node between 2 graphs, simply top-down by signature Assumption: nodes with the same
   * signature are matched
   */
  public void topDownMatch() {
    Set<SemanticNode> nodeSet1 = graph1.vertexSet();
    Set<SemanticNode> nodeSet2 = graph2.vertexSet();
    Map<Integer, SemanticNode> map1 =
        nodeSet1
            .stream()
            .filter(SemanticNode::getNeedToMerge)
            .collect(
                Collectors.toMap(
                    SemanticNode::hashCodeSignature,
                    Function.identity(),
                    (o, n) -> o,
                    HashMap::new));
    Map<Integer, SemanticNode> map2 =
        nodeSet2
            .stream()
            .filter(SemanticNode::getNeedToMerge)
            .collect(
                Collectors.toMap(
                    SemanticNode::hashCodeSignature,
                    Function.identity(),
                    (o, n) -> o,
                    HashMap::new));
    for (Entry<Integer, SemanticNode> entry : map1.entrySet()) {
      if (map2.containsKey(entry.getKey())) {
        // add the matched nodes into the matching relationships
        matching.one2oneMatchings.put(entry.getValue(), map2.get(entry.getKey()));
        // remove the mapped node from other
        map2.remove(entry.getKey());
      } else {
        matching.unmatchedNodes1.add(entry.getValue());
      }
    }
    map2.entrySet().forEach(entry -> matching.unmatchedNodes2.add(entry.getValue()));
  }

  /** Bottom-up match unmatched nodes in the last step, considering some kinds of refactorings */
  public void bottomUpMatch() {
    // divide and conquer: match each type of nodes separately
    Map<NodeType, List<SemanticNode>> unmatchedNodesByType1 =
        splitUnmatchedNodesByType(matching.unmatchedNodes1);
    Map<NodeType, List<SemanticNode>> unmatchedNodesByType2 =
        splitUnmatchedNodesByType(matching.unmatchedNodes2);

    // if only there are unmatched nodes, try to match
    List<SemanticNode> unmatchedMethods1 =
        unmatchedNodesByType1.getOrDefault(NodeType.METHOD, null);
    List<SemanticNode> unmatchedMethods2 =
        unmatchedNodesByType2.getOrDefault(NodeType.METHOD, null);
    if (unmatchedMethods1 != null && unmatchedMethods2 != null) {
      MethodDeclMatcher methodDeclMatcher = new MethodDeclMatcher();
      methodDeclMatcher.matchChangeMethodSignature(matching, unmatchedMethods1, unmatchedMethods2);
      methodDeclMatcher.matchExtractMethod(matching, unmatchedMethods1, unmatchedMethods2);
      matching.getRefactoredOne2OneMatching();
    }
  }

  /**
   * Split the unmatched nodes by type
   *
   * @param unmatchedNodes
   * @return
   */
  private Map<NodeType, List<SemanticNode>> splitUnmatchedNodesByType(
      List<SemanticNode> unmatchedNodes) {
    Map<NodeType, List<SemanticNode>> unmatchedNodesByType = new HashMap<>();
    for (SemanticNode node : unmatchedNodes) {
      if (unmatchedNodesByType.containsKey(node.getNodeType())) {
        unmatchedNodesByType.get(node.getNodeType()).add(node);
      } else {
        unmatchedNodesByType.put(node.getNodeType(), new ArrayList<>());
        unmatchedNodesByType.get(node.getNodeType()).add(node);
      }
    }
    return unmatchedNodesByType;
  }
  /**
   * Sort the node list in the reverse hierarchy order, i.e. bottom up in AST
   *
   * @param unmatchedNodes
   * @return
   */
  private ArrayList<SemanticNode> sortUnmatchedNodes(List<SemanticNode> unmatchedNodes) {
    ArrayList<SemanticNode> unMatchedNodesSorted =
        new ArrayList(
            unmatchedNodes
                .stream()
                .sorted(Comparator.comparing(SemanticNode::getLevel).reversed())
                .collect(Collectors.toList()));
    return unMatchedNodesSorted;
  }
}
