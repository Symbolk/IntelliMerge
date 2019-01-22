package edu.pku.intellimerge.util;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import info.debatty.java.stringsimilarity.Cosine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SimilarityAlg {

  /**
   * Compute the similarity between two method declarations, considering signature as well as
   * context
   *
   * @param n1
   * @param n2
   * @return
   */
  public static double method(MethodDeclNode n1, MethodDeclNode n2) {
    double similarity = 0.0;
    // naive average in all dimensions of context(incoming and outgoing edges)
    similarity += methodContext(n1.incomingEdges, n2.incomingEdges);
    similarity += methodContext(n1.outgoingEdges, n2.outgoingEdges);
    similarity /= (n1.incomingEdges.size() + n1.outgoingEdges.size());
    // navie string similarity of method signature
    similarity += methodSignature(n1.getQualifiedName(), n2.getQualifiedName());
    similarity /= 2;
    return similarity;
  }

  private static double methodSignature(String s1, String s2) {
    Cosine cosine = new Cosine();
    return cosine.similarity(s1, s2);
  }

  public static double methodContext(
      Map<EdgeType, List<SemanticNode>> edges1, Map<EdgeType, List<SemanticNode>> edges2) {
    double similarity = 0.0;
    for (Map.Entry<EdgeType, List<SemanticNode>> entry : edges1.entrySet()) {
      Set<String> targetQNames1 =
          entry.getValue().stream().map(SemanticNode::getQualifiedName).collect(Collectors.toSet());
      Set<String> targetQNames2 =
          edges2
              .get(entry.getKey())
              .stream()
              .map(SemanticNode::getQualifiedName)
              .collect(Collectors.toSet());
      if (targetQNames1.size() > 0 && targetQNames2.size() > 0) {
        similarity += jaccard(targetQNames1, targetQNames2);
      } else {
        similarity += 0; // unsure
      }
    }
    return similarity;
  }

  private static double jaccard(Set s1, Set s2) {
    Set<String> union = new HashSet<>();
    union.addAll(s1);
    union.addAll(s2);
    Set<String> intersection = new HashSet<>();
    intersection.addAll(s1);
    intersection.retainAll(s2);

    return (double) intersection.size() / union.size();
  }
}
