package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NonTerminalNode extends SemanticNode {

  private NonTerminalNode parent;
  private List<SemanticNode> children;

  public NonTerminalNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature) {
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature);
  }

  @Override
  public SemanticNode shallowClone() {
    return new NonTerminalNode(
        this.getNodeID(),
        this.needToMerge,
        this.getNodeType(),
        this.getDisplayName(),
        this.getQualifiedName(),
        this.getOriginalSignature());
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
      this.children = new ArrayList<>();
      outgoingEdges
          .entrySet()
          .stream()
          .filter(entry -> entry.getKey().isStructureEdge)
          .forEach(entry -> children.addAll(entry.getValue()));
    }
    return this.children;
  }

  public void addChild(SemanticNode child) {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
    this.children.add(child);
  }
}
