package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TerminalNode extends SemanticNode {

  private String body;

  public TerminalNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      List<String> annotations,
      List<String> modifiers,
      String body,
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
    this.body = body;
  }

  public TerminalNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      Optional<Range> range) {
    super(
        nodeID,
        needToMerge,
        nodeType,
        displayName,
        qualifiedName,
        originalSignature,
        "",
        new ArrayList<>(),
        new ArrayList<>(),
        false,
        range);
  }

  @Override
  public SemanticNode shallowClone() {
    return new TerminalNode(
        this.getNodeID(),
        this.needToMerge(),
        this.getNodeType(),
        this.getDisplayName(),
        this.getQualifiedName(),
        this.getOriginalSignature(),
        this.getComment(),
        this.getAnnotations(),
        this.getModifiers(),
        this.getBody(),
        this.getRange());
  }

  @Override
  public SemanticNode deepClone() {
    return shallowClone();
  }

  /**
   * Consider that terminal node has no children
   *
   * @return
   */
  @Override
  public List<SemanticNode> getChildren() {
    return new ArrayList<>();
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  @Override
  public String getSignature() {
    return getQualifiedName();
  }
}
