package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.List;

public class NonTerminalNode extends SemanticNode {

  public NonTerminalNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      List<String> annotations) {
    super(
        nodeID,
        needToMerge,
        nodeType,
        displayName,
        qualifiedName,
        originalSignature,
        comment,
        annotations);
  }

  @Override
  public SemanticNode shallowClone() {
    return new NonTerminalNode(
        this.getNodeID(),
        this.needToMerge(),
        this.getNodeType(),
        this.getDisplayName(),
        this.getQualifiedName(),
        this.getOriginalSignature(),
        this.getComment(),
        this.getAnnotations());
  }

  @Override
  public SemanticNode deepClone() {
    return null;
  }

  @Override
  public String getSignature() {
    return getQualifiedName();
  }
}
