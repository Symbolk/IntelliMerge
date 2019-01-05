package edu.pku.intellimerge.core;

import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import org.jgrapht.Graph;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;

import java.io.StringWriter;
import java.io.Writer;

public class SemanticGraphExporter {
  /** Export a graph into DOT format. */
  public static String exportAsDot(Graph<SemanticNode, SemanticEdge> semanticGraph) {
    try {

      // use helper classes to define how vertices should be rendered,
      // adhering to the DOT language restrictions
      ComponentNameProvider<SemanticNode> vertexIdProvider = node -> node.getNodeID().toString();
      ComponentNameProvider<SemanticNode> vertexLabelProvider = node -> node.getDisplayName();
      ComponentNameProvider<SemanticEdge> edgeLabelProvider = edge -> edge.getEdgeType().toString();
      GraphExporter<SemanticNode, SemanticEdge> exporter =
          new DOTExporter<>(vertexIdProvider, vertexLabelProvider, edgeLabelProvider);
      Writer writer = new StringWriter();
      exporter.exportGraph(semanticGraph, writer);
      return writer.toString();

    } catch (ExportException e) {
      e.printStackTrace();
      return "";
    }
  }

  /**
   * Print the graph for debugging
   *
   * @param graph
   */
  public static void printAsDot(Graph<SemanticNode, SemanticEdge> graph) {
    System.out.println(SemanticGraphExporter.exportAsDot(graph));
  }

  public static void printVertexAndEdge(Graph<SemanticNode, SemanticEdge> graph) {
    for (SemanticNode node : graph.vertexSet()) {
      System.out.println(node);
    }
    System.out.println("------------------------------");
    for (SemanticEdge edge : graph.edgeSet()) {
      SemanticNode source = graph.getEdgeSource(edge);
      SemanticNode target = graph.getEdgeTarget(edge);
      System.out.println(
          source.getDisplayName() + " " + edge.getEdgeType() + " " + target.getDisplayName());
    }
    System.out.println("------------------------------");
  }
}
