package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FieldDeclNode extends TerminalNode {
  private String access;
  private String fieldType;
  private String fieldName;
  // initializer as the body of field

  public FieldDeclNode(
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
      String fieldType,
      String fieldName,
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
        range); // body initializer or ""
    this.access = access;
    this.fieldType = fieldType;
    this.fieldName = fieldName;
  }

  public String getAccess() {
    return access;
  }

  public String getFieldType() {
    return fieldType;
  }

  public String getFieldName() {
    return fieldName;
  }
}
