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

/** Check whether the manually resolved&committed files are valid in syntax */
public class ManualResultsChecker {
  private static final Logger logger = LoggerFactory.getLogger(ManualResultsChecker.class);

  public static void main(String[] args) {
    String baseDir = "D:\\github\\ref_conflicts\\";
    String repoName = "cassandra";
    String collectedFilePath = "D:\\github\\incorrect_ground_truth\\";
    String summaryFilePath = collectedFilePath + "summary.txt";

    baseDir = baseDir + repoName;
    try {

      File file = new File(baseDir);
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
                String commitID =
                    f.getAbsolutePath()
                        .trim()
                        .substring(
                            f.getAbsolutePath().lastIndexOf(File.separator) + 1,
                            f.getAbsolutePath().length());
                File dstFile =
                    new File(
                        collectedFilePath
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

    // try to parse all merged files

    // collect files with syntax errors after merged3

  }
}
