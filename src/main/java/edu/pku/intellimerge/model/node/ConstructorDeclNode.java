package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.Optional;

/** access == public, no return type */
public class ConstructorDeclNode extends TerminalNode {
  private String constructorName; // signature
  private Boolean needToMerge;

  public ConstructorDeclNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String constructorName,
      String body,
      Optional<Range> range,
      Boolean needToMerge) {
    super(nodeID, nodeType, displayName, qualifiedName, originalSignature, body, range);
    this.constructorName = constructorName;
    this.needToMerge = needToMerge;

    this.incomingEdges.put(EdgeType.DEFINE_CONSTRUCTOR, new ArrayList<>());
    this.incomingEdges.put(EdgeType.CALL_CONSTRUCTOR, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.INIT_OBJECT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.READ_FIELD, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.WRITE_FIELD, new ArrayList<>());
  }

  @Override
  public String toString() {
    return "ConstructorDeclNode{" + "constructorName='" + constructorName + '\'' + '}';
  }

  @Override
  public String getSignature() {
    return constructorName;
  }


}
