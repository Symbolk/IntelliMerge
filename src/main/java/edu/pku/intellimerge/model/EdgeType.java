package edu.pku.intellimerge.model;

public class EdgeType {
  public static final Integer IMPORTS = 1;
  public static final Integer DEFINES = 2;
  public static final Integer CALLSMETHOD = 3;
  public static final Integer USESFIELD = 4;

  public static String getTypeAsString(Integer edgeType) {
    switch (edgeType) {
      case 1:
        return "IMPORTS";
      case 2:
        return "CONTAINS";
      case 3:
        return "CALLSMETHOD";
      case 4:
        return "USESFIELD";
      default:
        return "N/A";
    }
  }
}
