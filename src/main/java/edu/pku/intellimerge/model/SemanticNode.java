package edu.pku.intellimerge.model;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

public class SemanticNode {
    private Integer nodeID;
    private Integer nodeType;
    private String displayName;
    private String canonicalName;
    private String content;
    private Range range;
    private Node astNode;

    public SemanticNode(Integer nodeID, Integer nodeType, String displayName, String canonicalName, String content) {
        this.nodeID = nodeID;
        this.nodeType = nodeType;
        this.displayName = displayName;
        this.canonicalName = canonicalName;
        this.content = content;
    }


    public void setRange(Range range) {
        this.range = range;
    }

    public String getDisplayName(){
        return displayName;
    }

    @Override
    public String toString() {
        return "SemanticNode{" +
                "nodeID=" + nodeID +
                ", nodeType='" + nodeType + '\'' +
                ", displayName='" + displayName + '\'' +
                ", canonicalName='" + canonicalName + '\'' +
                ", range=" + range +
                '}';
    }
}

