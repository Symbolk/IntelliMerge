package edu.pku.intellimerge.model.mapping;

import edu.pku.intellimerge.model.SemanticEdge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodeContext {
  private Set<SemanticEdge> incomingEdges;
  private Set<SemanticEdge> outgoingEdges;

  // use map to save vectors for random access
  private Map<Integer, Integer> incomingVector = new HashMap<>();
  private Map<Integer, Integer> outgoingVector = new HashMap<>();

  public NodeContext(
      Set<SemanticEdge> incomingEdges,
      Set<SemanticEdge> outgoingEdges,
      Map<Integer, Integer> incomingVector,
      Map<Integer, Integer> outgoingVector) {
    this.incomingEdges = incomingEdges;
    this.outgoingEdges = outgoingEdges;
    this.incomingVector = incomingVector;
    this.outgoingVector = outgoingVector;
  }

  public NodeContext(Set<SemanticEdge> incomingEdges, Set<SemanticEdge> outgoingEdges) {
    this.incomingEdges = incomingEdges;
    this.outgoingEdges = outgoingEdges;
    for (Integer i = 0; i < EdgeLabel.values().length; i++) {
      this.incomingVector.put(i, 0);
      this.outgoingVector.put(i, 0);
    }
  }

  public Set<SemanticEdge> getIncomingEdges() {
    return incomingEdges;
  }

  public void setOutgoingEdges(Set edges) {
    this.outgoingEdges = edges;
  }

  public Set<SemanticEdge> getOutgoingEdges() {
    return outgoingEdges;
  }

  public Map<Integer, Integer> getIncomingVector() {
    return incomingVector;
  }

  public Map<Integer, Integer> getOutgoingVector() {
    return outgoingVector;
  }

  public void putIncomingVector(Integer key, Integer value) {
    this.incomingVector.put(key, value);
  }

  public void putOutgoingVector(Integer key, Integer value) {
    this.outgoingVector.put(key, value);
  }

  public NodeContext join(NodeContext context) {
    Set<SemanticEdge> combinedIncomingEdges = new HashSet<>();
    Set<SemanticEdge> combinedOutgoingEdges = new HashSet<>();
    combinedIncomingEdges.addAll(this.incomingEdges);
    combinedIncomingEdges.addAll(context.getIncomingEdges());
    combinedOutgoingEdges.addAll(this.outgoingEdges);
    combinedOutgoingEdges.addAll(context.getOutgoingEdges());
    this.incomingEdges = combinedIncomingEdges;
    this.outgoingEdges = combinedOutgoingEdges;
    context
        .getIncomingVector()
        .forEach((key, value) -> this.incomingVector.merge(key, value, (v1, v2) -> (v1 + v2)));
    context
        .getOutgoingVector()
        .forEach((key, value) -> this.outgoingVector.merge(key, value, (v1, v2) -> (v1 + v2)));

    return this;
  }
}
