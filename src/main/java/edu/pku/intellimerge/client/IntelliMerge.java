package edu.pku.intellimerge.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.github.javaparser.JavaParser;
import com.google.common.base.Stopwatch;
import edu.pku.intellimerge.core.GraphBuilderV2;
import edu.pku.intellimerge.core.GraphMerger;
import edu.pku.intellimerge.exception.RangeNullException;
import edu.pku.intellimerge.io.SourceFileCollector;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.Refactoring;
import edu.pku.intellimerge.util.GitService;
import edu.pku.intellimerge.util.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** The class is responsible to provide CLI and API service to users */
public class IntelliMerge {
  private static final Logger logger = LoggerFactory.getLogger(IntelliMerge.class);
  // command line options
  @Parameter(
      names = {"-r", "--repo"},
      arity = 1,
      description = "Absolute path of the target Git repository.")
  String repoPath = "";

  @Parameter(
      names = {"-b", "--branches"},
      arity = 2,
      description =
          "Names of two branches to be merged. The order should be <left> <right> to merge <right> branch to <left>.")
  List<String> branchNames = new ArrayList<>();

  @Parameter(
      names = {"-d", "--directories"},
      arity = 3,
      description =
          "Absolute paths of three directories with Java files inside to be merged. The order should be <left> <base> <right>.")
  List<String> directoryPaths = new ArrayList<>();

  @Parameter(
      names = {"-o", "--output"},
      arity = 1,
      description = "Absolute path of an empty directory to save the merging results.")
  String outputPath = "";

  @Parameter(
      names = {"-s", "--hasSubModule"},
      arity = 1,
      description = "Whether the repository has sub-module.")
  boolean hasSubModule = true;

  @Parameter(
      names = {"-t", "--threshold"},
      arity = 1,
      description = "[Optional] The threshold value for heuristic rules, default: 0.618.")
  String thresholdString = "0.618";

  public IntelliMerge() {
    JavaParser.getStaticConfiguration().setAttributeComments(true);
  }

  public static void main(String[] args) {
    // config the logger with properties files when developing
    //    PropertyConfigurator.configure("log4j.properties");
    // use basic configuration when packaging
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);

