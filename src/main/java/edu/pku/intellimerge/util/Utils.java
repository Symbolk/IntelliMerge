package edu.pku.intellimerge.util;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import edu.pku.intellimerge.model.ConflictBlock;
import edu.pku.intellimerge.model.SourceFile;
import edu.pku.intellimerge.model.constant.Side;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  /**
   * Change the format of files under a folder
   *
   * @param path
   * @param formatBefore
   * @param formatAfter
   */
  public static void renameFiles(String path, String formatBefore, String formatAfter) {
    File file = new File(path);
    if (file.exists() && file.isDirectory()) {
      File[] files = file.listFiles();
      for (File f : files) {
        if (f.isFile()) {
          f.renameTo(new File(f.getAbsolutePath().replace(formatBefore, formatAfter)));
        }
      }
    }
  }

  /**
   * Checks if the given file is adequate for parsing.
   *
   * @param file to be parsed
   * @return true if the file is appropriated, or false
   * @throws FileNotFoundException
   */
  public static boolean isValidFile(File file) throws FileNotFoundException {
    if (Utils.readContentFromFile(file).isEmpty()) {
      throw new FileNotFoundException();
    } else if (file != null && (isJavaFile(file))) {
      return true;
    } else if (file != null && !isJavaFile(file)) {
      System.out.println("The file " + file.getName() + " is not a valid .java file.");
      return false;
    } else {
      return false;
    }
  }

  /**
   * Checks if a given file is a .java file.
   *
   * @param file
   * @return true in case file extension is <i>java</i>, or false
   */
  private static boolean isJavaFile(File file) {
    // return FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("java");
    return file.getName().toLowerCase().contains(".java");
  }

  /**
   * Read the content of a given file.
   *
   * @param file to be read
   * @return string content of the file, or null in case of errors.
   */
  public static String readContentFromFile(File file) {
    String content = "";
    if (file.exists()) {

      String fileEncoding = FileEncoding.retrieveEncoding(file);
      try (BufferedReader reader =
          Files.newBufferedReader(
              Paths.get(file.getAbsolutePath()), Charset.forName(fileEncoding))) {
        content = reader.lines().collect(Collectors.joining("\n"));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      logger.error("{} does not exist!", file.getAbsolutePath());
    }
    return content;
  }

  /**
   * Read the content of a given file.
   *
   * @param path to be read
   * @return string content of the file, or null in case of errors.
   */
  public static String readContentFromPath(String path) {
    String content = "";
    File file = new File(path);
    if (file.exists()) {
      String fileEncoding = FileEncoding.retrieveEncoding(file);
      try (BufferedReader reader =
          Files.newBufferedReader(Paths.get(path), Charset.forName(fileEncoding))) {
        content = reader.lines().collect(Collectors.joining("\n"));
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      logger.error("{} does not exist!", path);
    }
    return content;
  }

  /**
   * Read the content of a given file.
   *
   * @param path to be read
   * @return string content of the file, or null in case of errors.
   */
  public static List<String> readContentLinesFromPath(String path) {
    List<String> lines = new ArrayList<>();
    File file = new File(path);
    if (file.exists()) {
      String fileEncoding = FileEncoding.retrieveEncoding(file);
      try (BufferedReader reader =
          Files.newBufferedReader(Paths.get(path), Charset.forName(fileEncoding))) {
        lines = reader.lines().collect(Collectors.toList());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      logger.error("{} does not exist!", path);
    }
    return lines;
  }

  /**
   * Read csv and return a list of separated items
   *
   * @param path
   * @param separator
   * @return
   */
  public static List<String[]> readCSV(String path, String separator) {
    List<String[]> results = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
      reader.readLine(); // header
      String line = null;
      while ((line = reader.readLine()) != null) {
        String items[] = line.split(separator);
        results.add(items);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return results;
  }

  /**
   * Writes the given content in the file of the given file path.
   *
   * @param filePath
   * @param content
   * @return boolean indicating the success of the write operation.
   */
  public static boolean writeContent(String filePath, String content) {
    if (!content.isEmpty()) {
      try {
        File file = new File(filePath);
        if (!file.exists()) {
          file.getParentFile().mkdirs();
          file.createNewFile();
        }
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
        writer.write(content);
        writer.flush();
        writer.close();
      } catch (NullPointerException ne) {
        ne.printStackTrace();
        // empty, necessary for integration with git version control system
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }
    return true;
  }

  /**
   * Append content to a file
   *
   * @param filePath
   * @param content
   */
  private static void appendContent(String filePath, String content) {
    Path path = Paths.get(filePath);
    byte[] contentBytes = (content + System.lineSeparator()).getBytes();
    try {
      Files.write(path, contentBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Scan all java files under the directory, return a list of SourceFiles
   *
   * @param path
   * @param javaSourceFiles
   * @param repoPath to get the relative path
   * @return
   * @throws Exception
   */
  public static ArrayList<SourceFile> scanJavaSourceFiles(
      String path, ArrayList<SourceFile> javaSourceFiles, String repoPath) throws Exception {
    File file = new File(path);
    if (file.exists()) {
      File[] files = file.listFiles();
      for (File f : files) {
        if (f.isDirectory()) {
          scanJavaSourceFiles(f.getAbsolutePath(), javaSourceFiles, repoPath);
        } else if (f.isFile() && isJavaFile(f)) {
          String absoultePath = formatPathSeparator(f.getAbsolutePath());
          String relativePath = absoultePath.substring(repoPath.length());
          String qualifiedName = getQualifiedName(f);
          SourceFile sourceFile =
              new SourceFile(f.getName(), qualifiedName, relativePath, absoultePath);
          javaSourceFiles.add(sourceFile);
        }
      }
    } else {
      logger.error("{} does not exist!", path);
    }
    return javaSourceFiles;
  }

  /**
   * Get qualified name of a java file, by reading its package declaration
   *
   * @param file
   * @return
   * @throws Exception
   */
  private static String getQualifiedName(File file) throws Exception {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String firstLine = reader.readLine();
    reader.close();
    return firstLine.replace("package ", "").replace(";", ".")
        + file.getName().replace(".java", "");
  }

  /**
   * Prepare the directory for output
   *
   * @param dir absolute path
   * @return
   */
  public static void prepareDir(String dir) {
    // if exist, remove all files under it
    File dirFile = new File(dir);
    if (dirFile.exists() && dirFile.isDirectory()) {
      clearDir(dir);
    } else {
      // if not exist, create
      dirFile.mkdirs();
    }
  }

  /**
   * Delete all files and subfolders to clear the directory
   *
   * @param dir absolute path
   * @return
   */
  public static boolean clearDir(String dir) {
    File file = new File(dir);
    if (!file.exists()) {
      logger.error("{} does not exist!", dir);
      return false;
    }

    String[] content = file.list();
    for (String name : content) {
      File temp = new File(dir, name);
      if (temp.isDirectory()) {
        clearDir(temp.getAbsolutePath());
        temp.delete();
      } else {
        if (!temp.delete()) {
          System.err.println("Failed to delete the directory: " + name);
        }
      }
    }
    return true;
  }

  /**
   * Format separator in path to jgit style "/"
   *
   * @param path
   * @return
   */
  public static String formatPathSeparator(String path) {
    return path.replaceAll(Pattern.quote(File.separator), "/");
  }

  /**
   * Get the project root directory path, e.g. F:\workspace\dev\IntelliMerge
   *
   * @return
   */
  public static String getProjectRootDir() {
    return System.getProperty("user.dir");
  }

  /**
   * Extracts the merge conflicts of a string representation of merged code.
   *
   * @param mergedCode
   * @return list o merge conflicts
   */
  public static List<ConflictBlock> extractConflictBlocks(String mergedCode) {
    String CONFLICT_HEADER_BEGIN = "<<<<<<<";
    String CONFLICT_BASE_BEGIN = "|||||||";
    String CONFLICT_BASE_END = "=======";
    String CONFLICT_HEADER_END = ">>>>>>>";
    String leftConflictingContent = "";
    String baseConflictingContent = "";
    String rightConflictingContent = "";
    boolean isConflictOpen = false;
    boolean isBaseContent = false;
    boolean isLeftContent = false;
    int lineCounter = 0;
    int startLOC = 0;
    int endLOC = 0;

    List<ConflictBlock> mergeConflicts = new ArrayList<ConflictBlock>();
    List<String> lines = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new StringReader(mergedCode));
    lines = reader.lines().collect(Collectors.toList());
    Iterator<String> itlines = lines.iterator();
    while (itlines.hasNext()) {
      String line = itlines.next();
      lineCounter++;
      if (line.contains(CONFLICT_HEADER_BEGIN)) {
        isConflictOpen = true;
        isLeftContent = true;
        startLOC = lineCounter;
      } else if (line.contains(CONFLICT_BASE_BEGIN)) {
        isLeftContent = false;
        isBaseContent = true;
      } else if (line.contains(CONFLICT_BASE_END)) {
        isBaseContent = false;
      } else if (line.contains(CONFLICT_HEADER_END)) {
        endLOC = lineCounter;
        ConflictBlock mergeConflict =
            new ConflictBlock(
                leftConflictingContent,
                baseConflictingContent,
                rightConflictingContent,
                startLOC,
                endLOC);
        mergeConflicts.add(mergeConflict);

        // reseting the flags
        isConflictOpen = false;
        isBaseContent = false;
        isLeftContent = false;
        leftConflictingContent = "";
        baseConflictingContent = "";
        rightConflictingContent = "";
      } else {
        if (isConflictOpen) {
          if (isLeftContent) {
            leftConflictingContent += line + "\n";
          } else if (isBaseContent) {
            baseConflictingContent += line + "\n";
          } else {
            rightConflictingContent += line + "\n";
          }
        }
      }
    }
    return mergeConflicts;
  }

  /**
   * Get simple name for directory e.g. /home/xyz/github/repo/javaparser(/) --> javaparser
   *
   * @param dir
   * @return
   */
  public static String getDirSimpleName(String dir) {
    int offset = 0;
    if (dir.endsWith(File.separator)) {
      dir = dir.substring(0, dir.lastIndexOf(File.separator));
      offset = dir.lastIndexOf(File.separator);
    } else {
      offset = dir.lastIndexOf(File.separator);
    }
    return dir.substring(offset + 1);
  }

  /** Format all java files with google-java-formatter in a directory */
  public static void formatAllJavaFiles(String dir) {
    // read file content as string
    File file = new File(dir);
    if (file.exists()) {
      File[] files = file.listFiles();
      for (File f : files) {
        if (f.isDirectory()) {
          formatAllJavaFiles(f.getAbsolutePath());
        } else if (f.isFile() && isJavaFile(f)) {
          String code = readContentFromFile(f);
          try {
            // format with google-java-formatter
            String reformattedCode = new Formatter().formatSource(code);
            // write string back into the original file
            boolean isSuccessful = writeContent(f.getAbsolutePath(), reformattedCode);
            logger.info("Formatting {} : {}", f.getAbsolutePath(), isSuccessful);
          } catch (FormatterException e) {
            e.printStackTrace();
          }
        }
      }
    } else {
      logger.error("{} does not exist!", dir);
    }
  }

  /**
   * (For evaluation) Copy all versions of specific file from the source folder to the target folder
   * (like a sandbox)
   *
   * @param sourceDir
   * @param relativePaths
   * @param targetDir
   */
  public static void copyAllVersions(
      String sourceDir, List<String> relativePaths, String targetDir) {
    try {
      copyOneVersion(sourceDir, relativePaths, targetDir, Side.OURS);
      copyOneVersion(sourceDir, relativePaths, targetDir, Side.BASE);
      copyOneVersion(sourceDir, relativePaths, targetDir, Side.THEIRS);
      copyOneVersion(sourceDir, relativePaths, targetDir, Side.MANUAL);
      copyOneVersion(sourceDir, relativePaths, targetDir, Side.GIT);
      //      copyOneVersion(sourceDir, relativePaths, targetDir, Side.INTELLI);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Copy one version every time
   *
   * @param sourceDir
   * @param relativePaths
   * @param targetDir
   * @param side
   * @throws IOException
   */
  private static void copyOneVersion(
      String sourceDir, List<String> relativePaths, String targetDir, Side side)
      throws IOException {
    prepareDir(targetDir + File.separator + side.asString());
    for (String relativePath : relativePaths) {
      File sourceFile =
          new File(sourceDir + File.separator + side.asString() + File.separator + relativePath);
      File targetFile =
          new File(
              targetDir + File.separator + side.asString() + File.separator + sourceFile.getName());
      if (sourceFile.exists()) {
        FileUtils.copyFile(sourceFile, targetFile);
        logger.info("Done with {} : {}...", side.toString(), sourceFile.getName());
      } else {
        logger.error("{} : {} does not exist!", side.toString(), sourceFile.getAbsolutePath());
      }
    }
  }

  /**
   * Run system command and return the output
   *
   * @param dir
   * @param commands
   * @return
   */
  public static String runSystemCommand(String dir, String... commands) {
    StringBuilder builder = new StringBuilder();
    try {
      //            if (verbose) {
      //                for (String command : commands) {
      //                    System.out.print(command + " ");
      //                }
      //                System.out.println();
      //            }
      Runtime rt = Runtime.getRuntime();
      Process proc = rt.exec(commands, null, new File(dir));

      BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

      BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

      String s = null;
      while ((s = stdInput.readLine()) != null) {
        builder.append(s);
        builder.append("\n");
        //                if (verbose) log(s);
      }

      while ((s = stdError.readLine()) != null) {
        builder.append(s);
        builder.append("\n");
        //                if (verbose) log(s);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return builder.toString();
  }

  /**
   * Returns a single line no spaced representation of a given string.
   *
   * @param content
   * @return
   */
  public static String getStringContentOneLine(String content) {
    return (content.trim().replaceAll("\\r\\n|\\r|\\n", "")).replaceAll("\\s+", "");
  }
}
