package edu.pku.intellimerge.model;

public class SourceFile {
  private String fileName;
  private String qualifiedName;
  private String relativePath;
  private String absolutePath;
  public boolean isCopied;
  public boolean isParsed; // maybe unnecessary, since sourceroot only parse every file once

  public SourceFile(
      String fileName, String qualifiedName, String relativePath, String absolutePath) {
    this.fileName = fileName;
    this.qualifiedName = qualifiedName;
    this.relativePath = relativePath;
    this.absolutePath = absolutePath;
    this.isCopied = false;
    this.isParsed = false;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  public void setQualifiedName(String qualifiedName) {
    this.qualifiedName = qualifiedName;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }

  public void setAbsolutePath(String absolutePath) {
    this.absolutePath = absolutePath;
  }

}
