package edu.pku.intellimerge.model.node;

import edu.pku.intellimerge.model.SemanticNode;

import java.util.List;

public class FieldDeclNode extends SemanticNode {
    private String access;
    private List<String> modifiers;
    private String fieldType;
    private String fieldName;

}
