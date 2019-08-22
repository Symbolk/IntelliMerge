package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.Optional;

public class OrphanCommentNode extends SemanticNode {

  public OrphanCommentNode(
      Integer nodeID,
      String displayName,
      String qualifiedName,
      String originalSignature,
      Optional<Range> range) {
    super(
        nodeID,
        true,
        NodeType.ORPHAN_COMMENT,
        displayName,
        qualifiedName,
        originalSignature,
        "",
        new ArrayList<>(),
        new ArrayList<>(),
        range);
  }

  @Override
  public String getSignature() {
    return getOriginalSignature();
  }

  @Override
  public SemanticNode shallowClone() {
    return new OrphanCommentNode(
        this.getNodeID(),
        this.getDisplayName(),
        this.getQualifiedName(),
        this.getOriginalSignature(),
        this.getRange());
  }

  @Override
  public SemanticNode deepClone() {
    return new OrphanCommentNode(
        this.getNodeID(),
        this.getDisplayName(),
        this.getQualifiedName(),
        this.getOriginalSignature(),
        this.getRange());
  }
}
