package edu.pku.intellimerge.evaluation;

import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Class responsbile to collect and debug with a single file */
public class SingleFileTester {
  public static void main(String[] args) {
    PropertyConfigurator.configure("log4j.properties");

    String REPO_NAME = "javaparser";
    String mergeCommit = "90415702d623180f30e52e3d9426d3ef10b98276";
    String sourceDir = "D:\\github\\merges\\" + REPO_NAME + File.separator + mergeCommit;

    String targetDir = "D:\\github\\test2";
    List<String> relativePaths = new ArrayList<>();

    relativePaths.add(
        "javaparser-core/src/main/java/com/github/javaparser/ast/body/MethodDeclaration.java");
    Utils.copyAllVersions(sourceDir, relativePaths, targetDir);
  }
}
