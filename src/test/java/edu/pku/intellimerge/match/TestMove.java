package edu.pku.intellimerge.match;

import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.model.constant.MatchingType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.MatchingEdge;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.util.FilesManager;
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
  public void testMoveInsideFile() throws Exception {
    String targetDir =
        FilesManager.getProjectRootDir() + "/src/test/resources/Move/MoveMethod/InsideFile";
    String resultDir = targetDir + File.separator + Side.INTELLI.asString();
    ThreewayGraphMerger merger = Util.matchGraphs(targetDir, resultDir);
    Set<MatchingEdge> refsOurs =
        merger
            .b2oMatchings
            .biPartite
            .edgeSet()
            .stream()
            .filter(edge -> edge.getMatchingType().equals(MatchingType.MATCHED_METHOD))
            .collect(Collectors.toSet());
    Set<MatchingEdge> refsTheirs =
        merger
            .b2oMatchings
            .biPartite
            .edgeSet()
            .stream()
            .filter(edge -> edge.getMatchingType().equals(MatchingType.MATCHED_METHOD))
            .collect(Collectors.toSet());

    assertThat(refsOurs.size()).isOne();
    assertThat(refsTheirs.size()).isOne();
  }

  @Test
  public void testMoveAcrossFiles() throws Exception {
    String targetDir =
        FilesManager.getProjectRootDir() + "/src/test/resources/Move/MoveMethod/AcrossFiles";
    TwowayMatching matching = Util.matchTwoGraphs(targetDir, Side.BASE, Side.OURS);
    Set<MatchingEdge> refsOurs =
        matching
            .biPartite
            .edgeSet()
            .stream()
            .filter(edge -> edge.getMatchingType().equals(MatchingType.MATCHED_METHOD))
            .collect(Collectors.toSet());
    assertThat(refsOurs.size()).isEqualTo(48);
  }
}
