package edu.pku.intellimerge.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;
import edu.pku.intellimerge.core.SemanticGraphBuilder2;
import edu.pku.intellimerge.core.ThreewayGraphMerger;
import edu.pku.intellimerge.exception.RangeNullException;
import edu.pku.intellimerge.io.SourceFileCollector;
import edu.pku.intellimerge.model.MergeScenario;
import edu.pku.intellimerge.model.SemanticEdge;
import edu.pku.intellimerge.model.SemanticNode;
import edu.pku.intellimerge.model.constant.Side;
import edu.pku.intellimerge.model.mapping.Refactoring;
import edu.pku.intellimerge.model.mapping.TwowayMatching;
import edu.pku.intellimerge.util.GitService;
import edu.pku.intellimerge.util.Utils;
import org.apache.log4j.PropertyConfigurator;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
      description = "Names of branches to be merged. The order should be ours, theirs.")
  List<String> branchNames = new ArrayList<String>();

  @Parameter(
      names = {"-d", "--directories"},
      arity = 3,
      description =
          "Absolute path of three directories, which contains Java files to be merged. The order should be ours, base, theirs.")
  List<String> directoryPaths = new ArrayList<>();

  @Parameter(
      names = {"-o", "--output"},
      arity = 1,
      description = "Output directory to save the merged files.")
  String outputPath = "";

  @Parameter(
      names = {"-t", "--threshold"},
      arity = 1,
      description = "[Optional] The threshold value for heuristic rules, default: 0.618.")
  String thresholdString = "0.618";

  private String folderPath;
  private List<String> fileRelativePaths;
  private String resultFolderPath;
  //  Double threshold = Double.valueOf(thresholdString);

  public IntelliMerge() {}

  public IntelliMerge(String folderPath, List<String> fileRelativePaths, String resultFolderPath) {
    this.folderPath = folderPath;
    this.fileRelativePaths = fileRelativePaths;
    this.resultFolderPath = resultFolderPath;
  }

  public static void main(String[] args) {
    // config the logger
    PropertyConfigurator.configure("log4j.properties");

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
      throw new ParameterException("Please specify ONE of the following options: -r, -d.");
    } else if (!merger.branchNames.isEmpty() && !merger.directoryPaths.isEmpty()) {
      throw new ParameterException("Please specify ONE of the following options: -r, -d.");
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
        mergeBranches(repoPath, branchNames, outputPath, true);

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
   * @param hasMultipleModule whether the repo has sub-modules
   * @throws Exception
   */
  public void mergeBranches(
      String repoPath, List<String> branchNames, String outputPath, boolean hasMultipleModule)
      throws Exception {

    // 1. Collect diff java files and imported files between ours/theirs commit and base commit

    // Collect source files to be analyzed in the system temp dir
    String collectedDir =
        System.getProperty("java.io.tmpdir") + File.separator + "IntelliMerge" + File.separator;

    SourceFileCollector collector = new SourceFileCollector(repoPath, branchNames, collectedDir);

    collector.collectFilesForAllSides();
    logger.info("Files collected in {}", collectedDir);

    // 2.1 Build graphs from collected files
    Stopwatch stopwatch = Stopwatch.createStarted();
    ExecutorService executorService = Executors.newFixedThreadPool(3);

    MergeScenario mergeScenario = collector.getMergeScenario();
    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(mergeScenario, Side.OURS, collectedDir, hasMultipleModule));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(mergeScenario, Side.BASE, collectedDir, hasMultipleModule));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(mergeScenario, Side.THEIRS, collectedDir, hasMultipleModule));
    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

    stopwatch.stop();
    logger.info("({}ms) Done building graphs.", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    executorService.shutdown();

    Utils.prepareDir(outputPath);
    ThreewayGraphMerger merger =
        new ThreewayGraphMerger(outputPath, oursGraph, baseGraph, theirsGraph);
    // 3. Match node and merge the 3-way graphs

    stopwatch.reset().start();
    merger.threewayMap();
    stopwatch.stop();

    logger.info("({}ms) Done matching graphs.", stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // 4. Print the merged graph into code, keep the original format as possible
    stopwatch.reset().start();
    merger.threewayMerge();
    stopwatch.stop();
    logger.info("({}ms) Done merging programs.", stopwatch.elapsed(TimeUnit.MILLISECONDS));

    // Clear and remove temp directory
    //    Utils.removeDir(collectedFileDir);
  }

  /**
   * Analyze all java files under the target directory, as if they are collected from merge scenario
   *
   * @return runtime at each phase in order
   * @throws Exception
   */
  public List<Long> mergeDirectories(List<String> directoryPaths, String outputPath)
      throws Exception {

    ExecutorService executorService = Executors.newFixedThreadPool(3);

    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.OURS, directoryPaths.get(0)));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.BASE, directoryPaths.get(1)));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.THEIRS, directoryPaths.get(2)));

    Stopwatch stopwatch = Stopwatch.createStarted();
    Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
    Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
    Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

    stopwatch.stop();
    executorService.shutdown();
    long buildingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done building graphs.", buildingTime);

    Utils.prepareDir(outputPath);
    ThreewayGraphMerger merger =
        new ThreewayGraphMerger(outputPath, oursGraph, baseGraph, theirsGraph);
    // 3. Match node and merge the 3-way graphs.
    stopwatch.reset().start();
    merger.threewayMap();
    stopwatch.stop();
    long matchingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done matching graphs.", matchingTime);

    // save the detected refactorings into csv for human validation and debugging
    String b2oCsvFilePath = outputPath + File.separator + "ours_refactorings.csv";
    String b2tCsvFilePath = outputPath + File.separator + "theirs_refactorings.csv";
    saveAlignment(b2oCsvFilePath, merger.b2oMatching);
    saveAlignment(b2tCsvFilePath, merger.b2tMatching);

    // 4. Print the merged graph into code, keep the original format as possible
    stopwatch.reset().start();
    List<String> mergedFilePaths = merger.threewayMerge();
    stopwatch.stop();
    long mergingTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    logger.info("({}ms) Done merging programs.", matchingTime);

    long overall = buildingTime + matchingTime + mergingTime;
    logger.info("Overall time cost: {}ms.", overall);

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
   * @param matching
   */
  private void saveAlignment(String filePath, TwowayMatching matching) {
    if (!matching.refactorings.isEmpty()) {
      Utils.writeContent(
          filePath,
          "refactoring_type;node_type;confidence;before_location;before_node;after_location;after_node\n",
          false);
      try {

        for (Refactoring refactoring : matching.refactorings) {
          StringBuilder builder = new StringBuilder();
          builder.append(refactoring.getRefactoringType().getLabel()).append(";");
          builder.append(refactoring.getNodeType().asString()).append(";");
          builder.append(refactoring.getConfidence()).append(";");
          builder.append(refactoring.getBeforeRange().begin.line).append("-");
          builder.append(refactoring.getBeforeRange().end.line).append(";");
          builder.append(refactoring.getBefore().getOriginalSignature()).append(";");
          builder.append(refactoring.getAfterRange().begin.line).append("-");
          builder.append(refactoring.getAfterRange().end.line).append(";");
          builder.append(refactoring.getAfter().getOriginalSignature()).append("\n");

          Utils.writeContent(filePath, builder.toString(), true);
        }
      } catch (RangeNullException e) {
        e.printStackTrace();
      }
    }
  }

  /** Merge a given list of files */
  public void merge() {
    ExecutorService executorService = Executors.newFixedThreadPool(3);

    Future<Graph<SemanticNode, SemanticEdge>> oursBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.OURS, folderPath, fileRelativePaths));
    Future<Graph<SemanticNode, SemanticEdge>> baseBuilder =
        executorService.submit(new SemanticGraphBuilder2(Side.BASE, folderPath, fileRelativePaths));
    Future<Graph<SemanticNode, SemanticEdge>> theirsBuilder =
        executorService.submit(
            new SemanticGraphBuilder2(Side.THEIRS, folderPath, fileRelativePaths));

    try {
      Graph<SemanticNode, SemanticEdge> oursGraph = oursBuilder.get();
      Graph<SemanticNode, SemanticEdge> baseGraph = baseBuilder.get();
      Graph<SemanticNode, SemanticEdge> theirsGraph = theirsBuilder.get();

      logger.info("Graph building done.");
      executorService.shutdown();

      Utils.prepareDir(resultFolderPath);
      ThreewayGraphMerger graphMerger =
          new ThreewayGraphMerger(resultFolderPath, oursGraph, baseGraph, theirsGraph);
      graphMerger.threewayMap();
      logger.info("Graph mapping done.");

      graphMerger.threewayMerge();
      logger.info("Graph merging done.");

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }
}
