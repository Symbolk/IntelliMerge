package edu.pku.intellimerge.core.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.TreeVisitor;

import java.io.FileInputStream;
import java.io.IOException;

public class OrderedTreeVisitor extends TreeVisitor {
  private static final String FILE_PATH =
      "D:\\github\\merges\\javaparser\\d9c990a94c725b8d112ba02897988b7400100ce3\\ours\\javaparser-core\\src\\main\\java\\com\\github\\javaparser\\utils\\SourceRoot.java";

  public static void main(String[] args) {
    try {
      CompilationUnit cu = JavaParser.parse(new FileInputStream(FILE_PATH));
      OrderedTreeVisitor visitor = new OrderedTreeVisitor();
      visitor.visitPreOrder(cu);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void process(Node node) {
    if (node instanceof TypeDeclaration) {
      TypeDeclaration typeDeclaration = (TypeDeclaration) node;
      // if not type declaration, create the terminal node
      if (typeDeclaration.isTopLevelType()) {
        // child of the cu
        if (typeDeclaration.isEnumDeclaration()) {}

        if (typeDeclaration.isAnnotationDeclaration()) {}

        if (typeDeclaration.isClassOrInterfaceDeclaration()) {}

        System.out.println(typeDeclaration.getName());
      }
      if (typeDeclaration.isNestedType()) {
        // child of another type
        TypeDeclaration parentType = (TypeDeclaration) typeDeclaration.getParentNode().get();
        System.out.println(parentType.getName().toString() + " ::: " + typeDeclaration.getName());
      }
    } else {
      if (node.getParentNode().isPresent()) {
        if (node.getParentNode().get() instanceof TypeDeclaration) {
          TypeDeclaration parentType = (TypeDeclaration) node.getParentNode().get();
          if (node instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) node;
            System.out.println(
                parentType.getName() + "-" + constructorDeclaration.getNameAsString());
          }
          if (node instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
            System.out.println(parentType.getName() + "-" + fieldDeclaration.getVariables());
          }
          if (node instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) node;
            System.out.println(parentType.getName() + "-" + methodDeclaration.getNameAsString());
          }
        }
      }
    }
  }
}
