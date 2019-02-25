package edu.pku.intellimerge.core;

import edu.pku.intellimerge.io.Graph2CodePrinter;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.ThreewayMapping;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.CompilationUnitNode;
import edu.pku.intellimerge.model.node.NonTerminalNode;
import edu.pku.intellimerge.model.node.TerminalNode;
import edu.pku.intellimerge.util.FilesManager;
import org.apache.commons.lang3.tuple.Triple;
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
  public TwowayMatching b2oMatching;
  public TwowayMatching b2tMatching;
  public List<ThreewayMapping> mapping;
  private String resultDir; // merge result path
  private Graph<SemanticNode, SemanticEdge> oursGraph;
  private Graph<SemanticNode, SemanticEdge> baseGraph;
  private Graph<SemanticNode, SemanticEdge> theirsGraph;

  public ThreewayGraphMerger(
      String resultDir,
      Graph<SemanticNode, SemanticEdge> oursGraph,
      Graph<SemanticNode, SemanticEdge> baseGraph,
      Graph<SemanticNode, SemanticEdge> theirsGraph) {
    this.resultDir = resultDir;
    this.oursGraph = oursGraph;
    this.baseGraph = baseGraph;
    this.theirsGraph = theirsGraph;
    this.mapping = new ArrayList<>();
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
    b2oMatching = b2oMatcher.matching;
    b2tMatching = b2tMatcher.matching;

    // collect CU mapping that need to merge
    for (SemanticNode node : baseGraph.vertexSet()) {
      if (node instanceof CompilationUnitNode) {
        CompilationUnitNode cu = (CompilationUnitNode) node;
        if (cu.getNeedToMerge() == true) {
          ThreewayMapping mapping =
              new ThreewayMapping(
                  Optional.ofNullable(b2oMatching.one2oneMatchings.getOrDefault(node, null)),
                  Optional.of(node),
                  Optional.ofNullable(b2tMatching.one2oneMatchings.getOrDefault(node, null)));
          this.mapping.add(mapping);
        }
      }
    }
  }

  /**
   * Merge CUs according to the mapping
   *
   * @return
   */
  public List<String> threewayMerge() {
    // bottom up merge children of the needToMerge CU
    List<String> mergedFilePaths = new ArrayList<>();
    for (ThreewayMapping mapping : mapping) {
      if (mapping.baseNode.isPresent()) {
        // merge the CU by merging its content
        SemanticNode mergedCU = mergeSingleNode(mapping.baseNode.get());
        // merge the package declaration and imports
        CompilationUnitNode mergedPackageAndImports =
            mergePackageAndImports(mapping.baseNode.get());
        if (mergedCU != null && mergedPackageAndImports != null) {
          // save the merged result to file
          String resultFilePath =
              Graph2CodePrinter.printCU(
                  mergedCU, mergedPackageAndImports, FilesManager.formatPathSeparator(resultDir));
          mergedFilePaths.add(resultFilePath);
        }
      }
    }
    return mergedFilePaths;
  }

  private CompilationUnitNode mergePackageAndImports(SemanticNode node) {
    if (node instanceof CompilationUnitNode) {
      CompilationUnitNode mergedCU = (CompilationUnitNode) node;
      SemanticNode oursNode = b2oMatching.one2oneMatchings.getOrDefault(node, null);
      SemanticNode theirsNode = b2tMatching.one2oneMatchings.getOrDefault(node, null);
      if (oursNode != null && theirsNode != null) {
        CompilationUnitNode oursCU = (CompilationUnitNode) oursNode;
        CompilationUnitNode theirsCU = (CompilationUnitNode) theirsNode;
        mergedCU.setPackageStatement(
            mergeTextually(
                oursCU.getPackageStatement(),
                mergedCU.getPackageStatement(),
                theirsCU.getPackageStatement()));

        Set<String> union = new LinkedHashSet<>();
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

  /**
   * Merge a single node and its children in iterative way
   * @param node
   * @return
   */
  private SemanticNode mergeSingleNode(SemanticNode node) {
    // if node is terminal: merge and return result
    SemanticNode mergedNode = node.shallowClone();
    if (node instanceof TerminalNode) {
      TerminalNode mergedTerminal = (TerminalNode) mergedNode;
      SemanticNode oursNode = b2oMatching.one2oneMatchings.getOrDefault(node, null);
      SemanticNode theirsNode = b2tMatching.one2oneMatchings.getOrDefault(node, null);
      if (oursNode != null && theirsNode != null) {
        // exist in BothSides side
        TerminalNode oursTerminal = (TerminalNode) oursNode;
        TerminalNode baseTerminal = (TerminalNode) node;
        TerminalNode theirsTerminal = (TerminalNode) theirsNode;
        String mergedComment =
            mergeTextually(
                oursTerminal.getComment(), baseTerminal.getComment(), theirsTerminal.getComment());
        String mergedSignature =
            mergeTextually(
                oursTerminal.getOriginalSignature(),
                baseTerminal.getOriginalSignature(),
                theirsTerminal.getOriginalSignature());
        String mergedBody =
            mergeTextually(
                oursTerminal.getBody(), baseTerminal.getBody(), theirsTerminal.getBody());
        mergedTerminal.setComment(mergedComment);
        mergedTerminal.setOriginalSignature(mergedSignature);
        mergedTerminal.setBody(mergedBody);
        return mergedTerminal;
      } else {
        // deleted in one side --> delete
        return null;
      }
    } else {
      // nonterminal: iteratively merge its children
      List<SemanticNode> children = node.getChildren();
      NonTerminalNode mergedNonTerminal = (NonTerminalNode) mergedNode;
      for (SemanticNode child : children) {
        SemanticNode mergedChild = mergeSingleNode(child);
        if (mergedChild != null) {
          mergedNonTerminal.appendChild(mergedChild);
        }
      }
      // consider unmatched nodes as added ones
      // if parent matched, insert it into the children of parent, between nearest neighbors
      mergeUnmatchedNodes(node, mergedNonTerminal, b2oMatching);
      mergeUnmatchedNodes(node, mergedNonTerminal, b2tMatching);
      return mergedNonTerminal;
    }
  }

  /**
   * Merge unmatched nodes (added) from ours and theirs
   *
   * @param node
   * @param mergedNonTerminal
   * @param matching
   */
  private void mergeUnmatchedNodes(
      SemanticNode node, NonTerminalNode mergedNonTerminal, TwowayMatching matching) {
    SemanticNode matchedNodeOurs = matching.one2oneMatchings.getOrDefault(node, null);
    if (matchedNodeOurs != null) {
      for (SemanticNode addedOurs : matching.unmatchedNodes2) {
        SemanticNode parent = addedOurs.getParent();
        if (parent.equals(matchedNodeOurs)) {
          insertBetweenNeighbors(mergedNonTerminal, getNeighbors(parent, addedOurs));
        }
      }
    }
  }
  /**
   * Insert the added node betwen its neighbors
   *
   * @param node
   * @param triple
   */
  private void insertBetweenNeighbors(
      SemanticNode node, Triple<SemanticNode, SemanticNode, SemanticNode> triple) {
    boolean foundNeighor = false;
    if (triple.getLeft() != null) {
      int position = node.getChildPosition(triple.getLeft());
      if (position != -1) {
        node.insertChild(triple.getMiddle(), position + 1);
        foundNeighor = true;
      }
    }
    if (!foundNeighor && triple.getRight() != null) {
      int position = node.getChildPosition(triple.getRight());
      if (position != -1) {
        node.insertChild(triple.getMiddle(), position);
        foundNeighor = true;
      }
    }
    if (!foundNeighor) {
      node.appendChild(triple.getMiddle());
    }
  }
  /**
   * Get two neighbours before and after the child in its siblings
   *
   * @param parent
   * @param child
   * @return
   */
  private Triple<SemanticNode, SemanticNode, SemanticNode> getNeighbors(
      SemanticNode parent, SemanticNode child) {
    SemanticNode nodeBefore = null;
    SemanticNode nodeAfter = null;
    int position = parent.getChildPosition(child);
    if (position > 0) {
      nodeBefore = parent.getChildAtPosition(position - 1);
    }
    if (position < parent.getChildren().size() - 1) {
      nodeAfter = parent.getChildAtPosition(position + 1);
    }
    return Triple.of(nodeBefore, child, nodeAfter);
  }

  /**
   * Merge content string textually with jGit
   *
   * @param leftContent
   * @param baseContent
   * @param rightContent
   * @return
   */
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
          .formatMerge(
              output,
              mergeResult,
              Side.BASE.asString(),
              Side.OURS.asString(),
              Side.THEIRS.asString(),
              StandardCharsets.UTF_8);
      textualMergeResult = new String(output.toByteArray(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return textualMergeResult;
  }
}
