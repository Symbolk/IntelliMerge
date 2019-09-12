package edu.pku.intellimerge.model.constant;

/** Match mismatched nodes because of refactoring or other changes */
public enum RefactoringType {
  // one2one match
  // rename terminal/move terminal/pull up terminal/push down terminal
  CHANGE_METHOD_SIGNATURE("Change Method Signature", true),
  CHANGE_CONSTRUCTOR_SIGNATURE("Change Constructor Signature", true),
  // rename field/move field/pull up field/push down field
  CHANGE_FIELD_SIGNATURE("Change Field Signature", true),
  // rename type/move type
  CHANGE_TYPE_SIGNATURE("Change Type Signature", true),

  // many2many match
  EXTRACT_METHOD("Extract Method", false),
  INLINE_METHOD("Inline Method", false);

  private String label;
  private boolean isOneToOne;

  RefactoringType(String label, boolean isOneToOne) {
    this.label = label;
    this.isOneToOne = isOneToOne;
  }

  public String getLabel() {
    return label;
  }

  public boolean isOneToOne() {
    return isOneToOne;
  }
}
