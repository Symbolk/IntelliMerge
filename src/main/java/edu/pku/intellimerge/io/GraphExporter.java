package edu.pku.intellimerge.io;

import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.util.Utils;
import org.jgrapht.Graph;
import org.jgrapht.io.*;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;
import java.util.stream.Collectors;

public class SemanticGraphExporter {
  /** Export a graph into DOT format. */
  public static String exportAsDot(Graph<SemanticNode, SemanticEdge> semanticGraph) {
    try {
      // use helper classes to define how vertices should be rendered,
      // adhering to the DOT language restrictions
      ComponentNameProvider<SemanticNode> vertexIdProvider = node -> node.getNodeID().toString();
      ComponentNameProvider<SemanticNode> vertexLabelProvider = node -> node.getDisplayName();
      ComponentNameProvider<SemanticEdge> edgeLabelProvider = edge -> edge.getEdgeType().asString();
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

  public static String exportAsDotWithType(Graph<SemanticNode, SemanticEdge> semanticGraph) {
    try {

      // use helper classes to define how vertices should be rendered,
      // adhering to the DOT language restrictions
      ComponentNameProvider<SemanticNode> vertexIdProvider = node -> node.getNodeID().toString();
      ComponentAttributeProvider<SemanticNode> vertexAttributeProvider = new TypeProvider();
      ComponentNameProvider<SemanticNode> vertexLabelProvider = node -> node.getDisplayName();
      ComponentAttributeProvider<SemanticEdge> edgeAttributeProvider = new TypeProvider();
      ComponentNameProvider<SemanticEdge> edgeLabelProvider = edge -> edge.getEdgeType().asString();
      GraphExporter<SemanticNode, SemanticEdge> exporter =
          new DOTExporter<>(
              vertexIdProvider,
              vertexLabelProvider,
              edgeLabelProvider,
              vertexAttributeProvider,
              edgeAttributeProvider);
      Writer writer = new StringWriter();
      exporter.exportGraph(semanticGraph, writer);
      return writer.toString();

    } catch (ExportException e) {
      e.printStackTrace();
      return "";
    }
  }

  /**
   * Print the graph to console for debugging
   *
   * @param graph
   */
  public static void printAsDot(Graph<SemanticNode, SemanticEdge> graph, boolean showExternal) {
    if(showExternal){
      System.out.println(exportAsDotWithType(graph));
    }else{
      Set<SemanticEdge> externalEdges = graph.edgeSet().stream().filter(edge -> edge.isInternal()==false).collect(Collectors.toSet());
      graph.removeAllEdges(externalEdges);
      Set<SemanticNode> externalVertices = graph.vertexSet().stream().filter(node -> node.isInternal()== false).collect(Collectors.toSet());
      graph.removeAllVertices(externalVertices);
      System.out.println(exportAsDotWithType(graph));
    }
  }

  /**
   * Save the exported dot to file
   *
   * @param graph
   */
  public static void saveAsDot(Graph<SemanticNode, SemanticEdge> graph, String filePath) {
    Utils.writeContent(filePath, exportAsDotWithType(graph), false);
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
