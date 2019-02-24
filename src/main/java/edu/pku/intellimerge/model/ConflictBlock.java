package edu.pku.intellimerge.model;

public class ConflictBlock {
    private String left;
    private String base;
    private String right;

    // line ranges in the merged code
    private int startLine;
    private int endLine;

    public ConflictBlock(String left, String base, String right, int startLine, int endLine) {
        this.left = left;
        this.base = base;
        this.right = right;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getLeft() {
        return left;
    }

    public String getBase() {
        return base;
    }

    public String getRight() {
        return right;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }
}
