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

  public FieldDeclNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String access,
      List<String> modifiers,
      String fieldType,
      String fieldName,
      String body,
      Optional<Range> range) {
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature, body, range); // body initializer or ""
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
}
