package edu.pku.intellimerge.model.constant;

public enum EdgeType {
  // inter-file edges
  CONTAIN(0, "contains"), // physical relation
  IMPORT(1, "imports"),
  EXTEND(2, "extends"),
  IMPLEMENT(3, "implements"),
  // internal edges
  DEFINE_FIELD(4, "defines field"),
  DEFINE_METHOD(5, "defines method"),
  DEFINE_CONSTRUCTOR(6, "defines constructor"),
  DEFINE_INNER_CLASS(7, "defines constructor"),
  // inter-field/method edges
  READ_FIELD(8, "reads field"),
  WRITE_FIELD(9, "writes field"),
  CALL_METHOD(10, "calls method"),
  CALL_CONSTRUCTOR(11, "calls constructor"),
  // inter-class edges
  DECL_OBJECT(12, "declares object"),
  INIT_OBJECT(13, "creates object");

  public Integer index;
  public String label;

  EdgeType(Integer index, String label) {
    this.index = index;
    this.label = label;
  }

  public String asString() {
    return label.toUpperCase();
  }
}
