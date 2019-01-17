package edu.pku.intellimerge.model.constant;

public enum NodeType {
  // physical nodes
  PROJECT(0, "project"), // logical node to represent folder
  PACKAGE(1, "package"),
  CU(2, "compilation_unit"), // logical node to represent file
  // logical nodes
  CLASS(3, "class"),
  INTERFACE(3, "interface"),
  ENUM(3, "enum"),

  INNER_CLASS(4, "inner_class"),
  LOCAL_CLASS(5, "local_class"),

  CONSTRUCTOR(6, "constructor"),
  FIELD(6, "field"),
  METHOD(6, "method");

  // the hierachy/nesting level of code elements, from outer to inner
  public Integer level;
  public String label;

  NodeType(Integer level, String label) {
    this.level = level;
    this.label = label;
  }

  public String asString() {
    return label.toUpperCase();
  }
}
