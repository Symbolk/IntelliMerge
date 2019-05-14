package edu.pku.intellimerge.model.mapping;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.SimilarityAlg;

import java.util.ArrayList;
import java.util.List;

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
    // recompute the entropy
    // TODO: add the context entropy
    this.entropy = computeContentEntropy();
  }

  private double computeContentEntropy() {
    double contentEntropy = 0D;
    int numOfNodes = getNumOfNodes();
    for (SemanticNode nX : getAllNodes()) {
      double pX = 1.0 / numOfNodes;
      double sum = 0D;
      for (SemanticNode nY : getAllNodes()) {
        double pY = 1.0 / numOfNodes;
        double simXY = SimilarityAlg.nodeSimilarity(nX, nY);
        sum += pY * simXY;
      }
      contentEntropy += pX * Math.log(sum) / Math.log(2);
    }
    return Math.abs(contentEntropy) * -1;
  }

  private int getNumOfNodes() {
    return baseNodes.size() + leftNodes.size() + rightNodes.size();
  }

  public double getEntropy() {
    return entropy;
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
