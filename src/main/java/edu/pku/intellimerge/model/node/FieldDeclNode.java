package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
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
  // initializer as the body of field

  public FieldDeclNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      String access,
      List<String> modifiers,
      String fieldType,
      String fieldName,
      String body,
      Optional<Range> range) {
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature, comment, body, range); // body initializer or ""
    this.access = access;
    this.modifiers = modifiers;
    this.fieldType = fieldType;
    this.fieldName = fieldName;

    this.incomingEdges.put(EdgeType.DEFINE, new ArrayList<>());
    this.incomingEdges.put(EdgeType.READ, new ArrayList<>());
    this.incomingEdges.put(EdgeType.WRITE, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DECLARE, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.INITIALIZE, new ArrayList<>());
  }

  public String getAccess() {
    return access;
  }

  public List<String> getModifiers() {
    return modifiers;
  }

  public String getFieldType() {
    return fieldType;
  }

  public String getFieldName() {
    return fieldName;
  }
}
