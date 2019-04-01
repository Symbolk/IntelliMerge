package edu.pku.intellimerge.model.constant;

/** Match mismatched nodes because of refactoring or other changes */
public enum RefactoringType {
  // one2one match
  // rename method/move method/pull up method/push down method
  CHANGE_METHOD_SIGNATURE("change_method_signature", true),
  // rename field/move field/pull up field/push down field
  CHANGE_FIELD_SIGNATURE("change_field_signature", true),
  // rename type/move type
  CHANGE_TYPE_SIGNATURE("change_field_signature", true),

  // many2many match
  // extract method
  EXTRACT_FROM_METHOD("Extract From Method", false), // from which the new method is extracted
  EXTRACT_TO_METHOD("Extract To Method", false), // the newly added method because of extracting
  EXTRACT_FROM_TYPE("Extract From Type", false),
  EXTRACT_TO_TYPE("Extract To Type", false);

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
