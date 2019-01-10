package edu.pku.intellimerge.core;

import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import org.jgrapht.Graph;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TwowayGraphMatcher {
  private Graph<SemanticNode, SemanticEdge> graph1;
  private Graph<SemanticNode, SemanticEdge> graph2;
  public List<Map<SemanticNode, SemanticNode>> mappings;
  public List<SemanticNode> unmatchedNodes1;
  public List<SemanticNode> unmatchedNodes2;

  public TwowayGraphMatcher(
          Graph<SemanticNode, SemanticEdge> graph1, Graph<SemanticNode, SemanticEdge> graph2) {
    this.graph1 = graph1;
    this.graph2 = graph2;
    this.mappings = new ArrayList<>();
    this.unmatchedNodes1 = new ArrayList<>();
    this.unmatchedNodes2 = new ArrayList<>();
  }

  /** Map node between base and other graph, simply top-down by signature */
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
        Map<SemanticNode, SemanticNode> mapping = new HashMap<>();
        mapping.put(entry.getValue(), map2.get(entry.getKey()));
        mappings.add(mapping);
        // remove the mapped node from other
        map2.remove(entry.getKey());
      } else {
        unmatchedNodes1.add(entry.getValue());
      }
    }
    map2.entrySet().forEach(entry -> unmatchedNodes2.add(entry.getValue()));
  }

  // quickly test methods
  private List<MethodDeclNode> getAllMethodDeclNodes(Graph<SemanticNode, SemanticEdge> graph) {
    return graph
        .vertexSet()
        .stream()
        .filter(node -> node.getNodeType().equals(NodeType.METHOD))
        .map(node -> (MethodDeclNode) node)
        .collect(Collectors.toList());
  }

  public void bottomUpMatch() {

  }
}
