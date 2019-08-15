package edu.pku.intellimerge.core.matcher;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.CompilationUnitNode;

import java.util.ArrayList;
import java.util.List;

public class CUMatcher {
  /**
   * Match CUs that slip from topdown because base is empty
   *
   * @param matching
   * @param nodes1
   * @param nodes2
   */
  public void matchCUs(
      TwowayMatching matching, List<SemanticNode> nodes1, List<SemanticNode> nodes2) {
    // to avoid ConcurrentModificationException
    List<SemanticNode> nodes1Clone = new ArrayList<>(nodes1);
    List<SemanticNode> nodes2Clone = new ArrayList<>(nodes2);
    for (SemanticNode node1 : nodes1Clone) {
      for (SemanticNode node2 : nodes2Clone) {
        if (((CompilationUnitNode) node1)
            .getRelativePath()
            .equals(((CompilationUnitNode) node2).getRelativePath())) {
          matching.one2oneMatchings.put(node1, node2);
          matching.unmatchedNodes1.get(NodeType.COMPILATION_UNIT).remove(node1);
          matching.unmatchedNodes2.get(NodeType.COMPILATION_UNIT).remove(node2);
        }
      }
    }
  }
}
