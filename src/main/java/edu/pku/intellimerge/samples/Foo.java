package edu.pku.intellimerge.samples;

class Foo {
  Bar bar;
  Bar newBar = new Bar();
  int id, nid;
  private A a;

  public static void main(String[] args) {
    Bar bar = new Bar();
    bar.aMethod();
    bar.a = "b";
    String b = bar.a;
  }

  public void fun(String[] args) {
    a.getFull();
    A.foo(a);
    System.out.println();
  }
}
