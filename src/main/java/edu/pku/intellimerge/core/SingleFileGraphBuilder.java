package edu.pku.intellimerge.core;

import com.github.javaparser.JavaParser;
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
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/** Build Semantic Graph for one single java file (Mainly for testing) */
public class SingleFileGraphBuilder {
  private static final Logger logger = LoggerFactory.getLogger(SingleFileGraphBuilder.class);
  private String folderPath;
  private String fileRelativePath;
  private Graph<SemanticNode, SemanticEdge> graph;
  // incremental id, unique in one side's graph
  private int nodeCount;
  private int edgeCount;
  private JavaSymbolSolver symbolSolver;
  private JavaParserFacade javaParserFacade;
  /*
   * a series of temp containers to keep relationships between node and symbol
   * if the symbol is internal: draw the edge in graph;
   * else:
   */
  private Map<SemanticNode, List<String>> importEdges = new HashMap<>();
  private Map<SemanticNode, List<String>> extendEdges = new HashMap<>();
  private Map<SemanticNode, List<String>> implementEdges = new HashMap<>();
  private Map<SemanticNode, List<String>> declObjectEdges = new HashMap<>();
  private Map<SemanticNode, List<String>> initObjectEdges = new HashMap<>();
  private Map<SemanticNode, List<String>> readFieldEdges = new HashMap<>();
  private Map<SemanticNode, List<String>> writeFieldEdges = new HashMap<>();
  private Map<SemanticNode, List<String>> callMethodEdges = new HashMap<>();

  public SingleFileGraphBuilder(String folderPath, String fileRelativePath) {
    this.folderPath = folderPath;
    this.fileRelativePath = fileRelativePath;
    this.graph = initGraph();
    this.nodeCount = 0;
    this.edgeCount = 0;
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

    Graph<SemanticNode, SemanticEdge> graph = build(side);
    if (graph == null) {
      logger.error(side.toString() + " graph is null!");
    }
    return graph;
  }

  private Graph<SemanticNode, SemanticEdge> build(Side side) throws IOException {

    // set up the typsolver
    TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    TypeSolver javaParserTypeSolver =
        new JavaParserTypeSolver(new File(folderPath + File.separator + side.asString()));
    reflectionTypeSolver.setParent(reflectionTypeSolver);
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(reflectionTypeSolver);
    combinedTypeSolver.add(javaParserTypeSolver);
    this.symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
    JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);
    this.javaParserFacade = JavaParserFacade.get(combinedTypeSolver);

    // just for sure: reinit the graph
    this.graph = initGraph();

    /*
     * build the graph by analyzing every CU
     */
    String fileAbsPath =
        folderPath + File.separator + side.asString() + File.separator + fileRelativePath;
    CompilationUnit cu = JavaParser.parse(new File(fileAbsPath));
    String fileName = cu.getStorage().map(CompilationUnit.Storage::getFileName).orElse("");
    String absolutePath =
        cu.getStorage().map(CompilationUnit.Storage::getPath).map(Path::toString).orElse("");
    String relativePath = fileRelativePath;

    // whether this file is modified: if yes, all nodes in it need to be merged (rough way)
    boolean isInChangedFile = true;

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
      TypeDeclNode tdNode = processTypeDeclaration(td, packageName, nodeCount++, isInChangedFile);
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

  /**
   * Process the type declaration itself
   *
   * @param td
   * @param packageName
   * @param nodeCount
   * @param isInChangedFile
   * @return
   */
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
            nodeCount,
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
  /**
   * Process members (child nodes that are field, constructor or method) of type declaration
   *
   * @param td
   * @param tdNode
   * @param packageName
   * @param isInChangedFile
   */
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
              processTypeDeclaration(childTD, packageName, nodeCount++, isInChangedFile);
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

          processMethodOrConstructorBody(cd, cdNode);
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

          processMethodOrConstructorBody(md, mdNode);
        }
      }
    }
  }

  /**
   * Process interactions with other nodes inside CallableDeclaration (i.e. method or constructor)
   * body
   *
   * @param cd
   * @param node
   */
  private void processMethodOrConstructorBody(CallableDeclaration cd, TerminalNode node) {
    String displayName = "";
    String qualifiedName = "";
    // 1 new instance
    List<ObjectCreationExpr> objectCreationExprs = cd.findAll(ObjectCreationExpr.class);
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
        String typeQualifiedName = objectCreationExpr.resolve().declaringType().getQualifiedName();
        createObjectNames.add(typeQualifiedName);
      } catch (UnsolvedSymbolException e) {
        continue;
      }
    }
    if (createObjectNames.size() > 0) {
      initObjectEdges.put(node, createObjectNames);
    }

    // 2 field access
    // TODO support self field access
    List<FieldAccessExpr> fieldAccessExprs = cd.findAll(FieldAccessExpr.class);
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
      readFieldEdges.put(node, readFieldNames);
    }
    if (writeFieldNames.size() > 0) {
      writeFieldEdges.put(node, writeFieldNames);
    }
    // 3 method call
    List<MethodCallExpr> methodCallExprs = cd.findAll(MethodCallExpr.class);
    List<String> methodCalledNames = new ArrayList<>();
    for (MethodCallExpr methodCallExpr : methodCallExprs) {
      // try to solve the symbol, create node&edge for internal types, and save the
      // external
      // relationships
      try {
        SymbolReference<ResolvedMethodDeclaration> ref = javaParserFacade.solve((methodCallExpr));
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
    callMethodEdges.put(node, methodCalledNames);
  }

  /**
   * Get signature of field in original code
   *
   * @param fieldDeclaration
   * @return
   */
  private String getFieldOriginalSignature(FieldDeclaration fieldDeclaration) {
    String source = fieldDeclaration.toString();
    if (fieldDeclaration.getComment().isPresent()) {
      source = source.replace(fieldDeclaration.getComment().get().getContent(), "");
    }
    return source
        .substring(0, (source.contains("=") ? source.indexOf("=") : source.indexOf(";")))
        .trim();
  }

  /** Get signature of type in original code */
  private String getTypeOriginalSignature(TypeDeclaration typeDeclaration) {
    // remove comment if there is in string representation
    String source = typeDeclaration.toString();
    if (typeDeclaration.getComment().isPresent()) {
      source = source.replace(typeDeclaration.getComment().get().getContent(), "");
    }
    return source.substring(0, source.indexOf("{")).trim();
  }

  /**
   * Add edges according to recorded temp mappings
   *
   * @param edgeCount edge id
   * @param edges recorded temp mapping from source node to qualified name of target node
   * @param edgeType edge type
   * @param targetNodeType target node type
   * @return
   */
  private int buildEdges(
      Graph<SemanticNode, SemanticEdge> semanticGraph,
      int edgeCount,
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
}
