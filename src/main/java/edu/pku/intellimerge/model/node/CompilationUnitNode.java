package edu.pku.intellimerge.model.node;

import com.github.javaparser.ast.CompilationUnit;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;

public class CompilationUnitNode extends SemanticNode {
  private String fileName;
  private String relativePath; // the same as absolute path currently
  private String absolutePath; // file path in the collected folder, not the original repo
  private CompilationUnit cu; // corresponding AST node
  private Boolean needToMerge;

  public CompilationUnitNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String content,
      String fileName,
      String relativePath,
      String absolutePath,
      CompilationUnit cu,
      Boolean needToMerge) {
    super(nodeID, nodeType, displayName, qualifiedName, content);
    this.fileName = fileName;
    this.relativePath = relativePath;
    this.absolutePath = absolutePath;
    this.cu = cu;
    this.incomingEdges.put(EdgeType.CONTAIN, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.CONTAIN, new ArrayList<>());
    this.needToMerge = needToMerge;
  }

  @Override
  public String toString() {
    return "CompilationUnitNode{"
        + "fileName='"
        + fileName
        + '\''
        + ", relativePath='"
        + relativePath
        + '\''
        + ", absolutePath='"
        + absolutePath
        + '\''
        + ", cu="
        + cu
        + '}';
  }

  @Override
  public String getSignature() {
    return getQualifiedName();
  }
}
