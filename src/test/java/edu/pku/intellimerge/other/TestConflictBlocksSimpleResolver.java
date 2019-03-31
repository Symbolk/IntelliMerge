package edu.pku.intellimerge.other;

import edu.pku.intellimerge.model.ConflictBlock;
import edu.pku.intellimerge.util.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConflictBlocksSimpleResolver {
  @Test
  public void testResolveConflictsDiff3() {
    String path =
        Utils.getProjectRootDir()
            + "/src/test/resources/Rename/RenameField/gitMerged/SourceRoot.java";
    String codeWithConflicts = Utils.readFileToString(path);
    List<ConflictBlock> conflictBlocks = Utils.extractConflictBlocksDiff3(path, false);
    assertThat(conflictBlocks.size()).isEqualTo(2);
    Utils.resolveConflictsSimply(codeWithConflicts, true);
  }
}
