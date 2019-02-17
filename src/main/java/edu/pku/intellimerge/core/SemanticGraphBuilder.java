package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
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
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.node.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/** Build SemanticGraph among collected java files */
public class SemanticGraphBuilder {
  private static final Logger logger = LoggerFactory.getLogger(SemanticGraphBuilder.class);
  // init the semantic graph
  Graph<SemanticNode, SemanticEdge> graph;
  JavaSymbolSolver symbolSolver;
  JavaParserFacade javaParserFacade;
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

  private MergeScenario mergeScenario;
  private Side side;
  private String collectedFilePath;
  //  the context files for symbolsolving, i.e. the source folder of this java project
  private String srcPath;

  public SemanticGraphBuilder(MergeScenario mergeScenario, Side side, String collectedFilePath) {
    this.mergeScenario = mergeScenario;
    this.side = side;
    this.collectedFilePath = collectedFilePath;
    this.srcPath = mergeScenario.repoPath + mergeScenario.srcPath;

    this.graph = initGraph();
    // set up the typsolver
    TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(srcPath));
    reflectionTypeSolver.setParent(reflectionTypeSolver);
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(reflectionTypeSolver);
    combinedTypeSolver.add(javaParserTypeSolver);
    this.symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
    JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);
    this.javaParserFacade = JavaParserFacade.get(combinedTypeSolver);
  }

  /**
   * Build the graph by parsing the collected files Try to solve symbols, leading to 3 results:
   *
   * <p>1. Solved: qualified name got 1.1 By JavaParserTypeSolver(internal): the symbol is defined
   * in other java files in the current project, so create one edge for the def-use 1.2 By
   * ReflectionTypeSolver(JDK): the symbol is defined in jdk libs 2. UnsolvedSymbolException: no
   * qualified name, for the symbol is defined in unavailable jars
   *
   * @return
   */
  public Graph<SemanticNode, SemanticEdge> build() {

    // the folder path which contains collected files to build the graph upon
    String sideDiffPath = collectedFilePath + File.separator + side.asString() + File.separator;
    // just for sure: reinit the graph
    this.graph = initGraph();

    // parse all java files in the file
    // regular project: only one source folder
    File root = new File(sideDiffPath);
    //    SourceRoot sourceRoot = new SourceRoot(root.toPath());
    //    sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);

    // multi-module project: separated source folder for sub-projects/modules
    //      ProjectRoot projectRoot = new ParserCollectionStrategy().collect(root.toPath());
    ProjectRoot projectRoot =
        new SymbolSolverCollectionStrategy(
                JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver))
            .collect(root.toPath());
    List<CompilationUnit> compilationUnits = new ArrayList<>();

    for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
      List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParseParallelized();
      //      List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();
      compilationUnits.addAll(
          parseResults
              .stream()
              .filter(ParseResult::isSuccessful)
              .map(r -> r.getResult().get())
              .collect(Collectors.toList()));
    }

    /*
     * build the graph by analyzing every CU
     */
    for (CompilationUnit cu : compilationUnits) {
      String fileName = cu.getStorage().map(CompilationUnit.Storage::getFileName).orElse("");
      String absolutePath =
          cu.getStorage().map(CompilationUnit.Storage::getPath).map(Path::toString).orElse("");
      String relativePath = absolutePath.replace(collectedFilePath, "");

      // whether this file is modified: if yes, all nodes in it need to be merged (rough way)
      Boolean isInChangedFile = mergeScenario.isInChangedFile(side, relativePath);

      CompilationUnitNode cuNode =
          new CompilationUnitNode(
              nodeCount++,
              isInChangedFile,
              NodeType.CU,
              fileName,
              "",
              fileName,
              cu.getComment().map(Comment::getContent).orElse(""),
              fileName,
              relativePath,
              absolutePath,
              cu.getPackageDeclaration().map(PackageDeclaration::toString).orElse(""),
              cu.getImports()
                  .stream()
                  .map(ImportDeclaration::toString)
                  .collect(Collectors.toCollection(LinkedHashSet::new)));

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
                  isInChangedFile,
                  NodeType.PACKAGE,
                  finalPackageName,
                  packageDeclaration.getNameAsString(),
                  packageDeclaration.toString().trim(),
                  packageDeclaration.getComment().map(Comment::getContent).orElse(""),
                  finalPackageName,
                  Arrays.asList(finalPackageName.split(".")));
          graph.addVertex(cuNode);
          graph.addVertex(packageDeclNode);
          graph.addEdge(
              packageDeclNode,
              cuNode,
              new SemanticEdge(edgeCount++, EdgeType.CONTAIN, packageDeclNode, cuNode));
        } else {
          graph.addVertex(cuNode);
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

      // 3. type declaration: annotation, enum, class/interface
      // getTypes() returns top level types declared in this compilation unit
      for (TypeDeclaration td : cu.getTypes()) {
        //        td.getMembers()
        TypeDeclNode tdNode = processTypeDeclaration(td, packageName, nodeCount, isInChangedFile);
        nodeCount++;
        graph.addVertex(tdNode);

        if (td.isTopLevelType()) {
          graph.addEdge(
              cuNode, tdNode, new SemanticEdge(edgeCount++, EdgeType.DEFINE_TYPE, cuNode, tdNode));

          if (td.isClassOrInterfaceDeclaration()) {
            ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) td;
            // extend/implement
            if (cid.getExtendedTypes().size() > 0) {
              // single extends
              String extendedType =
                  cid.getExtendedTypes().get(0).resolve().asReferenceType().getQualifiedName();
              List<String> temp = new ArrayList<>();
              temp.add(extendedType);
              tdNode.setExtendType(extendedType);
              extendEdges.put(tdNode, temp);
            }
            if (cid.getImplementedTypes().size() > 0) {
              List<String> implementedTypes = new ArrayList<>();
              // multiple implements
              cid.getImplementedTypes()
                  .forEach(
                      implementedType ->
                          implementedTypes.add(
                              implementedType.resolve().asReferenceType().getQualifiedName()));
              tdNode.setImplementTypes(implementedTypes);
              implementEdges.put(tdNode, implementedTypes);
            }

            // class-imports-class(es)
            importEdges.put(cuNode, importedClassNames);
          }
          processMemebers(td, tdNode, packageName, isInChangedFile);
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

  private void processMemebers(
      TypeDeclaration td, TypeDeclNode tdNode, String packageName, boolean isInChangedFile) {
    String qualifiedTypeName = packageName + "." + td.getNameAsString();
    List<String> modifiers;
    String access, displayName, qualifiedName, originalSignature;
    // if contains nested type declaration, iterate into it
    for (Node child : td.getChildNodes()) {
      if (child instanceof TypeDeclaration) {
        TypeDeclaration childTD = (TypeDeclaration) child;
        if (childTD.isNestedType()) {
          // add edge from the parent td to the nested td
          TypeDeclNode childTDNode =
              processTypeDeclaration(childTD, packageName, nodeCount, isInChangedFile);
          graph.addVertex(childTDNode);
          graph.addEdge(
              tdNode,
              childTDNode,
              new SemanticEdge(edgeCount++, EdgeType.DEFINE_TYPE, tdNode, childTDNode));
          // process nested td members iteratively
          processMemebers(childTD, childTDNode, packageName, isInChangedFile);
        }
      } else {
        // for other members (constructor, field, method), create the node
        // add the edge from the parent td to the member
        // 4. field
        if (child instanceof FieldDeclaration) {
          FieldDeclaration fd = (FieldDeclaration) child;
          // there can be more than one var declared in one field declaration, add one node for
          // each
          modifiers =
              fd.getModifiers().stream().map(Modifier::toString).collect(Collectors.toList());

          access = fd.getAccessSpecifier().asString();
          for (VariableDeclarator field : fd.getVariables()) {
            displayName = field.toString();
            qualifiedName = qualifiedTypeName + "." + displayName;
            originalSignature = getFieldOriginalSignature(fd);
            String body =
                field.getInitializer().isPresent()
                    ? "=" + field.getInitializer().get().toString() + ";"
                    : ";";

            FieldDeclNode fdNode =
                new FieldDeclNode(
                    nodeCount++,
                    isInChangedFile,
                    NodeType.FIELD,
                    displayName,
                    qualifiedName,
                    originalSignature,
                    fd.getComment().map(Comment::getContent).orElse(""),
                    access,
                    modifiers,
                    field.getTypeAsString(),
                    field.getNameAsString(),
                    body,
                    field.getRange());
            graph.addVertex(fdNode);
            // add edge between field and class
            graph.addEdge(
                tdNode,
                fdNode,
                new SemanticEdge(edgeCount++, EdgeType.DEFINE_FIELD, tdNode, fdNode));
            // 4.1 object creation in field declaration
            List<String> declClassNames = new ArrayList<>();
            List<String> initClassNames = new ArrayList<>();
            //            if (!field.getType().isClassOrInterfaceType()) {
            //              ClassOrInterfaceType type = (ClassOrInterfaceType) field.getType();
            //              SymbolReference<ResolvedTypeDeclaration> ref =
            // javaParserFacade.solve();
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
              declObjectEdges.put(fdNode, declClassNames);
            }
            if (initClassNames.size() > 0) {
              initObjectEdges.put(fdNode, initClassNames);
            }
          }
        }

        // 5. constructor
        if (child instanceof ConstructorDeclaration) {
          ConstructorDeclaration cd = (ConstructorDeclaration) child;
          displayName = cd.getSignature().toString();
          qualifiedName = qualifiedTypeName + "." + displayName;
          ConstructorDeclNode cdNode =
              new ConstructorDeclNode(
                  nodeCount++,
                  isInChangedFile,
                  NodeType.CONSTRUCTOR,
                  displayName,
                  qualifiedName,
                  cd.getDeclarationAsString(),
                  cd.getComment().map(Comment::getContent).orElse(""),
                  displayName,
                  cd.getBody().toString(),
                  cd.getRange());
          graph.addVertex(cdNode);
          graph.addEdge(
              tdNode,
              cdNode,
              new SemanticEdge(edgeCount++, EdgeType.DEFINE_CONSTRUCTOR, tdNode, cdNode));
        }
        // 6. method
        if (child instanceof MethodDeclaration) {
          MethodDeclaration md = (MethodDeclaration) child;
          if (md.getAnnotations().size() > 0) {
            if (md.isAnnotationPresent("Override")) {
              // search the method signature in its superclass or interface
            }
          }
          displayName = md.getSignature().toString();
          qualifiedName = qualifiedTypeName + "." + displayName;
          modifiers =
              md.getModifiers().stream().map(Modifier::toString).collect(Collectors.toList());
          List<String> parameterTypes =
              md.getParameters()
                  .stream()
                  .map(Parameter::getType)
                  .map(Type::asString)
                  .collect(Collectors.toList());
          List<String> parameterNames =
              md.getParameters()
                  .stream()
                  .map(Parameter::getNameAsString)
                  .collect(Collectors.toList());
          access = md.getAccessSpecifier().asString();
          List<String> throwsExceptions =
              md.getThrownExceptions()
                  .stream()
                  .map(ReferenceType::toString)
                  .collect(Collectors.toList());
          MethodDeclNode mdNode =
              new MethodDeclNode(
                  nodeCount++,
                  isInChangedFile,
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
          graph.addEdge(
              tdNode,
              mdNode,
              new SemanticEdge(edgeCount++, EdgeType.DEFINE_METHOD, tdNode, mdNode));

          // 6.1 new instance
          List<ObjectCreationExpr> objectCreationExprs = md.findAll(ObjectCreationExpr.class);
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
            initObjectEdges.put(mdNode, createObjectNames);
          }

          // 6.2 field access
          // TODO support self field access
          List<FieldAccessExpr> fieldAccessExprs = md.findAll(FieldAccessExpr.class);
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
            readFieldEdges.put(mdNode, readFieldNames);
          }
          if (writeFieldNames.size() > 0) {
            writeFieldEdges.put(mdNode, writeFieldNames);
          }
          // 6.3 method call
          List<MethodCallExpr> methodCallExprs = md.findAll(MethodCallExpr.class);
          List<String> methodCalledNames = new ArrayList<>();
          for (MethodCallExpr methodCallExpr : methodCallExprs) {
            // try to solve the symbol, create node&edge for internal types, and save the
            // external
            // relationships
            try {
              SymbolReference<ResolvedMethodDeclaration> ref =
                  javaParserFacade.solve((methodCallExpr));
              if (ref.isSolved()) {
                ResolvedMethodDeclaration resolvedMethodDeclaration = methodCallExpr.resolve();
                StringBuilder sb = new StringBuilder();
                sb.append(resolvedMethodDeclaration.getQualifiedName());
                sb.append("(");
                for (int i = 0; i < resolvedMethodDeclaration.getNumberOfParams(); i++) {
                  if (i != 0) {
                    sb.append(", ");
                  }
                  String qualifiedType = resolvedMethodDeclaration.getParam(i).describeType();
                  sb.append(qualifiedType.substring(qualifiedType.lastIndexOf(".") + 1));
                }
                sb.append(")");
                methodCalledNames.add(sb.toString());
              }
            } catch (UnsolvedSymbolException e) {
              continue;
            }
          }
          callMethodEdges.put(mdNode, methodCalledNames);
        }
      }
    }
  }

  private TypeDeclNode processTypeDeclaration(
      TypeDeclaration td, String packageName, int nodeCount, boolean isInChangedFile) {
    String displayName = td.getNameAsString();
    String qualifiedName = packageName + "." + displayName;
    // enum/interface/inner/local class
    NodeType nodeType = NodeType.CLASS; // default
    nodeType = td.isEnumDeclaration() ? NodeType.ENUM : nodeType;
    nodeType = td.isAnnotationDeclaration() ? NodeType.ANNOTATION : nodeType;
    if (td.isClassOrInterfaceDeclaration()) {
      ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) td;
      nodeType = cid.isInterface() ? NodeType.INTERFACE : nodeType;
      nodeType = cid.isInnerClass() ? NodeType.INNER_CLASS : nodeType;
      nodeType = cid.isLocalClassDeclaration() ? NodeType.LOCAL_CLASS : nodeType;
    }
    // TODO why the toString cannot be resolved?
    List<String> modifiers = new ArrayList<>();
    //    List<String> modifiers =
    //        td.getModifiers().stream().map(Modifier::toString).collect(Collectors.toList());
    String access = td.getAccessSpecifier().asString();
    //
    // Modifier.getAccessSpecifier(classOrInterfaceDeclaration.getModifiers()).asString();
    String originalSignature = getTypeOriginalSignature(td);

    TypeDeclNode tdNode =
        new TypeDeclNode(
            nodeCount++,
            isInChangedFile,
            nodeType,
            displayName,
            qualifiedName,
            originalSignature,
            td.getComment().map(Comment::getContent).orElse(""),
            access,
            modifiers,
            nodeType.asString(),
            displayName);
    return tdNode;
  }

  private String getFieldOriginalSignature(FieldDeclaration fieldDeclaration) {
    String source = fieldDeclaration.toString();
    if (fieldDeclaration.getComment().isPresent()) {
      source = source.replace(fieldDeclaration.getComment().get().getContent(), "");
    }
    return source
        .substring(0, (source.contains("=") ? source.indexOf("=") : source.indexOf(";")))
        .trim();
  }

  private String getTypeOriginalSignature(TypeDeclaration typeDeclaration) {
    // remove comment if there is in string representation
    String source = typeDeclaration.toString();
    if (typeDeclaration.getComment().isPresent()) {
      source = source.replace(typeDeclaration.getComment().get().getContent(), "");
    }
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
          // if the edge was added to the graph, returns true; if the edges already exists, returns
          // false
          boolean isSuccessful =
              semanticGraph.addEdge(
                  sourceNode,
                  targetNode,
                  new SemanticEdge(edgeCount++, edgeType, sourceNode, targetNode));
          if (!isSuccessful) {
            SemanticEdge edge = semanticGraph.getEdge(sourceNode, targetNode);
            edge.setWeight(edge.getWeight() + 1);
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
    return targetNodeOpt.orElse(null);
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
