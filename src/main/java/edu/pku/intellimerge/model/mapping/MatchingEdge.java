package edu.pku.intellimerge.model.mapping;

import edu.pku.intellimerge.model.constant.MatchingType;
import org.jgrapht.graph.DefaultWeightedEdge;

/** Labelled and weighted undirected edge */
public class MatchingEdge extends DefaultWeightedEdge {
  private MatchingType matchingType;

  public MatchingEdge() {}

  public void setMatchingType(MatchingType matchingType) {
    this.matchingType = matchingType;
  }

  public MatchingType getMatchingType() {
    return matchingType;
  }

  public MatchingEdge(MatchingType matchingType) {
    this.matchingType = matchingType;
  }
  // source/target/weight should be accessed by graph api
}
