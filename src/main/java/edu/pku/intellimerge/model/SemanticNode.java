package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SemanticNode {
  public Map<EdgeType, List<SemanticNode>> incomingEdges = new HashMap<>();
  public Map<EdgeType, List<SemanticNode>> outgoingEdges = new HashMap<>();
  private Integer nodeID;
  private NodeType nodeType;
  private String displayName;
  private String qualifiedName;

  public SemanticNode() {}

  public SemanticNode(Integer nodeID, NodeType nodeType, String displayName, String qualifiedName) {
    this.nodeID = nodeID;
    this.nodeType = nodeType;
    this.displayName = displayName;
    this.qualifiedName = qualifiedName;
  }

  public Integer getNodeID() {
    return nodeID;
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public Integer getLevel() {
    return nodeType.level;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public void setQualifiedName(String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }

  @Override
  public String toString() {
    return "SemanticNode{"
        + "nodeType='"
        + nodeType
        + '\''
        + ", displayName='"
        + displayName
        + '\''
        + ", qualifiedName='"
        + qualifiedName
        + "}";
  }

  /**
   * Mainly for visualization
   *
   * @return
   */
  public String asString() {
    return nodeType.asString() + "::" + displayName;
  }

  /**
   * To compare if two nodes are equal
   *
   * @return
   */
  public int hashCode() {
    return toString().hashCode();
  }

  public boolean equals(Object o) {
    return (o instanceof SemanticNode) && (toString().equals(o.toString()));
  }

  /**
   * Get the unique fully qualified signature in this project Concretely implemented in subclasses.
   */
  public abstract String getSignature();

  public abstract SemanticNode shallowClone();

  public abstract SemanticNode deepClone();

  public abstract SemanticNode getParent();

  public abstract List<SemanticNode> getChildren();

  public Integer hashCodeSignature() {
    return getSignature().hashCode();
  }
}
