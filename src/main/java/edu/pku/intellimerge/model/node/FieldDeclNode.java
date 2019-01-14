package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;

public class FieldDeclNode extends SemanticNode {
  private String access;
  private List<String> modifiers;
  private String fieldType;
  private String fieldName;

  public FieldDeclNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String content,
      String access,
      List<String> modifiers,
      String fieldType,
      String fieldName) {
    super(nodeID, nodeType, displayName, qualifiedName, content);
    this.access = access;
    this.modifiers = modifiers;
    this.fieldType = fieldType;
    this.fieldName = fieldName;
    this.incomingEdges.put(EdgeType.DEFINE_FIELD, new ArrayList<>());
    this.incomingEdges.put(EdgeType.READ_FIELD, new ArrayList<>());
    this.incomingEdges.put(EdgeType.WRITE_FIELD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DECL_OBJECT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.INIT_OBJECT, new ArrayList<>());
  }

  @Override
  public String toString() {
    return "FieldDeclNode{"
        + "access='"
        + access
        + '\''
        + ", modifiers="
        + modifiers
        + ", fieldType='"
        + fieldType
        + '\''
        + ", fieldName='"
        + fieldName
        + '\''
        + '}';
  }

  @Override
  public String getSignature() {
    // qualified signature of field, without assigned value/spaces
    StringBuilder builder = new StringBuilder();
    builder.append(access);
    modifiers.forEach(modifier -> builder.append(modifier));
    builder.append(fieldType);
    builder.append(fieldName);
    return builder.toString();
  }
}
