package edu.pku.intellimerge.other;

import edu.pku.intellimerge.model.ConflictBlock;
import edu.pku.intellimerge.util.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConflictBlockExtraction {
  @Test
  public void testExtractConflictBlocksDiff3() {
    String path =
        Utils.getProjectRootDir()
            + "/src/test/resources/Rename/RenameMethod/BothSides/gitMerged/SourceRoot.java";

    List<ConflictBlock> conflictBlocks = Utils.extractConflictBlocksDiff3(path, false);
    assertThat(conflictBlocks.size()).isOne();
    assertThat(conflictBlocks.get(0).getBase())
        .contains("Map<Path, ParseResult<CompilationUnit>> tryToParse(JavaParser parser)");
    assertThat(conflictBlocks.get(0).getRight())
        .contains("Map<Path, ParseResult<CompilationUnit>> tryToParse(JavaParser parser)");
    assertThat(conflictBlocks.get(0).getStartLine()).isEqualTo(60);
  }

  @Test
  public void testExtractConflictBlocksDiff2() {
    String path =
        Utils.getProjectRootDir()
            + "/src/test/resources/Extract/ExtractMethod/gitMerged/SourceRoot.java";

    List<ConflictBlock> conflictBlocks = Utils.extractConflictBlocksDiff2(path, false);
    assertThat(conflictBlocks.size()).isEqualTo(4);
    assertThat(conflictBlocks.get(0).getBase().length()).isZero();
    assertThat(conflictBlocks.get(1).getRight())
        .contains("private JavaParser javaParser = new JavaParser();");
    assertThat(conflictBlocks.get(2).getStartLine()).isEqualTo(127);
    assertThat(conflictBlocks.get(3).getEndLine()).isEqualTo(229);
  }
}
