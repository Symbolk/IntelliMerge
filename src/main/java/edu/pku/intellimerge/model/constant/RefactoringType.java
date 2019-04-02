package edu.pku.intellimerge.model.constant;

/** Match mismatched nodes because of refactoring or other changes */
public enum RefactoringType {
  // one2one match
  // rename terminalNodeSimilarity/move terminalNodeSimilarity/pull up terminalNodeSimilarity/push down terminalNodeSimilarity
  CHANGE_METHOD_SIGNATURE("Change Method Signature", true),
  CHANGE_CONSTRUCTOR_SIGNATURE("Change Constructor Signature", true),
  // rename field/move field/pull up field/push down field
  CHANGE_FIELD_SIGNATURE("Change Field Signature", true),
  // rename type/move type
  CHANGE_TYPE_SIGNATURE("Change Type Signature", true),

  // many2many match
  // extract terminalNodeSimilarity
  EXTRACT_FROM_METHOD("Extract From Method", false), // from which the new terminalNodeSimilarity is extracted
  EXTRACT_TO_METHOD("Extract To Method", false), // the newly added terminalNodeSimilarity because of extracting
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
