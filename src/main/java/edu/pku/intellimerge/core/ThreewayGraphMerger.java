package edu.pku.intellimerge.core;

import edu.pku.intellimerge.io.Graph2CodePrinter;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.ThreewayMapping;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.model.node.*;
import edu.pku.intellimerge.util.Utils;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/** Only the diff file/cu needs to be merged */
public class ThreewayGraphMerger {
  public TwowayMatching b2oMatching;
  public TwowayMatching b2tMatching;
  public List<ThreewayMapping> mapping;
  private Logger logger = LoggerFactory.getLogger(ThreewayGraphMerger.class);
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

    try {
      ExecutorService executorService = Executors.newFixedThreadPool(2);
      Future<TwowayMatching> task1 = executorService.submit(b2oMatcher);
      Future<TwowayMatching> task2 = executorService.submit(b2tMatcher);

      b2oMatching = task1.get();
      b2tMatching = task2.get();

      executorService.shutdown();

      // collect CU mapping that need to merge
      Set<SemanticNode> internalAndNeedToMergeNodes =
          baseGraph
              .vertexSet()
              .stream()
              .filter(SemanticNode::isInternal)
              .filter(SemanticNode::needToMerge)
              .collect(Collectors.toSet());
      for (SemanticNode node : internalAndNeedToMergeNodes) {
        if (node instanceof CompilationUnitNode) {
          CompilationUnitNode cu = (CompilationUnitNode) node;
          if (cu.needToMerge() == true) {
            ThreewayMapping mapping =
                new ThreewayMapping(
                    Optional.ofNullable(b2oMatching.one2oneMatchings.getOrDefault(node, null)),
                    Optional.of(node),
                    Optional.ofNullable(b2tMatching.one2oneMatchings.getOrDefault(node, null)));
            this.mapping.add(mapping);
          }
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
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
        CompilationUnitNode mergedPackageAndImports = mergeCUHeader(mapping.baseNode.get());
        if (mergedCU != null && mergedPackageAndImports != null) {
          // save the merged result to file
          String resultFilePath =
              Graph2CodePrinter.printCU(
                  mergedCU, mergedPackageAndImports, Utils.formatPathSeparator(resultDir));
          mergedFilePaths.add(resultFilePath);
        }
      }
    }
    return mergedFilePaths;
  }

  /**
   * Merge the header part of CU, including comment, package and imports
   *
   * @param node
   * @return a CUNode with header merged
   */
  private CompilationUnitNode mergeCUHeader(SemanticNode node) {
    if (node instanceof CompilationUnitNode) {
      CompilationUnitNode mergedCU = (CompilationUnitNode) node;
      SemanticNode oursNode = b2oMatching.one2oneMatchings.getOrDefault(node, null);
      SemanticNode theirsNode = b2tMatching.one2oneMatchings.getOrDefault(node, null);
      if (oursNode != null && theirsNode != null) {
        CompilationUnitNode oursCU = (CompilationUnitNode) oursNode;
        CompilationUnitNode theirsCU = (CompilationUnitNode) theirsNode;
        mergedCU.setComment(
            mergeTextually(oursCU.getComment(), node.getComment(), theirsCU.getComment()));
        mergedCU.setPackageStatement(
            mergeTextually(
                oursCU.getPackageStatement(),
                mergedCU.getPackageStatement(),
                theirsCU.getPackageStatement()));

        // conservative strategy: remove no imports in case of latent bugs
        Set<String> union = new LinkedHashSet<>();
        union.addAll(oursCU.getImportStatements());
        union.addAll(theirsCU.getImportStatements());
        mergedCU.setImportStatements(union);
        return mergedCU;
      }
    }
    return null;
  }

