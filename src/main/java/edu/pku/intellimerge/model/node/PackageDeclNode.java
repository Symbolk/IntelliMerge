package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.SemanticNode;

import java.util.List;

public class PackageDeclNode extends SemanticNode {
    private String packageName;
    private List<String> packageNameHierachy;

    public PackageDeclNode(String packageName, List<String> packageNameHierachy) {
        this.packageName = packageName;
        this.packageNameHierachy = packageNameHierachy;
    }
}
