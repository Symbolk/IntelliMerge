package edu.pku.intellimerge.model.mapping;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.SimilarityAlg;

import java.util.*;
import java.util.stream.Collectors;

public class NodeCluster {
  private List<SemanticNode> baseNodes; // assert: non empty
  private List<SemanticNode> leftNodes;
  private List<SemanticNode> rightNodes;
  private double entropy;
  private NodeContext context;

  public NodeCluster() {
    this.baseNodes = new ArrayList<>();
    this.leftNodes = new ArrayList<>();
    this.rightNodes = new ArrayList<>();
    this.entropy = 0D;
    this.context = new NodeContext();
  }

  public void addNode(SemanticNode node, Side side) {
    // add nodes to merge
    switch (side) {
      case BASE:
        this.baseNodes.add(node);
        break;
      case OURS:
        this.leftNodes.add(node);
        break;
      case THEIRS:
        this.rightNodes.add(node);
        break;
    }
    // join the context
    this.context.join(node.getContext());
  }

  /**
   * Compute the content of node cluster
   *
   * @return
   */
  private double computeContentEntropy() {
    double entropy = 0D;
    int numOfNodes = getNumOfNodes();
    for (SemanticNode nX : getAllNodes()) {
      double pX = 1.0 / numOfNodes;
      double sum = 0D;
      for (SemanticNode nY : getAllNodes()) {
        double pY = 1.0 / numOfNodes;
        double simXY = SimilarityAlg.nodeSimilarity(nX, nY);
        sum += pY * simXY;
      }
      entropy += pX * Math.log(sum) / Math.log(2);
    }
    return Math.abs(entropy);
  }

  /**
   * Compute context entropy for incoming or outgoing edges
   *
   * @param inOrOut true == in
   * @return entropy >= 0.0
   */
  private double computeContextEntropy(boolean inOrOut) {
    double entropy = 0D;
    // views from graphs composes view set
    List<Set<Integer>> viewCollection = new ArrayList<>();

    // for every graph, compute types that are not 0 from vector as its view
    viewCollection.add(getViewFromGraph(baseNodes, inOrOut));
    viewCollection.add(getViewFromGraph(leftNodes, inOrOut));
    viewCollection.add(getViewFromGraph(rightNodes, inOrOut));

    viewCollection =
        viewCollection.stream()
            .filter(collection -> collection.size() > 0)
            .collect(Collectors.toList());
    // compute entropy from the view set
    int numOfViews = viewCollection.size();
    for (Set<Integer> x : viewCollection) {
      double pX = 1.0 / numOfViews;
      double sum = 0D;
      for (Set<Integer> y : viewCollection) {
        double pY = 1.0 / numOfViews;
        double simXY = SimilarityAlg.jaccard(x, y);
        sum += pY * simXY;
      }
      // sum != 0
      entropy += pX * Math.log(sum) / Math.log(2);
    }
    return Math.abs(entropy);
  }

  /**
   * Get view of edges from one graph
   *
   * @param nodes
   * @param inOrOut
   * @return
   */
  private Set<Integer> getViewFromGraph(List<SemanticNode> nodes, boolean inOrOut) {
    Set<Integer> view = new HashSet<>();
    for (SemanticNode node : nodes) {
      Map<Integer, Integer> vector =
          inOrOut ? node.getContext().getIncomingVector() : node.getContext().getOutgoingVector();
      vector.entrySet().stream()
          .filter(entry -> !entry.getValue().equals(0))
          .forEach(entry -> view.add(entry.getKey()));
    }
    return view;
  }

  private int getNumOfNodes() {
    return baseNodes.size() + leftNodes.size() + rightNodes.size();
  }

  public double getEntropy() {
    // recompute the entropy
    this.entropy =
        computeContentEntropy() + (computeContextEntropy(true) + computeContextEntropy(false)) / 2;
    return this.entropy;
  }

  public List<SemanticNode> getBaseNodes() {
    return baseNodes;
  }

  public List<SemanticNode> getLeftNodes() {
    return leftNodes;
  }

  public List<SemanticNode> getRightNodes() {
    return rightNodes;
  }

  public List<SemanticNode> getAllNodes() {
    List<SemanticNode> allNodes = new ArrayList<>();
    allNodes.addAll(baseNodes);
    allNodes.addAll(leftNodes);
    allNodes.addAll(rightNodes);
    return allNodes;
  }
}
