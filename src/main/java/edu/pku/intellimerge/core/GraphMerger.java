package edu.pku.intellimerge.core;

import com.google.common.collect.BiMap;
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
import java.util.stream.Collectors;

/** Only the diff file/cu needs to be merged */
public class GraphMerger {
  public TwowayMatching b2oMatching;
  public TwowayMatching b2tMatching;
  public List<ThreewayMapping> mapping;
  private Logger logger = LoggerFactory.getLogger(GraphMerger.class);
  private String resultDir; // merge result path
  private Graph<SemanticNode, SemanticEdge> oursGraph;
  private Graph<SemanticNode, SemanticEdge> baseGraph;
  private Graph<SemanticNode, SemanticEdge> theirsGraph;

  public GraphMerger(
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
    GraphMatcher b2oMatcher = new GraphMatcher(baseGraph, oursGraph);
    GraphMatcher b2tMatcher = new GraphMatcher(baseGraph, theirsGraph);
    // temporarily disable multithread, considering the debug effort with the performance
    // improvement
    //    try {
    //      ExecutorService executorService = Executors.newFixedThreadPool(2);
    //      Future<TwowayMatching> task1 = executorService.submit(b2oMatcher);
    //      Future<TwowayMatching> task2 = executorService.submit(b2tMatcher);

    //      b2oMatching = task1.get();
    //      b2tMatching = task2.get();

    //      executorService.shutdown();
    b2oMatcher.topDownMatch();
    b2oMatcher.bottomUpMatch();
    b2tMatcher.topDownMatch();
    b2tMatcher.bottomUpMatch();
    b2oMatching = b2oMatcher.matching;
    b2tMatching = b2tMatcher.matching;

    // collect COMPILATION_UNIT mapping that need to merge
    Set<SemanticNode> internalAndNeedToMergeNodes =
        baseGraph.vertexSet().stream()
            .filter(SemanticNode::isInternal)
            .filter(SemanticNode::needToMerge)
            .collect(Collectors.toSet());
    for (SemanticNode node : internalAndNeedToMergeNodes) {
      if (node instanceof CompilationUnitNode) {
        CompilationUnitNode cu = (CompilationUnitNode) node;
        if (cu.needToMerge() == true) {
          // temporarily keep the mapping of cus
          ThreewayMapping mapping =
              new ThreewayMapping(
                  Optional.ofNullable(b2oMatching.one2oneMatchings.getOrDefault(node, null)),
                  Optional.of(node),
                  Optional.ofNullable(b2tMatching.one2oneMatchings.getOrDefault(node, null)));
          this.mapping.add(mapping);
        }
      }
    }
    //    } catch (InterruptedException e) {
    //      e.printStackTrace();
    //    } catch (ExecutionException e) {
    //      e.printStackTrace();
    //    }
  }

  /**
   * Merge CUs according to the mapping
   *
   * @return
   */
  public List<String> threewayMerge() {
    // bottom up merge children of the needToMerge COMPILATION_UNIT
    List<String> mergedFilePaths = new ArrayList<>();
    for (ThreewayMapping mapping : mapping) {
      if (mapping.baseNode.isPresent()) {
        // merge the COMPILATION_UNIT by merging its content
        //        SemanticNode mergedCU = mergeSingleNode(mapping.baseNode.get());
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
   * Merge the header part of COMPILATION_UNIT, including comment, package and imports
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
        // conservative strategy: simply union the imports
        //        Set<String> union = new LinkedHashSet<>(oursCU.getImportStatements());
        //        union.addAll(theirsCU.getImportStatements());
        //        mergedCU.setImportStatements(union);
        //        List<String> oursList = new ArrayList<>(oursCU.getImportStatements());
        //        List<String> baseList = new ArrayList<>(((CompilationUnitNode)
        // node).getImportStatements());
        //        List<String> theirsList = new ArrayList<>(theirsCU.getImportStatements());
        //        List<String> mergedList =
        //            Stream.of(baseList, oursList, theirsList)
        //                .flatMap(Collection::stream)
        //                .distinct()
        //                .collect(Collectors.toList());
        //        mergedCU.setImportStatements(new LinkedHashSet<>(mergedList));
        // proactive way: apply changes to base
        List<String> mergedImports = new ArrayList<>();
        List<String> baseImports =
            new ArrayList<>(((CompilationUnitNode) node).getImportStatements());

        List<String> oursImports = new ArrayList<>(oursCU.getImportStatements());

        List<String> addedInOurs = new ArrayList<>(oursCU.getImportStatements());

        for (String str : theirsCU.getImportStatements()) {
          int index = getChildIndexTrimed(oursImports, str);
          if (index >= 0) {
            addedInOurs.set(index, "");
          }
          mergedImports.add(str);
        }

        for (int i = 0; i < addedInOurs.size(); ++i) {
          String str = addedInOurs.get(i);
          if (!str.isEmpty() && getChildIndexTrimed(baseImports, str) < 0) {
            int j = -1; // the position to insert the str
            if (i == 0) {
              j = 0;
            } else {
              // get the previous one that exist in merged imports
              String previousImport = "";
              for (int k = i - 1; k >= 0; k--) {
                if (addedInOurs.get(k).equals("")) {
                  // "" should have been in mergedImports
                  previousImport = oursImports.get(k);
                  j = getChildIndexTrimed(mergedImports, previousImport);
                  break;
                } else {
                  previousImport = addedInOurs.get(k);
                  j = getChildIndexTrimed(mergedImports, previousImport);
                  if (j >= 0) {
                    break;
                  }
                }
              }
            }
            if (j + 1 >= 0 && j + 1 < mergedImports.size()) {
              mergedImports.add(j + 1, str);
            } else {
              mergedImports.add(str);
            }
          }
        }

        mergedCU.setImportStatements(new LinkedHashSet<>(mergedImports));

        return mergedCU;
      }
    }
    return null;
  }

