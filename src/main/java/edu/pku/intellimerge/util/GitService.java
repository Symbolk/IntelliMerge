package edu.pku.intellimerge.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

public interface GitService {
  Repository cloneIfNotExists(String projectPath, String cloneUrl) throws Exception;

  Repository cloneIfNotExistsWithBranch(String projectPath, String cloneUrl, String branch)
      throws Exception;

  void checkout(Repository repository, String commitID) throws Exception;

  List<DiffEntry> listDiffJavaFiles(
      Repository repository, String oldCommit, String newCommit)
      throws GitAPIException, IOException;
}
