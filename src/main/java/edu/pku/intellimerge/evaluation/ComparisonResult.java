package edu.pku.intellimerge.evaluation;

import org.bson.Document;

import java.util.List;

/** Expected result by comparing auto-merged code with manual-merged code for one merge scenario */
public class ComparisonResult {
  // sum of file LOC in this scenario
  private Integer totalAutoMergeLOC;
  private Integer totalManualMergeLOC;
  private Integer totalSameAutoMergeLOC; // same with the manual
  private Double autoMergePrecision;
  private Double autoMergeRecall;
  private List<Document> autoMergedDiffDocs;
  //    private List<Document> mergeConflictDocs;

  public ComparisonResult(
      Integer totalAutoMergeLOC,
      Integer totalManualMergeLOC,
      Integer totalSameAutoMergeLOC,
      Double autoMergePrecision,
      Double autoMergeRecall,
      List<Document> autoMergedDiffDocs) {
    this.totalAutoMergeLOC = totalAutoMergeLOC;
    this.totalManualMergeLOC = totalManualMergeLOC;
    this.totalSameAutoMergeLOC = totalSameAutoMergeLOC;
    this.autoMergePrecision = autoMergePrecision;
    this.autoMergeRecall = autoMergeRecall;
    this.autoMergedDiffDocs = autoMergedDiffDocs;
  }

  public Double getAutoMergePrecision() {
    return autoMergePrecision;
  }

  public void setAutoMergePrecision(Double autoMergePrecision) {
    this.autoMergePrecision = autoMergePrecision;
  }

  public Double getAutoMergeRecall() {
    return autoMergeRecall;
  }

  public void setAutoMergeRecall(Double autoMergeRecall) {
    this.autoMergeRecall = autoMergeRecall;
  }

  public Integer getTotalAutoMergeLOC() {
    return totalAutoMergeLOC;
  }

  public void setTotalAutoMergeLOC(Integer totalAutoMergeLOC) {
    this.totalAutoMergeLOC = totalAutoMergeLOC;
  }

  public Integer getTotalManualMergeLOC() {
    return totalManualMergeLOC;
  }

  public void setTotalManualMergeLOC(Integer totalManualMergeLOC) {
    this.totalManualMergeLOC = totalManualMergeLOC;
  }

  public Integer getTotalSameAutoMergeLOC() {
    return totalSameAutoMergeLOC;
  }

  public void setTotalSameAutoMergeLOC(Integer totalSameAutoMergeLOC) {
    this.totalSameAutoMergeLOC = totalSameAutoMergeLOC;
  }

  public List<Document> getAutoMergedDiffDocs() {
    return autoMergedDiffDocs;
  }

  public void setAutoMergedDiffDocs(List<Document> autoMergedDiffDocs) {
    this.autoMergedDiffDocs = autoMergedDiffDocs;
  }
}
