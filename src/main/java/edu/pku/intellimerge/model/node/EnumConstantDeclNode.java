package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnumConstantDeclNode extends TerminalNode {
  // e.g.   CONTAIN(0, true, "contains"), // physical relation
  private String name;
  private List<String> arguments;

  public EnumConstantDeclNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      String body,
      Optional<Range> range) {
    super(
        nodeID,
        needToMerge,
        nodeType,
        displayName,
        qualifiedName,
        originalSignature,
        comment,
        new ArrayList<>(),
        new ArrayList<>(),
        body,
        range);
    this.incomingEdges.put(EdgeType.DEFINE, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE, new ArrayList<>());
  }
}
