package edu.pku.intellimerge.io;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

public class FormatPreservingPrinter {
  public static void main(String[] args) {
    // a quick example showing the usage
    try {
      String code = "// Hey, this is a comment\n\n\n// Another one\n\nclass A { }";
      CompilationUnit cu = JavaParser.parse(code);
      LexicalPreservingPrinter.setup(cu);
      System.out.println(LexicalPreservingPrinter.print(cu));

      System.out.println("----------------");

      ClassOrInterfaceDeclaration myClass = cu.getClassByName("A").get();
      myClass.setName("MyNewClassName");
      System.out.println(LexicalPreservingPrinter.print(cu));

      System.out.println("----------------");

      myClass = cu.getClassByName("MyNewClassName").get();
      myClass.setName("MyNewClassName");
      myClass.addModifier(Modifier.PUBLIC);
      System.out.println(LexicalPreservingPrinter.print(cu));

      System.out.println("----------------");

      myClass = cu.getClassByName("MyNewClassName").get();
      myClass.setName("MyNewClassName");
      myClass.addModifier(Modifier.PUBLIC);
      cu.setPackageDeclaration("org.javaparser.samples");
      System.out.println(LexicalPreservingPrinter.print(cu));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
