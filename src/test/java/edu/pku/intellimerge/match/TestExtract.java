package edu.pku.intellimerge.match;

import edu.pku.intellimerge.model.constant.RefactoringType;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.Refactoring;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestExtract {
  @BeforeAll
  public static void setUpBeforeAll() {
    PropertyConfigurator.configure("log4j.properties");
  }

  @Test
  public void testExtractMethod() {
    String targetDir =
        Utils.getProjectRootDir() + "/src/test/resources/Extract/ExtractMethod/";
    TwowayMatching matching = Util.matchGraphsTwoway(targetDir, Side.BASE, Side.OURS);
    Set<Refactoring> refsOurs =
        matching
            .refactorings
            .stream()
            .filter(edge -> edge.getRefactoringType().equals(RefactoringType.EXTRACT_TO_METHOD))
            .collect(Collectors.toSet());
    assertThat(refsOurs.size()).isOne();
  }
}
