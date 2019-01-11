package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** To refactor in the future, as the superclass of all concrete nodes */
public abstract class AbstractNode {
  public Map<EdgeType, List<SemanticNode>> incomingEdges = new HashMap<>();
  public Map<EdgeType, List<SemanticNode>> outgoingEdges = new HashMap<>();
  private Integer nodeID;
  private NodeType nodeType;
  private String signature;
  private String content;

  public Integer getNodeID() {
    return nodeID;
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public abstract String getSignature();

  public abstract String toString();

  public abstract Integer hashSignature();
}
