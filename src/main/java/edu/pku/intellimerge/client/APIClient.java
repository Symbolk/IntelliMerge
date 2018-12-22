package edu.pku.intellimerge.client;

import edu.pku.intellimerge.core.SemanticGraphBuilder;
import edu.pku.intellimerge.core.SemanticGraphExporter;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.util.GitService;
import edu.pku.intellimerge.util.GitServiceImpl;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.jgrapht.Graph;

import java.util.List;

public class APIClient {
  // this path is the root of the relative path/package
  private static final String REPO_PATH = "D:\\github\\repos\\IntelliMerge";
  private static final String GIT_URL = "https://github.com/javaparser/javaparser.git";
  private static final String SRC_PATH = "src/main/java/";
  private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";
  private static final String FILE_PATH = "src/main/java/edu/pku/intellimerge/samples/Foo.java";

  public static void main(String[] args) {
      PropertyConfigurator.configure("log4j.properties");
//      BasicConfigurator.configure();

      String mergeCommitID = "3ceb2c9453198631adf0f49afc10ece85ccfc295";
      String oursCommitID = "3ab30428c5c85039cafdf380627436a80386b353";
      String theirsCommitID = "3ae7bb49d9331107b941a72c97b84042eebf9c7e";
      String baseCommitID = "003eba5af74699132eb15343c9cb39cab51eb85c";

    try {
      // 1. Get changed java files between parent commit and merge base commit
      GitService gitService = new GitServiceImpl();
      Repository repository = gitService.cloneIfNotExists(REPO_PATH, GIT_URL);
      List<DiffEntry> javaDiffs =
          gitService.listDiffJavaFiles(repository, baseCommitID, oursCommitID);
      for (DiffEntry javaDiff : javaDiffs) {
        System.out.println(javaDiff.getChangeType() + " " + javaDiff.getNewPath());
      }
//      System.out.println(gitService.getFileContentAtCommit(repository, theirsCommitID, javaDiffs.get(0).getNewPath()));
      // 2.1 Build ours/theirs graphs among changed files & their imported files (one hop)

      // 2.2 Build base/merge graphs among ours/theirs files

      // 3. Merge the 3-way graphs

      // 4. Print the merged graph into code

//      Graph<SemanticNode, SemanticEdge> semanticGraph =
//          SemanticGraphBuilder.buildForProject(PROJECT_PATH, SRC_PATH);
//      if (semanticGraph == null) {
//        System.out.println("SemanticGraph is null!");
//        return;
//      }
//      //        for (SemanticNode node : semanticGraph.vertexSet()) {
//      //            System.out.println(node);
//      //        }
//      //        System.out.println("------------------------------");
//      for (SemanticEdge edge : semanticGraph.edgeSet()) {
//        SemanticNode source = semanticGraph.getEdgeSource(edge);
//        SemanticNode target = semanticGraph.getEdgeTarget(edge);
//        System.out.println(
//            source.getDisplayName() + " " + edge.getEdgeType() + " " + target.getDisplayName());
//      }
//      //        System.out.println("------------------------------");
//      System.out.println(SemanticGraphExporter.exportAsDot(semanticGraph));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
