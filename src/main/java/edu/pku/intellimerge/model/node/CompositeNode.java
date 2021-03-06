package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompositeNode extends SemanticNode {

  // prefix strings before '{'
  public String curlyBracePrefix = "";
  // number of EOLs between { and the first child
  public int beforeFirstChildEOL = 1;
  // type decl
  public CompositeNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      List<String> annotations,
      List<String> modifiers,
      Optional<Range> range) {
    super(
        nodeID,
        needToMerge,
        nodeType,
        displayName,
        qualifiedName,
        originalSignature,
        comment,
        annotations,
        modifiers,
        range);
  }

  // package decl nodes do not have annotations or modifiers
  public CompositeNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      List<String> annotations,
      Optional<Range> range) {
    super(
        nodeID,
        needToMerge,
        nodeType,
        displayName,
        qualifiedName,
        originalSignature,
        comment,
        annotations,
        new ArrayList<>(),
        range);
  }

  // compilation unit decl
  public CompositeNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment) {
    super(
        nodeID,
        needToMerge,
        nodeType,
        displayName,
        qualifiedName,
        originalSignature,
        comment,
        new ArrayList<>(),
        new ArrayList<>(),
        Optional.empty());
  }

  @Override
  public SemanticNode shallowClone() {
    CompositeNode clone =
        new CompositeNode(
            this.getNodeID(),
            this.needToMerge(),
            this.getNodeType(),
            this.getDisplayName(),
            this.getQualifiedName(),
            this.getOriginalSignature(),
            this.getComment(),
            this.getAnnotations(),
            this.getModifiers(),
            this.getRange());
    clone.curlyBracePrefix = this.curlyBracePrefix;
    clone.beforeFirstChildEOL = this.beforeFirstChildEOL;
    clone.followingEOL = this.followingEOL;
    return clone;
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
