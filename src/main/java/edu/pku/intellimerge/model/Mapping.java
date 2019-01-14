package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.MappingType;

import java.util.Optional;

/**
 * Nodes are not one-to-one matched
 */
public class Mapping {
    private Optional<SemanticNode> oursNode;
    private Optional<SemanticNode> baseNode;
    private Optional<SemanticNode> theirsNode;
    private MappingType mappingType;

}
