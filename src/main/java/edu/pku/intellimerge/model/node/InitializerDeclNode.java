package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.Optional;

public class InitializerDeclNode extends TerminalNode {
  // instance initialization block --> false, static initialization block --> true
  private boolean isStatic;

  public InitializerDeclNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      boolean isStatic,
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
    this.isStatic = isStatic;
  }

  public boolean isStatic() {
    return isStatic;
  }
}
