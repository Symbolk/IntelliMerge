package edu.pku.intellimerge.model.mapping;

import org.jgrapht.graph.DefaultWeightedEdge;

/** Labelled and weighted undirected edge */
public class MatchingEdge extends DefaultWeightedEdge {
  private String label;

  public MatchingEdge() {}

  public MatchingEdge(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  // source/target/weight should be accessed by graph api
}
