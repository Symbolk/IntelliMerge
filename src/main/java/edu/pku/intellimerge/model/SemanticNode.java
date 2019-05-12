package edu.pku.intellimerge.model;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.mapping.NodeContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class SemanticNode {
  // context info
  public NodeContext context;

  // self attributes
  // signature
  private Boolean needToMerge;

  private SemanticNode parent;
  private List<SemanticNode> children;

  private Integer nodeID;
  private NodeType nodeType;
  private String displayName;
  private String qualifiedName;
  // original signature in source code, here we generalize the definition of signature
  private String originalSignature;
  private String comment;
  // annotations can be used before package, class, constructor, terminalNodeSimilarity/interface,
  // field, parameter,
  // local variables
  private List<String> annotations;
  private List<String> modifiers;
  // whether the node is defined inside the graph or not
  private boolean isInternal;
  private Optional<Range> range;

  public SemanticNode() {}

  public SemanticNode(
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
    this.nodeID = nodeID;
    this.needToMerge = needToMerge;
    this.nodeType = nodeType;
    this.displayName = displayName;
    this.qualifiedName = qualifiedName;
    this.originalSignature = originalSignature;
    this.comment = comment;
    this.annotations = annotations;
    this.modifiers = modifiers;
    this.children = new ArrayList<>();
    this.isInternal = true;
    this.range = range;
  }

  public SemanticNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      List<String> annotations,
      List<String> modifiers,
      boolean isInternal,
      Optional<Range> range) {
    this.nodeID = nodeID;
    this.needToMerge = needToMerge;
    this.nodeType = nodeType;
    this.displayName = displayName;
    this.qualifiedName = qualifiedName;
    this.originalSignature = originalSignature;
    this.comment = comment;
    this.annotations = annotations;
    this.modifiers = modifiers;
    this.children = new ArrayList<>();
    this.range = range;
  }

  public boolean isInternal() {
    return isInternal;
  }

  public void setInternal(boolean internal) {
    isInternal = internal;
  }

  public Integer getNodeID() {
    return nodeID;
  }

  public List<String> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(List<String> annotations) {
    this.annotations = annotations;
  }

  public String getAnnotationsAsString() {
    return annotations.stream().collect(Collectors.joining("\n"));
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public Integer getLevel() {
    return nodeType.level;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public void setQualifiedName(String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }

  public String getOriginalSignature() {
    return originalSignature;
  }

  public void setOriginalSignature(String originalSignature) {
    this.originalSignature = originalSignature;
  }

  public List<String> getModifiers() {
    return modifiers;
  }

  public void setModifiers(List<String> modifiers) {
    this.modifiers = modifiers;
  }

  public Optional<Range> getRange() {
    return range;
  }

  public void setRange(Optional<Range> range) {
    this.range = range;
  }
  /** The unique identifier of the node in this project */
  public abstract String getSignature();

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Integer hashCodeSignature() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getNodeType() == null) ? 0 : getNodeType().hashCode());
    result = prime * result + ((getSignature() == null) ? 0 : getSignature().hashCode());
    return result;
  }

  public Boolean needToMerge() {
    return needToMerge;
  }

  /**
   * Clone the object without children and edges
   *
   * @return
   */
  public abstract SemanticNode shallowClone();

  /**
   * Clone the object with cloning children and edges
   *
   * @return
   */
  public abstract SemanticNode deepClone();

  /** A series of methods to operate the tree structure */
  public SemanticNode getParent() {
    return parent;
  }

  public void setParent(SemanticNode parent) {
    this.parent = parent;
  }

  public List<SemanticNode> getChildren() {
    return children;
  }

  public void appendChild(SemanticNode child) {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
    this.children.add(child);
    child.setParent(this);
  }

  public void insertChild(SemanticNode child, int position) {
    if (position >= 0 && position < children.size()) {
      children.add(position, child);
    } else if (position >= children.size()) {
      appendChild(child);
    }
  }

  public SemanticNode getChildAtPosition(int position) {
    if (position >= 0 && position < children.size()) {
      return children.get(position);
    } else {
      return null;
    }
  }

  public int getChildPosition(SemanticNode child) {
    return children.indexOf(child);
  }

  public NodeContext getContext() {
    return context;
  }

  public void setContext(NodeContext context) {
    this.context = context;
  }

  /**
   * Mainly for debugging
   *
   * @return
   */
  @Override
  public String toString() {
    return nodeType.toPrettyString() + "{" + originalSignature + "}";
  }

  /**
   * Mainly for visualization
   *
   * @return
   */
  public String toPrettyString() {
    return nodeType.asString() + "::" + displayName;
  }

  /**
   * Complete string representation
   *
   * @return
   */
  public String asString() {
    return "nodeType=" + nodeType + ", qualifiedName='" + qualifiedName + '}';
  }

  /**
   * To compare if two nodes are equal
   *
   * @return
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((nodeType == null) ? 0 : nodeType.hashCode());
    result = prime * result + ((qualifiedName == null) ? 0 : qualifiedName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof SemanticNode) && (asString().equals(((SemanticNode) o).asString()));
  }
}
