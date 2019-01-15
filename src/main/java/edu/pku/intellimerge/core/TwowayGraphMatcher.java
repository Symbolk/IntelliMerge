package edu.pku.intellimerge.core;

import edu.pku.intellimerge.core.matcher.ChangeSignatureMatcher;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.node.FieldDeclNode;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import org.jgrapht.Graph;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TwowayGraphMatcher {
  // keep nodes in the hierachy order
  public Map<SemanticNode, SemanticNode> matchings;
  public List<SemanticNode> unmatchedNodes1; // possibly deleted nodes
  public List<SemanticNode> unmatchedNodes2; // possibly added nodes
  private Graph<SemanticNode, SemanticEdge> graph1; // old graph(base)
  private Graph<SemanticNode, SemanticEdge> graph2; // new graph(ours/theirs)


  public TwowayGraphMatcher(
      Graph<SemanticNode, SemanticEdge> graph1, Graph<SemanticNode, SemanticEdge> graph2) {
    this.graph1 = graph1;
    this.graph2 = graph2;
    this.matchings = new HashMap<>();
    this.unmatchedNodes1 = new ArrayList<>();
    this.unmatchedNodes2 = new ArrayList<>();
  }

  /**
   * Map all node between 2 graphs, simply top-down by signature Assumption: nodes with the same
   * signature are matched
   */
  public void topDownMatch() {
    Map<Integer, SemanticNode> map1;
    Map<Integer, SemanticNode> map2;

    Set<SemanticNode> baseNodes = graph1.vertexSet();
    Set<SemanticNode> otherNodes = graph2.vertexSet();
    map1 =
        baseNodes
            .stream()
            .collect(
                Collectors.toMap(
                    SemanticNode::hashCodeSignature,
                    Function.identity(),
                    (o, n) -> o,
                    HashMap::new));
    map2 =
        otherNodes
            .stream()
            .collect(
                Collectors.toMap(
                    SemanticNode::hashCodeSignature,
                    Function.identity(),
                    (o, n) -> o,
                    HashMap::new));
    for (Entry<Integer, SemanticNode> entry : map1.entrySet()) {
      if (map2.containsKey(entry.getKey())) {
        matchings.put(entry.getValue(), map2.get(entry.getKey()));
        // remove the mapped node from other
        map2.remove(entry.getKey());
      } else {
        unmatchedNodes1.add(entry.getValue());
      }
    }
    map2.entrySet().forEach(entry -> unmatchedNodes2.add(entry.getValue()));
  }

  /** Bottom-up match unmatched nodes in the last step, considering some kinds of refactorings */
  public void bottomUpMatch() {
    //       Split the node list into concrete node lists with different node type
    ArrayList<FieldDeclNode> fieldDeclNodes1 = new ArrayList<>();
    ArrayList<FieldDeclNode> fieldDeclNodes2 = new ArrayList<>();
    ArrayList<MethodDeclNode> methodDeclNodes1 = new ArrayList<>();
    ArrayList<MethodDeclNode> methodDeclNodes2 = new ArrayList<>();
    splitUnmatchedNodesByType(unmatchedNodes1, fieldDeclNodes1, methodDeclNodes1);
    splitUnmatchedNodesByType(unmatchedNodes2, fieldDeclNodes2, methodDeclNodes2);

    ChangeSignatureMatcher.matchChangeMethodSignature(matchings, methodDeclNodes1, methodDeclNodes2);
  }

  private void splitUnmatchedNodesByType(
      List<SemanticNode> unmatchedNodes,
      List<FieldDeclNode> fieldDeclNodes,
      List<MethodDeclNode> methodDeclNodes) {
    for (SemanticNode node : unmatchedNodes) {
      if (node.getNodeType().equals(NodeType.FIELD) && node instanceof FieldDeclNode) {
        fieldDeclNodes.add((FieldDeclNode) node);
      } else if (node.getNodeType().equals(NodeType.METHOD) && node instanceof MethodDeclNode) {
        methodDeclNodes.add((MethodDeclNode) node);
      }
    }
  }
  /**
   * Sort the node list in the reverse hierachy order, i.e. bottom up in AST
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
