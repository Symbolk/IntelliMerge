package edu.pku.intellimerge.client;

import edu.pku.intellimerge.core.SemanticGraphBuilder;
import edu.pku.intellimerge.core.SemanticGraphExporter;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import org.jgrapht.Graph;

public class APIClient {
    private static final String FILE_PATH = "src/main/java/edu/pku/intellimerge/samples/Foo.java";
    // this path is the root of the relative path/package
    private static final String PACKAGE_PATH = "src/main/java/";
    private static final String PROJECT_PATH = "src/main/java/edu/pku/intellimerge/samples";

    public static void main(String[] args) {

        Graph<SemanticNode, SemanticEdge> semanticGraph =
                SemanticGraphBuilder.buildForProject(PROJECT_PATH, PACKAGE_PATH);
        if (semanticGraph == null) {
            System.out.println("SemanticGraph is null!");
            return;
        }
//        for (SemanticNode node : semanticGraph.vertexSet()) {
//            System.out.println(node);
//        }
//        System.out.println("------------------------------");
//        for (SemanticEdge edge : semanticGraph.edgeSet()) {
//            SemanticNode source = semanticGraph.getEdgeSource(edge);
//            SemanticNode target = semanticGraph.getEdgeTarget(edge);
//            System.out.println(
//                    source.getDisplayName() + " " + edge.getEdgeType() + " " + target.getDisplayName());
//        }
//        System.out.println("------------------------------");
        System.out.println(SemanticGraphExporter.exportAsDot(semanticGraph));
    }
}
