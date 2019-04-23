package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodDeclNode extends TerminalNode {
  private String access;
  private List<String> typeParameters;
  private String returnType;
  private String methodName;
  private List<String> parameterTypes;
  private List<String> parameterNames;
  // fully parameter list, with modifier type name
  private List<String> parameterList;
  private List<String> throwExceptions;

  public MethodDeclNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      List<String> annotations,
      String access,
      List<String> modifiers,
      List<String> typeParameters,
      String returnType,
      String methodName,
      List<String> parameterTypes,
      List<String> parameterNames,
      List<String> throwExceptions,
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
        body,
        range); // block or ""(abstract terminalNodeSimilarity or interface)
    this.access = access;
    this.typeParameters = typeParameters;
    this.returnType = returnType;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
    this.parameterNames = parameterNames;
    this.throwExceptions = throwExceptions;
  }

  // dummy/external node
  public MethodDeclNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String methodName,
      List<String> parameterNames,
      Optional<Range> range) {
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature, range);
    this.methodName = methodName;
    this.parameterNames = parameterNames;
  }

  public String getAccess() {
    return access;
  }

  public String getReturnType() {
    return returnType;
  }

  public List<String> getThrowExceptions() {
    return throwExceptions;
  }

  public String getMethodName() {
    return methodName;
  }

  public List<String> getParameterTypes() {
    return parameterTypes;
  }

  public List<String> getParameterNames() {
    return parameterNames;
  }

  public void setParameterList(List<String> parameterList) {
    this.parameterList = parameterList;
  }

  public String getParameterString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < parameterList.size(); ++i) {
      builder.append(parameterList.get(i));
      if (i < parameterList.size() - 1) {
        builder.append(", ");
      }
    }
    return builder.toString();
  }

  public List<String> getTypeParameters() {
    return typeParameters;
  }
}
