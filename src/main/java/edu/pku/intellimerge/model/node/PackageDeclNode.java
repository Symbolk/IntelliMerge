package edu.pku.intellimerge.model.node;

import com.github.javaparser.Range;
import edu.pku.intellimerge.model.constant.NodeType;

import java.util.List;
import java.util.Optional;

public class PackageDeclNode extends CompositeNode {
  private String packageName;
  private List<String> packageNameHierachy; // qualified_package_name.split(".").remove(";")

  public PackageDeclNode(
      Integer nodeID,
      Boolean needToMerge,
      NodeType nodeType,
      String displayName,
      String qualifiedName,
      String originalSignature,
      String comment,
      List<String> annotations,
      String packageName,
      List<String> packageNameHierachy,
      Optional<Range> range) {
    super(
        nodeID,
        needToMerge,
        nodeType,
        displayName,
        qualifiedName,
        originalSignature,
        comment,
        annotations,
        range);
    this.packageName = packageName;
    this.packageNameHierachy = packageNameHierachy;
  }
}
