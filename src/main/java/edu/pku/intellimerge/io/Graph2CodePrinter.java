package edu.pku.intellimerge.io;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.node.CompilationUnitNode;
import edu.pku.intellimerge.model.node.NonTerminalNode;
import edu.pku.intellimerge.model.node.TerminalNode;
import edu.pku.intellimerge.util.FilesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Graph2CodePrinter {

  private static final Logger logger = LoggerFactory.getLogger(Graph2CodePrinter.class);

  public static void printCU(SemanticNode node, CompilationUnitNode cu, String resultFolder) {
    String resultFilePath = resultFolder + File.separator + cu.getRelativePath();
    // merged package imports
    StringBuilder builder = new StringBuilder();
    builder.append(cu.getPackageStatement());
    cu.getImportStatements().forEach(importStatement -> builder.append(importStatement));
    // merged content
    builder.append(printNode(node));
    FilesManager.writeContent(resultFilePath, builder.toString());
    logger.info("Merge result: {}", resultFilePath);
  }

  private static String printNode(SemanticNode node) {
    StringBuilder builder = new StringBuilder();
    builder.append(node.getOriginalSignature());
    if (node instanceof TerminalNode) {
      builder.append(" ").append(((TerminalNode) node).getBody()).append("\n");
    } else if (node instanceof NonTerminalNode) {
      builder.append(" {\n");
      for (SemanticNode child : node.getChildren()) {
        builder.append(printNode(child));
      }
      builder.append("\n}\n");
    }
    return builder.toString();
  }
}
