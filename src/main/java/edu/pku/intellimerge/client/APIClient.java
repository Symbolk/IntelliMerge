package edu.pku.intellimerge.client;

import edu.pku.intellimerge.core.SemanticGraphBuilder;
import edu.pku.intellimerge.core.SemanticGraphExporter;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.util.GitService;
import edu.pku.intellimerge.util.GitServiceImpl;
import org.apache.log4j.BasicConfigurator;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.jgrapht.Graph;

import java.util.List;

public class APIClient {
  // this path is the root of the relative path/package
  private static final String REPO_PATH = "D:\\workspace\\repos\\javaparser";
  private static final String GIT_URL = "https://github.com/javaparser/javaparser.git";
  private static final String SRC_PATH = "src/main/java/";
  private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";
  private static final String FILE_PATH = "src/main/java/edu/pku/intellimerge/samples/Foo.java";

  public static void main(String[] args) {
    String mergeCommitID = "31ece213bcac1552f14af3678396b688b0856888";
    String oursCommitID = "73133a02272a7fa846268cc1b3d9743323455694";
    String theirsCommitID = "56f90e1d26e0beba51bda25d1c952abe2443f85e";
    String baseCommitID = "3a2f69e5aad82e1cc3aa2d7c934e60eb4ebcd45b";
    BasicConfigurator.configure();

    try {
      // 1. Get changed java files between parent commit and merge base commit
      GitService gitService = new GitServiceImpl();
      Repository repository = gitService.cloneIfNotExists(REPO_PATH, GIT_URL);
      List<DiffEntry> javaDiffs =
          gitService.listDiffJavaFiles(repository, baseCommitID, oursCommitID);
      for (DiffEntry javaDiff : javaDiffs) {
//        System.out.println(javaDiff.getChangeType() + " " + javaDiff.getNewPath());
      }
      System.out.println(gitService.getFileContentAtCommit(repository, oursCommitID, javaDiffs.get(0).getNewPath()));
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
