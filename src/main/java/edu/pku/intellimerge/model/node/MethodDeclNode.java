package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.SemanticNode;

import java.util.ArrayList;
import java.util.List;

public class MethodDeclNode extends SemanticNode {
  private String access;
  private List<String> modifiers;
  private String returnType;
  private String methodName;
  private List<String> parameterTypes;

  public MethodDeclNode() {
    super();
  }

  public MethodDeclNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String content,
      String access,
      List<String> modifiers,
      String returnType,
      String methodName,
      List<String> parameterTypes) {
    super(nodeID, nodeType, displayName, qualifiedName, content);
    this.access = access;
    this.modifiers = modifiers;
    this.returnType = returnType;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
    this.incomingEdges.put(EdgeType.DEFINE_METHOD, new ArrayList<>());
    this.incomingEdges.put(EdgeType.CALL_METHOD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.INIT_OBJECT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DECL_OBJECT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.CALL_METHOD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.READ_FIELD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.WRITE_FIELD, new ArrayList<>());
  }

  public void addIncommingEdge(EdgeType edgeType, SemanticNode semanticNode) {
    this.incomingEdges.get(edgeType).add(semanticNode);
  }

  public void addOutgoingEdge(EdgeType edgeType, SemanticNode semanticNode) {
    this.outgoingEdges.get(edgeType).add(semanticNode);
  }
}
