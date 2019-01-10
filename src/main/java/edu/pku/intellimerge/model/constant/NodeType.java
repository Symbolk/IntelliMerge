package edu.pku.intellimerge.model.constant;

public enum NodeType {
  PROJECT(0, "project"),
  PACKAGE(1, "package"),
  FILE(2, "file"),
  CLASS(3, "class"),
  INTERFACE(3, "interface"),
  ENUM(3, "enum"),
  INNER_CLASS(4, "inner_class"),
  LOCAL_CLASS(5, "local_class"),
  FIELD(6, "field"),
  METHOD(6, "method"),
  CONSTRUCTOR(6, "constructor");

  private Integer level;
  private String label;

  NodeType(Integer level, String label) {
    this.level = level;
    this.label = label;
  }

  public String asString() {
    return label;
  }
}
