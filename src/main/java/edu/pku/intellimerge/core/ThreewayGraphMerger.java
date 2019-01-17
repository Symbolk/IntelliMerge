package edu.pku.intellimerge.core;

import edu.pku.intellimerge.model.Mapping;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.node.CompilationUnitNode;
import edu.pku.intellimerge.model.node.FieldDeclNode;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;
import org.jgrapht.Graph;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Only the diff file/cu needs to be merged */
public class ThreewayGraphMerger {
  private Graph<SemanticNode, SemanticEdge> oursGraph;
  private Graph<SemanticNode, SemanticEdge> baseGraph;
  private Graph<SemanticNode, SemanticEdge> theirsGraph;
  private Map<SemanticNode, SemanticNode> b2oMatchings;
  private Map<SemanticNode, SemanticNode> b2tMatchings;
  private List<Mapping> mappings;

  public ThreewayGraphMerger(
      Graph<SemanticNode, SemanticEdge> oursGraph,
      Graph<SemanticNode, SemanticEdge> baseGraph,
      Graph<SemanticNode, SemanticEdge> theirsGraph) {
    this.oursGraph = oursGraph;
    this.baseGraph = baseGraph;
    this.theirsGraph = theirsGraph;
    this.mappings = new ArrayList<>();
  }

  /** Threeway map the CUs that need to merge */
  public void threewayMap() {
    // two way matching to get three way mapping
    TwowayGraphMatcher b2oMatcher = new TwowayGraphMatcher(baseGraph, oursGraph);
    TwowayGraphMatcher b2tMatcher = new TwowayGraphMatcher(baseGraph, theirsGraph);
    b2oMatcher.topDownMatch();
    b2oMatcher.bottomUpMatch();
    b2tMatcher.topDownMatch();
    b2tMatcher.bottomUpMatch();
    b2oMatchings = b2oMatcher.matchings;
    b2tMatchings = b2tMatcher.matchings;

    // collect CU mappings that need to merge
    for (SemanticNode node : baseGraph.vertexSet()) {
      if (node instanceof CompilationUnitNode) {
        CompilationUnitNode cu = (CompilationUnitNode) node;
        if (cu.needToMerge == true) {
          Mapping mapping =
              new Mapping(
                  Optional.ofNullable(b2oMatchings.getOrDefault(node, null)),
                  Optional.of(node),
                  Optional.ofNullable(b2tMatchings.getOrDefault(node, null)));
          mappings.add(mapping);
        }
      }
    }
  }

  /** Merge CUs according to the mappings */
  public void threewayMerge() {
    threewayMap();
    // bottom up merge children of the needToMerge CU
    for (Mapping mapping : mappings) {
      if (mapping.baseNode.isPresent()) {
        mergeSingleNode(mapping.baseNode.get());
      }
    }
    // save the content to a clone CU node
    mappings.size();
    // once merging done, save the CU content to result file
  }

  /** Merge 3 graphs simply with jgrapht functions */
  private void mergeSimply() {
    Graph<SemanticNode, SemanticEdge> mergedGraph =
        baseGraph; // inference copy, in fact mergedGraph points to baseGraph
    //      System.out.println(Graphs.addGraph(mergedGraph, oursGraph));
    //    System.out.println(Graphs.addGraph(mergedGraph, theirsGraph));
    //    SemanticGraphExporter.printAsDot(mergedGraph);
  }

  private void mergeSingleNode(SemanticNode node) {
    // if node is terminal: merge and return result
    if (node instanceof FieldDeclNode || node instanceof MethodDeclNode) {
      SemanticNode oursNode = b2oMatchings.getOrDefault(node, null);
      SemanticNode theirsNode = b2tMatchings.getOrDefault(node, null);
      if (oursNode != null && theirsNode != null) {
        // exist in both side
        String mergeResult =
            mergeNodeContent(oursNode.getContent(), node.getContent(), theirsNode.getContent());
        node.setContent(mergeResult);
      } else {
        // deleted in one side
        node.setContent("");
      }
    } else {
      // nonterminal: iteratively merge its children
      List<SemanticNode> children = node.getChildren();
      children.forEach(this::mergeSingleNode);
    }
  }

  private String mergeNodeContent(String leftContent, String baseContent, String rightContent) {
    String textualMergeResult = null;
    try {
      RawTextComparator textComparator = RawTextComparator.WS_IGNORE_ALL; //  ignoreWhiteSpaces
      @SuppressWarnings("rawtypes")
      MergeResult mergeCommand =
          new MergeAlgorithm()
              .merge(
                  textComparator,
                  new RawText(Constants.encode(baseContent)),
                  new RawText(Constants.encode(leftContent)),
                  new RawText(Constants.encode(rightContent)));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      (new MergeFormatter())
          .formatMerge(output, mergeCommand, "BASE", "MINE", "YOURS", Constants.CHARACTER_ENCODING);
      textualMergeResult = new String(output.toByteArray(), Constants.CHARACTER_ENCODING);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return textualMergeResult;
  }
}
