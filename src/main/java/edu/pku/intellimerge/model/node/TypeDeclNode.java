package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Class or Interface Declaration
 */
public class TypeDeclNode extends CompositeNode {
  private String access; // can be empty for most inner class
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
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature, comment, annotations, modifiers);
    this.access = access;
    this.type = type;
    this.typeName = typeName;

    this.implementTypes = new ArrayList<>();
  }

  public void setExtendType(String extendType) {
    this.extendType = extendType;
  }

  public void setImplementTypes(List<String> implementTypes) {
    this.implementTypes = implementTypes;
  }

}
