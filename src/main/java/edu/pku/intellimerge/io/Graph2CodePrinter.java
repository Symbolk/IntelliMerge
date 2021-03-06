package edu.pku.intellimerge.io;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.node.CompilationUnitNode;
import edu.pku.intellimerge.model.node.CompositeNode;
import edu.pku.intellimerge.model.node.OrphanCommentNode;
import edu.pku.intellimerge.model.node.TerminalNode;
import edu.pku.intellimerge.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
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
    builder.append(
        cu.getComment().isEmpty()
            ? ""
            : cu.getComment() + System.lineSeparator() + System.lineSeparator());
    builder.append(cu.getPackageStatement());
    cu.getImportStatements().forEach(importStatement -> builder.append(importStatement));
    // merged content, field-constructor-terminal, and reformat in google-java-format
    builder.append(printNode(node));
    //    String reformattedCode = reformatCode(builder.toString());
    //    String reformattedCode = Utils.formatCodeWithConflicts(builder.toString(), false);
    Utils.writeContent(resultFilePath, builder.toString(), false);
    return resultFilePath;
  }

  /**
   * Print the node content and children into code string
   *
   * @param node
   * @return
   */
  private static String printNode(SemanticNode node) {
    int indent = 0;
    if (node.getRange().isPresent()) {
      indent = node.getRange().get().begin.column - 1;
    }
    StringBuilder builder = new StringBuilder();
    if (node instanceof TerminalNode) {
      builder.append(
          node.getComment().isEmpty() ? "" : (node.getComment().trim() + System.lineSeparator()));
      builder.append(
          node.getAnnotations().isEmpty()
              ? ""
              : (node.getAnnotations().stream().collect(Collectors.joining(System.lineSeparator()))
                  + System.lineSeparator()));
      if (node.getNodeType().equals(NodeType.INITIALIZER_BLOCK)) {
        builder.append(node.getOriginalSignature().contains("static") ? "static" : "");
      } else {
        builder.append(node.getOriginalSignature());
      }
      builder.append(((TerminalNode) node).getBody());
      for (int i = 0; i < node.followingEOL; ++i) {
        builder.append(System.lineSeparator());
      }
    } else if (node instanceof CompositeNode) {
      if (!node.getNodeType().equals(NodeType.COMPILATION_UNIT)) {
        builder.append(
            node.getComment().isEmpty() ? "" : (node.getComment().trim() + System.lineSeparator()));
        builder.append(
            node.getAnnotations().isEmpty()
                ? ""
                : (node.getAnnotations().stream()
                        .collect(Collectors.joining(System.lineSeparator()))
                    + System.lineSeparator()));
        builder.append(node.getOriginalSignature());
        builder.append(((CompositeNode) node).curlyBracePrefix);
        builder.append("{");
        for (int i = 0; i < ((CompositeNode) node).beforeFirstChildEOL; ++i) {
          builder.append(System.lineSeparator());
        }
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
          builder.append(printNode(child)).append(System.lineSeparator());
        }
      }

      if (!node.getNodeType().equals(NodeType.COMPILATION_UNIT)) {
        builder.append("}");
      }

      // since the last child has appened on EOL
      if (node.followingEOL > 1) {
        for (int i = 0; i < node.followingEOL; ++i) {
          builder.append(System.lineSeparator());
        }
      }
    } else if (node instanceof OrphanCommentNode) {
      builder.append(node.getOriginalSignature());
      for (int i = 0; i < node.followingEOL; ++i) {
        builder.append(System.lineSeparator());
      }
    }

    return indentCodeLines(builder.toString(), indent);
  }

  /**
   * Indent lines of code
   *
   * @param code
   * @param indent
   */
  private static String indentCodeLines(String code, int indent) {
    List<String> indentedLines = new ArrayList<>();
    try {

      BufferedReader bufReader = new BufferedReader(new StringReader(code));

      String line = null;
      while ((line = bufReader.readLine()) != null) {
        int spaceCount = line.indexOf(line.trim());
        String indentedLine = "";
        if (spaceCount < indent) {
          indentedLine = String.join("", Collections.nCopies(indent, " ")) + line;
        } else {
          indentedLine = line;
        }
        if (line.contains(Utils.CONFLICT_LEFT_BEGIN)
            || line.contains(Utils.CONFLICT_BASE_BEGIN)
            || line.contains(Utils.CONFLICT_RIGHT_BEGIN)
            || line.contains(Utils.CONFLICT_RIGHT_END)) {
          indentedLine = line;
        }
        indentedLines.add(indentedLine);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return indentedLines.stream().collect(Collectors.joining(System.lineSeparator()));
  }
}
