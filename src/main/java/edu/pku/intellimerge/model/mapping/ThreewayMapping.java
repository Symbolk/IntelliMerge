package edu.pku.intellimerge.model.mapping;

import edu.pku.intellimerge.model.SemanticNode;

import java.util.Optional;

/**
 * Only keep the CUs for now
 */
public class ThreewayMapping {
  public Optional<SemanticNode> oursNode;
  public Optional<SemanticNode> baseNode;
  public Optional<SemanticNode> theirsNode;

  public ThreewayMapping() {}

  public ThreewayMapping(
      Optional<SemanticNode> oursNode,
      Optional<SemanticNode> baseNode,
      Optional<SemanticNode> theirsNode) {
    this.oursNode = oursNode;
    this.baseNode = baseNode;
    this.theirsNode = theirsNode;
  }

  @Override
  public String toString() {
    return "ThreewayMapping{"
        + "oursNode="
        + oursNode
        + ", baseNode="
        + baseNode
        + ", theirsNode="
        + theirsNode
        + '}';
  }
}
