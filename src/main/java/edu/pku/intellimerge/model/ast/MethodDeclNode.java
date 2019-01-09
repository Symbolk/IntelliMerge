package edu.pku.intellimerge.model.ast;

import edu.pku.intellimerge.model.SemanticNode;

import java.util.List;

public class MethodDeclNode extends SemanticNode {
    private List<String> modifiers;
    private String returnType;
    private String methodName;
    private List<String> parameterTypes;

}