  /**
   * Merge a single node and its children in iterative way
   *
   * @param node
   * @return
   */
  private SemanticNode mergeSingleNode(SemanticNode node) {
    // if node is terminal: merge and return result
    SemanticNode mergedNode = node.shallowClone();
    SemanticNode oursNode = b2oMatching.one2oneMatchings.getOrDefault(node, null);
    SemanticNode theirsNode = b2tMatching.one2oneMatchings.getOrDefault(node, null);
    if (node instanceof TerminalNode) {
      TerminalNode mergedTerminal = (TerminalNode) mergedNode;
      if (oursNode != null && theirsNode != null) {
        // exist in BothSides side
        TerminalNode oursTerminal = (TerminalNode) oursNode;
        TerminalNode baseTerminal = (TerminalNode) node;
        TerminalNode theirsTerminal = (TerminalNode) theirsNode;
        String mergedComment =
            mergeTextually(
                oursTerminal.getComment(), baseTerminal.getComment(), theirsTerminal.getComment());
        List<String> mergedAnnotations =
            mergeByUnion(
                oursTerminal.getAnnotations(),
                baseTerminal.getAnnotations(),
                theirsTerminal.getAnnotations());
        List<String> mergedModifiers =
            mergeByUnion(
                oursTerminal.getModifiers(),
                baseTerminal.getModifiers(),
                theirsTerminal.getModifiers());
        String mergedSignature = mergeComponents(oursTerminal, baseTerminal, theirsTerminal);
        String mergedBody =
            mergeTextually(
                oursTerminal.getBody(), baseTerminal.getBody(), theirsTerminal.getBody());
        mergedTerminal.setComment(mergedComment);
        mergedTerminal.setAnnotations(mergedAnnotations);
        mergedTerminal.setModifiers(mergedModifiers);
        mergedTerminal.setOriginalSignature(mergedSignature);
        mergedTerminal.setBody(mergedBody);
        return mergedTerminal;
      } else {
        // deleted in one side --> delete
        return null;
      }
    } else {
      // nonterminal
      if (oursNode != null && theirsNode != null) {
        NonTerminalNode mergedNonTerminal = (NonTerminalNode) mergedNode;

        // merge the comment and signature
        String mergedComment =
            mergeTextually(oursNode.getComment(), node.getComment(), theirsNode.getComment());
        List<String> mergedAnnotations =
            mergeByUnion(
                oursNode.getAnnotations(), node.getAnnotations(), theirsNode.getAnnotations());
        List<String> mergedModifiers =
            mergeByUnion(oursNode.getModifiers(), node.getModifiers(), theirsNode.getModifiers());
        String mergedSignature = mergeComponents(oursNode, node, theirsNode);
        mergedNonTerminal.setComment(mergedComment);
        mergedNonTerminal.setAnnotations(mergedAnnotations);
        mergedNonTerminal.setModifiers(mergedModifiers);
        mergedNonTerminal.setOriginalSignature(mergedSignature);

        // iteratively merge its children
        List<SemanticNode> children = node.getChildren();
        for (SemanticNode child : children) {
          SemanticNode mergedChild = mergeSingleNode(child);
          if (mergedChild != null) {
            mergedNonTerminal.appendChild(mergedChild);
          }
        }
        // consider unmatched nodes as added ones
        // if parent matched, insert it into the children of parent, between nearest neighbors
        // TODO deduplicate added nodes
        mergeUnmatchedNodes(node, mergedNonTerminal, b2oMatching);
        mergeUnmatchedNodes(node, mergedNonTerminal, b2tMatching);

        return mergedNonTerminal;
      } else {
        // deleted in one side --> delete
        return null;
      }
    }
  }

