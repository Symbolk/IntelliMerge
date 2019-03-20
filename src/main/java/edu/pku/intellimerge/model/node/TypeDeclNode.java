package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Class or Interface Declaration
 */
public class TypeDeclNode extends NonTerminalNode {
  private String access; // can be empty for most inner class
  private List<String> modifiers; // abstract
  private String type; // annotation/class/interface/enum
  private String typeName;
  private String extendType; // java only allows single extending
  private List<String> implementTypes;

  public TypeDeclNode(
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
      String type,
      String typeName) {
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature, comment, annotations);
    this.access = access;
    this.modifiers = modifiers;
    this.type = type;
    this.typeName = typeName;

    this.implementTypes = new ArrayList<>();
    // Notice: here the order matters
    this.incomingEdges.put(EdgeType.IMPORT, new ArrayList<>());
    this.incomingEdges.put(EdgeType.DEFINE, new ArrayList<>());
    this.incomingEdges.put(EdgeType.DECLARE, new ArrayList<>());
    this.incomingEdges.put(EdgeType.INITIALIZE, new ArrayList<>());
    this.incomingEdges.put(EdgeType.IMPLEMENT, new ArrayList<>());
    this.incomingEdges.put(EdgeType.EXTEND, new ArrayList<>());

    this.outgoingEdges.put(EdgeType.IMPLEMENT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.EXTEND, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE, new ArrayList<>());
  }

  public void setExtendType(String extendType) {
    this.extendType = extendType;
  }

  public void setImplementTypes(List<String> implementTypes) {
    this.implementTypes = implementTypes;
  }

}
