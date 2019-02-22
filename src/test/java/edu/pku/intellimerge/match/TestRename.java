package edu.pku.intellimerge;

import edu.pku.intellimerge.client.APIClient;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRename {
  @BeforeAll
  public void setUpBeforeAll(){
      PropertyConfigurator.configure("log4j.properties");
  }

  @Test
  public void testRenameMethod() {
      // process single file
      String folderPath = "src/test/resources/RenameMethod/BothSides/";
      String fileRelativePath = "SourceRoot.java";
      APIClient apiClient = new APIClient()
            processSingleFiles(folderPath, fileRelativePath);
  }
}
