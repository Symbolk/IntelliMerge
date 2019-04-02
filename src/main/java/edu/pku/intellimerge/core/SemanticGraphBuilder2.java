package edu.pku.intellimerge.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.node.*;
import edu.pku.intellimerge.util.Utils;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Build Semantic Graph for one merge scenario, with fuzzy matching instead of symbolsolving */
public class SemanticGraphBuilder2 implements Callable<Graph<SemanticNode, SemanticEdge>> {
  private static final Logger logger = LoggerFactory.getLogger(SemanticGraphBuilder2.class);
  private Graph<SemanticNode, SemanticEdge> graph;
  // incremental id, unique in one side's graph
  private int nodeCount;
  private int edgeCount;
  private boolean hasMultiModule;
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
  private Map<SemanticNode, List<FieldAccessExpr>> readFieldEdges = new HashMap<>();
  private Map<SemanticNode, List<FieldAccessExpr>> writeFieldEdges = new HashMap<>();
  private Map<SemanticNode, List<MethodCallExpr>> methodCallExprs = new HashMap<>();

  private MergeScenario mergeScenario;
  private Side side;
  private String targetDir; // directory of the target files to be analyzed

  public SemanticGraphBuilder2(
      MergeScenario mergeScenario, Side side, String targetDir, boolean hasMultiModule) {
    this.mergeScenario = mergeScenario;
    this.side = side;
    this.targetDir = targetDir;
    this.hasMultiModule = hasMultiModule;

    this.graph = initGraph();
    this.nodeCount = 0;
    this.edgeCount = 0;
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

  /**
   * Build the graph by parsing the collected files
   *
   * @return
   */
  @Override
  public Graph<SemanticNode, SemanticEdge> call() {

    // the folder path which contains collected files to build the graph upon
    String sideDir = targetDir + File.separator + side.asString() + File.separator;
    // just for sure: reinit the graph
    this.graph = initGraph();

    // parse all java files in the file
    // regular project: only one source folder
    File root = new File(sideDir);
    //    sourceRoot.getParserConfiguration().setSymbolResolver(symbolSolver);

    List<CompilationUnit> compilationUnits = new ArrayList<>();
    List<ParseResult<CompilationUnit>> parseResults = new ArrayList<>();
    if (hasMultiModule) {
      // multi-module project: separated source folder for sub-projects/modules
      ProjectRoot projectRoot = new ParserCollectionStrategy().collect(root.toPath());
      for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
        parseResults.addAll(sourceRoot.tryToParseParallelized());
      }
    } else {
      SourceRoot sourceRoot = new SourceRoot(root.toPath());
      parseResults = sourceRoot.tryToParseParallelized();
    }
    compilationUnits.addAll(
        parseResults.stream()
            .filter(ParseResult::isSuccessful)
            .map(r -> r.getResult().get())
            .collect(Collectors.toList()));

    /*
     * build the graph by analyzing every CU
     */
    logger.info("({}) CUs in {}", compilationUnits.size(), side);
    for (CompilationUnit cu : compilationUnits) {
      processCompilationUnit(cu);
    }

    // now vertices are fixed

    // build the recorded edges actually
    // TODO import can be any type, even inner type
    edgeCount = buildEdges(graph, edgeCount, importEdges, EdgeType.IMPORT, NodeType.CLASS);
    edgeCount = buildEdges(graph, edgeCount, extendEdges, EdgeType.EXTEND, NodeType.CLASS);
    edgeCount =
        buildEdges(graph, edgeCount, implementEdges, EdgeType.IMPLEMENT, NodeType.INTERFACE);
    edgeCount = buildEdges(graph, edgeCount, declObjectEdges, EdgeType.DECLARE, NodeType.CLASS);
    edgeCount = buildEdges(graph, edgeCount, initObjectEdges, EdgeType.INITIALIZE, NodeType.CLASS);

    //    edgeCount = buildEdges(graph, edgeCount, readFieldEdges, EdgeType.READ,
    // NodeType.FIELD);
    //    edgeCount = buildEdges(graph, edgeCount, writeFieldEdges, EdgeType.WRITE,
    // NodeType.FIELD);

    edgeCount = buildEdgesForMethodCall(edgeCount, methodCallExprs);

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
   * Process one CompilationUnit every time
   *
   * @param cu
   */
  private void processCompilationUnit(CompilationUnit cu) {
    String fileName = cu.getStorage().map(CompilationUnit.Storage::getFileName).orElse("");
    String absolutePath =
        Utils.formatPathSeparator(
            cu.getStorage().map(CompilationUnit.Storage::getPath).map(Path::toString).orElse(""));
    String relativePath =
        absolutePath.replace(
            Utils.formatPathSeparator(
                targetDir + File.separator + side.asString() + File.separator),
            "");

    // whether this file is modified: if yes, all nodes in it need to be merged (rough way)
    boolean isInChangedFile =
        mergeScenario == null ? true : mergeScenario.isInChangedFile(side, relativePath);

    CompilationUnitNode cuNode =
        new CompilationUnitNode(
            nodeCount++,
            isInChangedFile,
            NodeType.CU,
            fileName,
            fileName,
            fileName,
            cu.getComment().map(Comment::toString).orElse(""),
            fileName,
            relativePath,
            absolutePath,
            cu.getPackageDeclaration().map(PackageDeclaration::toString).orElse(""),
            cu.getImports().stream()
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
          graph.vertexSet().stream()
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
                packageDeclaration.getComment().map(Comment::toString).orElse(""),
                packageDeclaration.getAnnotations().stream()
                    .map(AnnotationExpr::toString)
                    .collect(Collectors.toList()),
                finalPackageName,
                Arrays.asList(finalPackageName.split(".")));
        graph.addVertex(cuNode);
        graph.addVertex(packageDeclNode);

        packageDeclNode.appendChild(cuNode);
        graph.addEdge(
            packageDeclNode,
            cuNode,
            new SemanticEdge(edgeCount++, EdgeType.CONTAIN, packageDeclNode, cuNode));
      } else {
        graph.addVertex(cuNode);
        packageDeclNodeOpt.get().appendChild(cuNode);
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

        cuNode.appendChild(tdNode);
        graph.addEdge(
            cuNode, tdNode, new SemanticEdge(edgeCount++, EdgeType.DEFINE, cuNode, tdNode));

        if (td.isClassOrInterfaceDeclaration()) {
          ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) td;
          // extend/implement
          if (cid.getExtendedTypes().size() > 0) {
            // single extends
            String extendedType = cid.getExtendedTypes().get(0).getNameAsString();
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
                    implementedType -> implementedTypes.add(implementedType.getNameAsString()));
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
    List<String> modifiers = new ArrayList<>();

    String access = td.getAccessSpecifier().asString();
    // why the map(Modifier::toString) cannot be resolved for td, but no problem with md and fd?
    modifiers =
        (List<String>)
            td.getModifiers().stream()
                .map(modifier -> modifier.toString())
                .collect(Collectors.toList());

    List<String> annotations =
        (List<String>)
            td.getAnnotations().stream().map(anno -> anno.toString()).collect(Collectors.toList());
    String originalSignature = getTypeOriginalSignature(td, modifiers.get(0));

    TypeDeclNode tdNode =
        new TypeDeclNode(
            nodeCount,
            isInChangedFile,
            nodeType,
            displayName,
            qualifiedName,
            originalSignature,
            td.getComment().map(Comment::toString).orElse(""),
            annotations,
            access,
            modifiers,
            nodeType.asString(),
            displayName);
    return tdNode;
  }

  /**
   * Process members (child nodes that are field, constructor or terminalNodeSimilarity) of type
   * declaration
   *
   * @param td
   * @param tdNode
   * @param packageName
   * @param isInChangedFile
   */
  private void processMemebers(
      TypeDeclaration td, TypeDeclNode tdNode, String packageName, boolean isInChangedFile) {
    String qualifiedTypeName = packageName + "." + td.getNameAsString();
    List<String> annotations, modifiers;
    String comment, access, displayName, qualifiedName, originalSignature, body;
    // if contains nested type declaration, iterate into it
    List<Node> orderedChildNodes = new ArrayList<>(td.getChildNodes());
    orderedChildNodes.sort(
        new Comparator<Node>() {
          @Override
          public int compare(Node o1, Node o2) {
            Integer startLine1 = o1.getRange().get().begin.line;
            Integer startLine2 = o2.getRange().get().begin.line;
            return startLine1.compareTo(startLine2);
          }
        });

    for (Node child : orderedChildNodes) {
      if (child instanceof TypeDeclaration) {
        TypeDeclaration childTD = (TypeDeclaration) child;
        if (childTD.isNestedType()) {
          // add edge from the parent td to the nested td
          TypeDeclNode childTDNode =
              processTypeDeclaration(childTD, qualifiedTypeName, nodeCount++, isInChangedFile);
          graph.addVertex(childTDNode);

          tdNode.appendChild(childTDNode);
          graph.addEdge(
              tdNode,
              childTDNode,
              new SemanticEdge(edgeCount++, EdgeType.DEFINE, tdNode, childTDNode));
          // process nested td members iteratively
          processMemebers(childTD, childTDNode, qualifiedTypeName, isInChangedFile);
        }
      } else {
        // for other members (constructor, field, terminalNodeSimilarity), create the node
        // add the edge from the parent td to the member
        if (child instanceof EnumConstantDeclaration) {
          EnumConstantDeclaration ecd = (EnumConstantDeclaration) child;
          comment = ecd.getComment().map(Comment::toString).orElse("");
          displayName = ecd.getNameAsString();
          qualifiedName = qualifiedTypeName + "." + displayName;
          body =
              ecd.getArguments().size() > 0
                  ? ecd.toString().replaceFirst(ecd.getNameAsString(), "")
                  : "";
          EnumConstantDeclNode ecdNode =
              new EnumConstantDeclNode(
                  nodeCount++,
                  isInChangedFile,
                  NodeType.ENUM_CONSTANT,
                  displayName,
                  qualifiedName,
                  displayName,
                  comment,
                  body,
                  ecd.getRange());
          graph.addVertex(ecdNode);

          // add edge between field and class
          tdNode.appendChild(ecdNode);
          graph.addEdge(
              tdNode, ecdNode, new SemanticEdge(edgeCount++, EdgeType.DEFINE, tdNode, ecdNode));
        }
        // 4. field
        if (child instanceof FieldDeclaration) {
          FieldDeclaration fd = (FieldDeclaration) child;
          // there can be more than one var declared in one field declaration, add one node for
          // each
          comment = fd.getComment().map(Comment::toString).orElse("");
          access = fd.getAccessSpecifier().asString();
          modifiers =
              fd.getModifiers().stream().map(Modifier::toString).collect(Collectors.toList());
          for (VariableDeclarator field : fd.getVariables()) {
            displayName = field.toString();
            qualifiedName = qualifiedTypeName + "." + displayName;
            originalSignature = field.getTypeAsString() + " " + field.getNameAsString();
            body =
                field.getInitializer().isPresent()
                    ? "=" + field.getInitializer().get().toString() + ";"
                    : ";";

            annotations =
                fd.getAnnotations().stream()
                    .map(AnnotationExpr::toString)
                    .collect(Collectors.toList());
            FieldDeclNode fdNode =
                new FieldDeclNode(
                    nodeCount++,
                    isInChangedFile,
                    NodeType.FIELD,
                    displayName,
                    qualifiedName,
                    originalSignature,
                    comment,
                    annotations,
                    access,
                    modifiers,
                    field.getTypeAsString(),
                    field.getNameAsString(),
                    body,
                    field.getRange());
            graph.addVertex(fdNode);

            // add edge between field and class
            tdNode.appendChild(fdNode);
            graph.addEdge(
                tdNode, fdNode, new SemanticEdge(edgeCount++, EdgeType.DEFINE, tdNode, fdNode));
            // 4.1 object creation in field declaration
            List<String> declClassNames = new ArrayList<>();
            List<String> initClassNames = new ArrayList<>();
            if (field.getType().isClassOrInterfaceType()) {
              ClassOrInterfaceType type = (ClassOrInterfaceType) field.getType();
              String classUsedInFieldName = type.getNameAsString();
              if (field.getInitializer().isPresent()) {
                initClassNames.add(classUsedInFieldName);
              } else {
                declClassNames.add(classUsedInFieldName);
              }
            }
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
          comment = cd.getComment().map(Comment::toString).orElse("");
          annotations =
              cd.getAnnotations().stream()
                  .map(AnnotationExpr::toString)
                  .collect(Collectors.toList());
          displayName = cd.getSignature().toString();
          modifiers =
              cd.getModifiers().stream().map(Modifier::toString).collect(Collectors.toList());

          qualifiedName = qualifiedTypeName + "." + displayName;
          ConstructorDeclNode cdNode =
              new ConstructorDeclNode(
                  nodeCount++,
                  isInChangedFile,
                  NodeType.CONSTRUCTOR,
                  displayName,
                  qualifiedName,
                  cd.getDeclarationAsString(false, true, true),
                  comment,
                  annotations,
                  modifiers,
                  displayName,
                  cd.getBody().toString(),
                  cd.getRange());
          graph.addVertex(cdNode);

          tdNode.appendChild(cdNode);
          graph.addEdge(
              tdNode, cdNode, new SemanticEdge(edgeCount++, EdgeType.DEFINE, tdNode, cdNode));

          processBodyContent(cd, cdNode);
        }
        // 6. terminalNodeSimilarity
        if (child instanceof MethodDeclaration) {
          MethodDeclaration md = (MethodDeclaration) child;
          if (md.getAnnotations().size() > 0) {
            if (md.isAnnotationPresent("Override")) {
              // search the terminalNodeSimilarity signature in its superclass or interface
            }
          }
          comment = md.getComment().map(Comment::toString).orElse("");
          displayName = md.getSignature().toString();
          qualifiedName = qualifiedTypeName + "." + displayName;

          access = md.getAccessSpecifier().asString();
          modifiers =
              md.getModifiers().stream().map(Modifier::toString).collect(Collectors.toList());
          annotations =
              md.getAnnotations().stream()
                  .map(AnnotationExpr::toString)
                  .collect(Collectors.toList());
          List<String> typeParameters =
              md.getTypeParameters().stream()
                  .map(TypeParameter::asString)
                  .collect(Collectors.toList());

          List<String> parameterList =
              md.getParameters().stream().map(Parameter::toString).collect(Collectors.toList());

          List<String> parameterTypes =
              md.getParameters().stream()
                  .map(Parameter::getType)
                  .map(Type::asString)
                  .collect(Collectors.toList());
          List<String> parameterNames =
              md.getParameters().stream()
                  .map(Parameter::getNameAsString)
                  .collect(Collectors.toList());
          List<String> throwsExceptions =
              md.getThrownExceptions().stream()
                  .map(ReferenceType::toString)
                  .collect(Collectors.toList());

          // md.getDeclarationAsString() does not include type parameters, so we need to insert type
          // [<type parameters>] [return type] [terminalNodeSimilarity name] [parameter type]
          if (typeParameters.size() > 0) {
            String typeParametersAsString =
                "<" + typeParameters.stream().collect(Collectors.joining(",")) + ">";
            originalSignature =
                md.getDeclarationAsString(false, true, true)
                    .trim()
                    .replaceFirst(
                        Pattern.quote(md.getTypeAsString()),
                        typeParametersAsString + " " + md.getTypeAsString());
          } else {
            originalSignature = md.getDeclarationAsString(false, true, true);
          }

          MethodDeclNode mdNode =
              new MethodDeclNode(
                  nodeCount++,
                  isInChangedFile,
                  NodeType.METHOD,
                  displayName,
                  qualifiedName,
                  originalSignature,
                  comment,
                  annotations,
                  access,
                  modifiers,
                  typeParameters,
                  md.getTypeAsString(),
                  displayName.substring(0, displayName.indexOf("(")),
                  parameterTypes,
                  parameterNames,
                  throwsExceptions,
                  md.getBody().map(BlockStmt::toString).orElse(";"),
                  md.getRange());
          mdNode.setParameterList(parameterList);
          graph.addVertex(mdNode);

          tdNode.appendChild(mdNode);
          graph.addEdge(
              tdNode, mdNode, new SemanticEdge(edgeCount++, EdgeType.DEFINE, tdNode, mdNode));

          processBodyContent(md, mdNode);
        }
      }

      // 7. initializer block
      if (child instanceof InitializerDeclaration) {
        InitializerDeclaration id = (InitializerDeclaration) child;
        // since initializer has no name, use the parent type declaration as its name]
        comment = id.getComment().map(Comment::toString).orElse("");
        displayName = td.getNameAsString();
        qualifiedName = packageName + "." + displayName + "{}";
        String signature = displayName + "." + (id.isStatic() ? "static{}" : "{}");
        InitializerDeclNode idNode =
            new InitializerDeclNode(
                nodeCount++,
                isInChangedFile,
                NodeType.INITIALIZER_BLOCK,
                displayName,
                qualifiedName,
                signature,
                comment,
                id.isStatic(),
                id.getBody().toString(),
                id.getRange());
        graph.addVertex(idNode);

        tdNode.appendChild(idNode);
        graph.addEdge(
            tdNode, idNode, new SemanticEdge(edgeCount++, EdgeType.DEFINE, tdNode, idNode));

        processBodyContent(id, idNode);
      }

      // 8, annotation member declaration
      if (child instanceof AnnotationMemberDeclaration) {
        AnnotationMemberDeclaration amd = (AnnotationMemberDeclaration) child;
        // since initializer has no name, use the parent type declaration as its name]
        comment = amd.getComment().map(Comment::toString).orElse("");
        displayName = amd.getNameAsString();
        qualifiedName = qualifiedTypeName + "." + displayName;
        String signature =
            amd.toString().contains("default")
                ? amd.toString().substring(0, amd.toString().indexOf("default")).trim()
                : amd.toString().trim();
        body =
            amd.toString().contains("default")
                ? amd.toString().substring(amd.toString().indexOf("default")).trim()
                : ";";
        AnnotationMemberNode idNode =
            new AnnotationMemberNode(
                nodeCount++,
                isInChangedFile,
                NodeType.ANNOTATION_MEMBER,
                displayName,
                qualifiedName,
                signature,
                comment,
                new ArrayList<>(), // no annotations
                new ArrayList<>(), // no modifiers
                body,
                amd.getRange());
        graph.addVertex(idNode);

        tdNode.appendChild(idNode);
        graph.addEdge(
            tdNode, idNode, new SemanticEdge(edgeCount++, EdgeType.DEFINE, tdNode, idNode));

        processBodyContent(amd, idNode);
      }
    }
  }

  /**
   * Process interactions with other nodes inside CallableDeclaration (i.e. terminalNodeSimilarity
   * or constructor) body
   *
   * @param cd
   * @param node
   */
  private void processBodyContent(Node cd, TerminalNode node) {
    // 1 new instance
    List<ObjectCreationExpr> objectCreationExprs = cd.findAll(ObjectCreationExpr.class);
    List<String> createObjectNames = new ArrayList<>();
    for (ObjectCreationExpr objectCreationExpr : objectCreationExprs) {
      String typeName = objectCreationExpr.getTypeAsString();
      createObjectNames.add(typeName);
    }
    if (createObjectNames.size() > 0) {
      initObjectEdges.put(node, createObjectNames);
    }

    // 2 field access
    // TODO support self field access
    List<FieldAccessExpr> fieldAccessExprs = cd.findAll(FieldAccessExpr.class);
    List<FieldAccessExpr> readFieldExprs = new ArrayList<>();
    List<FieldAccessExpr> writeFieldExprs = new ArrayList<>();
    for (FieldAccessExpr fieldAccessExpr : fieldAccessExprs) {
      // internal types
      // whether the field is assigned a value
      if (fieldAccessExpr.getParentNode().isPresent()) {
        Node parent = fieldAccessExpr.getParentNode().get();
        if (parent instanceof AssignExpr) {
          AssignExpr parentAssign = (AssignExpr) parent;
          if (parentAssign.getTarget().equals(fieldAccessExpr)) {
            writeFieldExprs.add(fieldAccessExpr);
          }
        }
      }
      readFieldExprs.add(fieldAccessExpr);
    }
    if (readFieldExprs.size() > 0) {
      readFieldEdges.put(node, readFieldExprs);
    }
    if (writeFieldExprs.size() > 0) {
      writeFieldEdges.put(node, writeFieldExprs);
    }
    // 3 terminalNodeSimilarity call
    List<MethodCallExpr> methodCallExprs = cd.findAll(MethodCallExpr.class);
    this.methodCallExprs.put(node, methodCallExprs);
  }

  /** Get signature of type in original code */
  private String getTypeOriginalSignature(TypeDeclaration typeDeclaration, String firstModifier) {
    // remove comment if there is in string representation
//        String source = removeComment(typeDeclaration.toString());
    String source = typeDeclaration.toString();
    if (typeDeclaration.getComment().isPresent()) {
      source = source.replace(Pattern.quote(typeDeclaration.getComment().get().toString()), "");
    }
    // if the comment bug in JavaParser is triggered, the comment is not completely removed
//    List<String> lines = Arrays.asList(source.split("\n"));
//    source = lines.stream().filter(line -> !line.startsWith("\\s\\*")).collect(Collectors.joining("\n"));
    /** @Target({FIELD, METHOD}) public @interface DataPoint { String[] value() default {}; } * */
    if (firstModifier.length() > 0 && source.indexOf(firstModifier) > 0) {
      source = source.substring(source.indexOf(firstModifier));
    }
    if(source.indexOf("{") > 0){
      return source.substring(0, source.indexOf("{")).trim();
    }else{
      return source.trim();
    }
  }

  /**
   * Get signature of field in original code
   *
   * @param fieldDeclaration
   * @return
   */
  private String getFieldOriginalSignature(FieldDeclaration fieldDeclaration) {
    String source = removeComment(fieldDeclaration.toString());
    //    if (fieldDeclaration.getComment().isPresent()) {
    //      source = source.replace(fieldDeclaration.getComment().get().getContent(), "");
    //    }
    return source
        .substring(0, (source.contains("=") ? source.indexOf("=") : source.indexOf(";")))
        .trim();
  }

  /**
   * Remove comment from a string
   * Unfortunately, Java's builtin regex support has problems with regexes containing repetitive alternative paths (that is, (A|B)*),
   * so this may lead to StackOverflowError
   * @param source
   * @return
   */
  private String removeComment(String source) {
    return source.replaceAll(
        "((['\"])(?:(?!\\2|\\\\).|\\\\.)*\\2)|\\/\\/[^\\n]*|\\/\\*(?:[^*]|\\*(?!\\/))*\\*\\/", "");
    //    return source.replaceAll("[^:]//.*|/\\\\*((?!=*/)(?s:.))+\\\\*", "");
    //    return source.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
  }

  /**
   * Fuzzy match methods, by terminalNodeSimilarity name and argument numbers (to be refined)
   *
   * @param edgeCount
   * @param methodCallExprs
   * @return
   */
  private int buildEdgesForMethodCall(
      int edgeCount, Map<SemanticNode, List<MethodCallExpr>> methodCallExprs) {
    // for every terminalNodeSimilarity call, find its declaration by terminalNodeSimilarity name
    // and paramater num
    for (Map.Entry<SemanticNode, List<MethodCallExpr>> entry : methodCallExprs.entrySet()) {
      SemanticNode caller = entry.getKey();
      List<MethodCallExpr> exprs = entry.getValue();
      for (MethodCallExpr expr : exprs) {
        boolean edgeBuilt = false;
        String methodName = expr.getNameAsString();
        int argNum = expr.getArguments().size();
        List<SemanticNode> candidates =
            graph.vertexSet().stream()
                .filter(
                    node -> {
                      if (node.getNodeType().equals(NodeType.METHOD)) {
                        MethodDeclNode method = (MethodDeclNode) node;
                        return method.getMethodName().equals(methodName)
                            && method.getParameterNames().size() == argNum;
                      }
                      return false;
                    })
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
          // fail to find the target node and build the edge, consider it as external
          List<String> argumentNames =
              expr.getArguments().stream().map(Expression::toString).collect(Collectors.toList());
          MethodDeclNode externalMethod =
              new MethodDeclNode(
                  nodeCount++,
                  false,
                  NodeType.METHOD,
                  expr.getNameAsString(),
                  expr.getNameAsString(),
                  expr.toString(),
                  methodName,
                  argumentNames,
                  expr.getRange());
          graph.addVertex(externalMethod);
          createEdge(edgeCount++, caller, externalMethod, EdgeType.CALL, false);
        } else {
          // TODO if fuzzy matching gets multiple results, just select the first one for now
          createEdge(
              edgeCount++,
              caller,
              candidates.get(0),
              EdgeType.CALL,
              candidates.get(0).isInternal());
        }
      }
    }
    return edgeCount;
  }

  /**
   * Create an edge in the graph, if it already exists, increase the weight by one
   *
   * @param source
   * @param target
   * @param edgeId
   * @param edgeType
   * @return
   */
  private boolean createEdge(
      int edgeId, SemanticNode source, SemanticNode target, EdgeType edgeType, boolean isInternal) {
    boolean isSuccessful =
        graph.addEdge(
            source, target, new SemanticEdge(edgeId, edgeType, source, target, isInternal));
    if (!isSuccessful) {
      SemanticEdge edge = graph.getEdge(source, target);
      if (edge != null) {
        edge.setWeight(edge.getWeight() + 1);
        isSuccessful = true;
      }
    }
    return isSuccessful;
  }
  /**
   * Add edges according to recorded temp mapping
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
        SemanticNode targetNode = null;
        if (targetNodeType.equals(NodeType.FIELD)) {
          targetNode = getTargetNodeForField(vertexSet, targeNodeName, targetNodeType);
        } else if (targetNodeType.equals(NodeType.CLASS)) {
          targetNode = getTargetNodeForType(vertexSet, targeNodeName, targetNodeType);
        } else {
          targetNode = getTargetNode(vertexSet, targeNodeName, targetNodeType);
        }
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
            if (edge != null) {
              edge.setWeight(edge.getWeight() + 1);
            }
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
        vertexSet.stream()
            .filter(
                node ->
                    node.getNodeType().equals(targetNodeType)
                        && node.getQualifiedName().equals(targetQualifiedName))
            .findAny();
    return targetNodeOpt.orElse(null);
  }

  /**
   * Get the target node for type decl or init by fuzzy matching
   *
   * @param vertexSet
   * @param displayName
   * @param targetNodeType
   * @return
   */
  public SemanticNode getTargetNodeForType(
      Set<SemanticNode> vertexSet, String displayName, NodeType targetNodeType) {
    Optional<SemanticNode> targetNodeOpt =
        vertexSet.stream()
            .filter(
                node ->
                    node.getNodeType().equals(targetNodeType)
                        && node.getQualifiedName().equals(displayName))
            .findAny();
    return targetNodeOpt.orElse(null);
  }

  /**
   * Get the target node for field access by fuzzy matching
   *
   * @param vertexSet
   * @param fieldAccessString
   * @param targetNodeType
   * @return
   */
  public SemanticNode getTargetNodeForField(
      Set<SemanticNode> vertexSet, String fieldAccessString, NodeType targetNodeType) {
    // for field, match by field name
    if (fieldAccessString.contains(".")) {
      fieldAccessString =
          fieldAccessString.substring(
              fieldAccessString.lastIndexOf("."), fieldAccessString.length());
    }
    String displayName = fieldAccessString;
    // for terminalNodeSimilarity, match by terminalNodeSimilarity name and paramater num
    Optional<SemanticNode> targetNodeOpt = Optional.empty();
    if (targetNodeType.equals(NodeType.FIELD)) {

      targetNodeOpt =
          vertexSet.stream()
              .filter(
                  node ->
                      node.getNodeType().equals(targetNodeType)
                          && node.getDisplayName().equals(displayName))
              .findAny();
    }

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
}
