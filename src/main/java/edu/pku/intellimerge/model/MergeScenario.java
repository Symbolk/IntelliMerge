package edu.pku.intellimerge.model;

public class MergeScenario {
    public String repoName;
    public String repoPath;
    public String srcPath;
    public String mergeCommitID;
    public String oursCommitID;
    public String baseCommitID;
    public String theirsCommitID;

    public MergeScenario(String repoName, String repoPath, String srcPath, String mergeCommitID, String oursCommitID, String baseCommitID, String theirsCommitID) {
        this.repoName = repoName;
        this.repoPath = repoPath;
        this.srcPath = srcPath;
        this.mergeCommitID = mergeCommitID;
        this.oursCommitID = oursCommitID;
        this.baseCommitID = baseCommitID;
        this.theirsCommitID = theirsCommitID;
    }

    public MergeScenario(String mergeCommitID, String oursCommitID, String baseCommitID, String theirsCommitID) {
        this.mergeCommitID = mergeCommitID;
        this.oursCommitID = oursCommitID;
        this.baseCommitID = baseCommitID;
        this.theirsCommitID = theirsCommitID;
    }

}
