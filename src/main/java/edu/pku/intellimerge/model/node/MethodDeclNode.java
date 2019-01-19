package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;

public class MethodDeclNode extends TerminalNode {
  private String access;
  private List<String> modifiers;
  private String returnType;
  private String methodName;
  private List<String> parameterTypes;
  private List<String> parameterNames;
  private List<String> throwExceptions;
  private String body; // block or ""(abstract method or interface)
  private Boolean needToMerge;

  public MethodDeclNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String access,
      List<String> modifiers,
      String returnType,
      String methodName,
      List<String> parameterTypes,
      List<String> parameterNames,
      List<String> throwExceptions,
      String body,
      Boolean needToMerge) {
    super(nodeID, nodeType, displayName, qualifiedName);
    this.access = access;
    this.modifiers = modifiers;
    this.returnType = returnType;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
    this.parameterNames = parameterNames;
    this.throwExceptions = throwExceptions;
    this.body = body;
    this.needToMerge = needToMerge;

    this.incomingEdges.put(EdgeType.DEFINE_METHOD, new ArrayList<>());
    this.incomingEdges.put(EdgeType.CALL_METHOD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.INIT_OBJECT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DECL_OBJECT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.CALL_METHOD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.READ_FIELD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.WRITE_FIELD, new ArrayList<>());
  }

  @Override
  public String toString() {
    return "MethodDeclNode{"
        + "access='"
        + access
        + '\''
        + ", modifiers="
        + modifiers
        + ", returnType='"
        + returnType
        + '\''
        + ", methodName='"
        + methodName
        + '\''
        + ", parameterTypes="
        + parameterTypes
        + '}';
  }

  @Override
  public String getSignature() {
    // qualified signature of method, without the parameter names/spaces/commmas/brackets
    StringBuilder builder = new StringBuilder();
    builder.append(access);
    modifiers.forEach(modifier -> builder.append(modifier));
    builder.append(returnType);
    builder.append(methodName);
    if (parameterTypes.size() > 0) {
      parameterTypes.forEach(type -> builder.append(type));
    }
    if (throwExceptions.size() > 0) {
      throwExceptions.forEach(exception -> builder.append(exception));
    }
    return toString();
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
