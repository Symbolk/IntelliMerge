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
import java.util.stream.Collectors;

public class FilesManager {

  private static final Logger logger = LoggerFactory.getLogger(FilesManager.class);

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
    try {
      String fileEncoding = FilesEncoding.retrieveEncoding(file);

      BufferedReader reader =
          Files.newBufferedReader(Paths.get(file.getAbsolutePath()), Charset.forName(fileEncoding));
      content = reader.lines().collect(Collectors.joining("\n"));
    } catch (Exception e) {
      // System.err.println(e.getMessage());
    }
    return content;
  }

  /**
   * Writes the given content in the file of the given file path.
   * @param filePath
   * @param content
   * @return boolean indicating the success of the write operation.
   */
  public static boolean writeContent(String filePath, String content){
    if(!content.isEmpty()){
      try{
        File file = new File(filePath);
        if(!file.exists()){
          file.getParentFile().mkdirs();
          file.createNewFile();
        }
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
        writer.write(content);
        writer.flush();	writer.close();
      } catch(NullPointerException ne){
        ne.printStackTrace();
        //empty, necessary for integration with git version control system
      } catch(Exception e){
        e.printStackTrace();
        return false;
      }
    }
    return true;
  }

  /**
   * Append content to a file
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
   * Prepare the result folder
   *
   * @param folderPath absolute path
   * @return
   */
  public static void prepareResultFolder(String folderPath) {
    // if exist, remove all files under it
    File folderFile = new File(folderPath);
    if (folderFile.exists()) {
      emptyFolder(folderPath);
    } else {
      // if not exist, create
      folderFile.mkdirs();
    }
  }

  /**
   * Delete all files and subfolders to empty the folder
   *
   * @param path absolute path
   * @return
   */
  public static boolean emptyFolder(String path) {
    File file = new File(path);
    if (!file.exists()) {
      System.err.println("The dir are not exists!");
      return false;
    }

    String[] content = file.list();
    for (String name : content) {
      File temp = new File(path, name);
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
}
