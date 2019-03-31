package edu.pku.intellimerge.model.constant;

/** Match mismatched nodes because of refactoring or other changes */
public enum RefactoringType {
  // one2one match
  // rename method/move method/pull up method/push down method
  CHANGE_METHOD_SIGNATURE("change_method_signature"),
  // rename field/move field/pull up field/push down field
  CHANGE_FIELD_SIGNATURE("change_field_signature"),
  // rename type/move type
  CHANGE_TYPE_SIGNATURE("change_field_signature"),

  // many2many match
  // extract method
  EXTRACT_FROM_METHOD("Extract From Method"), // from which the new method is extracted
  EXTRACT_TO_METHOD("Extract To Method"), // the newly added method because of extracting
  EXTRACT_FROM_TYPE(""),
  EXTRACT_TO_TYPE("");

  private String label;

  RefactoringType(String label) {
    this.label = label;
  }
}
