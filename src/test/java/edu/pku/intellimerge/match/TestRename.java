package edu.pku.intellimerge.match;

import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.model.constant.MatchingType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.MatchingEdge;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRename {

  @BeforeAll
  public static void setUpBeforeAll() {
    PropertyConfigurator.configure("log4j.properties");
  }

  @Test
  public void testRenameMethodOneSide() throws Exception {
    String targetDir =
        Utils.getProjectRootDir() + "/src/test/resources/Rename/RenameMethod/BothSides/";
    String resultDir = targetDir + Side.INTELLI.asString() + File.separator;
    ThreewayGraphMerger merger = Util.matchGraphsThreeway(targetDir, resultDir);
    Set<MatchingEdge> refsOurs =
        merger
            .b2oMatching
            .biPartite
            .edgeSet()
            .stream()
            .filter(edge -> edge.getMatchingType().equals(MatchingType.MATCHED_METHOD))
            .collect(Collectors.toSet());

    assertThat(refsOurs.size()).isEqualTo(3);
  }

  @Test
  public void testRenameFieldOneSide() throws Exception {
    String targetDir = Utils.getProjectRootDir() + "/src/test/resources/Rename/RenameField/";
    String resultDir = targetDir + Side.INTELLI.asString() + File.separator ;
    ThreewayGraphMerger merger = Util.matchGraphsThreeway(targetDir, resultDir);
    Set<MatchingEdge> refsOurs =
        merger
            .b2oMatching
            .biPartite
            .edgeSet()
            .stream()
            .filter(edge -> edge.getMatchingType().equals(MatchingType.MATCHED_FIELD))
            .collect(Collectors.toSet());

    assertThat(refsOurs.size()).isEqualTo(1);
  }
}
