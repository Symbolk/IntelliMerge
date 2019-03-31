package edu.pku.intellimerge.match;

import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.model.constant.RefactoringType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.Refactoring;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMove {

  @BeforeAll
  public static void setUpBeforeAll() {
    PropertyConfigurator.configure("log4j.properties");
  }

  @Test
  public void testMoveMethodInsideFile() throws Exception {
    String targetDir =
        Utils.getProjectRootDir() + "/src/test/resources/Move/MoveMethod/InsideFile/";
    String resultDir = targetDir + Side.INTELLI.asString() + File.separator;
    ThreewayGraphMerger merger = Util.matchGraphsThreeway(targetDir, resultDir);
    Set<Refactoring> refsOurs =
        merger.b2oMatching.refactorings.stream()
            .filter(edge -> edge.getRefactoringType().equals(RefactoringType.CHANGE_METHOD_SIGNATURE))
            .collect(Collectors.toSet());
    Set<Refactoring> refsTheirs =
        merger.b2oMatching.refactorings.stream()
            .filter(edge -> edge.getRefactoringType().equals(RefactoringType.CHANGE_METHOD_SIGNATURE))
            .collect(Collectors.toSet());

    assertThat(refsOurs.size()).isOne();
    assertThat(refsTheirs.size()).isOne();
  }

  @Test
  public void testMoveMethodAcrossFiles() throws Exception {
    String targetDir =
        Utils.getProjectRootDir() + "/src/test/resources/Move/MoveMethod/AcrossFiles/";
    TwowayMatching matching = Util.matchGraphsTwoway(targetDir, Side.BASE, Side.OURS);
    Set<Refactoring> refsOurs =
        matching.refactorings.stream()
            .filter(edge -> edge.getRefactoringType().equals(RefactoringType.CHANGE_METHOD_SIGNATURE))
            .collect(Collectors.toSet());
    assertThat(refsOurs.size()).isEqualTo(48);
  }

  @Test
  public void testMoveFieldInsideFile() throws Exception {
    String targetDir = Utils.getProjectRootDir() + "/src/test/resources/Move/MoveField/InsideFile/";
    TwowayMatching matching = Util.matchGraphsTwoway(targetDir, Side.BASE, Side.OURS);
    Set<Refactoring> refsOurs =
        matching.refactorings.stream()
            .filter(edge -> edge.getRefactoringType().equals(RefactoringType.CHANGE_FIELD_SIGNATURE))
            .collect(Collectors.toSet());
    assertThat(refsOurs.size()).isEqualTo(4);
  }
}
