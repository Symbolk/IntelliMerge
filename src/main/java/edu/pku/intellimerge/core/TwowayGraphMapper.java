package edu.pku.intellimerge.core;

import edu.pku.intellimerge.model.NodeType;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.ast.MethodDeclNode;
import org.jgrapht.Graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TwowayGraphMapper {
  private Graph<SemanticNode, SemanticEdge> baseGraph;
  private Graph<SemanticNode, SemanticEdge> otherGraph;
  private List<Map<SemanticNode, SemanticNode>> mappings;
  private List<SemanticNode> baseUnmatched;
  private List<SemanticNode> otherUnmatched;

  public TwowayGraphMapper(
      Graph<SemanticNode, SemanticEdge> baseGraph, Graph<SemanticNode, SemanticEdge> otherGraph) {
    this.baseGraph = baseGraph;
    this.otherGraph = otherGraph;
  }

  /** Map nodes between base and other graph, simply top-down by signature */
  private void topDownMapBySignature() {
    Map<Integer, SemanticNode> signature2nodeBase;
    Map<Integer, SemanticNode> signature2nodeOther;

    Set<SemanticNode> baseNodes = baseGraph.vertexSet();
    Set<SemanticNode> otherNodes = otherGraph.vertexSet();
    signature2nodeBase =
        baseNodes
            .stream()
            .collect(
                Collectors.toMap(
                    SemanticNode::hashCodeSignature,
                    Function.identity(),
                    (o, n) -> o,
                    HashMap::new));
    signature2nodeOther =
        otherNodes
            .stream()
            .collect(
                Collectors.toMap(
                    SemanticNode::hashCodeSignature,
                    Function.identity(),
                    (o, n) -> o,
                    HashMap::new));
    for (Entry<Integer, SemanticNode> entry : signature2nodeBase.entrySet()) {
      if (signature2nodeOther.containsKey(entry.getKey())) {
        Map<SemanticNode, SemanticNode> mapping = new HashMap<>();
        mapping.put(entry.getValue(), signature2nodeOther.get(entry.getKey()));
        mappings.add(mapping);
        // remove the mapped node from other
        signature2nodeOther.remove(entry.getKey());
      } else {
        baseUnmatched.add(entry.getValue());
      }
    }
    signature2nodeOther.entrySet().forEach(entry -> otherUnmatched.add(entry.getValue()));
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

  private void bottomUpMapByContext() {

  }
}
