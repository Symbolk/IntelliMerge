package edu.pku.intellimerge.model;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticNode {
  private Integer nodeID;
  private NodeType nodeType;
  private String displayName;
  private String qualifiedName;
  private String content;
  private Range range;
  private Node astNode;
  public Map<EdgeType, List<SemanticNode>> incomingEdges = new HashMap<>();
  public Map<EdgeType, List<SemanticNode>> outgoingEdges = new HashMap<>();

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

  public void setRange(Range range) {
    this.range = range;
  }

  public String getDisplayName() {
    return nodeType.toString() + ":" + displayName;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public NodeType getNodeType() {
    return nodeType;
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

  public int hashCode() {
    return toString().hashCode();
  }

  public boolean equals(Object o) {
    return (o instanceof SemanticNode) && (toString().equals(o.toString()));
  }

  public Integer hashCodeSignature() {
    return (nodeType + qualifiedName).hashCode();
  }
}
