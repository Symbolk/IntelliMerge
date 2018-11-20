package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import edu.pku.intellimerge.model.EdgeType;
import edu.pku.intellimerge.model.NodeType;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.util.FilesManager;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
      if (!FilesManager.isValidFile(new File(filePath))) {
        return null;
      }

      // parse the file to get CompilationUnit
      CompilationUnit cu = JavaParser.parse(new File(filePath));

      Integer count = 0;

      // 1. package
      String packageName = "";
      if (cu.getPackageDeclaration().isPresent()) {
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration().get();
        packageName = packageDeclaration.getNameAsString();
        SemanticNode node =
            new SemanticNode(
                count++,
                NodeType.PACKAGEDECLARATION,
                packageDeclaration.getNameAsString(),
                packageDeclaration.getNameAsString(),
                packageDeclaration.toString());
        semanticGraph.addVertex(node);
      }

      // 2. import
      List<ImportDeclaration> importDeclarations = cu.getImports();
      List<SemanticNode> importDeclarationNodes = new ArrayList<>();
      for (ImportDeclaration importDeclaration : importDeclarations) {
        SemanticNode node =
            new SemanticNode(
                count++,
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
        String displayName = classOrInterfaceDeclaration.getNameAsString();
        String qualifiedName = packageName + "." + displayName;
        String qualifiedClassName = qualifiedName;
        SemanticNode classDeclarationNode =
            new SemanticNode(
                count++,
                NodeType.CLASSORINTERFACEDECLARATION,
                displayName,
                qualifiedName,
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
          displayName = fieldDeclaration.getVariables().toString();
          qualifiedName = qualifiedClassName + "." + displayName;
          SemanticNode fieldDeclarationNode =
              new SemanticNode(
                  count++,
                  NodeType.FIELDDECLARATION,
                  displayName,
                  qualifiedName,
                  fieldDeclaration.toString());
          if (fieldDeclaration.getRange().isPresent()) {
            fieldDeclarationNode.setRange(fieldDeclaration.getRange().get());
          }
          semanticGraph.addVertex(fieldDeclarationNode);
          // add edge between field and class
          semanticGraph.addEdge(
              classDeclarationNode,
              fieldDeclarationNode,
              new SemanticEdge(EdgeType.HASFIELD, classDeclarationNode, fieldDeclarationNode));
          // 4.1 field declaration
          if (!fieldDeclaration.getVariables().get(0).getType().isPrimitiveType()) {
            System.out.println(
                fieldDeclaration
                    .getVariables()
                    .get(0)
                    .getType()
                    .resolve()
                    .asReferenceType()
                    .getQualifiedName());
          }
        }

        // 5. constructor
        List<ConstructorDeclaration> constructorDeclarations =
            classOrInterfaceDeclaration.getConstructors();
        for (ConstructorDeclaration constructorDeclaration : constructorDeclarations) {
          displayName = constructorDeclaration.getSignature().toString();
          qualifiedName = qualifiedClassName + "." + displayName;
          SemanticNode constructorDeclNode =
              new SemanticNode(
                  count++,
                  NodeType.METHODDECLARATION,
                  displayName,
                  qualifiedName,
                  constructorDeclaration.toString());
          if (constructorDeclaration.getRange().isPresent()) {
            constructorDeclNode.setRange(constructorDeclaration.getRange().get());
          }
          semanticGraph.addVertex(constructorDeclNode);
          semanticGraph.addEdge(
              classDeclarationNode,
              constructorDeclNode,
              new SemanticEdge(EdgeType.HASCONSTRUCTOR, classDeclarationNode, constructorDeclNode));
        }
        // 6. method
        List<MethodDeclaration> methodDeclarations = classOrInterfaceDeclaration.getMethods();
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
          displayName = methodDeclaration.getSignature().toString();
          qualifiedName = qualifiedClassName + "." + displayName;
          SemanticNode methodDeclarationNode =
              new SemanticNode(
                  count++,
                  NodeType.METHODDECLARATION,
                  displayName,
                  qualifiedName,
                  methodDeclaration.toString());
          if (methodDeclaration.getRange().isPresent()) {
            methodDeclarationNode.setRange(methodDeclaration.getRange().get());
          }
          semanticGraph.addVertex(methodDeclarationNode);
          // add edge between field and class
          semanticGraph.addEdge(
              classDeclarationNode,
              methodDeclarationNode,
              new SemanticEdge(EdgeType.HASMETHOD, classDeclarationNode, methodDeclarationNode));

          // 6.1 field access
          List<FieldAccessExpr> fieldAccessExprs = methodDeclaration.findAll(FieldAccessExpr.class);
          for (FieldAccessExpr fieldAccessExpr : fieldAccessExprs) {
            // resolve the field declaration and draw the edge
            final SymbolReference<? extends ResolvedValueDeclaration> ref =
                javaParserFacade.solve(fieldAccessExpr);
            ResolvedFieldDeclaration resolvedFieldDeclaration =
                ref.getCorrespondingDeclaration().asField();
            displayName = resolvedFieldDeclaration.getName();
            qualifiedName =
                resolvedFieldDeclaration.declaringType().getQualifiedName()
                    + "."
                    + resolvedFieldDeclaration.getName();
            SemanticNode resolvedFieldDeclNode =
                new SemanticNode(
                    count++, NodeType.FIELDDECLARATION, displayName, qualifiedName, "");
            System.out.println(
                "!!"
                    + resolvedFieldDeclaration.declaringType().getQualifiedName()
                    + "."
                    + resolvedFieldDeclaration.getName());
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

    // set up the typsolver
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
    Graph<SemanticNode, SemanticEdge> semanticGraph = buildGraph();

    // parse all java files in project folder
    try {
      //        ParserConfiguration parserConfiguration
      File root = new File(folderPath);
      SourceRoot sourceRoot = new SourceRoot(root.toPath());
      List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();
      List<CompilationUnit> compilationUnits =
          parseResults
              .stream()
              .filter(ParseResult::isSuccessful)
              .map(r -> r.getResult().get())
              .collect(Collectors.toList());
      // build the graph from the cus
      // save nodes into SemanticGraph, keep edges in several maps to save later
      Integer nodeCount = 0;
      Integer edgeCount = 0;
      //      List<SemanticNode> semanticNodes=new ArrayList<>();
      Map<SemanticNode, String> packageEdges = new HashMap<SemanticNode, String>();
      Map<SemanticNode, String> importEdges = new HashMap<SemanticNode, String>();
      Map<SemanticNode, String> useFieldEdges = new HashMap<SemanticNode, String>();
      Map<SemanticNode, String> callMethodEdges = new HashMap<SemanticNode, String>();
      for (CompilationUnit cu : compilationUnits) {
        // 1. package
        String packageName = "";
        if (cu.getPackageDeclaration().isPresent()) {
          PackageDeclaration packageDeclaration = cu.getPackageDeclaration().get();
          packageName = packageDeclaration.getNameAsString();
          // check if the package node exists
          // if not exist, create one
          String finalPackageName = packageName;
          if (semanticGraph
              .vertexSet()
              .stream()
              .noneMatch(
                  node ->
                      node.getNodeType().equals(NodeType.PACKAGEDECLARATION)
                          && node.getQualifiedName().equals(finalPackageName))) {
            SemanticNode packageDeclNode =
                new SemanticNode(
                    nodeCount++,
                    NodeType.PACKAGEDECLARATION,
                    packageDeclaration.getNameAsString(),
                    packageDeclaration.getNameAsString(),
                    packageDeclaration.toString());
            semanticGraph.addVertex(packageDeclNode);
          }
        }
        // 2. import
        List<ImportDeclaration> importDeclarations = cu.getImports();
        List<SemanticNode> importDeclNodes = new ArrayList<>();
        for (ImportDeclaration importDeclaration : importDeclarations) {
          SemanticNode importDeclNode =
              new SemanticNode(
                  nodeCount++,
                  NodeType.IMPORTDECLARATION,
                  importDeclaration.toString(),
                  importDeclaration.toString(),
                  importDeclaration.toString());
          if (importDeclaration.getRange().isPresent()) {
            importDeclNode.setRange(importDeclaration.getRange().get());
          }
          semanticGraph.addVertex(importDeclNode);
          importDeclNodes.add(importDeclNode);
        }
        // 3. class or interface
        List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations =
            cu.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration :
            classOrInterfaceDeclarations) {
          String displayName = classOrInterfaceDeclaration.getNameAsString();
          String qualifiedName = packageName + "." + displayName;
          String qualifiedClassName = qualifiedName;
          SemanticNode classDeclarationNode =
              new SemanticNode(
                  nodeCount++,
                  NodeType.CLASSORINTERFACEDECLARATION,
                  displayName,
                  qualifiedName,
                  classOrInterfaceDeclaration.toString());
          semanticGraph.addVertex(classDeclarationNode);
          // package-packages-class
          String finalPackageName1 = packageName;
          Optional<SemanticNode> packageDeclNodeOpt =
              semanticGraph
                  .vertexSet()
                  .stream()
                  .filter(
                      node ->
                          node.getNodeType().equals(NodeType.PACKAGEDECLARATION)
                              && node.getQualifiedName().equals(finalPackageName1))
                  .findAny();
          if (packageDeclNodeOpt.isPresent()) {
            SemanticNode packageDeclNode = packageDeclNodeOpt.get();
            semanticGraph.addEdge(
                packageDeclNode,
                classDeclarationNode,
                new SemanticEdge(EdgeType.PACKAGES, packageDeclNode, classDeclarationNode));
          }

          // class-imports-class(es)
          for (SemanticNode importDeclNode : importDeclNodes) {
            importEdges.put(
                classDeclarationNode,
                importDeclNode.getQualifiedName().trim().replace("import ", "").replace(";", ""));
          }
        }
      }
      // build the external edges
      if (!importEdges.isEmpty()) {
        for (Map.Entry<SemanticNode, String> entry : importEdges.entrySet()) {
          SemanticNode classDeclNode = entry.getKey();
          String importedClassName = entry.getValue();
          Optional<SemanticNode> importedClassOpt =
              semanticGraph
                  .vertexSet()
                  .stream()
                  .filter(
                      node ->
                          node.getNodeType().equals(NodeType.CLASSORINTERFACEDECLARATION)
                              && node.getQualifiedName().equals(importedClassName))
                  .findAny();
          if (importedClassOpt.isPresent()) {
            SemanticNode importedClassNode = importedClassOpt.get();
            semanticGraph.addEdge(
                classDeclNode,
                importedClassNode,
                new SemanticEdge(EdgeType.IMPORTS, classDeclNode, importedClassNode));
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return semanticGraph;
  }

  /**
   * Setup and config the JavaSymbolSolver
   *
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
   *
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
