package edu.pku.intellimerge.model;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticNode {
  public Map<EdgeType, List<SemanticNode>> incomingEdges = new HashMap<>();
  public Map<EdgeType, List<SemanticNode>> outgoingEdges = new HashMap<>();
  private Integer nodeID;
  private NodeType nodeType;
  private String displayName;
  private String qualifiedName;
  private String content;
  private Range range;
  private Node astNode;

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

  public String getDisplayName() {
    return displayName;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public NodeType getNodeType() {
    return nodeType;
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

  public Node getAstNode() {
    return astNode;
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

  public String asString() {
    return nodeType.asString() + "::" + displayName;
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
