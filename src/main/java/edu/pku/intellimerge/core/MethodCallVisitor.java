package edu.pku.intellimerge.core;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class MethodCallVisitor extends VoidVisitorAdapter<Void> {
  @Override
  public void visit(MethodCallExpr n, Void arg) {
    // Found a method call
    System.out.println(n.getScope() + " - " + n.getName());
    // Don't forget to call super, it may find more method calls inside the arguments of this method
    // call, for example.
    super.visit(n, arg);
  }
}
