package edu.pku.intellimerge.match;

import edu.pku.intellimerge.client.APIClient;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.MatchingEdge;
import edu.pku.intellimerge.util.FilesManager;
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
    // process single file
    String targetDir =
            FilesManager.getProjectRootDir() +  "/src/test/resources/Rename/RenameMethod/BothSides";
    String resultDir = targetDir + File.separator + Side.INTELLI.asString();
    APIClient apiClient = new APIClient();
    ThreewayGraphMerger merger = apiClient.processDirectory(targetDir, resultDir);
    Set<MatchingEdge> refsOurs =
        merger
            .b2oMatchings
            .biPartite
            .edgeSet()
            .stream()
            .filter(edge -> edge.getLabel().equals("change_signature"))
            .collect(Collectors.toSet());

    assertThat(refsOurs.size()).isEqualTo(3);
  }
}
