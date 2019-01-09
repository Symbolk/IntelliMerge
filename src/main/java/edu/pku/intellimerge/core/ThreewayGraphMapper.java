package edu.pku.intellimerge.core;

import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import org.jgrapht.Graph;

import java.util.Set;

public class ThreewayGraphMapper {
    private Graph<SemanticNode, SemanticEdge> oursGraph;
    private Graph<SemanticNode, SemanticEdge> baseGraph;
    private Graph<SemanticNode, SemanticEdge> theirsGraph;

    public ThreewayGraphMapper(Graph<SemanticNode, SemanticEdge> oursGraph, Graph<SemanticNode, SemanticEdge> baseGraph, Graph<SemanticNode, SemanticEdge> theirsGraph) {
        this.oursGraph = oursGraph;
        this.baseGraph = baseGraph;
        this.theirsGraph = theirsGraph;
    }

}
