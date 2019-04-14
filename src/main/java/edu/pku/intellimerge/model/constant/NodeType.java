package edu.pku.intellimerge.model.constant;

public enum NodeType {
  // physical nodes
  PROJECT(0, "project"), // logical node to represent folder
  PACKAGE(1, "package"),
  COMPILATION_UNIT(2, "compilation_unit"), // logical node to represent file

  // logical nodes
  // nonterminal
  CLASS(3, "class"),
  INTERFACE(3, "interface"),
  ENUM(3, "enum"),
  ANNOTATION(3, "@interface"), // annotation type declaration
  INNER_CLASS(4, "class"),
  LOCAL_CLASS(5, "class"),

  // terminal
  CONSTRUCTOR(6, "constructor"),
  FIELD(6, "field"),
  METHOD(6, "method"),
  ENUM_CONSTANT(6, "enum_constant"),
  INITIALIZER_BLOCK(6, "initializer_block"),
  ANNOTATION_MEMBER(6, "annotation_member");

  // the hierarchy/nesting level of code elements, from outer to inner
  public int level;
  public String label;

  NodeType(int level, String label) {
    this.level = level;
    this.label = label;
  }

  public String asString() {
    return label;
  }

  /**
   * Mainly for visualization
   *
   * @return
   */
  public String toPrettyString() {
    return label.toUpperCase();
  }
}
