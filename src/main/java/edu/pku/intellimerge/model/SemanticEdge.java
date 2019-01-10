package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.EdgeType;

public class SemanticEdge {
  private Integer edgeID;
  private EdgeType edgeType;
  private String label;
  private SemanticNode source;
  private SemanticNode target;

  public SemanticEdge(Integer edgeID, EdgeType edgeType, SemanticNode source, SemanticNode target) {
    this.edgeID = edgeID;
    this.edgeType = edgeType;
    this.source = source;
    this.target = target;
  }

  public EdgeType getEdgeType() {
    return edgeType;
  }

  @Override
  public String toString() {
    return "SemanticEdge{" +
            "edgeType=" + edgeType +
            ", label='" + label + '\'' +
            ", source=" + source +
            ", target=" + target +
            '}';
  }

  public int hashCode() {
    return toString().hashCode();
  }

  public boolean equals(Object o) {
    return (o instanceof SemanticEdge) && (toString().equals(o.toString()));
  }
}
