package edu.pku.intellimerge.match;

import edu.pku.intellimerge.core.SemanticGraphBuilder2;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.MatchingType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.MatchingEdge;
import edu.pku.intellimerge.util.FilesManager;
import org.apache.log4j.PropertyConfigurator;
import org.jgrapht.Graph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        FilesManager.getProjectRootDir() + "/src/test/resources/Rename/RenameMethod/BothSides";
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

    assertThat(refsOurs.size()).isEqualTo(3);
  }
}
