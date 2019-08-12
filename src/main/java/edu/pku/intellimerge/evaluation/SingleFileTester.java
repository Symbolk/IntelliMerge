package edu.pku.intellimerge.evaluation;

import br.ufpe.cin.app.JFSTMerge;
import edu.pku.intellimerge.client.IntelliMerge;
import edu.pku.intellimerge.model.constant.Side;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Class responsbile to collect and debug with a single file */
public class SingleFileTester {
  private static final String REPO_NAME = "junit4";
  private static final String REPO_DIR = "D:\\github\\repos\\" + REPO_NAME;
  private static final String SRC_DIR =
      "/javaparser-core/src/main/java/"; // java project source folder
  //  private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";

  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("log4j.properties");

//    String mergeCommit = "f4682ce2558cdca60d12fbef39e9ca0370eba592";
//    String sourceDir = "D:\\github\\ref_conflicts\\" + REPO_NAME + File.separator + mergeCommit;
//    String intelliMergedDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
//
//    String jfstMergedDir = sourceDir + File.separator + Side.JFST.asString() + File.separator;
//    String manualMergedDir =
//        sourceDir + File.separator + Side.MANUAL.asString() + "_Formatted" + File.separator;
//
//    String targetDir = "D:\\github\\test";
//    List<String> relativePaths = new ArrayList<>();

    // Test Copying
//    relativePaths.add("src/test/java/org/junit/tests/running/classes/TestClassTest.java");
    //                Utils.copyAllVersions(sourceDir, relativePaths, targetDir);
    //    Utils.removeAllComments(intelliMergedDir);
    //    Utils.removeAllComments(manualMergedDir);
    //    Utils.removeAllComments(jfstMergedDir);

    // Test Merging
    String dirToMerge = "D:\\github\\test";
    mergeWithIntelli(dirToMerge);
    mergeWithJFST(dirToMerge);
  }

  private static void mergeWithIntelli(String sourceDir) throws Exception {
    String mergeResultDir = sourceDir + File.separator + Side.INTELLI.asString() + File.separator;
    IntelliMerge intelliMerge = new IntelliMerge();
    List<String> directories = new ArrayList<>();
    directories.add(sourceDir + File.separator + Side.OURS.asString());
    directories.add(sourceDir + File.separator + Side.BASE.asString());
    directories.add(sourceDir + File.separator + Side.THEIRS.asString());
    List<Long> runtimes = intelliMerge.mergeDirectories(directories, mergeResultDir, false);
    //    Utils.removeAllComments(mergeResultDir);
    //    Utils.formatAllJavaFiles(manualMergedDir);
  }

  private static void mergeWithJFST(String sourceDir) {
    String jfstMergedDir = sourceDir + File.separator + Side.JFST.asString() + File.separator;
    JFSTMerge jfstMerge = new JFSTMerge();
    jfstMerge.mergeDirectories(
        sourceDir + File.separator + Side.OURS.asString(),
        sourceDir + File.separator + Side.BASE.asString(),
        sourceDir + File.separator + Side.THEIRS.asString(),
        jfstMergedDir);
  }
}
