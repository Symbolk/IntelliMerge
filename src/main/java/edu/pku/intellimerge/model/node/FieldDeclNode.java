package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.exception.RangeNullException;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FieldDeclNode extends TerminalNode {
  private String access;
  private List<String> modifiers;
  private String fieldType;
  private String fieldName;
  private String signature;
  private String body; // initializer or ""
  private Boolean needToMerge;

  public FieldDeclNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String access,
      List<String> modifiers,
      String fieldType,
      String fieldName,
      String body,
      Boolean needToMerge) {
    super(nodeID, nodeType, displayName, qualifiedName);
    this.access = access;
    this.modifiers = modifiers;
    this.fieldType = fieldType;
    this.fieldName = fieldName;
    this.body = body;
    this.needToMerge = needToMerge;

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
    if (this.signature == null) {
      // qualified signature of field, without assigned value/spaces
      StringBuilder builder = new StringBuilder();
      builder.append(access);
      modifiers.forEach(modifier -> builder.append(modifier));
      builder.append(fieldType);
      builder.append(fieldName);
      this.signature = builder.toString();
    }
    return this.signature;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }


}
