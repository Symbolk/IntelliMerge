package edu.pku.intellimerge.io;

import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.node.CompilationUnitNode;
import edu.pku.intellimerge.model.node.NonTerminalNode;
import edu.pku.intellimerge.model.node.TerminalNode;
import edu.pku.intellimerge.util.Utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    String resultFilePath =
        Utils.formatPathSeparator(resultDir + File.separator + cu.getRelativePath());
    // merged package imports
    StringBuilder builder = new StringBuilder();
    builder.append(cu.getComment()).append("\n");
    builder.append(cu.getPackageStatement()).append("\n");
    cu.getImportStatements().forEach(importStatement -> builder.append(importStatement));
    // merged content, field-constructor-terminalNodeSimilarity, and reformat in google-java-format
    builder.append("\n").append(printNode(node));
    //    String reformattedCode = reformatCode(builder.toString());
    String reformattedCode = Utils.formatCodeWithConflicts(builder.toString(), false);
    Utils.writeContent(resultFilePath, reformattedCode, false);
    return resultFilePath;
  }

  /**
   * Reformat the printed code in google-java-format
   *
   * @param code
   * @return
   * @deprecated replaced by Utils.formatCodeWithConflicts()
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
    } catch (FormatterException e) {
      // print +/- 5 lines as the context around the line that causes the exception
      // to avoid output disaster
      for (FormatterDiagnostic diagnostic : e.diagnostics()) {
        List<String> lines = Arrays.asList(reformattedCode.split("\\r?\\n"));
        int lineNumber = diagnostic.line();
        int contextStart = lineNumber >= 5 ? lineNumber - 5 : 0;
        int contextEnd = lineNumber + 5 < lines.size() ? lineNumber + 5 : lines.size();
        for (int i = contextStart; i < contextEnd; ++i) {
          System.err.println(lines.get(i));
        }
      }
      e.printStackTrace();
      return reformattedCode;
    }
    reformattedCode =
        reformattedCode
            .replaceAll(
                "/\\* <<<<<<< " + Side.OURS.asString() + " \\*/", "<<<<<<< " + Side.OURS.asString())
            .replaceAll("/\\* ======= \\*/", "=======")
            .replaceAll(
                "/\\* >>>>>>> " + Side.THEIRS.asString() + " \\*/",
                ">>>>>>> " + Side.THEIRS.asString());
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
      builder.append(node.getAnnotations().stream().collect(Collectors.joining("\n"))).append("\n");
      builder.append(node.getModifiers().stream().collect(Collectors.joining(" "))).append(" ");
      if (node.getNodeType().equals(NodeType.INITIALIZER_BLOCK)) {
        builder.append(node.getOriginalSignature().contains("static") ? "static" : "");
      } else {
        builder.append(node.getOriginalSignature());
      }
      builder.append(((TerminalNode) node).getBody()).append("\n");
    } else if (node instanceof NonTerminalNode) {
      if (!node.getNodeType().equals(NodeType.CU)) {
        builder.append(node.getComment());
        builder.append(node.getAnnotations().stream().collect(Collectors.joining("\n"))).append("\n");
        builder.append(node.getOriginalSignature());
        builder.append("{\n");
      }
      if (node.getNodeType().equals(NodeType.ENUM)) {
        int childrenSize = node.getChildren().size();
        for (int i = 0; i < childrenSize; ++i) {
          SemanticNode child = node.getChildAtPosition(i);
          builder.append(printNode(child));
          if (i + 1 < childrenSize && child.getNodeType().equals(NodeType.ENUM_CONSTANT)) {
            // if next is another constant
            if (node.getChildAtPosition(i + 1).getNodeType().equals(NodeType.ENUM_CONSTANT)) {
              builder.append(",");
            } else {
              builder.append(";");
            }
          }
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
