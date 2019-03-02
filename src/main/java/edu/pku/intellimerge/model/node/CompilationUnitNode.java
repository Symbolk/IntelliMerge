package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.Set;

public class CompilationUnitNode extends NonTerminalNode {
  private String packageStatement;
  private Set<String> importStatements; // ordered, so use LinkedHashSet
  private String fileName;
  private String relativePath; // the same as absolute path currently
  private String absolutePath; // file path in the collected folder, not the original repo
  //  private CompilationUnit cu; // corresponding AST node, to get package and import contents in
  // merging

  public CompilationUnitNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      String fileName,
      String relativePath,
      String absolutePath,
      String packageStatement,
      Set<String> importStatements) {
    super(nodeID, needToMerge, nodeType, displayName, qualifiedName, originalSignature, comment);
    this.fileName = fileName;
    this.relativePath = relativePath;
    this.absolutePath = absolutePath;
    this.packageStatement = packageStatement;
    this.importStatements = importStatements;
    this.incomingEdges.put(EdgeType.IMPORT, new ArrayList<>());
    this.incomingEdges.put(EdgeType.CONTAIN, new ArrayList<>());

    this.outgoingEdges.put(EdgeType.IMPORT, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.DEFINE, new ArrayList<>());
  }

  public String getPackageStatement() {
    return packageStatement;
  }

  public void setPackageStatement(String packageStatement) {
    this.packageStatement = packageStatement;
  }

  public Set<String> getImportStatements() {
    return importStatements;
  }

  public void setImportStatements(Set<String> importStatements) {
    this.importStatements = importStatements;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }
}
