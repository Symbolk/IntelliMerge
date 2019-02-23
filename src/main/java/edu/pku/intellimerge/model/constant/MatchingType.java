package edu.pku.intellimerge.model.constant;

public enum MatchingType {
  // one2one match
  MATCHED_METHOD("Matched Method"),
  MATCHED_FIELD("Matched Field"),

  // many2many match
  EXTRACT_FROM_METHOD("Extract From Method"),
  EXTRACT_TO_METHOD("Extract To Method");

  private String label;

  MatchingType(String label) {
    this.label = label;
  }
}
