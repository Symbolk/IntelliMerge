package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class NonTerminalNode extends SemanticNode {

  private NonTerminalNode parent;
  private List<SemanticNode> children;

  public NonTerminalNode(
      Integer nodeID, NodeType nodeType, String displayName, String qualifiedName) {
    super(nodeID, nodeType, displayName, qualifiedName);
  }

  @Override
  public String getSignature() {
    return null;
  }

  @Override
  public SemanticNode shallowClone() {
    return new NonTerminalNode(
        this.getNodeID(), this.getNodeType(), this.getDisplayName(), this.getQualifiedName());
  }

  @Override
  public SemanticNode deepClone() {
    return null;
  }

  /**
   * Assume that every node has one or zero direct parent
   *
   * @return Optional better
   */
  @Override
  public SemanticNode getParent() {
    if (this.parent == null) {
      Optional<Map.Entry<EdgeType, List<SemanticNode>>> parentEntry =
          incomingEdges
              .entrySet()
              .stream()
              .filter(entry -> entry.getKey().isStructureEdge)
              .findAny();
      this.parent =
          (NonTerminalNode)
              parentEntry
                  .map(Map.Entry::getValue)
                  .filter(value -> value.size() == 1)
                  .map(list -> list.get(0))
                  .orElse(null);
    }
    return this.parent;
  }

  /**
   * Get all direct children nodes in AST
   *
   * @return
   */
  public List<SemanticNode> getChildren() {
    if (this.children == null) {
      List<Map.Entry<EdgeType, List<SemanticNode>>> childrenEntries =
          outgoingEdges
              .entrySet()
              .stream()
              .filter(entry -> entry.getKey().isStructureEdge)
              .collect(Collectors.toList());
      this.children = new ArrayList<>();
      childrenEntries.forEach(entry -> children.addAll(entry.getValue()));
    }
    return this.children;
  }
}
