package edu.pku.intellimerge.model.constant;

public enum EdgeType {
  // inter-file edges
  CONTAIN(0, "contains"),
  IMPORT(1, "imports"),
  EXTEND(2, "extends"),
  IMPLEMENT(3, ""),
  // internal edges
  DEFINE_FIELD(4, "defines field"),
  DEFINE_METHOD(5, "defines method"),
  DEFINE_CONSTRUCTOR(6, "defines constructor"),
  // inter-field/method edges
  READ_FIELD(7, "reads field"),
  WRITE_FIELD(8, "writes field"),
  CALL_METHOD(9, "calls method"),
  // inter-class edges
  DECL_OBJECT(10, "declares object"),
  INIT_OBJECT(11, "creates object");

  private Integer index;
  private String label;

  EdgeType(Integer index, String label) {
    this.index = index;
    this.label = label;
  }

  public String asString() {
    return label.toUpperCase();
  }
}