  private int getChildIndexTrimed(List<String> list, String s) {
    for (int i = 0; i < list.size(); ++i) {
      if (list.get(i).trim().equals(s.trim())) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Merge single node but in favor of the node order of the last version (usually theirs)
   *
   * @param node
   * @return
   */
  private SemanticNode mergeSingleNode(SemanticNode node) {
    SemanticNode mergedNode = node.shallowClone();
    SemanticNode oursNode = b2oMatching.one2oneMatchings.getOrDefault(node, null);
    SemanticNode theirsNode = b2tMatching.one2oneMatchings.getOrDefault(node, null);
    if (oursNode != null && theirsNode != null) {
      // for orphan comment, merge if matched
      if (node instanceof OrphanCommentNode) {
        mergedNode.setOriginalSignature(
            mergeTextually(
                oursNode.getOriginalSignature(),
                node.getOriginalSignature(),
                theirsNode.getOriginalSignature()));
        return mergedNode;
      } else if (node instanceof TerminalNode) {
        // for terminal: merge each part and return merged node
        TerminalNode mergedTerminal = (TerminalNode) mergedNode;
        // exist in BothSides side
        TerminalNode oursTerminal = (TerminalNode) oursNode;
        TerminalNode baseTerminal = (TerminalNode) node;
        TerminalNode theirsTerminal = (TerminalNode) theirsNode;
        String mergedComment =
            mergeTextually(
                oursTerminal.getComment(), baseTerminal.getComment(), theirsTerminal.getComment());
        String mergedAnnotations =
            mergeTextually(
                oursNode.getAnnotationsAsString(),
                node.getAnnotationsAsString(),
                theirsNode.getAnnotationsAsString());
        List<String> mergedModifiers =
            mergeListTextually(
                oursTerminal.getModifiers(),
                baseTerminal.getModifiers(),
                theirsTerminal.getModifiers());
        //        String mergedSignature = mergeComponents(oursTerminal, baseTerminal,
        // theirsTerminal);
        String mergedSignature =
            mergeTextually(
                oursTerminal.getOriginalSignature(),
                baseTerminal.getOriginalSignature(),
                theirsTerminal.getOriginalSignature());
        String mergedBody =
            mergeTextually(
                oursTerminal.getBody(), baseTerminal.getBody(), theirsTerminal.getBody());
        mergedTerminal.setComment(mergedComment);
        if (mergedAnnotations.length() > 0) {
          mergedTerminal.setAnnotations(Arrays.asList(mergedAnnotations.split("\n")));
        }
        mergedTerminal.setModifiers(mergedModifiers);
        mergedTerminal.setOriginalSignature(mergedSignature);
        mergedTerminal.setBody(mergedBody);

        // update name for hash
        mergedTerminal.setQualifiedName(
            mergeTextually(
                oursTerminal.getQualifiedName(),
                baseTerminal.getQualifiedName(),
                theirsTerminal.getQualifiedName()));

        return mergedTerminal;
      } else {
        // for composite: merge comment/annotation/signature, and then children
        CompositeNode mergedNonTerminal = (CompositeNode) mergedNode;

        // merge the comment and signature
        String mergedComment =
            mergeTextually(oursNode.getComment(), node.getComment(), theirsNode.getComment());
        String mergedAnnotations =
            mergeTextually(
                oursNode.getAnnotationsAsString(),
                node.getAnnotationsAsString(),
                theirsNode.getAnnotationsAsString());
        List<String> mergedModifiers =
            mergeByUnion(oursNode.getModifiers(), node.getModifiers(), theirsNode.getModifiers());
        //        String mergedSignature = mergeComponents(oursNode, node, theirsNode);
        String mergedSignature =
            mergeTextually(
                oursNode.getOriginalSignature(),
                node.getOriginalSignature(),
                theirsNode.getOriginalSignature());
        mergedNonTerminal.setComment(mergedComment);
        if (mergedAnnotations.length() > 0) {
          mergedNonTerminal.setAnnotations(Arrays.asList(mergedAnnotations.split("\n")));
        }
        if (mergedAnnotations.length() > 0) {
          mergedNonTerminal.setModifiers(mergedModifiers);
        }
        mergedNonTerminal.setOriginalSignature(mergedSignature);
        // update name for hash
        mergedNonTerminal.setQualifiedName(
            mergeTextually(
                oursNode.getQualifiedName(),
                node.getQualifiedName(),
                theirsNode.getQualifiedName()));
        // follow the format of the right side
        mergedNonTerminal.followingEOL = theirsNode.followingEOL;

        // iteratively merge its children (in base order)
        List<SemanticNode> children = theirsNode.getChildren();
        BiMap<SemanticNode, SemanticNode> inversedMatching = b2tMatching.one2oneMatchings;
        inversedMatching = inversedMatching.inverse();
        for (SemanticNode child : children) {
          SemanticNode childInBase = inversedMatching.getOrDefault(child, null);
          if (childInBase == null) {
            // insert nodes added in theirs
            mergedNonTerminal.appendChild(child);
          } else {
            SemanticNode mergedChild = mergeSingleNode(childInBase);
            if (mergedChild != null) {
              mergedChild.followingEOL = child.followingEOL;
              mergedNonTerminal.appendChild(mergedChild);
            }
          }
        }
        // insert nodes added in ours
        List<SemanticNode> addedOurs =
            removeDuplicates(
                filterAddedNodes(node, b2oMatching), filterAddedNodes(node, b2tMatching));
        mergeUnmatchedNodes(mergedNonTerminal, addedOurs);

        return mergedNonTerminal;
      }
    } else {
      // if delete in one side, delete it
      return null;
    }
  }

  /**
   * Get the added node under the composite node
   *
   * @param node
   * @param matching
   * @return
   */
  private List<SemanticNode> filterAddedNodes(SemanticNode node, TwowayMatching matching) {
    Map<NodeType, List<SemanticNode>> unmatchedNodes = matching.unmatchedNodes2;
    // for each type of newly added nodes
    List<SemanticNode> addedNodes = new ArrayList<>();
    List<SemanticNode> results = new ArrayList<>();
    for (Map.Entry<NodeType, List<SemanticNode>> entry : unmatchedNodes.entrySet()) {
      addedNodes.addAll(entry.getValue());
    }
    SemanticNode matchedParentNode = matching.one2oneMatchings.getOrDefault(node, null);
    if (matchedParentNode != null) {
      for (SemanticNode newlyAdded : addedNodes) {
        SemanticNode parent = newlyAdded.getParent();
        if (parent != null && parent.equals(matchedParentNode)) {
          results.add(newlyAdded);
        }
      }
    }

    results =
        new ArrayList(
            results.stream()
                .sorted(Comparator.comparing(SemanticNode::getNodeID))
                .collect(Collectors.toList()));
    return results;
  }

  /**
   * Remove nodes with the same signature but added in both sides
   *
   * @param addedOurs
   * @param addedTheirs
   */
  private List<SemanticNode> removeDuplicates(
      List<SemanticNode> addedOurs, List<SemanticNode> addedTheirs) {

    List<SemanticNode> nodes2Copy = new ArrayList<>(addedOurs);
    for (SemanticNode n1 : addedTheirs) {
      for (SemanticNode n2 : nodes2Copy) {
        if (n1.getOriginalSignature().equals(n2.getOriginalSignature())) {
          // if the signature is duplicate, remove one of them
          addedOurs.remove(n2);
        }
      }
    }
    return addedOurs;
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
   * @param mergedNonTerminal
   */
  private void mergeUnmatchedNodes(CompositeNode mergedNonTerminal, List<SemanticNode> addedNodes) {
    for (SemanticNode newlyAdded : addedNodes) {
      SemanticNode parent = newlyAdded.getParent();
      insertBetweenNeighbors(mergedNonTerminal, getNeighbors(parent, newlyAdded));
    }
  }
  /**
   * Insert the added node betwen its neighbors
   *
   * @param parent
   * @param triple
   */
  private void insertBetweenNeighbors(
      CompositeNode parent, Triple<SemanticNode, SemanticNode, SemanticNode> triple) {
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
    // jgit has bug with single side change
    if (leftContent.equals(baseContent)) {
      return rightContent;
    }
    if (rightContent.equals(baseContent)) {
      return leftContent;
    }
    try {
      // TODO merge with git-merge for diff3 conflict style
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

  /**
   * Convert a list of string to one string and merge textually
   *
   * @param left
   * @param base
   * @param right
   * @return
   */
  private List<String> mergeListTextually(
      List<String> left, List<String> base, List<String> right) {
    String leftString = left.stream().collect(Collectors.joining("\n"));
    String baseString = base.stream().collect(Collectors.joining("\n"));
    String rightString = right.stream().collect(Collectors.joining("\n"));
    String mergedString = mergeTextually(leftString, baseString, rightString);
    return Arrays.asList(mergedString.split("\n"));
  }
}
