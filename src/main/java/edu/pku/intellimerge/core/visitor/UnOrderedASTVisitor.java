package edu.pku.intellimerge.core.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** An AST Visitor that visit nodes in an unspecified order */
public class UnOrderedASTVisitor extends VoidVisitorAdapter<Graph<SemanticNode, SemanticEdge>> {
  public static void main(String[] args) {
    try {
      File resourcesDirectory = new File("src/test/resources");
      String FILE_PATH =
          resourcesDirectory.getAbsolutePath() + "/Extract/ExtractMethod/base/SourceRoot.java";
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
    // Don't forget to call super, it may find more terminalNodeSimilarity calls inside the arguments of this terminalNodeSimilarity
    // call, for example.
    super.visit(cu, graph);
    System.out.println("COMPILATION_UNIT " + cu.getClass());
  }

  @Override
  public void visit(EnumDeclaration ed, Graph graph) {
    // Don't forget to call super, it may find more terminalNodeSimilarity calls inside the arguments of this terminalNodeSimilarity
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
    List<String> annotations =
        md.getAnnotations().stream().map(AnnotationExpr::toString).collect(Collectors.toList());
    List<String> typeParameters =
        md.getTypeParameters().stream().map(TypeParameter::asString).collect(Collectors.toList());

    String access = md.getAccessSpecifier().asString();
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
            annotations,
            access,
            modifiers,
            typeParameters,
            md.getTypeAsString(),
            displayName.substring(0, displayName.indexOf("(")),
            parameterTypes,
            parameterNames,
            throwsExceptions,
            md.getBody().map(BlockStmt::toString).orElse(""),
            md.getRange());
    graph.addVertex(mdNode);
    // need to find the parent node, but maybe it haven't been visited
    //    graph.addEdge(
    //            classOrInterfaceDeclNode,
    //            mdNode,
    //            new SemanticEdge(
    //                    edgeCount++, EdgeType.DEFINE, classOrInterfaceDeclNode, mdNode));
    System.out.println("Method " + md.getName());
  }
}
