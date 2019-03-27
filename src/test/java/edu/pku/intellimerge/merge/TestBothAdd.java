package edu.pku.intellimerge.merge;

import edu.pku.intellimerge.match.Util;
import edu.pku.intellimerge.model.ConflictBlock;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;
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
        Utils.getProjectRootDir() + "/src/test/resources/Extract/ExtractMethod/";
    String resultDir = targetDir + Side.INTELLI.asString() + File.separator;

    List<String> mergedFilePaths = Util.mergeGraphsThreeway(targetDir, resultDir);
    for (String path : mergedFilePaths) {
      List<ConflictBlock> conflictBlocks = Utils.extractConflictBlocksDiff3(path, false);
      assertThat(conflictBlocks.size()).isEqualTo(0);
    }
  }
}
