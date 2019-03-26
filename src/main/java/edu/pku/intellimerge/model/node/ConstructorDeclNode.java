package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** access == public, no return type */
public class ConstructorDeclNode extends TerminalNode {
  private String constructorName; // signature

  public ConstructorDeclNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      List<String> annotations,
      List<String> modifiers,
      String constructorName,
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
        annotations,modifiers,
        body,
        range);
    this.constructorName = constructorName;

    this.incomingEdges.put(EdgeType.DEFINE, new ArrayList<>());
    this.incomingEdges.put(EdgeType.CALL, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.INITIALIZE, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.READ, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.WRITE, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.CALL, new ArrayList<>());
  }
}
