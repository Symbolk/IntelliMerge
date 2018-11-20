package edu.pku.intellimerge.model;

public class EdgeType {
    // between-class edges
    public static final Integer PACKAGES = 0;
    public static final Integer IMPORTS = 1;
    // inner edges
    public static final Integer DEFINES = 2;
    public static final Integer HASFIELD = 3;
    public static final Integer HASMETHOD = 4;
    public static final Integer HASCONSTRUCTOR = 5;
    // method-field/method edges
    public static final Integer USESFIELD = 6;
    public static final Integer CALLSMETHOD = 7;

    public static String getTypeAsString(Integer edgeType) {
        switch (edgeType) {
            case 0:
                return "PACKAGES";
            case 1:
                return "IMPORTS";
            case 2:
                return "DEFINES";
            case 3:
                return "HASFIELD";
            case 4:
                return "HASMETHOD";
            case 5:
                return "HASCONSTRUCTOR";
            case 6:
                return "USESFIELD";
            case 7:
                return "CALLSMETHOD";
            default:
                return "N/A";
        }
    }
}
