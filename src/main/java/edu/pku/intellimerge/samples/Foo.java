package edu.pku.intellimerge.samples;

class Foo {
    Bar bar;
    int id;
    private A a;

    public static void main(String[] args) {
        Bar bar = new Bar();
        bar.aMethod();
        String b = bar.a;
    }

    public void fun(String[] args) {
        a.getFull();
        A.foo();
        System.out.println();
    }
}