  /**
   * Merge signature of terminal nodes by merging composing components
   *
   * @param ours
   * @param base
   * @param theirs
   */
  private String mergeComponents(SemanticNode ours, SemanticNode base, SemanticNode theirs) {
    StringBuilder builder = new StringBuilder();
    if (ours.getNodeType().equals(NodeType.METHOD)) {
      MethodDeclNode oursMD = (MethodDeclNode) ours;
      MethodDeclNode baseMD = (MethodDeclNode) base;
      MethodDeclNode theirsMD = (MethodDeclNode) theirs;

      if (oursMD.getTypeParameters().size()
              + baseMD.getTypeParameters().size()
              + theirsMD.getTypeParameters().size()
          > 0) {
        builder
            .append("<")
            .append(
                mergeByUnionToString(
                    oursMD.getTypeParameters(),
                    baseMD.getTypeParameters(),
                    theirsMD.getTypeParameters(),
                    ","))
            .append("> ");
      }
      builder
          .append(
              mergeTextually(
                  oursMD.getReturnType(), baseMD.getReturnType(), theirsMD.getReturnType()))
          .append(" ");
      builder
          .append(
              mergeTextually(
                  oursMD.getMethodName(), baseMD.getMethodName(), theirsMD.getMethodName()))
          .append("(");
      // parameters
      builder
          .append(
              mergeTextually(
                  oursMD.getParameterString(),
                  baseMD.getParameterString(),
                  theirsMD.getParameterString()))
          .append(")");
      if (oursMD.getThrowExceptions().size()
              + baseMD.getThrowExceptions().size()
              + theirsMD.getThrowExceptions().size()
          > 0) {
        builder
            .append(" throws ")
            .append(
                mergeByUnionToString(
                    oursMD.getThrowExceptions(),
                    baseMD.getThrowExceptions(),
                    theirsMD.getThrowExceptions(),
                    ","));
      }
    } else if (ours.getNodeType().equals(NodeType.FIELD)) {
      FieldDeclNode oursFD = (FieldDeclNode) ours;
      FieldDeclNode baseFD = (FieldDeclNode) base;
      FieldDeclNode theirsFD = (FieldDeclNode) theirs;
      builder
          .append(
              mergeTextually(oursFD.getFieldType(), baseFD.getFieldType(), theirsFD.getFieldType()))
          .append(" ");
      builder
          .append(
              mergeTextually(oursFD.getFieldName(), baseFD.getFieldName(), theirsFD.getFieldName()))
          .append(" ");
    } else {
      builder.append(
          mergeTextually(
              ours.getOriginalSignature(),
              base.getOriginalSignature(),
              theirs.getOriginalSignature()));
    }
    return builder.toString();
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
      for (Map.Entry<NodeType, List<SemanticNode>> entry : matching.unmatchedNodes2.entrySet()) {
        for (SemanticNode newlyAdded : entry.getValue()) {
          SemanticNode parent = newlyAdded.getParent();
          if (parent.equals(matchedNodeOurs)) {
            insertBetweenNeighbors(mergedNonTerminal, getNeighbors(parent, newlyAdded));
          }
        }
      }
    }
  }
  /**
   * Insert the added node betwen its neighbors
   *
   * @param parent
   * @param triple
   */
  private void insertBetweenNeighbors(
      NonTerminalNode parent, Triple<SemanticNode, SemanticNode, SemanticNode> triple) {
    boolean foundNeighor = false;
    if (triple.getLeft() != null) {
      int position = parent.getChildPosition(triple.getLeft());
      if (position != -1) {
        parent.insertChild(triple.getMiddle(), position + 1);
        foundNeighor = true;
      }
    }
    if (!foundNeighor && triple.getRight() != null) {
      int position = parent.getChildPosition(triple.getRight());
      if (position != -1) {
        parent.insertChild(triple.getMiddle(), position);
        foundNeighor = true;
      }
    }
    if (!foundNeighor) {
      parent.appendChild(triple.getMiddle());
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

  /**
   * Merge list of strings by union, when order doesn't matter
   *
   * @param left
   * @param base
   * @param right
   * @return
   */
  private String mergeByUnionToString(
      List<String> left, List<String> base, List<String> right, String delimiter) {
    List<String> unionList = new ArrayList<>();
    unionList.addAll(left);
    unionList.addAll(base);
    unionList.addAll(right);
    String unionString = unionList.stream().distinct().collect(Collectors.joining(delimiter));
    return unionString;
  }

  /**
   * Merge list of strings by union, when order doesn't matter
   *
   * @param left
   * @param base
   * @param right
   * @return
   */
  private List<String> mergeByUnion(List<String> left, List<String> base, List<String> right) {
    Set<String> unionList = new LinkedHashSet<>();
    unionList.addAll(left);
    unionList.addAll(base);
    unionList.addAll(right);
    return new ArrayList<>(unionList);
  }
}
