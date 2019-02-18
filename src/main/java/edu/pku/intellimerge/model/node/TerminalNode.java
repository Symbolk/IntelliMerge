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
  private String body;

  public TerminalNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      String body,
      Optional<Range> range) {
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature, comment);
    this.body = body;
    this.range = range; // since javaparser stores range in Optional, so directly save it
  }

  @Override
  public SemanticNode shallowClone() {
    return new TerminalNode(
        this.getNodeID(),
        this.needToMerge,
        this.getNodeType(),
        this.getDisplayName(),
        this.getQualifiedName(),
        this.getOriginalSignature(),
        this.getComment(),
        this.getBody(),
        this.range);
  }

  @Override
  public SemanticNode deepClone() {
    return shallowClone();
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

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}