package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.exception.RangeNullException;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TerminalNode extends SemanticNode {

  private NonTerminalNode parent;
  private Optional<Range> range;

  public TerminalNode(Integer nodeID, NodeType nodeType, String displayName, String qualifiedName) {
    super(nodeID, nodeType, displayName, qualifiedName);
  }

  @Override
  public String getSignature() {
    return null;
  }

  @Override
  public SemanticNode shallowClone() {
    return null;
  }

  @Override
  public SemanticNode deepClone() {
    return null;
  }

  /**
   * Assume that every node has one or zero direct parent
   *
   * @return
   */
  @Override
  public SemanticNode getParent() {
    if (this.parent == null) {
      Optional<Map.Entry<EdgeType, List<SemanticNode>>> parentEntry =
          incomingEdges
              .entrySet()
              .stream()
              .filter(entry -> entry.getKey().isStructureEdge)
              .findAny();
      this.parent =
          (NonTerminalNode)
              parentEntry
                  .map(Map.Entry::getValue)
                  .filter(value -> value.size() == 1)
                  .map(list -> list.get(0))
                  .orElse(null);
    }
    return this.parent;
  }

  public void setParent(NonTerminalNode parent) {
    this.parent = parent;
  }

  /**
   * Consider that terminal node has no children
   *
   * @return
   */
  @Override
  public List<SemanticNode> getChildren() {
    return null;
  }

  public Range getRange() throws RangeNullException {
    return range.orElseThrow(() -> new RangeNullException("Range is null for :", this));
  }

  public void setRange(Optional<Range> range) {
    this.range = range;
  }
}
