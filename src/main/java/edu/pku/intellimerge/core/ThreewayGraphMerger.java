package edu.pku.intellimerge.core;

import com.github.javaparser.ast.stmt.BlockStmt;
import edu.pku.intellimerge.io.Graph2CodePrinter;
import edu.pku.intellimerge.model.Mapping;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.node.CompilationUnitNode;
import edu.pku.intellimerge.model.node.NonTerminalNode;
import edu.pku.intellimerge.model.node.TerminalNode;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;
import org.jgrapht.Graph;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Only the diff file/cu needs to be merged */
public class ThreewayGraphMerger {
  private String resultFolder; // merge result path
  private Graph<SemanticNode, SemanticEdge> oursGraph;
  private Graph<SemanticNode, SemanticEdge> baseGraph;
  private Graph<SemanticNode, SemanticEdge> theirsGraph;
  private Map<SemanticNode, SemanticNode> b2oMatchings;
  private Map<SemanticNode, SemanticNode> b2tMatchings;
  private List<Mapping> mappings;

  public ThreewayGraphMerger(
          String resultFolder,
      Graph<SemanticNode, SemanticEdge> oursGraph,
      Graph<SemanticNode, SemanticEdge> baseGraph,
      Graph<SemanticNode, SemanticEdge> theirsGraph) {
    this.resultFolder = resultFolder;
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
//    b2oMatcher.topDownMatch();
//    b2oMatcher.bottomUpMatch();
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
        // merge the CU by merging its content
        SemanticNode mergedCU = mergeSingleNode(mapping.baseNode.get());
        // merge the package declaration and imports
        CompilationUnitNode  mergedPackageAndImports= mergePackageAndImports(mapping.baseNode.get());
        if (mergedCU != null) {
          // save the merged result to file
          Graph2CodePrinter.printCU(mergedCU, mergedPackageAndImports, resultFolder);
        }
      }
    }
  }

  private CompilationUnitNode mergePackageAndImports(SemanticNode node) {
    if (node instanceof CompilationUnitNode) {
      CompilationUnitNode mergedCU = (CompilationUnitNode) node;
      SemanticNode oursNode = b2oMatchings.getOrDefault(node, null);
      SemanticNode theirsNode = b2oMatchings.getOrDefault(node, null);
      if (oursNode != null && theirsNode != null) {
        CompilationUnitNode oursCU = (CompilationUnitNode) oursNode;
        CompilationUnitNode theirsCU = (CompilationUnitNode) theirsNode;
        mergedCU.setPackageStatement(
            mergeTextually(
                oursCU.getPackageStatement(),
                mergedCU.getPackageStatement(),
                theirsCU.getPackageStatement()));

        Set<String> union = new HashSet<>();
        union.addAll(oursCU.getImportStatements());
        union.addAll(theirsCU.getImportStatements());
        mergedCU.setImportStatements(union);
        return mergedCU;
      }
    }
    return null;
  }

  /** Merge 3 graphs simply with jgrapht functions */
  private void mergeByAdding() {
    Graph<SemanticNode, SemanticEdge> mergedGraph =
        baseGraph; // inference copy, in fact mergedGraph points to baseGraph
    //      System.out.println(Graphs.addGraph(mergedGraph, oursGraph));
    //    System.out.println(Graphs.addGraph(mergedGraph, theirsGraph));
    //    SemanticGraphExporter.printAsDot(mergedGraph);
  }

  private SemanticNode mergeSingleNode(SemanticNode node) {
    // if node is terminal: merge and return result
    SemanticNode mergedNode = node.shallowClone();
    if (node instanceof TerminalNode) {
      TerminalNode mergedTerminal = (TerminalNode) mergedNode;
      SemanticNode oursNode = b2oMatchings.getOrDefault(node, null);
      SemanticNode theirsNode = b2tMatchings.getOrDefault(node, null);
      if (oursNode != null && theirsNode != null) {
        // exist in both side
        TerminalNode oursTerminal = (TerminalNode) oursNode;
        TerminalNode baseTerminal = (TerminalNode) node;
        TerminalNode theirsTerminal = (TerminalNode) theirsNode;
        String mergeResult =
            mergeTextually(
                oursTerminal.getBody(), baseTerminal.getBody(), theirsTerminal.getBody());
        mergedTerminal.setBody(mergeResult);
      } else {
        // deleted in one side --> delete
        mergedTerminal.setBody("");
      }
      return mergedTerminal;
    } else {
      // nonterminal: iteratively merge its children
      List<SemanticNode> children = node.getChildren();
      NonTerminalNode mergedNonTerminal = (NonTerminalNode) mergedNode;
      for (SemanticNode child : children) {
        mergedNonTerminal.addChild(mergeSingleNode(child));
      }
      return mergedNonTerminal;
    }
  }

  private String mergeTextually(String leftContent, String baseContent, String rightContent) {
    String textualMergeResult = null;
    try {
      RawTextComparator textComparator = RawTextComparator.WS_IGNORE_ALL; //  ignoreWhiteSpaces
      @SuppressWarnings("rawtypes")
      MergeResult mergeResult =
          new MergeAlgorithm()
              .merge(
                  textComparator,
                  new RawText(Constants.encode(baseContent)),
                  new RawText(Constants.encode(leftContent)),
                  new RawText(Constants.encode(rightContent)));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      (new MergeFormatter())
          .formatMerge(output, mergeResult, "BASE", "MINE", "YOURS", StandardCharsets.UTF_8);
      textualMergeResult = new String(output.toByteArray(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return textualMergeResult;
  }
}
