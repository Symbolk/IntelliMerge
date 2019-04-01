package edu.pku.intellimerge.model.mapping;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.RefactoringType;
import edu.pku.intellimerge.model.constant.NodeType;

/** Class to save the detected matching node pairs*/
public class Refactoring {
  private RefactoringType refactoringType;
  private NodeType nodeType;
  private double confidence;
  private SemanticNode source; // from graph1
  private SemanticNode target; // from graph2


  public Refactoring(RefactoringType refactoringType, NodeType nodeType, double confidence, SemanticNode source, SemanticNode target) {
    this.refactoringType = refactoringType;
    this.nodeType = nodeType;
    this.confidence = confidence;
    this.source = source;
    this.target = target;
  }

  public void setRefactoringType(RefactoringType refactoringType) {
    this.refactoringType = refactoringType;
  }

  public RefactoringType getRefactoringType() {
    return refactoringType;
  }

  public Refactoring(RefactoringType refactoringType) {
    this.refactoringType = refactoringType;
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public double getConfidence() {
    return confidence;
  }

  public SemanticNode getSource() {
    return source;
  }

  public SemanticNode getTarget() {
    return target;
  }

  public boolean isOneToOne(){
    return refactoringType.isOneToOne();
  }
}
