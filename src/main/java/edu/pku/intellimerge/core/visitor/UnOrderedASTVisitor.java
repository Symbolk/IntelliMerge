package edu.pku.intellimerge.core.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** An AST Visitor that visit nodes in an unspecified order */
public class UnOrderedASTVisitor extends VoidVisitorAdapter<Graph<SemanticNode, SemanticEdge>> {
  private static final String FILE_PATH =
      "D:\\github\\merges\\javaparser\\d9c990a94c725b8d112ba02897988b7400100ce3\\ours\\javaparser-core\\src\\main\\java\\com\\github\\javaparser\\utils\\SourceRoot.java";

  public static void main(String[] args) {
    try {
      CompilationUnit cu = JavaParser.parse(new FileInputStream(FILE_PATH));
      Graph<SemanticNode, SemanticEdge> graph = initGraph();

      UnOrderedASTVisitor visitor = new UnOrderedASTVisitor();
      visitor.visit(cu, graph);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Build and initialize an empty Graph
   *
   * @return
   */
  public static Graph<SemanticNode, SemanticEdge> initGraph() {
    return GraphTypeBuilder.<SemanticNode, SemanticEdge>directed()
        .allowingMultipleEdges(true)
        .allowingSelfLoops(true) // recursion
        .edgeClass(SemanticEdge.class)
        .weighted(true)
        .buildGraph();
  }

  @Override
  public void visit(CompilationUnit cu, Graph graph) {
    // Don't forget to call super, it may find more method calls inside the arguments of this method
    // call, for example.
    super.visit(cu, graph);
    System.out.println("CU " + cu.getClass());
  }

  @Override
  public void visit(EnumDeclaration ed, Graph graph) {
    // Don't forget to call super, it may find more method calls inside the arguments of this method
    // call, for example.
    super.visit(ed, graph);
    System.out.println("Enum " + ed.getNameAsString());
  }

  @Override
  public void visit(ClassOrInterfaceDeclaration cid, Graph graph) {
    super.visit(cid, graph);
    System.out.println("Class " + cid.getNameAsString());
  }

  @Override
  public void visit(FieldDeclaration fd, Graph graph) {
    super.visit(fd, graph);
    System.out.println("Field " + fd.getVariables());
  }

  @Override
  public void visit(ConstructorDeclaration cd, Graph graph) {
    super.visit(cd, graph);
    System.out.println("Constructor " + cd.getNameAsString());
  }

  @Override
  public void visit(MethodDeclaration md, Graph graph) {
    super.visit(md, graph);
    int nodeCount = graph.vertexSet().size();
    int edgeCount = graph.edgeSet().size();
    String displayName = md.getSignature().toString();
    String qualifiedName = "" + "." + displayName;
    List<String> modifiers =
        md.getModifiers().stream().map(Modifier::toString).collect(Collectors.toList());
    List<String> parameterTypes =
        md.getParameters()
            .stream()
            .map(Parameter::getType)
            .map(Type::asString)
            .collect(Collectors.toList());
    List<String> parameterNames =
        md.getParameters().stream().map(Parameter::getNameAsString).collect(Collectors.toList());
    String access = md.getAccessSpecifier().asString();
    List<String> throwsExceptions =
        md.getThrownExceptions().stream().map(ReferenceType::toString).collect(Collectors.toList());
    MethodDeclNode mdNode =
        new MethodDeclNode(
            nodeCount++,
            false,
            NodeType.METHOD,
            displayName,
            qualifiedName,
            md.getDeclarationAsString(),
            md.getComment().map(Comment::getContent).orElse(""),
            access,
            modifiers,
            md.getTypeAsString(),
            displayName.substring(0, displayName.indexOf("(")),
            parameterTypes,
            parameterNames,
            throwsExceptions,
            md.getBody().map(BlockStmt::toString).orElse(""),
            md.getRange());
    graph.addVertex(mdNode);
    // TODO need to find the parent node, but maybe it haven't been visited
    //    graph.addEdge(
    //            classOrInterfaceDeclNode,
    //            mdNode,
    //            new SemanticEdge(
    //                    edgeCount++, EdgeType.DEFINE_METHOD, classOrInterfaceDeclNode, mdNode));
    System.out.println("Method " + md.getName());
  }
}
