package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.EdgeType;

public class SemanticEdge {
  private Integer edgeID;
  private EdgeType edgeType;
  private Integer weight; // how many times the same edge
  // not good to keep vertex here, since the edge and the connectivity are suggested to be separated
  private SemanticNode source;
  private SemanticNode target;
  // whether the target node is defined inside the graph or not
  private boolean isInternal;

  public SemanticEdge(Integer edgeID, EdgeType edgeType, SemanticNode source, SemanticNode target) {
    this.edgeID = edgeID;
    this.edgeType = edgeType;
    this.source = source;
    this.target = target;
    this.weight = 1;
    this.isInternal = true;
  }

  public SemanticEdge(Integer edgeID, EdgeType edgeType, SemanticNode source, SemanticNode target, boolean isInternal) {
    this.edgeID = edgeID;
    this.edgeType = edgeType;
    this.source = source;
    this.target = target;
    this.weight = 1;
    this.isInternal = isInternal;
  }

  public EdgeType getEdgeType() {
    return edgeType;
  }

  @Override
  public String toString() {
    return "SemanticEdge{"
        + "edgeType="
        + edgeType
        + ", source="
        + source
        + ", target="
        + target
        + '}';
  }
  public boolean isInternal() {
    return isInternal;
  }

  public void setInternal(boolean internal) {
    isInternal = internal;
  }

  public int hashCode() {
    return toString().hashCode();
  }

  public boolean equals(Object o) {
    return (o instanceof SemanticEdge) && (toString().equals(o.toString()));
  }

  public Integer getEdgeID() {
    return edgeID;
  }

  public void setEdgeID(Integer edgeID) {
    this.edgeID = edgeID;
  }

  public void setEdgeType(EdgeType edgeType) {
    this.edgeType = edgeType;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }

  public boolean isStructuredEdge(){
    return this.edgeType.isStructureEdge;
  }
}