    try {
      IntelliMerge merger = new IntelliMerge();
      merger.run(args);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Check whether given arguments are valid before working
   *
   * @param merger
   */
  public static void checkArguments(IntelliMerge merger) {

    if (merger.branchNames.isEmpty() && merger.directoryPaths.isEmpty()) {
      throw new ParameterException("Please specify ONE of the following options: -r, -d, -f.");
    } else if (!merger.branchNames.isEmpty() && !merger.directoryPaths.isEmpty()) {
      throw new ParameterException("Please specify ONE of the following options: -r, -d, -f.");
    } else if (!merger.branchNames.isEmpty()) { // option: -r
      if (merger.repoPath.length() == 0) {
        throw new ParameterException("Please specify the path of the target repository.");
      } else {
        File d = new File(merger.repoPath);
        if (!d.isDirectory()) {
          throw new ParameterException(merger.repoPath + " is not a valid directory path.");
        }
        if (!d.exists()) {
          throw new ParameterException(merger.repoPath + " does not exists.");
        }
      }

      if (merger.branchNames.size() != 2) {
        throw new ParameterException("Invalid number of branch names, expected 2.");
      } else {
        // check if the branch names are valid
        if (!GitService.checkIfBranchesValid(merger.repoPath, merger.branchNames.get(0))) {
          throw new ParameterException("The first branch is not valid.");
        }
        if (!GitService.checkIfBranchesValid(merger.repoPath, merger.branchNames.get(1))) {
          throw new ParameterException("The second branch is not valid.");
        }
      }

      if (merger.outputPath == null || merger.outputPath.length() <= 0) {
        throw new ParameterException("Output path must be specified.");
      }

    } else if (!merger.directoryPaths.isEmpty()) { // option: -d
      if (merger.directoryPaths.size() != 3) { // three directories path must be given
        throw new ParameterException("Invalid number of directories, expected 3.");
      } else {
        for (String path : merger.directoryPaths) {
          File d = new File(path);
          if (!d.isDirectory()) {
            throw new ParameterException(path + " is not a valid directory path.");
          }
          if (!d.exists()) {
            throw new ParameterException(path + " does not exists.");
          }
        }
        if (merger.outputPath == null || merger.outputPath.length() <= 0) {
          throw new ParameterException("Output path must be specified.");
        }
      }
    }
  }

  /**
   * Run merging according to given options
   *
   * @param args
   */
  private void run(String[] args) throws Exception {
    JCommander commandLineOptions = new JCommander(this);
    try {
      commandLineOptions.parse(args);
      checkArguments(this);
      if (repoPath.length() > 0 && !branchNames.isEmpty()) {
        mergeBranches(repoPath, branchNames, outputPath, hasSubModule);
      } else if (!directoryPaths.isEmpty()) {
        mergeDirectories(directoryPaths, outputPath);
      }
    } catch (ParameterException pe) {
      System.err.println(pe.getMessage());
      commandLineOptions.setProgramName("IntelliMerge");
      commandLineOptions.usage();
    }
  }

  /**
   * Collect, analyze, match and merge java files collected in one merge scenario
   *
   * @param hasSubModule whether the repo has sub-modules
   * @throws Exception
   */
  public List<String> mergeBranches(
      String repoPath, List<String> branchNames, String outputPath, boolean hasSubModule)
      throws Exception {

    // 1. Collect diff java files and imported files between ours/theirs commit and base commit
    // Collect source files to be analyzed in the system temp dir
    // For Windows: C:\Users\USERNAME\AppData\Local\Temp\IntelliMerge\
    String collectedDir = System.getProperty("java.io.tmpdir") + "IntelliMerge2" + File.separator;

    SourceFileCollector collector = new SourceFileCollector(repoPath, branchNames, collectedDir);

    collector.collectFilesForAllSides();
    logger.info("Done collecting files into {}", collectedDir);

    // 2. Build graphs from collected files
    Stopwatch stopwatch = Stopwatch.createStarted();
    ExecutorService executorService = Executors.newFixedThreadPool(3);

    MergeScenario mergeScenario = collector.getMergeScenario();
    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(
            new GraphBuilderV2(mergeScenario, Side.OURS, collectedDir, hasSubModule));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(
            new GraphBuilderV2(mergeScenario, Side.BASE, collectedDir, hasSubModule));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(
            new GraphBuilderV2(mergeScenario, Side.THEIRS, collectedDir, hasSubModule));
    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

    stopwatch.stop();
    long buildingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done building graphs.", buildingTime);
    executorService.shutdown();

    Utils.prepareDir(outputPath);
    GraphMerger merger = new GraphMerger(outputPath, oursGraph, baseGraph, theirsGraph);

    //    GraphExporter.printAsDot(baseGraph, false);
    // 3. Match nodes and merge programs with the 3-way graphs
    stopwatch.reset().start();
    Pair<List<Refactoring>, List<Refactoring>> refactorings = merger.threewayMap();
    stopwatch.stop();
    long matchingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done matching graphs.", matchingTime);

    // save the detected refactorings into csv for human validation and debugging
    String b2oCsvFilePath = outputPath + File.separator + "ours_refactorings.csv";
    String b2tCsvFilePath = outputPath + File.separator + "theirs_refactorings.csv";
    saveRefactorings(b2oCsvFilePath, refactorings.getLeft());
    saveRefactorings(b2tCsvFilePath, refactorings.getRight());

    // 4. Print the merged graph into files, keeping the original format and directory structure
    stopwatch.reset().start();
    List<String> mergedFilePaths = merger.threewayMerge();
    stopwatch.stop();
    long mergingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done merging programs.", mergingTime);

    long overall = buildingTime + matchingTime + mergingTime;
    logger.info("Merged {} files. Overall time cost: {}ms.", mergedFilePaths.size(), overall);

    // Clear and remove temp directory
    Utils.removeDir(collectedDir);
    return mergedFilePaths;
  }

  /**
   * Merge directories in the order <left> <base> <right> and return merged file paths
   *
   * @return merging results
   * @throws Exception
   */
  public List<String> mergeDirectories(List<String> directoryPaths, String outputPath)
      throws Exception {

    ExecutorService executorService = Executors.newFixedThreadPool(3);

    // 1. Build graphs from given directories
    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(new GraphBuilderV2(directoryPaths.get(0), Side.OURS, false));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(new GraphBuilderV2(directoryPaths.get(1), Side.BASE, false));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(new GraphBuilderV2(directoryPaths.get(2), Side.THEIRS, false));

    Stopwatch stopwatch = Stopwatch.createStarted();
    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

    stopwatch.stop();
    executorService.shutdown();
    long buildingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done building graphs.", buildingTime);

    Utils.prepareDir(outputPath);
    GraphMerger merger = new GraphMerger(outputPath, oursGraph, baseGraph, theirsGraph);

    // 2. Match nodes across the 3-way graphs.
    stopwatch.reset().start();
    Pair<List<Refactoring>, List<Refactoring>> refactorings = merger.threewayMap();
    stopwatch.stop();
    long matchingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done matching graphs.", matchingTime);

    // save the detected refactorings into csv for human validation and debugging
    String b2oCsvFilePath = outputPath + File.separator + "ours_refactorings.csv";
    String b2tCsvFilePath = outputPath + File.separator + "theirs_refactorings.csv";
    saveRefactorings(b2oCsvFilePath, refactorings.getLeft());
    saveRefactorings(b2tCsvFilePath, refactorings.getRight());

    // 3. Merge programs with the 3-way graphs, keeping the original format and directory structure
    stopwatch.reset().start();
    List<String> mergedFilePaths = merger.threewayMerge();
    stopwatch.stop();
    long mergingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done merging programs.", mergingTime);

    long overall = buildingTime + matchingTime + mergingTime;
    logger.info("Merged {} files. Overall time cost: {}ms.", mergedFilePaths.size(), overall);

    return mergedFilePaths;
  }

  /**
   * Merge directories in the order <left> <base> <right> and return time cost for each phase
   *
   * @param directoryPaths
   * @param outputPath
   * @param hasSubModule
   * @return
   * @throws Exception
   */
  public List<Long> mergeDirectories(
      List<String> directoryPaths, String outputPath, boolean hasSubModule) throws Exception {

    ExecutorService executorService = Executors.newFixedThreadPool(3);

    // 1. Build graphs from given directories
    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(new GraphBuilderV2(directoryPaths.get(0), Side.OURS, hasSubModule));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(new GraphBuilderV2(directoryPaths.get(1), Side.BASE, hasSubModule));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(
            new GraphBuilderV2(directoryPaths.get(2), Side.THEIRS, hasSubModule));

    Stopwatch stopwatch = Stopwatch.createStarted();
    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

    stopwatch.stop();
    executorService.shutdown();
    long buildingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done building graphs.", buildingTime);

    Utils.prepareDir(outputPath);
    GraphMerger merger = new GraphMerger(outputPath, oursGraph, baseGraph, theirsGraph);

    // 2. Match nodes across the 3-way graphs.
    stopwatch.reset().start();
    Pair<List<Refactoring>, List<Refactoring>> refactorings = merger.threewayMap();
    stopwatch.stop();
    long matchingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done matching graphs.", matchingTime);

    // save the detected refactorings into csv for human validation and debugging
    String b2oCsvFilePath = outputPath + File.separator + "ours_refactorings.csv";
    String b2tCsvFilePath = outputPath + File.separator + "theirs_refactorings.csv";
    saveRefactorings(b2oCsvFilePath, refactorings.getLeft());
    saveRefactorings(b2tCsvFilePath, refactorings.getRight());

    // 3. Merge programs with the 3-way graphs, keeping the original format and directory structure
    stopwatch.reset().start();
    List<String> mergedFilePaths = merger.threewayMerge();
    stopwatch.stop();
    long mergingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done merging programs.", mergingTime);

    long overall = buildingTime + matchingTime + mergingTime;
    logger.info("Merged {} files. Overall time cost: {}ms.", mergedFilePaths.size(), overall);

    List<Long> runtimes = new ArrayList<>();
    runtimes.add(buildingTime);
    runtimes.add(matchingTime);
    runtimes.add(mergingTime);
    runtimes.add(overall);
    return runtimes;
  }

  /**
   * Save alignment information on disk
   *
   * @param filePath
   */
  private void saveRefactorings(String filePath, List<Refactoring> refactorings) {
    if (!refactorings.isEmpty()) {
      Utils.writeContent(
          filePath,
          "refactoring_type;node_type;confidence;before_location;before_node;after_location;after_node\n",
          false);
      try {

        for (Refactoring refactoring : refactorings) {
          StringBuilder builder = new StringBuilder();
          builder.append(refactoring.getRefactoringType().getLabel()).append(";");
          builder.append(refactoring.getNodeType().asString()).append(";");
          BigDecimal bigDecimal = new BigDecimal(refactoring.getConfidence())
              .setScale(2, RoundingMode.UP);
//          builder.append((double) Math.round(refactoring.getConfidence() * 100) / 100).append(";");
          builder.append(bigDecimal.doubleValue()).append(";");
          builder.append(refactoring.getBeforeRange().begin.line).append("-");
          builder.append(refactoring.getBeforeRange().end.line).append(";");
          builder.append(refactoring.getBefore().getQualifiedName()).append(";");
          builder.append(refactoring.getAfterRange().begin.line).append("-");
          builder.append(refactoring.getAfterRange().end.line).append(";");
          builder.append(refactoring.getAfter().getQualifiedName()).append("\n");

          Utils.writeContent(filePath, builder.toString(), true);
        }
      } catch (RangeNullException e) {
        e.printStackTrace();
      }
    }
  }
}
