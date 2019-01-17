package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.MappingType;
import edu.pku.intellimerge.model.constant.Side;

import java.util.Optional;

/**
 * Nodes are not one-to-one matched
 */
public class Mapping {
    public Optional<SemanticNode> oursNode;
    public Optional<SemanticNode> baseNode;
    public Optional<SemanticNode> theirsNode;
//    private MappingType mappingType;

    public Mapping() {}

    public Mapping(Optional<SemanticNode> oursNode, Optional<SemanticNode> baseNode, Optional<SemanticNode> theirsNode) {
        this.oursNode = oursNode;
        this.baseNode = baseNode;
        this.theirsNode = theirsNode;
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "oursNode=" + oursNode +
                ", baseNode=" + baseNode +
                ", theirsNode=" + theirsNode +
                '}';
    }
}
