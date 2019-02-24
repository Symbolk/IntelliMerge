package edu.pku.intellimerge.model.constant;

/** Match mismatched nodes because of refactoring or other changes */
public enum MatchingType {
  // one2one match
  // rename method/move method/pull up method/push down method
  MATCHED_METHOD("Matched Method"),
  // rename field/move field/pull up field/push down field
  MATCHED_FIELD("Matched Field"),
  // rename type/move type
  MATCHED_TYPE("Matched Type"),

  // many2many match
  // extract method
  EXTRACT_FROM_METHOD("Extract From Method"), // from which the new method is extracted
  EXTRACT_TO_METHOD("Extract To Method"), // the newly added method because of extracting
  EXTRACT_FROM_TYPE(""),
  EXTRACT_TO_TYPE("");

  private String label;

  MatchingType(String label) {
    this.label = label;
  }
}
