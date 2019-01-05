package edu.pku.intellimerge.model;

import org.apache.commons.lang3.tuple.Triple;

public class MergeScenario {
    public String mergeCommitID;
    public String oursCommitID;
    public String baseCommitID;
    public String theirsCommitID;

    public MergeScenario(String mergeCommitID, String oursCommitID, String baseCommitID, String theirsCommitID) {
        this.mergeCommitID = mergeCommitID;
        this.oursCommitID = oursCommitID;
        this.baseCommitID = baseCommitID;
        this.theirsCommitID = theirsCommitID;
    }

}
