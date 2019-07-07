package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class MergeScenario {
  public String repoName;
  public String repoPath;
  public String srcPath; // only needed in SemanticGraphBuilderV1 for symbolsolving
  public String mergeCommitID;
  public String oursCommitID;
  public String baseCommitID;
  public String theirsCommitID;
  public List<SimpleDiffEntry> oursDiffEntries;
  public List<SimpleDiffEntry> baseDiffEntries;
  public List<SimpleDiffEntry> theirsDiffEntries;
  public List<SimpleDiffEntry> bothModifiedEntries;

  public MergeScenario(
      String repoName,
      String repoPath,
      String srcPath,
      String mergeCommitID,
      String oursCommitID,
      String baseCommitID,
      String theirsCommitID) {
    this.repoName = repoName;
    this.repoPath = repoPath;
    this.srcPath = srcPath;
    this.mergeCommitID = mergeCommitID;
    this.oursCommitID = oursCommitID;
    this.baseCommitID = baseCommitID;
    this.theirsCommitID = theirsCommitID;
    this.oursDiffEntries = new ArrayList<>();
    this.baseDiffEntries = new ArrayList<>();
    this.theirsDiffEntries = new ArrayList<>();
  }

  public MergeScenario(
      String mergeCommitID, String oursCommitID, String baseCommitID, String theirsCommitID) {
    this.mergeCommitID = mergeCommitID;
    this.oursCommitID = oursCommitID;
    this.baseCommitID = baseCommitID;
    this.theirsCommitID = theirsCommitID;
    this.oursDiffEntries = new ArrayList<>();
    this.baseDiffEntries = new ArrayList<>();
    this.theirsDiffEntries = new ArrayList<>();
  }

  public MergeScenario(String oursCommitID, String baseCommitID, String theirsCommitID) {
    this.oursCommitID = oursCommitID;
    this.baseCommitID = baseCommitID;
    this.theirsCommitID = theirsCommitID;
    this.oursDiffEntries = new ArrayList<>();
    this.baseDiffEntries = new ArrayList<>();
    this.theirsDiffEntries = new ArrayList<>();
  }

  /**
   * Whether the file is changed in this side, if yes, it needs to be merged
   *
   * @param side
   * @param relativePath
   * @return
   */
  public boolean isInChangedFile(Side side, String relativePath) {
    // normalize to linux style separators
    String path = Utils.formatPathSeparator(relativePath);
    switch (side) {
      case OURS:
        return oursDiffEntries
            .stream()
            .anyMatch(simpleDiffEntry -> simpleDiffEntry.newPath.equals(path));
      case BASE:
        return baseDiffEntries
            .stream()
            .anyMatch(simpleDiffEntry -> simpleDiffEntry.newPath.equals(path));
      case THEIRS:
        return theirsDiffEntries
            .stream()
            .anyMatch(simpleDiffEntry -> simpleDiffEntry.newPath.equals(path));
      default:
        return false;
    }
  }
}
