package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class SemanticNode {
  public Map<EdgeType, List<SemanticNode>> incomingEdges = new LinkedHashMap<>();
  public Map<EdgeType, List<SemanticNode>> outgoingEdges = new LinkedHashMap<>();
  // signature
  public Boolean needToMerge;
  private Integer nodeID;
  private NodeType nodeType;
  private String displayName;
  private String qualifiedName;
  private String
      originalSignature; // original signature in code, here we generalize the definition of

  public SemanticNode() {}

  public SemanticNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName) {
    this.nodeID = nodeID;
    this.needToMerge = needToMerge;
    this.nodeType = nodeType;
    this.displayName = displayName;
    this.qualifiedName = qualifiedName;
  }

  public SemanticNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature) {
    this.nodeID = nodeID;
    this.needToMerge = needToMerge;
    this.nodeType = nodeType;
    this.displayName = displayName;
    this.qualifiedName = qualifiedName;
    this.originalSignature = originalSignature;
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
    return asString().hashCode();
  }

  public boolean equals(Object o) {
    return (o instanceof SemanticNode) && (toString().equals(o.toString()));
  }

  public String getOriginalSignature() {
    return originalSignature;
  }

  public void setOriginalSignature(String originalSignature) {
    this.originalSignature = originalSignature;
  }

  /** Get the unique fully qualified signature in this project, which should represent the MAIN identification of this node*/
  public String getSignature() {
    return getQualifiedName();
  }

  public Integer hashCodeSignature() {
    return getSignature().hashCode();
  }

  /**
   * Clone the object without children and edges
   *
   * @return
   */
  public abstract SemanticNode shallowClone();

  /**
   * Clone the object with cloning children and edges
   *
   * @return
   */
  public abstract SemanticNode deepClone();

  public abstract SemanticNode getParent();

  public abstract List<SemanticNode> getChildren();

  public Boolean getNeedToMerge() {
    return needToMerge;
  }
}
