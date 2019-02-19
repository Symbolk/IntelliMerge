package edu.pku.intellimerge.io;

import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.AttributeType;
import org.jgrapht.io.ComponentAttributeProvider;

import java.util.HashMap;
import java.util.Map;

public class TypeProvider implements ComponentAttributeProvider {
    @Override
    public Map<String, Attribute> getComponentAttributes(Object component) {
        if(component instanceof SemanticNode){
            SemanticNode node = (SemanticNode)component;
            Map<String, Attribute> map = new HashMap<>();
            map.put("type", new NodeAttribute(node));
            return map;
        }
        if(component instanceof SemanticEdge){
            SemanticEdge edge = (SemanticEdge)component;
            Map<String, Attribute> map = new HashMap<>();
            map.put("type", new EdgeAttribute(edge));
            return map;
        }
        return null;
    }
}

class NodeAttribute implements Attribute {
    private SemanticNode node;

    public NodeAttribute(SemanticNode node) {
        this.node = node;
    }

    @Override
    public String getValue() {
        return node.getNodeType().toPrettyString();
    }

    @Override
    public AttributeType getType() {
        return AttributeType.STRING;
    }
}
class EdgeAttribute implements Attribute {
    private SemanticEdge edge;

    public EdgeAttribute(SemanticEdge edge) {
        this.edge = edge;
    }

    @Override
    public String getValue() {
        return edge.getEdgeType().toPrettyString();
    }

    @Override
    public AttributeType getType() {
        return AttributeType.STRING;
    }
}