package edu.pku.intellimerge.core;

import org.eclipse.jgit.diff.DiffEntry;

public class SimpleDiffEntry {
  /** File name of the old (pre-image). */
  protected String oldPath;

  /** File name of the new (post-image). */
  protected String newPath;

  /** General type of change indicated by the patch. */
  protected DiffEntry.ChangeType changeType;

  /** Similarity score if {@link #changeType} is a copy or rename. */
  protected int score;

  public SimpleDiffEntry(String oldPath, String newPath, DiffEntry.ChangeType changeType) {
    this.oldPath = oldPath;
    this.newPath = newPath;
    this.changeType = changeType;
  }

  /**
   * Get the old name associated with this file.
   *
   * <p>The meaning of the old name can differ depending on the semantic meaning of this patch:
   *
   * <ul>
   *   <li><i>file add</i>: always <code>/dev/null</code>
   *   <li><i>file modify</i>: always {@link #getNewPath()}
   *   <li><i>file delete</i>: always the file being deleted
   *   <li><i>file copy</i>: source file the copy originates from
   *   <li><i>file rename</i>: source file the rename originates from
   * </ul>
   *
   * @return old name for this file.
   */
  public String getOldPath() {
    return oldPath;
  }

  /**
   * Get the new name associated with this file.
   *
   * <p>The meaning of the new name can differ depending on the semantic meaning of this patch:
   *
   * <ul>
   *   <li><i>file add</i>: always the file being created
   *   <li><i>file modify</i>: always {@link #getOldPath()}
   *   <li><i>file delete</i>: always <code>/dev/null</code>
   *   <li><i>file copy</i>: destination file the copy ends up at
   *   <li><i>file rename</i>: destination file the rename ends up at
   * </ul>
   *
   * @return new name for this file.
   */
  public String getNewPath() {
    return newPath;
  }

  /**
   * Get the change type
   *
   * @return the type of change this patch makes on {@link #getNewPath()}
   */
  public DiffEntry.ChangeType getChangeType() {
    return changeType;
  }
  /**
   * Get similarity score
   *
   * @return similarity score between {@link #getOldPath()} and {@link #getNewPath()} if {@link
   *     #getChangeType()} is {@link org.eclipse.jgit.diff.DiffEntry.ChangeType#COPY} or {@link
   *     org.eclipse.jgit.diff.DiffEntry.ChangeType#RENAME}.
   */
  public int getScore() {
    return score;
  }

  @Override
  public String toString() {
    return "SimpleDiffEntry{"
        + "oldPath='"
        + oldPath
        + '\''
        + ", newPath='"
        + newPath
        + '\''
        + ", changeType="
        + changeType
        + '}';
  }

  public int hashCode() {
    return toString().hashCode();
  }

  public boolean equals(Object o) {
    return (o instanceof SimpleDiffEntry) && (toString().equals(o.toString()));
  }
}
