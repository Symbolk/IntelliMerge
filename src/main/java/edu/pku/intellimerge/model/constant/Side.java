package edu.pku.intellimerge.model.constant;

public enum Side {
  BASE(0, "base"),
  OURS(1, "ours"),
  THEIRS(2, "theirs"),
  MANUAL(3, "manualMerged"),
  GIT(4, "gitMerged");

  private Integer index;
  private String label;

  Side(Integer index, String label) {
    this.index = index;
    this.label = label;
  }

  public String asString() {
    return label;
  }
}
