package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;

/** access == public, no return type */
public class ConstructorDeclNode extends SemanticNode {
  private String constructorName;
  private Boolean needToMerge;

  public ConstructorDeclNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String content,
      String constructorName,
      Boolean needToMerge) {
    super(nodeID, nodeType, displayName, qualifiedName, content);
    this.constructorName = constructorName;
    this.needToMerge = needToMerge;

    this.incomingEdges.put(EdgeType.DEFINE_CONSTRUCTOR, new ArrayList<>());
    this.incomingEdges.put(EdgeType.CALL_CONSTRUCTOR, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.INIT_OBJECT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.READ_FIELD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.WRITE_FIELD, new ArrayList<>());
  }

  @Override
  public String toString() {
    return "ConstructorDeclNode{" + "constructorName='" + constructorName + '\'' + '}';
  }

  @Override
  public String getSignature() {
    return getQualifiedName();
  }
}
