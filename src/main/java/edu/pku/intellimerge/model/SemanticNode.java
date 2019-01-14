package edu.pku.intellimerge.model;

import com.github.javaparser.Range;
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
  private String content;
  private Range range; // Optional<>
  private Boolean needToMerge; // only nodes in modified files need to be merged

  public SemanticNode() {}

  public SemanticNode(
      Integer nodeID, NodeType nodeType, String displayName, String qualifiedName, String content) {
    this.nodeID = nodeID;
    this.nodeType = nodeType;
    this.displayName = displayName;
    this.qualifiedName = qualifiedName;
    this.content = content;
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

  public String getContent() {
    return content;
  }

  public Range getRange() {
    return range;
  }

  public void setRange(Range range) {
    this.range = range;
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

  public Integer hashCodeSignature() {
    return getSignature().hashCode();
  }
}
