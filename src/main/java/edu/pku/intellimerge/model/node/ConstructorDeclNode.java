package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;

/** access == public, no return type */
public class ConstructorDeclNode extends TerminalNode {
  private String constructorName; // signature
  private String body;
  private Boolean needToMerge;

  public ConstructorDeclNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String constructorName,
      String body,
      Boolean needToMerge) {
    super(nodeID, nodeType, displayName, qualifiedName);
    this.constructorName = constructorName;
    this.body = body;
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
    return constructorName;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
