package edu.pku.intellimerge.model;

public class SemanticEdge {
    private Integer edgeID;
    private Integer edgeType;
    private String label;
    private SemanticNode source;
    private SemanticNode target;

    public SemanticEdge(Integer edgeID, Integer edgeType, SemanticNode source, SemanticNode target) {
        this.edgeID = edgeID;
        this.edgeType = edgeType;
        this.source = source;
        this.target = target;
    }

    public String getLabel() {
        String label=EdgeType.getTypeAsString(this.edgeType);
        return label;
    }
}
