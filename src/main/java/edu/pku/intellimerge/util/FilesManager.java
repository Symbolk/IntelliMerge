package edu.pku.intellimerge.util;

import edu.pku.intellimerge.model.SourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FilesManager {

  private static final Logger logger = LoggerFactory.getLogger(FilesManager.class);

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
    if (FilesManager.readFileContent(file).isEmpty()) {
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
  public static String readFileContent(File file) {
    String content = "";
    String fileEncoding = FilesEncoding.retrieveEncoding(file);
    try (BufferedReader reader =
        Files.newBufferedReader(Paths.get(file.getAbsolutePath()), Charset.forName(fileEncoding))) {
      content = reader.lines().collect(Collectors.joining("\n"));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return content;
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

  public static ArrayList<SourceFile> scanJavaSourceFiles(
      String path, ArrayList<SourceFile> javaSourceFiles, String repoPath) throws Exception {
    File file = new File(path);
    if (file.exists()) {
      File[] files = file.listFiles();
      for (File f : files) {
        if (f.isDirectory()) {
          scanJavaSourceFiles(f.getAbsolutePath(), javaSourceFiles, repoPath);
        } else if (f.isFile() && isJavaFile(f)) {
          String absoultePath = f.getAbsolutePath();
          String relativePath = absoultePath.substring(repoPath.length() + 1);
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
   * Empty the result folder
   *
   * @param resultDir absolute path
   * @return
   */
  public static void clearResultDir(String resultDir) {
    // if exist, remove all files under it
    File dirFile = new File(resultDir);
    if (dirFile.exists()) {
      emptyFolder(resultDir);
    } else {
      // if not exist, create
      dirFile.mkdirs();
    }
  }

  /**
   * Delete all files and subfolders to empty the folder
   *
   * @param dir absolute path
   * @return
   */
  public static boolean emptyFolder(String dir) {
    File file = new File(dir);
    if (!file.exists()) {
      System.err.println("The dir are not exists!");
      return false;
    }

    String[] content = file.list();
    for (String name : content) {
      File temp = new File(dir, name);
      if (temp.isDirectory()) {
        emptyFolder(temp.getAbsolutePath());
        temp.delete();
      } else {
        if (!temp.delete()) {
          System.err.println("Failed to delete " + name);
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
}
