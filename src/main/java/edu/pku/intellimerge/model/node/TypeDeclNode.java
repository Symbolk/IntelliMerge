package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;

public class TypeDeclNode extends NonTerminalNode {
  private String access; // can be empty for most inner class
  private List<String> modifiers; // abstract
  private String typeType; // class/enum/interface/annotation type
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
      String access,
      List<String> modifiers,
      String typeType,
      String typeName) {
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature, comment);
    this.access = access;
    this.modifiers = modifiers;
    this.typeType = typeType;
    this.typeName = typeName;
    this.needToMerge = needToMerge;

    this.implementTypes = new ArrayList<>();
    // Notice: here the order matters
    this.incomingEdges.put(EdgeType.IMPORT, new ArrayList<>());
    this.incomingEdges.put(EdgeType.DEFINE_TYPE, new ArrayList<>());
    this.incomingEdges.put(EdgeType.DECL_OBJECT, new ArrayList<>());
    this.incomingEdges.put(EdgeType.INIT_OBJECT, new ArrayList<>());
    this.incomingEdges.put(EdgeType.IMPLEMENT, new ArrayList<>());
    this.incomingEdges.put(EdgeType.EXTEND, new ArrayList<>());

    this.outgoingEdges.put(EdgeType.IMPLEMENT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.EXTEND, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE_FIELD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE_CONSTRUCTOR, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE_METHOD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE_INNER_CLASS, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE_ENUM, new ArrayList<>());
  }

  public void setExtendType(String extendType) {
    this.extendType = extendType;
  }

  public void setImplementTypes(List<String> implementTypes) {
    this.implementTypes = implementTypes;
  }

}
