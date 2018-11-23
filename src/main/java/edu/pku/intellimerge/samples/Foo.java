package edu.pku.intellimerge.samples;
import edu.pku.intellimerge.samples.Bar;

class Foo {
    Bar bar;
    int id;
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