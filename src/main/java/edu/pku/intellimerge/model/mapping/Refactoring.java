package edu.pku.intellimerge.model.mapping;

import com.github.javaparser.Range;
import edu.pku.intellimerge.exception.RangeNullException;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.RefactoringType;
import edu.pku.intellimerge.model.node.TerminalNode;

/** Class to save the detected matching node pairs */
public class Refactoring {
  private RefactoringType refactoringType;
  private NodeType nodeType;
  private double confidence;
  private SemanticNode before; // from graph1
  private SemanticNode after; // from graph2

  public Refactoring(
      RefactoringType refactoringType,
      NodeType nodeType,
      double confidence,
      SemanticNode before,
      SemanticNode after) {
    this.refactoringType = refactoringType;
    this.nodeType = nodeType;
    this.confidence = confidence;
    this.before = before;
    this.after = after;
  }

  public Refactoring(RefactoringType refactoringType) {
    this.refactoringType = refactoringType;
  }

  public RefactoringType getRefactoringType() {
    return refactoringType;
  }

  public void setRefactoringType(RefactoringType refactoringType) {
    this.refactoringType = refactoringType;
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public double getConfidence() {
    return confidence;
  }

  public SemanticNode getBefore() {
    return before;
  }

  public SemanticNode getAfter() {
    return after;
  }

  public boolean isOneToOne() {
    return refactoringType.isOneToOne();
  }

  public Range getBeforeRange() throws RangeNullException {
    return getRangeForTerminalNode(before);
  }

  public Range getAfterRange() throws RangeNullException {
    return getRangeForTerminalNode(after);
  }

  public Range getRangeForTerminalNode(SemanticNode node) throws RangeNullException {
    return node.getRange().orElseThrow(() -> new RangeNullException("Range is null for: ", node));
  }
}
