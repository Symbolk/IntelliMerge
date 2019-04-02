package edu.pku.intellimerge.core;

import edu.pku.intellimerge.core.matcher.ConstructorDeclMatcher;
import edu.pku.intellimerge.core.matcher.FieldDeclMatcher;
import edu.pku.intellimerge.core.matcher.MethodDeclMatcher;
import edu.pku.intellimerge.core.matcher.TypeDeclMatcher;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import org.jgrapht.Graph;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TwowayGraphMatcher implements Callable<TwowayMatching> {
  public TwowayMatching matching;

  private Graph<SemanticNode, SemanticEdge> graph1; // old graph(base)
  private Graph<SemanticNode, SemanticEdge> graph2; // new graph(ours/theirs)

  public TwowayGraphMatcher(
      Graph<SemanticNode, SemanticEdge> graph1, Graph<SemanticNode, SemanticEdge> graph2) {
    this.graph1 = graph1;
    this.graph2 = graph2;
    this.matching = new TwowayMatching();
  }

  /**
   * Map all node between 2 graphs, simply top-down by signature Assumption: nodes with the same
   * signature are matched
   */
  public void topDownMatch() {
    Set<SemanticNode> nodeSet1 = graph1.vertexSet();
    Set<SemanticNode> nodeSet2 = graph2.vertexSet();
    Map<Integer, SemanticNode> map1 =
        nodeSet1.stream()
            .filter(SemanticNode::needToMerge)
            .collect(
                Collectors.toMap(
                    SemanticNode::hashCodeSignature,
                    Function.identity(),
                    (o, n) -> o,
                    HashMap::new));
    Map<Integer, SemanticNode> map2 =
        nodeSet2.stream()
            .filter(SemanticNode::needToMerge)
            .collect(
                Collectors.toMap(
                    SemanticNode::hashCodeSignature,
                    Function.identity(),
                    (o, n) -> o,
                    HashMap::new));
    for (Entry<Integer, SemanticNode> entry : map1.entrySet()) {
      if (map2.containsKey(entry.getKey())) {
        // add the matched nodes into the matching relationships
        matching.one2oneMatchings.put(entry.getValue(), map2.get(entry.getKey()));
        // remove the mapped node from other
        map2.remove(entry.getKey());
      } else {
        matching.addUnmatchedNodes(entry.getValue(), true);
      }
    }
    map2.entrySet().forEach(entry -> matching.addUnmatchedNodes(entry.getValue(), false));
  }

  /** Bottom-up match unmatched nodes in the last step, considering some kinds of refactorings */
  public void bottomUpMatch() {
    // divide and conquer: match each type of nodes separately

    // 4. Types
    TypeDeclMatcher typeDeclMatcher = new TypeDeclMatcher();
    List<SemanticNode> unmatchedTypes1 =
        matching.unmatchedNodes1.getOrDefault(NodeType.CLASS, new ArrayList<>());
    List<SemanticNode> unmatchedTypes2 =
        matching.unmatchedNodes2.getOrDefault(NodeType.CLASS, new ArrayList<>());
    if (!unmatchedTypes1.isEmpty() && !unmatchedTypes2.isEmpty()) {
      typeDeclMatcher.matchClass(matching, unmatchedTypes1, unmatchedTypes2);
    }

    // 1. Methods
    // if only there are unmatched nodes, try to match
    MethodDeclMatcher methodDeclMatcher = new MethodDeclMatcher();
    List<SemanticNode> unmatchedMethods1 =
        matching.unmatchedNodes1.getOrDefault(NodeType.METHOD, new ArrayList<>());
    List<SemanticNode> unmatchedMethods2 =
        matching.unmatchedNodes2.getOrDefault(NodeType.METHOD, new ArrayList<>());
    // TODO avoid subjection
    if (!unmatchedMethods1.isEmpty() && !unmatchedMethods2.isEmpty()) {
      methodDeclMatcher.matchMethods(matching, unmatchedMethods1, unmatchedMethods2);
    }
    if (!unmatchedMethods2.isEmpty()) {
      methodDeclMatcher.matchExtractMethod(matching, unmatchedMethods2);
    }
    if (!unmatchedMethods2.isEmpty()) {}

    // 2. Fields
    FieldDeclMatcher fieldDeclMatcher = new FieldDeclMatcher();
    List<SemanticNode> unmatchedFields1 =
        matching.unmatchedNodes1.getOrDefault(NodeType.FIELD, new ArrayList<>());
    List<SemanticNode> unmatchedFields2 =
        matching.unmatchedNodes2.getOrDefault(NodeType.FIELD, new ArrayList<>());
    if (!unmatchedFields1.isEmpty() && !unmatchedFields2.isEmpty()) {
      fieldDeclMatcher.matchFields(matching, unmatchedFields1, unmatchedFields2);
    }

    // 3. Constructors
    ConstructorDeclMatcher constructorDeclMatcher = new ConstructorDeclMatcher();
    List<SemanticNode> unmatchedConstructors1 =
        matching.unmatchedNodes1.getOrDefault(NodeType.CONSTRUCTOR, new ArrayList<>());
    List<SemanticNode> unmatchedConstructors2 =
        matching.unmatchedNodes2.getOrDefault(NodeType.CONSTRUCTOR, new ArrayList<>());
    if (!unmatchedConstructors1.isEmpty() && !unmatchedConstructors2.isEmpty()) {
      constructorDeclMatcher.matchConstructors(
          matching, unmatchedConstructors1, unmatchedConstructors2);
    }

    matching.getOne2OneRefactoring();
  }

  /**
   * Sort the node list in the reverse hierarchy order, i.e. bottom up in AST
   *
   * @param unmatchedNodes
   * @return
   */
  private ArrayList<SemanticNode> sortUnmatchedNodes(List<SemanticNode> unmatchedNodes) {
    ArrayList<SemanticNode> unMatchedNodesSorted =
        new ArrayList(
            unmatchedNodes.stream()
                .sorted(Comparator.comparing(SemanticNode::getLevel).reversed())
                .collect(Collectors.toList()));
    return unMatchedNodesSorted;
  }

  @Override
  public TwowayMatching call() {
    topDownMatch();
    bottomUpMatch();
    return matching;
  }
}
