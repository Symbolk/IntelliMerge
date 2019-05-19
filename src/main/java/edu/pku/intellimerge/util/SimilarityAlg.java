package edu.pku.intellimerge.util;

import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.mapping.EdgeLabel;
import edu.pku.intellimerge.model.node.CompositeNode;
import edu.pku.intellimerge.model.node.FieldDeclNode;
import edu.pku.intellimerge.model.node.TerminalNode;
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
   * Compute the similarity between two nodes
   *
   * @param node1
   * @param node2
   * @return
   */
  public static double nodeSimilarity(SemanticNode node1, SemanticNode node2) {
    if (node1 instanceof TerminalNode) {
      return terminalNodeSimilarity((TerminalNode) node1, (TerminalNode) node2);
    } else {
      return compositeNodeSimilarity((CompositeNode) node1, (CompositeNode) node2);
    }
  }

  /**
   * Compute the similarity between two terminalNodeSimilarity declarations, considering signature
   * as well as context
   *
   * @param n1
   * @param n2
   * @return
   */
  public static double terminalNodeSimilarity(TerminalNode n1, TerminalNode n2) {
    double similarity = 0.0;
    // naive average in all dimensions of context(incoming and outgoing edges)
    similarity += contextSimilarity2(n1, n2);
    // naive string similarity of terminalNodeSimilarity signature
    similarity += stringSimilarity(n1.getQualifiedName(), n2.getQualifiedName());
    similarity += bodyASTSimilarity(n1.getBody(), n2.getBody());
    similarity /= 3;
    return similarity;
  }

  /**
   * Compute the similarity of context edges of 2 nodes
   *
   * @param node1
   * @param node2
   * @return
   */
  public static double contextSimilarity2(SemanticNode node1, SemanticNode node2) {
    // compute the cosine similarity
    double inVectorSim =
        vectorCosineSimilarity(
            node1.context.getIncomingVector(), node2.context.getIncomingVector());
    double outVectorSim =
        vectorCosineSimilarity(
            node1.context.getOutgoingVector(), node2.context.getOutgoingVector());
    return (inVectorSim + outVectorSim) / 2; // average for now
  }

  /**
   * Compute the cosine similarity of two vectors
   *
   * @param vector1
   * @param vector2
   * @return
   */
  private static double vectorCosineSimilarity(
      Map<Integer, Integer> vector1, Map<Integer, Integer> vector2) {
    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;

    for (Integer index = 0; index < EdgeLabel.values().length; ++index) {
      Integer a = vector1.get(index);
      Integer b = vector2.get(index);
      dotProduct += a * b;
      norm1 += Math.pow(a, 2);
      norm2 += Math.pow(b, 2);
    }
    norm1 = (Math.sqrt(norm1));
    norm2 = (Math.sqrt(norm2));

    double product = norm1 * norm2;
    return product == 0.0 ? 0.0 : dotProduct / product;
  }

  /**
   * Compute the similarity between two terminalNodeSimilarity declarations, considering signature
   * as well as context
   *
   * @param n1
   * @param n2
   * @return
   */
  public static double compositeNodeSimilarity(CompositeNode n1, CompositeNode n2) {
    double similarity = 0.0;
    // naive average in all dimensions of context(incoming and outgoing edges)
    similarity += contextSimilarity2(n1, n2);
    // navie string similarity of terminalNodeSimilarity signature
    similarity += 10 * stringSimilarity(n1.getQualifiedName(), n2.getQualifiedName());
    similarity /= 12;
    return similarity;
  }

  /**
   * Signature textual similarity, but terminalNodeSimilarity name and parameter types should be the
   * most important
   *
   * @param s1
   * @param s2
   * @return
   */
  private static double stringSimilarity(String s1, String s2) {
    Cosine cosine = new Cosine();
    return cosine.similarity(s1, s2);
  }

  public static double contextSimilarity(
      Map<EdgeType, List<SemanticNode>> edges1, Map<EdgeType, List<SemanticNode>> edges2) {
    double similarity = 0.0;
    int count = 0;
    // for every type of edge, calculate the similarity
    for (Map.Entry<EdgeType, List<SemanticNode>> entry : edges1.entrySet()) {
      Set<String> targetQNames1 =
          entry.getValue().stream().map(SemanticNode::getQualifiedName).collect(Collectors.toSet());
      Set<String> targetQNames2 =
          edges2.get(entry.getKey()).stream()
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
   * Compute terminalNodeSimilarity body subtree similarity based on gumtree
   *
   * @param body1
   * @param body2
   * @return
   */
  public static double bodyASTSimilarity(String body1, String body2) {
    double similarity = 0D;
    try {
      JdtTreeGenerator generator = new JdtTreeGenerator();
      generator.setKind(ASTParser.K_STATEMENTS);
      TreeContext baseContext = generator.generateFrom().string(body1);
      TreeContext othersContext = generator.generateFrom().string(body2);
      //            TreeContext src = Generators.getInstance().getTree(fSrc.getAbsolutePath());
      //            TreeContext dst = Generators.getInstance().getTree(fDst.getAbsolutePath());
      ITree baseRoot = baseContext.getRoot();
      ITree othersRoot = othersContext.getRoot();
      //      baseRoot.getDescendants();
      Matcher matcher = Matchers.getInstance().getMatcher();
      MappingStore mappings = matcher.match(baseRoot, othersRoot);
      similarity = SimilarityMetrics.chawatheSimilarity(baseRoot, othersRoot, mappings);

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
  public static double jaccard(Set s1, Set s2) {
    Set<String> union = new HashSet<>();
    union.addAll(s1);
    union.addAll(s2);
    Set<String> intersection = new HashSet<>();
    intersection.addAll(s1);
    intersection.retainAll(s2);

    return union.size() == 0 ? 0D : (double) intersection.size() / union.size();
  }

  /**
   * Compute field similarity according to field type, name and initializer
   *
   * @param f1
   * @param f2
   * @return
   */
  public static double field(FieldDeclNode f1, FieldDeclNode f2) {
    double similarity = 0.0;
    String fieldAsString1 = f1.getFieldType() + f1.getFieldName() + f1.getBody();
    String fieldAsString2 = f2.getFieldType() + f2.getFieldName() + f2.getBody();
    similarity = stringSimilarity(fieldAsString1, fieldAsString2);
    return similarity;
  }
}
