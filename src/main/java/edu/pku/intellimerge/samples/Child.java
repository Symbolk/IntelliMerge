package edu.pku.intellimerge.samples;

public class Child extends Bar implements Extractor {
  private String name;

  @Override
  public void print() {
    System.out.println(name);
  }
}
