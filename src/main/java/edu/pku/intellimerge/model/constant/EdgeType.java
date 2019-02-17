package edu.pku.intellimerge.model.constant;

public enum EdgeType {
  // inter-file edges
  CONTAIN(0, true, "contains"), // physical relation
  IMPORT(1, false, "imports"),
  EXTEND(2, false, "extends"),
  IMPLEMENT(3, false, "implements"),
  // internal edges
  DEFINE_FIELD(4, true, "defines field"),
  DEFINE_METHOD(5, true, "defines method"),
  DEFINE_CONSTRUCTOR(6, true, "defines constructor"),
  DEFINE_INNER_CLASS(7, true, "defines constructor"),
  // inter-field/method edges
  READ_FIELD(8, false, "reads field"),
  WRITE_FIELD(9, false, "writes field"),
  CALL_METHOD(10, false, "calls method"),
  CALL_CONSTRUCTOR(11, false, "calls constructor"),
  // inter-class edges
  DECL_OBJECT(12, false, "declares object"),
  INIT_OBJECT(13, false, "creates object"),
  // use DEFINE_CLASSORINTERFACE or concrete types?
  DEFINE_TYPE(14, true, "defines type");

  public int index;
  // whether the edge represent the nesting hierarchy structure, or the interaction with other nodes
  public boolean isStructureEdge;
  public String label;

  EdgeType(int index, String label) {
    this.index = index;
    this.label = label;
  }

  EdgeType(int index, Boolean isStructureEdge, String label) {
    this.index = index;
    this.isStructureEdge = isStructureEdge;
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
