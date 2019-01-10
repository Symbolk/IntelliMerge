package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import edu.pku.intellimerge.model.*;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.node.MethodDeclNode;
import org.apache.commons.lang3.tuple.Triple;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SemanticGraphBuilder {
  private static final Logger logger = LoggerFactory.getLogger(SemanticGraphBuilder.class);

  private MergeScenario mergeScenario;
  private String collectedFilePath;

  public SemanticGraphBuilder(MergeScenario mergeScenario, String collectedFilePath) {
    this.mergeScenario = mergeScenario;
    this.collectedFilePath = collectedFilePath;
  }

  /**
   * Build graphs once for all
   *
   * @return
   * @throws Exception
   */
  public Triple<
          Graph<SemanticNode, SemanticEdge>,
          Graph<SemanticNode, SemanticEdge>,
          Graph<SemanticNode, SemanticEdge>>
      buildGraphsForAllSides() throws Exception {
    Graph<SemanticNode, SemanticEdge> oursGraph = buildGraphForOneSide(Side.OURS);
    Graph<SemanticNode, SemanticEdge> theirsGraph = buildGraphForOneSide(Side.THEIRS);
    Graph<SemanticNode, SemanticEdge> baseGraph = buildGraphForOneSide(Side.BASE);
    return Triple.of(oursGraph, baseGraph, theirsGraph);
  }

  /**
   * Build the graph once for one side
   *
   * @return
   * @throws Exception
   */
  public Graph<SemanticNode, SemanticEdge> buildGraphForOneSide(Side side) throws Exception {
    String sideDiffPath = collectedFilePath + side.toString().toLowerCase() + "/";

    Graph<SemanticNode, SemanticEdge> graph =
        //        SemanticGraphBuilder.initGraph();
        build(sideDiffPath, mergeScenario.repoPath + mergeScenario.srcPath);
    if (graph == null) {
      logger.error(side.toString() + " graph is null!");
    }
    return graph;
  }

  /**
   * Build the graph by parsing the collected files Try to solve symbols, leading to 3 results: 1.
   * Solved: qualified name got 1.1 By JavaParserTypeSolver(internal): the symbol is defined in
   * other java files in the current project, so create one edge for the def-use 1.2 By
   * ReflectionTypeSolver(JDK): the symbol is defined in jdk libs 2. UnsolvedSymbolException: no
   * qualified name, for the symbol is defined in unavailable jars
   *
   * @param repoPath
   * @param srcPath
   * @return
   */
  public Graph<SemanticNode, SemanticEdge> build(String repoPath, String srcPath) throws Exception {

    // set up the typsolver
    TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(srcPath));
    reflectionTypeSolver.setParent(reflectionTypeSolver);
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(reflectionTypeSolver);
    combinedTypeSolver.add(javaParserTypeSolver);
    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
    JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);
    final JavaParserFacade javaParserFacade = JavaParserFacade.get(combinedTypeSolver);

    // init the semantic graph
    Graph<SemanticNode, SemanticEdge> semanticGraph = initGraph();

    // parse all java files in project folder
    //        ParserConfiguration parserConfiguration
    File root = new File(repoPath);
    SourceRoot sourceRoot = new SourceRoot(root.toPath());
    sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);

    //      ProjectRoot projectRoot = new ParserCollectionStrategy().collect(root.toPath());
    //      ProjectRoot projectRoot1 = new
    // SymbolSolverCollectionStrategy().collect(root.toPath());

    List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParseParallelized();
    //      List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();
    List<CompilationUnit> compilationUnits =
        parseResults
            .stream()
            .filter(ParseResult::isSuccessful)
            .map(r -> r.getResult().get())
            .collect(Collectors.toList());
    // build the graph from the cus
    // save node into SemanticGraph, keep edges in several maps to save later
    Integer nodeCount = 0;
    Integer edgeCount = 0;

    /*
     * a series of temp containers to keep relationships between node and symbol
     * if the symbol is internal: draw the edge in graph;
     * else:
     */
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
      for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : classOrInterfaceDeclarations) {
        String displayName = classOrInterfaceDeclaration.getNameAsString();
        String qualifiedName = packageName + "." + displayName;
        String qualifiedClassName = qualifiedName;
        // enum/interface/inner/local class
        NodeType nodeType = NodeType.CLASS; // default
        nodeType = classOrInterfaceDeclaration.isInterface() ? NodeType.INTERFACE : nodeType;
        nodeType = classOrInterfaceDeclaration.isEnumDeclaration() ? NodeType.ENUM : nodeType;
        nodeType = classOrInterfaceDeclaration.isInnerClass() ? NodeType.INNER_CLASS : nodeType;
        nodeType =
            classOrInterfaceDeclaration.isLocalClassDeclaration() ? NodeType.LOCAL_CLASS : nodeType;
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
          // singleton extend
          String extendedClassName =
              classOrInterfaceDeclaration
                  .getExtendedTypes()
                  .get(0)
                  .resolve()
                  .asReferenceType()
                  .getQualifiedName();
          List<String> temp = new ArrayList<>();
          temp.add(extendedClassName);
          extendEdges.put(classDeclarationNode, temp);
        }
        if (classOrInterfaceDeclaration.getImplementedTypes().size() > 0) {
          // multiple implements
          classOrInterfaceDeclaration
              .getImplementedTypes()
              .forEach(
                  implementedType ->
                      implementedTypes.add(
                          implementedType.resolve().asReferenceType().getQualifiedName()));
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
          for (VariableDeclarator field : fieldDeclaration.getVariables()) {
            displayName = field.toString();
            qualifiedName = qualifiedClassName + "." + displayName;
            SemanticNode fieldDeclarationNode =
                new SemanticNode(
                    nodeCount++, NodeType.FIELD, displayName, qualifiedName, field.toString());
            if (field.getRange().isPresent()) {
              fieldDeclarationNode.setRange(field.getRange().get());
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
            // 4.1 object creation in field declaration
            List<String> declClassNames = new ArrayList<>();
            List<String> initClassNames = new ArrayList<>();
            //            if (!field.getType().isClassOrInterfaceType()) {
            //              ClassOrInterfaceType type = (ClassOrInterfaceType) field.getType();
            //              SymbolReference<ResolvedTypeDeclaration> ref = javaParserFacade.solve();
            //              if (ref.isSolved()) {
            //                String classUsedInFieldName =
            // ref.getCorrespondingDeclaration().getQualifiedName();
            //                if (field.getInitializer().isPresent()) {
            //                  initClassNames.add(classUsedInFieldName);
            //                } else {
            //                  declClassNames.add(classUsedInFieldName);
            //                }
            //              }
            //            }
            if (declClassNames.size() > 0) {
              declObjectEdges.put(fieldDeclarationNode, declClassNames);
            }
            if (initClassNames.size() > 0) {
              initObjectEdges.put(fieldDeclarationNode, initClassNames);
            }
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
        // TODO override/overload
        List<MethodDeclaration> methodDeclarations = classOrInterfaceDeclaration.getMethods();
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
          if (methodDeclaration.getAnnotations().size() > 0) {
            if (methodDeclaration.isAnnotationPresent("Override")) {
              // search the method signature in its superclass or interface
            }
          }
          displayName = methodDeclaration.getSignature().toString();
          qualifiedName = qualifiedClassName + "." + displayName;
          List<String> modifiers =
              methodDeclaration
                  .getModifiers()
                  .stream()
                  .map(Enum::toString)
                  .collect(Collectors.toList());
          List<String> parameterTypes =
              methodDeclaration
                  .getParameters()
                  .stream()
                  .map(Parameter::getType)
                  .map(Type::asString)
                  .collect(Collectors.toList());
          String access = Modifier.getAccessSpecifier(methodDeclaration.getModifiers()).asString();
          MethodDeclNode methodDeclarationNode =
              new MethodDeclNode(
                  nodeCount++,
                  NodeType.METHOD,
                  displayName,
                  qualifiedName,
                  methodDeclaration.toString(),
                      access,
                  modifiers,
                  methodDeclaration.getTypeAsString(),
                  displayName.substring(0, displayName.indexOf("(")),
                  parameterTypes);
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
          methodDeclarationNode.addIncommingEdge(EdgeType.DEFINE_METHOD, classDeclarationNode);

          // 6.1 new instance
          List<ObjectCreationExpr> objectCreationExprs =
              methodDeclaration.findAll(ObjectCreationExpr.class);
          List<String> createObjectNames = new ArrayList<>();
          for (ObjectCreationExpr objectCreationExpr : objectCreationExprs) {
            //            SymbolReference<? extends ResolvedConstructorDeclaration> ref =
            //                    javaParserFacade.solve((objectCreationExpr));
            //            if(ref.isSolved()){
            //              String typeQualifiedName =
            // ref.getCorrespondingDeclaration().declaringType().getQualifiedName();
            //              createObjectNames.add(typeQualifiedName);
            //            }
            try {
              String typeQualifiedName =
                  objectCreationExpr.resolve().declaringType().getQualifiedName();
              createObjectNames.add(typeQualifiedName);
            } catch (UnsolvedSymbolException e) {
              continue;
            }
          }
          if (createObjectNames.size() > 0) {
            initObjectEdges.put(methodDeclarationNode, createObjectNames);
          }

          // 6.2 field access
          // TODO support self field access
          List<FieldAccessExpr> fieldAccessExprs = methodDeclaration.findAll(FieldAccessExpr.class);
          List<String> readFieldNames = new ArrayList<>();
          List<String> writeFieldNames = new ArrayList<>();
          for (FieldAccessExpr fieldAccessExpr : fieldAccessExprs) {
            // resolve the field declaration and draw the edge
            //            final SymbolReference<? extends ResolvedValueDeclaration> ref =
            //                javaParserFacade.solve(fieldAccessExpr);
            SymbolReference<? extends ResolvedValueDeclaration> ref =
                javaParserFacade.solve((fieldAccessExpr));
            if (ref.isSolved()) {
              // internal types
              ResolvedValueDeclaration resolvedDeclaration = ref.getCorrespondingDeclaration();
              if (resolvedDeclaration.isField()) {
                ResolvedFieldDeclaration resolvedFieldDeclaration = resolvedDeclaration.asField();
                displayName = resolvedFieldDeclaration.getName();
                qualifiedName =
                    resolvedFieldDeclaration.declaringType().getQualifiedName()
                        + "."
                        + resolvedFieldDeclaration.getName();
              } else if (resolvedDeclaration.isEnumConstant()) {
                ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration =
                    resolvedDeclaration.asEnumConstant();
                displayName = resolvedEnumConstantDeclaration.getName();
                // TODO: cannot get qualified name now
                qualifiedName = resolvedEnumConstantDeclaration.getName();
              }
            } else {
              // external types
            }
            // whether the field is assigned a value
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
          if (readFieldNames.size() > 0) {
            readFieldEdges.put(methodDeclarationNode, readFieldNames);
          }
          if (writeFieldNames.size() > 0) {
            writeFieldEdges.put(methodDeclarationNode, writeFieldNames);
          }
          // 6.3 method call
          List<MethodCallExpr> methodCallExprs = methodDeclaration.findAll(MethodCallExpr.class);
          List<String> methodCalledNames = new ArrayList<>();
          for (MethodCallExpr methodCallExpr : methodCallExprs) {
            // try to solve the symbol, create node&edge for internal types, and save the external
            // relationships
            try {
              SymbolReference<ResolvedMethodDeclaration> ref =
                  javaParserFacade.solve((methodCallExpr));
              if (ref.isSolved()) {
                ResolvedMethodDeclaration md = methodCallExpr.resolve();
                StringBuilder sb = new StringBuilder();
                sb.append(md.getQualifiedName());
                sb.append("(");
                for (int i = 0; i < md.getNumberOfParams(); i++) {
                  if (i != 0) {
                    sb.append(", ");
                  }
                  String qualifiedType = md.getParam(i).describeType();
                  sb.append(qualifiedType.substring(qualifiedType.lastIndexOf(".") + 1));
                }
                sb.append(")");
                methodCalledNames.add(sb.toString());
              }
            } catch (UnsolvedSymbolException e) {
              continue;
            }
          }
          callMethodEdges.put(methodDeclarationNode, methodCalledNames);
        }
      }
    }

    // now the vertex set is unchanged
    // build the external edges

    edgeCount = buildEdges(semanticGraph, edgeCount, importEdges, EdgeType.IMPORT, NodeType.CLASS);
    edgeCount = buildEdges(semanticGraph, edgeCount, extendEdges, EdgeType.EXTEND, NodeType.CLASS);
    edgeCount =
        buildEdges(
            semanticGraph, edgeCount, implementEdges, EdgeType.IMPLEMENT, NodeType.INTERFACE);
    edgeCount =
        buildEdges(semanticGraph, edgeCount, declObjectEdges, EdgeType.DECL_OBJECT, NodeType.CLASS);
    edgeCount =
        buildEdges(semanticGraph, edgeCount, initObjectEdges, EdgeType.INIT_OBJECT, NodeType.CLASS);
    edgeCount =
        buildEdges(semanticGraph, edgeCount, readFieldEdges, EdgeType.READ_FIELD, NodeType.FIELD);
    edgeCount =
        buildEdges(semanticGraph, edgeCount, writeFieldEdges, EdgeType.WRITE_FIELD, NodeType.FIELD);
    edgeCount =
        buildEdges(
            semanticGraph, edgeCount, callMethodEdges, EdgeType.CALL_METHOD, NodeType.METHOD);
    return semanticGraph;
  }

  /**
   * Add edges between recorded node
   *
   * @param edgeCount edge id
   * @param edges collected temp mapping from source node to qualified name of target node
   * @param edgeType edge type
   * @param targetNodeType target node type
   * @return
   */
  private Integer buildEdges(
      Graph<SemanticNode, SemanticEdge> semanticGraph,
      Integer edgeCount,
      Map<SemanticNode, List<String>> edges,
      EdgeType edgeType,
      NodeType targetNodeType) {
    if (edges.isEmpty()) {
      return edgeCount;
    }
    // TODO create edge for other node

    Set<SemanticNode> vertexSet = semanticGraph.vertexSet();
    for (Map.Entry<SemanticNode, List<String>> entry : edges.entrySet()) {
      SemanticNode sourceNode = entry.getKey();
      List<String> targetNodeNames = entry.getValue();
      for (String targeNodeName : targetNodeNames) {
        SemanticNode targetNode = getTargetNode(vertexSet, targeNodeName, targetNodeType);
        if (targetNode != null) {
          // TODO check if the edge already exists
          semanticGraph.addEdge(
              sourceNode,
              targetNode,
              new SemanticEdge(edgeCount++, edgeType, sourceNode, targetNode));
          // outgoing edge
          if (sourceNode instanceof MethodDeclNode) {
            MethodDeclNode methodDeclNode = (MethodDeclNode) sourceNode;
            methodDeclNode.addOutgoingEdge(edgeType, targetNode);
          }
          // incoming edge
          if (targetNodeType == NodeType.METHOD && targetNode instanceof MethodDeclNode) {
            MethodDeclNode methodDeclNode = (MethodDeclNode) targetNode;
            methodDeclNode.addIncommingEdge(edgeType, sourceNode);
          }
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
  public SemanticNode getTargetNode(
      Set<SemanticNode> vertexSet, String targetQualifiedName, NodeType targetNodeType) {
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
  private JavaSymbolSolver setUpSymbolSolver(String packagePath, String libPath) {
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
  public Graph<SemanticNode, SemanticEdge> initGraph() {
    return GraphTypeBuilder.<SemanticNode, SemanticEdge>directed()
        .allowingMultipleEdges(true)
        .allowingSelfLoops(false)
        .edgeClass(SemanticEdge.class)
        .weighted(true)
        .buildGraph();
  }
}
