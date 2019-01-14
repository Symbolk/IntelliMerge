package edu.pku.intellimerge.model;

import edu.pku.intellimerge.model.constant.Side;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MergeScenario {
  public String repoName;
  public String repoPath;
  public String srcPath;
  public String mergeCommitID;
  public String oursCommitID;
  public String baseCommitID;
  public String theirsCommitID;
  public List<SimpleDiffEntry> oursDiffEntries;
  public List<SimpleDiffEntry> baseDiffEntries;
  public List<SimpleDiffEntry> theirsDiffEntries;

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

  public Boolean isChangedFile(Side side, String relativePath) {
    // normalize to linux style separators
    String path = relativePath.replaceAll(Pattern.quote(File.separator), "/");
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
