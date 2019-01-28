package edu.pku.intellimerge.util;

import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import info.debatty.java.stringsimilarity.Cosine;
import org.eclipse.jdt.core.dom.ASTParser;

import java.io.IOException;
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
    // navie string similarity of method signature
    similarity += methodSignature(n1.getQualifiedName(), n2.getQualifiedName());
    similarity += methodBody(n1.getBody(), n2.getBody());
    similarity /= 4;
    return similarity;
  }

  /**
   * Signature textual similarity, but method name and parameter types should be the most important
   *
   * @param s1
   * @param s2
   * @return
   */
  private static double methodSignature(String s1, String s2) {
    Cosine cosine = new Cosine();
    return cosine.similarity(s1, s2);
  }

  public static double methodContext(
      Map<EdgeType, List<SemanticNode>> edges1, Map<EdgeType, List<SemanticNode>> edges2) {
    double similarity = 0.0;
    int count = 0;
    // for every type of edge, calculate the similarity
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
        count++;
      } else {
        similarity += 0; // unsure
      }
    }
    if (count > 0) {
      similarity /= count;
    }
    return similarity;
  }

  /**
   * Compute method body subtree similarity based on gumtree
   *
   * @param body1
   * @param body2
   * @return
   */
  public static double methodBody(String body1, String body2) {
    double similarity = 0D;
    try {
      JdtTreeGenerator generator = new JdtTreeGenerator();
      generator.setKind(ASTParser.K_STATEMENTS);
      TreeContext baseContext = generator.generateFromString(body1);
      TreeContext othersContext = generator.generateFromString(body2);
      //            TreeContext src = Generators.getInstance().getTree(fSrc.getAbsolutePath());
      //            TreeContext dst = Generators.getInstance().getTree(fDst.getAbsolutePath());
      ITree baseRoot = baseContext.getRoot();
      ITree othersRoot = othersContext.getRoot();
      baseRoot.getDescendants();
      Matcher matcher = Matchers.getInstance().getMatcher(baseRoot, othersRoot);
      baseContext.importTypeLabels(othersContext);
      matcher.match();
      similarity = matcher.jaccardSimilarity(baseRoot, othersRoot);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return similarity;
  }
  /**
   * Jaccard = Intersection/Union [0,1]
   *
   * @param s1
   * @param s2
   * @return
   */
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
