package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import edu.pku.intellimerge.model.EdgeType;
import edu.pku.intellimerge.model.NodeType;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class SemanticGraphBuilder {

  /**
   * Build the SemanticGraph for one java file
   *
   * @param filePath
   * @param packagePath
   * @return
   * @throws FileNotFoundException
   */
  public static Graph<SemanticNode, SemanticEdge> buildForFile(
      String filePath, String packagePath) {

    TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(packagePath));
    reflectionTypeSolver.setParent(reflectionTypeSolver);
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(reflectionTypeSolver);
    combinedTypeSolver.add(javaParserTypeSolver);
    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
    JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);
    final JavaParserFacade javaParserFacade = JavaParserFacade.get(combinedTypeSolver);

    // init the semantic graph
    //    Graph<SemanticNode, SemanticEdge> semanticGraph = new
    // DefaultDirectedGraph<>(SemanticEdge.class);
    Graph<SemanticNode, SemanticEdge> semanticGraph = buildGraph();

    try {

      // parse the file to get CompilationUnit
      CompilationUnit cu = JavaParser.parse(new File(filePath));

      Integer count = 0;

      // 1. package
      String packageName = "";
      if (cu.getPackageDeclaration().isPresent()) {
        count++;
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration().get();
        packageName = packageDeclaration.getNameAsString();
        SemanticNode node =
            new SemanticNode(
                count,
                NodeType.PACKAGEDECLARATION,
                packageDeclaration.getNameAsString(),
                packageDeclaration.getNameAsString(),
                packageDeclaration.toString());
      }

      // 2. import
      List<ImportDeclaration> importDeclarations = cu.getImports();
      List<SemanticNode> importDeclarationNodes = new ArrayList<>();
      for (ImportDeclaration importDeclaration : importDeclarations) {
        count++;
        SemanticNode node =
            new SemanticNode(
                count,
                NodeType.IMPORTDECLARATION,
                importDeclaration.toString(),
                importDeclaration.toString(),
                importDeclaration.toString());
        if (importDeclaration.getRange().isPresent()) {
          node.setRange(importDeclaration.getRange().get());
        }
        semanticGraph.addVertex(node);
        importDeclarationNodes.add(node);
      }

      // 3. class or interface
      List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations =
          cu.findAll(ClassOrInterfaceDeclaration.class);
      for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : classOrInterfaceDeclarations) {
        count++;
        String displayName = classOrInterfaceDeclaration.getNameAsString();
        String canonicalName = packageName + "." + displayName;
        String className = canonicalName;
        SemanticNode classDeclarationNode =
            new SemanticNode(
                count,
                NodeType.CLASSORINTERFACEDECLARATION,
                displayName,
                canonicalName,
                classOrInterfaceDeclaration.toString());
        semanticGraph.addVertex(classDeclarationNode);
        // add edges between defined class and imported class
        for (SemanticNode imported : importDeclarationNodes) {
          semanticGraph.addEdge(
              classDeclarationNode,
              imported,
              new SemanticEdge(EdgeType.IMPORTS, classDeclarationNode, imported));
        }

        // 4. field
        List<FieldDeclaration> fieldDeclarations = classOrInterfaceDeclaration.getFields();
        for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
          count++;
          displayName = fieldDeclaration.getVariables().toString();
          canonicalName = className + "." + displayName;
          SemanticNode fieldDeclarationNode =
              new SemanticNode(
                  count,
                  NodeType.FIELDDECLARATION,
                  displayName,
                  canonicalName,
                  fieldDeclaration.toString());
          if (fieldDeclaration.getRange().isPresent()) {
            fieldDeclarationNode.setRange(fieldDeclaration.getRange().get());
          }
          semanticGraph.addVertex(fieldDeclarationNode);
          // add edge between field and class
          semanticGraph.addEdge(
              classDeclarationNode,
              fieldDeclarationNode,
              new SemanticEdge(EdgeType.DEFINES, classDeclarationNode, fieldDeclarationNode));
          // 4.1 field declaration
          System.out.println(
              fieldDeclaration
                  .getVariables()
                  .get(0)
                  .getType()
                  .resolve()
                  .asReferenceType()
                  .getQualifiedName());
        }

        // 5. constructor
        //      classOrInterfaceDeclaration.getConstructors();

        // 6. method
        List<MethodDeclaration> methodDeclarations = classOrInterfaceDeclaration.getMethods();
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
          count++;
          displayName = methodDeclaration.getSignature().toString();
          canonicalName = className + "." + displayName;
          SemanticNode methodDeclarationNode =
              new SemanticNode(
                  count,
                  NodeType.METHODDECLARATION,
                  displayName,
                  canonicalName,
                  methodDeclaration.toString());
          if (methodDeclaration.getRange().isPresent()) {
            methodDeclarationNode.setRange(methodDeclaration.getRange().get());
          }
          semanticGraph.addVertex(methodDeclarationNode);
          // add edge between field and class
          semanticGraph.addEdge(
              classDeclarationNode,
              methodDeclarationNode,
              new SemanticEdge(EdgeType.DEFINES, classDeclarationNode, methodDeclarationNode));

          // 6.1 field access
          List<FieldAccessExpr> fieldAccessExprs = methodDeclaration.findAll(FieldAccessExpr.class);
          for (FieldAccessExpr fieldAccessExpr : fieldAccessExprs) {
            // resolve the field declaration and draw the edge
            final SymbolReference<? extends ResolvedValueDeclaration> ref =
                javaParserFacade.solve(fieldAccessExpr);
            System.out.println(
                ref.getCorrespondingDeclaration().asField().declaringType().getQualifiedName());
          }

          // 6.2 method call
          List<MethodCallExpr> methodCallExprs = methodDeclaration.findAll(MethodCallExpr.class);
          for (MethodCallExpr methodCallExpr : methodCallExprs) {
            // resolve the method declaration and draw the edge
            System.out.println(methodCallExpr.resolve().getQualifiedSignature());
          }
        }
      }

      return semanticGraph;

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Build the SemanticGraph for a whole project
   *
   * @param folderPath
   * @param packagePath
   * @return
   */
  public static Graph<SemanticNode, SemanticEdge> buildForProject(
      String folderPath, String packagePath) {
    // parse an entire project
    //    try{
    //      //        ParserConfiguration parserConfiguration
    //      File root = new File("F:\\workspace\\coding\\jgrapht\\jgrapht-demo\\src\\main\\java");
    //      SourceRoot sourceRoot = new SourceRoot(root.toPath());
    //      List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();
    //      List<CompilationUnit> compilationUnits =
    // parseResults.stream().filter(ParseResult::isSuccessful).map(r ->
    // r.getResult().get()).collect(Collectors.toList());
    //      System.out.println(compilationUnits);
    //    }catch (IOException e){
    //      e.printStackTrace();
    //    }
    return buildGraph();
  }

  /**
   * Setup and config the JavaSymbolSolver
   * @param packagePath
   * @param libPath
   * @return
   */
  private static JavaSymbolSolver setUpSymbolSolver(String packagePath, String libPath) {
    // set up the JavaSymbolSolver
    //    TypeSolver jarTypeSolver = JarTypeSolver.getJarTypeSolver(libPath);
    TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(packagePath));
    reflectionTypeSolver.setParent(reflectionTypeSolver);
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(reflectionTypeSolver);
    combinedTypeSolver.add(javaParserTypeSolver);
    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
    return symbolSolver;
  }

  /**
   * Build and initialize an empty Graph
   * @return
   */
  private static Graph<SemanticNode, SemanticEdge> buildGraph() {
    return GraphTypeBuilder.<SemanticNode, SemanticEdge>directed()
        .allowingMultipleEdges(true)
        .allowingSelfLoops(false)
        .edgeClass(SemanticEdge.class)
        .weighted(true)
        .buildGraph();
  }
}
