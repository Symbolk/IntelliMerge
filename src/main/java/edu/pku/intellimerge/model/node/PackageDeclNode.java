package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.EdgeType;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.ArrayList;
import java.util.List;

public class PackageDeclNode extends NonTerminalNode {
  private String packageName;
  private List<String> packageNameHierachy; // qualified_package_name.split(".").remove(";")

  public PackageDeclNode(
      Integer nodeID,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String packageName,
      List<String> packageNameHierachy) {
    super(nodeID, nodeType, displayName, qualifiedName);
    this.packageName = packageName;
    this.packageNameHierachy = packageNameHierachy;
    this.incomingEdges.put(EdgeType.CONTAIN, new ArrayList<>());
    this.outgoingEdges.put(EdgeType.CONTAIN, new ArrayList<>());
  }

    @Override
    public String toString() {
        return "PackageDeclNode{" +
                "packageName='" + packageName + '\'' +
                ", packageNameHierachy=" + packageNameHierachy +
                '}';
    }

  @Override
  public String getSignature() {
    // qualified signature of package, without spaces/dots
    StringBuilder builder = new StringBuilder();
    builder.append(packageName);
    return toString();
  }
}
