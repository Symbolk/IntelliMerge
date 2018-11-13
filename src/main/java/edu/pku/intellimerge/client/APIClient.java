package edu.pku.intellimerge.client;

import edu.pku.intellimerge.core.SemanticGraphBuilder;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import org.jgrapht.Graph;

public class APIClient {
  private static final String FILE_PATH = "src/main/java/edu/pku/intellimerge/samples/Foo.java";
  // this path is the root of the relative path/package
  private static final String PACKAGE_PATH = "src/main/java/";
  private static final String PROJECT_PATH = "" + "src/main/java";

  public static void main(String[] args) {
    Graph<SemanticNode, SemanticEdge> semanticGraph =
        SemanticGraphBuilder.buildForFile(FILE_PATH, PACKAGE_PATH);
    if (semanticGraph != null) {
      //    for(SemanticEdge edge:semanticGraph.edgeSet()){
      //      SemanticNode source = semanticGraph.getEdgeSource(edge);
      //      SemanticNode target = semanticGraph.getEdgeTarget(edge);
      //      System.out.println(source.getDisplayName() + " "+edge.getLabel() +" "+
      // target.getDisplayName());
      //    }
    }
  }
}
