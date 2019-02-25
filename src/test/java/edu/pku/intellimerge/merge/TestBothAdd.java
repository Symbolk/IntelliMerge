package edu.pku.intellimerge.merge;

import edu.pku.intellimerge.match.Util;
import edu.pku.intellimerge.model.ConflictBlock;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.FilesManager;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBothAdd {
  @BeforeAll
  public static void setUpBeforeAll() {
    PropertyConfigurator.configure("log4j.properties");
  }

  @Test
  public void testBothAdd() {
    String targetDir =
        FilesManager.getProjectRootDir() + "/src/test/resources/Extract/ExtractMethod/";
    String resultDir = targetDir  + Side.INTELLI.asString() + File.separator;

    List<String> mergedFilePaths = Util.mergeGraphsThreeway(targetDir, resultDir);
    for (String path : mergedFilePaths) {
      String code = FilesManager.readFileContent(new File(path));
      List<ConflictBlock> conflictBlocks = FilesManager.extractConflictBlocks(code);
      assertThat(conflictBlocks.size()).isEqualTo(1);
    }
  }
}
