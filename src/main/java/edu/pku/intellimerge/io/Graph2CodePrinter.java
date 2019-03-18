package edu.pku.intellimerge.io;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.node.CompilationUnitNode;
import edu.pku.intellimerge.model.node.NonTerminalNode;
import edu.pku.intellimerge.model.node.TerminalNode;
import edu.pku.intellimerge.util.FilesManager;

import java.io.File;

public class Graph2CodePrinter {

  /**
   * Print the merged compilation unit to file and return its path
   *
   * @param node
   * @param cu
   * @param resultDir
   * @return
   */
  public static String printCU(SemanticNode node, CompilationUnitNode cu, String resultDir) {
    String resultFilePath = FilesManager.formatPathSeparator(resultDir + File.separator + cu.getRelativePath());
    // merged package imports
    StringBuilder builder = new StringBuilder();
    builder.append(cu.getComment()).append("\n");
    builder.append(cu.getPackageStatement()).append("\n");
    cu.getImportStatements().forEach(importStatement -> builder.append(importStatement));
    // merged content, field-constructor-method, and reformat in google-java-format
    builder.append("\n").append(printNode(node));
    String reformattedCode = reformatCode(builder.toString());
    FilesManager.writeContent(resultFilePath, reformattedCode);
    return resultFilePath;
  }

  /**
   * Reformat the printed code in google-java-format
   *
   * @param code
   * @return
   */
  private static String reformatCode(String code) {
    String reformattedCode = "";
    try {
      // comment all conflict symbols because it causes exceptions for the formatter
      reformattedCode =
          code.replaceAll(
                  "<<<<<<< " + Side.OURS.asString(), "/* <<<<<<< " + Side.OURS.asString() + " */")
              .replaceAll("=======", "/* ======= */")
              .replaceAll(
                  ">>>>>>> " + Side.THEIRS.asString(),
                  "/* >>>>>>> " + Side.THEIRS.asString() + " */");

      reformattedCode = new Formatter().formatSource(reformattedCode);

      reformattedCode =
          reformattedCode
              .replaceAll(
                  "/\\* <<<<<<< " + Side.OURS.asString() + " \\*/",
                  "<<<<<<< " + Side.OURS.asString())
              .replaceAll("/\\* ======= \\*/", "=======")
              .replaceAll(
                  "/\\* >>>>>>> " + Side.THEIRS.asString() + " \\*/",
                  ">>>>>>> " + Side.THEIRS.asString());
    } catch (FormatterException e) {
      e.printStackTrace();
    }
    return reformattedCode;
  }

  /**
   * Print the node content and children into code string
   *
   * @param node
   * @return
   */
  private static String printNode(SemanticNode node) {
    StringBuilder builder = new StringBuilder();
    if (node instanceof TerminalNode) {
      builder.append(node.getComment());
      builder.append(node.getOriginalSignature());
      builder.append(((TerminalNode) node).getBody());
    } else if (node instanceof NonTerminalNode) {
      if (!node.getNodeType().equals(NodeType.CU)) {
        builder.append(node.getComment());
        builder.append(node.getOriginalSignature());
        builder.append("{\n");
      }
      if (node.getNodeType().equals(NodeType.ENUM)) {
        for (int i = 0; i < node.getChildren().size(); ++i) {
          if (i != 0) {
            builder.append(",");
          }
          builder.append(printNode(node.getChildAtPosition(i)));
        }
      } else {
        for (SemanticNode child : node.getChildren()) {
          builder.append(printNode(child)).append("\n");
        }
      }

      if (!node.getNodeType().equals(NodeType.CU)) {
        builder.append("\n}\n");
      }
    }
    return builder.toString();
  }
}
