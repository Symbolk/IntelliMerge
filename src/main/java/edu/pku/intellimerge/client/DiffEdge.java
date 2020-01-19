package edu.pku.intellimerge.client;

public class DiffEdge {
  private Integer id;
  private String type;
  private Double weight;


  public DiffEdge(Integer id, String type, Double weight) {
    this.id = id;
    this.type = type;
    this.weight = weight;
  }

  public Integer getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public Double getWeight() {
    return weight;
  }
}
