package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
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
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SemanticGraphBuilder {

  private static Graph<SemanticNode, SemanticEdge> semanticGraph;

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
    semanticGraph = buildGraph();

    // parse all java files in project folder
    try {
      //        ParserConfiguration parserConfiguration
      File root = new File(folderPath);
      SourceRoot sourceRoot = new SourceRoot(root.toPath());
      sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);
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
      Map<SemanticNode, List<String>> importEdges = new HashMap<>();
      Map<SemanticNode, List<String>> extendEdges = new HashMap<>();
      Map<SemanticNode, List<String>> implementEdges = new HashMap<>();
      Map<SemanticNode, List<String>> declObjectEdges = new HashMap<>();
      Map<SemanticNode, List<String>> initObjectEdges = new HashMap<>();
      Map<SemanticNode, List<String>> readFieldEdges = new HashMap<>();
      Map<SemanticNode, List<String>> writeFieldEdges = new HashMap<>();
      Map<SemanticNode, List<String>> callMethodEdges = new HashMap<>();
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
                      node.getNodeType().equals(NodeType.PACKAGE)
                          && node.getQualifiedName().equals(finalPackageName))) {
            SemanticNode packageDeclNode =
                new SemanticNode(
                    nodeCount++,
                    NodeType.PACKAGE,
                    packageDeclaration.getNameAsString(),
                    packageDeclaration.getNameAsString(),
                    packageDeclaration.toString());
            semanticGraph.addVertex(packageDeclNode);
          }
        }
        // 2. import
        List<ImportDeclaration> importDeclarations = cu.getImports();
        List<String> importedClassNames = new ArrayList<>();
        for (ImportDeclaration importDeclaration : importDeclarations) {
          importedClassNames.add(
              importDeclaration.getNameAsString().trim().replace("import ", "").replace(";", ""));
        }
        // 3. class or interface
        List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations =
            cu.findAll(ClassOrInterfaceDeclaration.class);
        List<String> implementedTypes = new ArrayList<>();
        for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration :
            classOrInterfaceDeclarations) {
          String displayName = classOrInterfaceDeclaration.getNameAsString();
          String qualifiedName = packageName + "." + displayName;
          String qualifiedClassName = qualifiedName;
          // enum/interface/inner/local class
          Enum nodeType=NodeType.CLASS; // default
          nodeType=classOrInterfaceDeclaration.isInterface() ? NodeType.INTERFACE:nodeType;
          nodeType=classOrInterfaceDeclaration.isEnumDeclaration() ? NodeType.ENUM:nodeType;
          nodeType=classOrInterfaceDeclaration.isInnerClass() ? NodeType.INNER_CLASS:nodeType;
          nodeType=classOrInterfaceDeclaration.isLocalClassDeclaration() ? NodeType.LOCAL_CLASS:nodeType;
          SemanticNode classDeclarationNode =
              new SemanticNode(
                  nodeCount++,
                  nodeType,
                  displayName,
                  qualifiedName,
                  classOrInterfaceDeclaration.toString());
          semanticGraph.addVertex(classDeclarationNode);

          // extend/implement
          if (classOrInterfaceDeclaration.getExtendedTypes().size() > 0) {
            String extendedClassName =
                classOrInterfaceDeclaration.getExtendedTypes().get(0).resolve().asReferenceType().getQualifiedName();
            List<String> temp = new ArrayList<>();
            temp.add(extendedClassName);
            extendEdges.put(classDeclarationNode, temp);
          }
          if (classOrInterfaceDeclaration.getImplementedTypes().size() > 0) {
            classOrInterfaceDeclaration
                .getImplementedTypes()
                .forEach(
                    implementedType -> implementedTypes.add(implementedType.resolve().asReferenceType().getQualifiedName()));
            implementEdges.put(classDeclarationNode, implementedTypes);
          }

          String finalPackageName1 = packageName;
          Optional<SemanticNode> packageDeclNodeOpt =
              semanticGraph
                  .vertexSet()
                  .stream()
                  .filter(
                      node ->
                          node.getNodeType().equals(NodeType.PACKAGE)
                              && node.getQualifiedName().equals(finalPackageName1))
                  .findAny();
          if (packageDeclNodeOpt.isPresent()) {
            SemanticNode packageDeclNode = packageDeclNodeOpt.get();
            semanticGraph.addEdge(
                packageDeclNode,
                classDeclarationNode,
                new SemanticEdge(
                    edgeCount++, EdgeType.CONTAIN, packageDeclNode, classDeclarationNode));
          }

          // class-imports-class(es)
          importEdges.put(classDeclarationNode, importedClassNames);

          // 4. field
          List<FieldDeclaration> fieldDeclarations = classOrInterfaceDeclaration.getFields();
          for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
            displayName = fieldDeclaration.getVariables().get(0).toString();
            qualifiedName = qualifiedClassName + "." + displayName;
            SemanticNode fieldDeclarationNode =
                new SemanticNode(
                    nodeCount++,
                    NodeType.FIELD,
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
                new SemanticEdge(
                    edgeCount++,
                    EdgeType.DEFINE_FIELD,
                    classDeclarationNode,
                    fieldDeclarationNode));
            // 4.1 field declaration
            List<String> classUsedInFieldNames = new ArrayList<>();
            if (!fieldDeclaration.getVariables().get(0).getType().isPrimitiveType()) {
              String classUsedInFieldName =
                  fieldDeclaration
                      .getVariables()
                      .get(0)
                      .getType()
                      .resolve()
                      .asReferenceType()
                      .getQualifiedName();
              // TODO decl or init object
              classUsedInFieldNames.add(classUsedInFieldName);
            }
            declObjectEdges.put(fieldDeclarationNode, classUsedInFieldNames);
          }
          // 5. constructor
          List<ConstructorDeclaration> constructorDeclarations =
              classOrInterfaceDeclaration.getConstructors();
          for (ConstructorDeclaration constructorDeclaration : constructorDeclarations) {
            displayName = constructorDeclaration.getSignature().toString();
            qualifiedName = qualifiedClassName + "." + displayName;
            SemanticNode constructorDeclNode =
                new SemanticNode(
                    nodeCount++,
                    NodeType.CONSTRUCTOR,
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
                new SemanticEdge(
                    edgeCount++,
                    EdgeType.DEFINE_CONSTRUCTOR,
                    classDeclarationNode,
                    constructorDeclNode));
          }
          // 6. method
          List<MethodDeclaration> methodDeclarations = classOrInterfaceDeclaration.getMethods();
          for (MethodDeclaration methodDeclaration : methodDeclarations) {
            displayName = methodDeclaration.getSignature().toString();
            qualifiedName = qualifiedClassName + "." + displayName;
            SemanticNode methodDeclarationNode =
                new SemanticNode(
                    nodeCount++,
                    NodeType.METHOD,
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
                new SemanticEdge(
                    edgeCount++,
                    EdgeType.DEFINE_METHOD,
                    classDeclarationNode,
                    methodDeclarationNode));

            // 6.1 field access
            List<FieldAccessExpr> fieldAccessExprs =
                methodDeclaration.findAll(FieldAccessExpr.class);
            List<String> readFieldNames = new ArrayList<>();
            List<String> writeFieldNames = new ArrayList<>();
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
              // modifty the value of the field
              if (fieldAccessExpr.getParentNode().isPresent()) {
                Node parent = fieldAccessExpr.getParentNode().get();
                if (parent instanceof AssignExpr) {
                  AssignExpr parentAssign = (AssignExpr) parent;
                  if (parentAssign.getTarget().equals(fieldAccessExpr)) {
                    writeFieldNames.add(qualifiedName);
                  }
                }
              }
              readFieldNames.add(qualifiedName);
            }
            readFieldEdges.put(methodDeclarationNode, readFieldNames);
            writeFieldEdges.put(methodDeclarationNode, writeFieldNames);
            // 6.2 method call
            List<MethodCallExpr> methodCallExprs = methodDeclaration.findAll(MethodCallExpr.class);
            List<String> methodCalledNames = new ArrayList<>();
            for (MethodCallExpr methodCallExpr : methodCallExprs) {
              // resolve the method declaration and draw the edge
              methodCalledNames.add(methodCallExpr.resolve().getQualifiedSignature());
            }
            callMethodEdges.put(methodDeclarationNode, methodCalledNames);
          }
        }
      }

      // build the external edges
      // now the vertex is determined
      Set<SemanticNode> vertexSet = semanticGraph.vertexSet();

      edgeCount = buildEdges(edgeCount, importEdges, EdgeType.IMPORT, NodeType.CLASS);
      edgeCount = buildEdges(edgeCount, extendEdges, EdgeType.EXTEND, NodeType.CLASS);
      edgeCount = buildEdges(edgeCount, implementEdges, EdgeType.IMPLEMENT, NodeType.INTERFACE);
      edgeCount = buildEdges(edgeCount, declObjectEdges, EdgeType.DECL_OBJECT, NodeType.CLASS);
      edgeCount = buildEdges(edgeCount, readFieldEdges, EdgeType.READ_FIELD, NodeType.FIELD);
      edgeCount = buildEdges(edgeCount, writeFieldEdges, EdgeType.WRITE_FIELD, NodeType.FIELD);
      edgeCount = buildEdges(edgeCount, callMethodEdges, EdgeType.CALL_METHOD, NodeType.METHOD);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return semanticGraph;
  }

  /**
   * Build edges from maps
   *
   * @param edgeCount
   * @param edges
   * @param edgeType
   * @param targetNodeType
   * @return
   */
  private static Integer buildEdges(
      Integer edgeCount,
      Map<SemanticNode, List<String>> edges,
      Enum edgeType,
      Enum targetNodeType) {
    if (edges.isEmpty()) {
      return edgeCount;
    }
    Set<SemanticNode> vertexSet = semanticGraph.vertexSet();
    for (Map.Entry<SemanticNode, List<String>> entry : edges.entrySet()) {
      SemanticNode sourceNode = entry.getKey();
      List<String> targetNodeNames = entry.getValue();
      // TODO class types
      for (String targeNodeName : targetNodeNames) {
        SemanticNode targetNode = getTargetNode(vertexSet, targeNodeName, targetNodeType);
        if (targetNode != null) {
          semanticGraph.addEdge(
              sourceNode,
              targetNode,
              new SemanticEdge(edgeCount++, edgeType, sourceNode, targetNode));
        }
      }
    }
    return edgeCount;
  }

  /**
   * Get the target node from vertex set according to qualified name
   *
   * @param vertexSet
   * @param targetQualifiedName
   * @param targetNodeType
   * @return
   */
  public static SemanticNode getTargetNode(
      Set<SemanticNode> vertexSet, String targetQualifiedName, Enum targetNodeType) {
    Optional<SemanticNode> targetNodeOpt =
        vertexSet
            .stream()
            .filter(
                node ->
                    node.getNodeType().equals(targetNodeType)
                        && node.getQualifiedName().equals(targetQualifiedName))
            .findAny();
    if (targetNodeOpt.isPresent()) {
      return targetNodeOpt.get();
    } else {
      return null;
    }
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
