package edu.pku.intellimerge.model.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.RefactoringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Stores matching relationships between two graphs */
public class TwowayMatching {
  private static final Logger logger = LoggerFactory.getLogger(TwowayMatching.class);

  // 2 kinds of matching: match by unchanged signature & signature changed but match by their roles
  public BiMap<SemanticNode, SemanticNode> one2oneMatchings; // confidence: 1
  public Map<NodeType, List<SemanticNode>> unmatchedNodes1; // possibly deleted nodes
  public Map<NodeType, List<SemanticNode>> unmatchedNodes2; // possibly added nodes
  public List<Refactoring> refactorings; // save the detected matched node pairs due to refactorings

  public TwowayMatching() {
    this.one2oneMatchings = HashBiMap.create();
    this.unmatchedNodes1 = new LinkedHashMap<>();
    this.unmatchedNodes2 = new LinkedHashMap<>();

    this.refactorings = new ArrayList<>();
  }

  /**
   * Add the refactoring detected
   *
   * @param node1 from the base graph
   * @param node2 from the other graph
   * @param refactoringType
   * @param confidence
   */
  public void markRefactoring(
      SemanticNode node1, SemanticNode node2, RefactoringType refactoringType, double confidence) {
    Refactoring refactoring =
        new Refactoring(refactoringType, node1.getNodeType(), confidence, node1, node2);
    refactorings.add(refactoring);
  }

  /** Collect one2one matchings from the detect refactorings, which can be merged later */
  public void getOne2OneRefactoring() {
    List<Refactoring> oneToOneRefactorings =
        refactorings.stream().filter(Refactoring::isOneToOne).collect(Collectors.toList());
    for (Refactoring refactoring : oneToOneRefactorings) {
      one2oneMatchings.put(refactoring.getSource(), refactoring.getTarget());
    }
  }

  /**
   * Add the given node to unmatched nodes list according to type
   *
   * @param node
   * @param isInBase whether the unmatched node is in base(1)
   */
  public void addUnmatchedNodes(SemanticNode node, boolean isInBase) {
    if (isInBase) {
      if (unmatchedNodes1.containsKey(node.getNodeType())) {
        unmatchedNodes1.get(node.getNodeType()).add(node);
      } else {
        unmatchedNodes1.put(node.getNodeType(), new ArrayList<>());
        unmatchedNodes1.get(node.getNodeType()).add(node);
      }
    } else {
      if (unmatchedNodes2.containsKey(node.getNodeType())) {
        unmatchedNodes2.get(node.getNodeType()).add(node);
      } else {
        unmatchedNodes2.put(node.getNodeType(), new ArrayList<>());
        unmatchedNodes2.get(node.getNodeType()).add(node);
      }
    }
  }
}
