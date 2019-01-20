package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ReferenceType;
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
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.node.*;
import org.apache.commons.lang3.tuple.Triple;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
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

    Graph<SemanticNode, SemanticEdge> graph =
        //        SemanticGraphBuilder.initGraph();
        build(side, mergeScenario.repoPath + mergeScenario.srcPath);
    if (graph == null) {
      logger.error(side.toString() + " graph is null!");
    }
    return graph;
  }

  /**
   * Build the graph by parsing the collected files Try to solve symbols, leading to 3 results:
   *
   * <p>1. Solved: qualified name got 1.1 By JavaParserTypeSolver(internal): the symbol is defined
   * in other java files in the current project, so create one edge for the def-use 1.2 By
   * ReflectionTypeSolver(JDK): the symbol is defined in jdk libs 2. UnsolvedSymbolException: no
   * qualified name, for the symbol is defined in unavailable jars
   *
   * @param side
   * @param srcPath the context files for symbolsolving, i.e. the source folder of this java project
   * @return
   */
  public Graph<SemanticNode, SemanticEdge> build(Side side, String srcPath) throws Exception {

    // the folder path which contains collected files to build the graph upon
    String sideDiffPath = collectedFilePath + side.toString().toLowerCase() + File.separator;

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
    Graph<SemanticNode, SemanticEdge> graph = initGraph();

    // parse all java files in project folder
    //        ParserConfiguration parserConfiguration
    File root = new File(sideDiffPath);
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

    // incremental id, unique in one side's graph
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

    /*
     * build the graph by analyzing every CU
     */
    for (CompilationUnit cu : compilationUnits) {
      String fileName = cu.getStorage().map(CompilationUnit.Storage::getFileName).orElse("");
      String absolutePath =
          cu.getStorage().map(CompilationUnit.Storage::getPath).map(Path::toString).orElse("");
      String relativePath = absolutePath.replace(sideDiffPath, "");

      // whether this file is modified: if yes, all nodes in it need to be merged (rough way)
      Boolean isChangedFile = mergeScenario.isChangedFile(side, relativePath);

      CompilationUnitNode cuNode =
          new CompilationUnitNode(
              nodeCount++,
              isChangedFile,
              NodeType.CU,
              fileName,
              absolutePath,
              fileName,
              relativePath,
              absolutePath,
              cu.getPackageDeclaration().map(PackageDeclaration::toString).orElse(""),
              cu.getImports()
                  .stream()
                  .map(ImportDeclaration::toString)
                  .collect(Collectors.toSet()));

      graph.addVertex(cuNode);
      // 1. package
      String packageName = "";
      if (cu.getPackageDeclaration().isPresent()) {
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration().get();
        packageName = packageDeclaration.getNameAsString();
        cuNode.setQualifiedName(packageName + "." + fileName);
        // check if the package node exists
        // if not exist, create one
        String finalPackageName = packageName;
        Optional<SemanticNode> packageDeclNodeOpt =
            graph
                .vertexSet()
                .stream()
                .filter(
                    node ->
                        node.getNodeType().equals(NodeType.PACKAGE)
                            && node.getQualifiedName().equals(finalPackageName))
                .findAny();
        if (!packageDeclNodeOpt.isPresent()) {
          PackageDeclNode packageDeclNode =
              new PackageDeclNode(
                  nodeCount++,
                  isChangedFile,
                  NodeType.PACKAGE,
                  finalPackageName,
                  packageDeclaration.toString(),
                  finalPackageName,
                  Arrays.asList(finalPackageName.split(".")));
          graph.addVertex(packageDeclNode);
          graph.addEdge(
              packageDeclNode,
              cuNode,
              new SemanticEdge(edgeCount++, EdgeType.CONTAIN, packageDeclNode, cuNode));
        } else {
          graph.addEdge(
              packageDeclNodeOpt.get(),
              cuNode,
              new SemanticEdge(edgeCount++, EdgeType.CONTAIN, packageDeclNodeOpt.get(), cuNode));
        }
      }
      // 2. import
      List<ImportDeclaration> importDeclarations = cu.getImports();
      List<String> importedClassNames = new ArrayList<>();
      for (ImportDeclaration importDeclaration : importDeclarations) {
        importedClassNames.add(
            importDeclaration.getNameAsString().trim().replace("import ", "").replace(";", ""));
      }
      // 3. type declaration: enum, class/interface
      List<EnumDeclaration> enumDeclarations = cu.findAll(EnumDeclaration.class);
      for (EnumDeclaration enumDeclaration : enumDeclarations) {
        String displayName = enumDeclaration.getNameAsString();
        String qualifiedName = packageName + "." + displayName;
        String qualifiedClassName = qualifiedName;
      }

      List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations =
          cu.findAll(ClassOrInterfaceDeclaration.class);
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

        List<String> modifiers =
            classOrInterfaceDeclaration
                .getModifiers()
                .stream()
                .map(Enum::toString)
                .collect(Collectors.toList());
        String access =
            Modifier.getAccessSpecifier(classOrInterfaceDeclaration.getModifiers()).asString();
        String originalSignature = getTypeOriginalSignature(classOrInterfaceDeclaration);

        TypeDeclNode typeDeclNode =
            new TypeDeclNode(
                nodeCount++,
                isChangedFile,
                nodeType,
                displayName,
                qualifiedName,
                originalSignature,
                access,
                modifiers,
                nodeType.asString(),
                displayName);
        graph.addVertex(typeDeclNode);

        graph.addEdge(
            cuNode,
            typeDeclNode,
            new SemanticEdge(edgeCount++, EdgeType.DEFINE_TYPE, cuNode, typeDeclNode));

        // extend/implement
        if (classOrInterfaceDeclaration.getExtendedTypes().size() > 0) {
          // single extends
          String extendedType =
              classOrInterfaceDeclaration
                  .getExtendedTypes()
                  .get(0)
                  .resolve()
                  .asReferenceType()
                  .getQualifiedName();
          List<String> temp = new ArrayList<>();
          temp.add(extendedType);
          typeDeclNode.setExtendType(extendedType);
          extendEdges.put(typeDeclNode, temp);
        }
        if (classOrInterfaceDeclaration.getImplementedTypes().size() > 0) {
          List<String> implementedTypes = new ArrayList<>();
          // multiple implements
          classOrInterfaceDeclaration
              .getImplementedTypes()
              .forEach(
                  implementedType ->
                      implementedTypes.add(
                          implementedType.resolve().asReferenceType().getQualifiedName()));
          typeDeclNode.setImplementTypes(implementedTypes);
          implementEdges.put(typeDeclNode, implementedTypes);
        }

        // class-imports-class(es)
        importEdges.put(cuNode, importedClassNames);

        // 4. field
        List<FieldDeclaration> fieldDeclarations = classOrInterfaceDeclaration.getFields();
        for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
          // there can be more than one var declared in one field declaration, add one node for each
          modifiers =
              fieldDeclaration
                  .getModifiers()
                  .stream()
                  .map(Enum::toString)
                  .collect(Collectors.toList());

          access = Modifier.getAccessSpecifier(fieldDeclaration.getModifiers()).asString();
          for (VariableDeclarator field : fieldDeclaration.getVariables()) {
            displayName = field.toString();
            qualifiedName = qualifiedClassName + "." + displayName;
            originalSignature = getFieldOriginalSignature(fieldDeclaration);

            FieldDeclNode fieldDeclarationNode =
                new FieldDeclNode(
                    nodeCount++,
                    isChangedFile,
                    NodeType.FIELD,
                    displayName,
                    qualifiedName,
                    originalSignature,
                    access,
                    modifiers,
                    field.getTypeAsString(),
                    field.getNameAsString(),
                    field.getInitializer().map(Expression::toString).orElse(""),
                    field.getRange());
            graph.addVertex(fieldDeclarationNode);
            // add edge between field and class
            graph.addEdge(
                typeDeclNode,
                fieldDeclarationNode,
                new SemanticEdge(
                    edgeCount++, EdgeType.DEFINE_FIELD, typeDeclNode, fieldDeclarationNode));
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
          ConstructorDeclNode constructorDeclNode =
              new ConstructorDeclNode(
                  nodeCount++,
                  isChangedFile,
                  NodeType.CONSTRUCTOR,
                  displayName,
                  qualifiedName,
                  constructorDeclaration.getDeclarationAsString(),
                  displayName,
                  constructorDeclaration.toString(),
                  constructorDeclaration.getRange());
          graph.addVertex(constructorDeclNode);
          graph.addEdge(
              typeDeclNode,
              constructorDeclNode,
              new SemanticEdge(
                  edgeCount++, EdgeType.DEFINE_CONSTRUCTOR, typeDeclNode, constructorDeclNode));
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
          modifiers =
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
          List<String> parameterNames =
              methodDeclaration
                  .getParameters()
                  .stream()
                  .map(Parameter::getNameAsString)
                  .collect(Collectors.toList());
          access = Modifier.getAccessSpecifier(methodDeclaration.getModifiers()).asString();
          List<String> throwsExceptions =
              methodDeclaration
                  .getThrownExceptions()
                  .stream()
                  .map(ReferenceType::toString)
                  .collect(Collectors.toList());
          MethodDeclNode methodDeclarationNode =
              new MethodDeclNode(
                  nodeCount++,
                  isChangedFile,
                  NodeType.METHOD,
                  displayName,
                  qualifiedName,
                  methodDeclaration.getDeclarationAsString(),
                  access,
                  modifiers,
                  methodDeclaration.getTypeAsString(),
                  displayName.substring(0, displayName.indexOf("(")),
                  parameterTypes,
                  parameterNames,
                  throwsExceptions,
                  methodDeclaration.getBody().map(BlockStmt::toString).orElse(""),
                  methodDeclaration.getRange());
          graph.addVertex(methodDeclarationNode);
          graph.addEdge(
              typeDeclNode,
              methodDeclarationNode,
              new SemanticEdge(
                  edgeCount++, EdgeType.DEFINE_METHOD, typeDeclNode, methodDeclarationNode));

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

    // now vertices are fixed

    // build the recorded edges actually
    // TODO import can be any type, even inner type
    edgeCount = buildEdges(graph, edgeCount, importEdges, EdgeType.IMPORT, NodeType.CLASS);
    edgeCount = buildEdges(graph, edgeCount, extendEdges, EdgeType.EXTEND, NodeType.CLASS);
    edgeCount =
        buildEdges(graph, edgeCount, implementEdges, EdgeType.IMPLEMENT, NodeType.INTERFACE);
    edgeCount = buildEdges(graph, edgeCount, declObjectEdges, EdgeType.DECL_OBJECT, NodeType.CLASS);
    edgeCount = buildEdges(graph, edgeCount, initObjectEdges, EdgeType.INIT_OBJECT, NodeType.CLASS);
    edgeCount = buildEdges(graph, edgeCount, readFieldEdges, EdgeType.READ_FIELD, NodeType.FIELD);
    edgeCount = buildEdges(graph, edgeCount, writeFieldEdges, EdgeType.WRITE_FIELD, NodeType.FIELD);
    edgeCount =
        buildEdges(graph, edgeCount, callMethodEdges, EdgeType.CALL_METHOD, NodeType.METHOD);

    // now edges are fixed
    // save incoming edges and outgoing edges in corresponding nodes
    for (SemanticNode node : graph.vertexSet()) {
      Set<SemanticEdge> incommingEdges = graph.incomingEdgesOf(node);
      for (SemanticEdge edge : incommingEdges) {
        if (node.incomingEdges.containsKey(edge.getEdgeType())) {
          node.incomingEdges.get(edge.getEdgeType()).add(graph.getEdgeSource(edge));
        } else {
          logger.error("Unexpected in edge:" + edge);
        }
      }
      Set<SemanticEdge> outgoingEdges = graph.outgoingEdgesOf(node);
      for (SemanticEdge edge : outgoingEdges) {
        if (node.outgoingEdges.containsKey(edge.getEdgeType())) {
          node.outgoingEdges.get(edge.getEdgeType()).add(graph.getEdgeTarget(edge));
        } else {
          logger.error("Unexpected out edge:" + edge);
        }
      }
    }

    return graph;
  }

  private String getFieldOriginalSignature(FieldDeclaration fieldDeclaration) {
    String source = fieldDeclaration.toString();
    return source
        .substring(0, (source.contains("=") ? source.indexOf("=") : source.indexOf(";")))
        .trim();
  }

  private String getTypeOriginalSignature(TypeDeclaration typeDeclaration) {
    String source = typeDeclaration.toString();
    return source.substring(0, source.indexOf("{")).trim();
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
        .allowingSelfLoops(true) // recursion
        .edgeClass(SemanticEdge.class)
        .weighted(true)
        .buildGraph();
  }
}
