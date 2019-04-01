package edu.pku.intellimerge.model.constant;

public enum EdgeType {
  /** file&folder level edges * */
  CONTAIN(0, true, "contains"), // physical relation
  IMPORT(1, false, "imports"),
  EXTEND(2, false, "extends"),
  IMPLEMENT(3, false, "implements"),
  /** inside-file edges * */
  // define field/terminalNodeSimilarity/constructor/inner type/constant
  DEFINE(4, true, "defines"),
  /** across-node edges * */
  // inter-field/terminalNodeSimilarity edges
  READ(5, false, "reads field"),
  WRITE(6, false, "writes field"),
  // call terminalNodeSimilarity/constructor
  CALL(7, false, "calls terminalNodeSimilarity"),
  // declare/initialize object
  DECLARE(8, false, "declares object"),
  INITIALIZE(9, false, "initializes object");

  public int index;
  // whether the edge represent the nesting hierarchy structure, or the interaction with other nodes
  public boolean isStructureEdge;
  public String label;

  EdgeType(int index, String label) {
    this.index = index;
    this.label = label;
  }

  EdgeType(int index, boolean isStructureEdge, String label) {
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
