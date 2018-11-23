package edu.pku.intellimerge.model;

public enum EdgeType {
    // inter-file edges
    CONTAIN, IMPORT,
    // internal edges
    DEFINE_FIELD, DEFINE_METHOD, DEFINE_CONSTRUCTOR,
    // inter-field/method edges
    READ_FIELD, WRITE_FIELD, CALL_METHOD,
    // inter-class edges
    DECL_OBJECT, INIT_OBJECT
}
