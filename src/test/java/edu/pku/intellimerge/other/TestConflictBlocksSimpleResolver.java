package edu.pku.intellimerge.other;

import edu.pku.intellimerge.model.ConflictBlock;
import edu.pku.intellimerge.util.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConflictBlocksSimpleResolver {
  @Test
  public void testResolveConflictsDiff3() {
    String inputFilePath =
        Utils.getProjectRootDir()
            + "/src/test/resources/ConflictFiles/Diff3/SourceRoot.java";
    String outputFilePath =
        Utils.getProjectRootDir()
            + "/src/test/resources/ConflictFiles/Diff3/SourceRoot_Formatted.java";
    String codeWithConflicts = Utils.readFileToString(inputFilePath);
    List<ConflictBlock> conflictBlocks = Utils.extractConflictBlocksDiff3(inputFilePath, false);
    assertThat(conflictBlocks.size()).isEqualTo(2);
    String formattedCode = Utils.formatCodeWithConflicts(codeWithConflicts, true);
    Utils.writeContent(outputFilePath, formattedCode);
    conflictBlocks = Utils.extractConflictBlocksDiff3(outputFilePath, false);
    assertThat(conflictBlocks.size()).isEqualTo(2);
  }

  @Test
  public void testResolveConflictsDiff2() {
    String inputFilePath =
            Utils.getProjectRootDir()
                    + "/src/test/resources/ConflictFiles/Diff2/SourceRoot.java";
    String outputFilePath =
            Utils.getProjectRootDir()
                    + "/src/test/resources/ConflictFiles/Diff2/SourceRoot_Formatted.java";
    String codeWithConflicts = Utils.readFileToString(inputFilePath);
    List<ConflictBlock> conflictBlocks = Utils.extractConflictBlocksDiff2(inputFilePath, false);
    assertThat(conflictBlocks.size()).isEqualTo(4);
    String formattedCode = Utils.formatCodeWithConflicts(codeWithConflicts, false);
    Utils.writeContent(outputFilePath, formattedCode);
    conflictBlocks = Utils.extractConflictBlocksDiff2(outputFilePath, false);
    assertThat(conflictBlocks.size()).isEqualTo(4);
  }
}
