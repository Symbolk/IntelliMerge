package edu.pku.intellimerge.model.node;

import com.github.javaparser.ast.CompilationUnit;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;

public class CompilationUnitNode extends NonTerminalNode {
  public Boolean needToMerge;
  private String fileName;
  private String relativePath; // the same as absolute path currently
  private String absolutePath; // file path in the collected folder, not the original repo
  private CompilationUnit cu; // corresponding AST node, to get package and import contents in merging

  public CompilationUnitNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String fileName,
      String relativePath,
      String absolutePath,
      CompilationUnit cu,
      Boolean needToMerge) {
    super(nodeID, nodeType, displayName, qualifiedName);
    this.fileName = fileName;
    this.relativePath = relativePath;
    this.absolutePath = absolutePath;
    this.cu = cu;
    this.incomingEdges.put(EdgeType.IMPORT, new ArrayList<>());
    this.incomingEdges.put(EdgeType.CONTAIN, new ArrayList<>());

    this.outgoingEdges.put(EdgeType.IMPORT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE_TYPE, new ArrayList<>());
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
        + '}';
  }

  @Override
  public String getSignature() {
    return getQualifiedName();
  }
}
