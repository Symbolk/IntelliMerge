package edu.pku.intellimerge.evaluation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import edu.pku.intellimerge.model.SourceFile;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Check whether the manually resolved&committed files are valid in syntax */
public class ManualResultsChecker {
  private static final Logger logger = LoggerFactory.getLogger(ManualResultsChecker.class);

  public static void main(String[] args) {
    checkIncorrectFiles();
    replaceIncorrectFiles();
  }

  private static void replaceIncorrectFiles() {
    String dataDir = "D:/github/ref_conflicts/";
    String repoName = "error-prone";
    String repoDir = "D:\\github\\repos\\" + repoName;
    String mergeCommit = "d51253011690def06db835d5ad605ca134c94d84";
    List<String> relativePaths = new ArrayList<>();
    relativePaths.add(
        "core/src/test/resources/com/google/errorprone/bugpatterns/ArrayToStringPositiveCases.java");

    // get the next commit of merge commit with my customized git command
    // "!bash -c 'git log --format=%H --reverse --ancestry-path ${1:-HEAD}..${2:\"$(git rev-parse
    // --abbrev-ref HEAD)\"} | head -1' -"
    String nextCommit = Utils.runSystemCommand(repoDir, "git", "child");
    for (String path : relativePaths) {
      String output = Utils.runSystemCommand(repoDir, "git", "show", nextCommit + ":" + path);
      // deleted at merge commit
      if (output.contains("fatal:")) {
        output = " ";
      }
      Utils.writeContent(
          dataDir + File.separator + repoName + Side.MANUAL.asString() + File.separator + path,
          output);
    }
  }

  private static void checkIncorrectFiles() {
    String inputPath = "D:\\github\\ref_conflicts\\";
    String repoName = "cassandra";
    String outputPath = "D:\\github\\incorrect_ground_truth\\";
    String summaryFilePath = outputPath + "summary.txt";

    inputPath = inputPath + repoName;
    try {
      File file = new File(inputPath);
      File[] files = file.listFiles();
      for (File f : files) {
        if (f.isDirectory()) {
          // traverse project folders
          String manualMergedDir =
              f.getAbsolutePath() + File.separator + Side.MANUAL.asString() + File.separator;
          ArrayList<SourceFile> temp = new ArrayList<>();
          ArrayList<SourceFile> manualMergedFiles =
              Utils.scanJavaSourceFiles(manualMergedDir, temp, manualMergedDir);
          for (SourceFile sourceFile : manualMergedFiles) {
            try {
              System.out.println(sourceFile.getAbsolutePath());
              File javaFile = new File(sourceFile.getAbsolutePath());
              CompilationUnit cu = JavaParser.parse(javaFile);
            } catch (Exception e) {
              // copy the file into the folder, and record the exception message
              File javaFile = new File(sourceFile.getAbsolutePath());
              if (javaFile.exists()) {
                String commitID = f.getName();
                //                    f.getAbsolutePath()
                //                        .trim()
                //                        .substring(
                //                            f.getAbsolutePath().lastIndexOf(File.separator) + 1,
                //                            f.getAbsolutePath().length());
                File dstFile =
                    new File(
                        outputPath
                            + repoName
                            + File.separator
                            + commitID
                            + File.separator
                            + sourceFile.getFileName());

                FileUtils.copyFile(javaFile, dstFile);
                String summary =
                    "-----------------"
                        + System.lineSeparator()
                        + repoName
                        + System.lineSeparator()
                        + commitID
                        + ":"
                        + sourceFile.getRelativePath()
                        + System.lineSeparator()
                        + e.toString();
                Utils.writeContent(summaryFilePath, summary, true);
              }
              e.printStackTrace();
              continue;
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
