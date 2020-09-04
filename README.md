# IntelliMerge

> Notice: This repo is now maintained independently and privately as an industrial project since 2020, which means no updates for the open source version.

#### IntelliMerge is a graph-based refactoring-aware three-way merging tool for Java programs and repositories.

## Introduction

Refactoring is a popular practice in object-oriented programming, especially with the support of automatic refactoring tools. However, it brings trouble to existing merging tools, both text-based ones (like git-merge) or tree-based ones (like jFSTMerge). On one hand, it tends to cause more merge conflicts that are hard to understand and resolve. On the other hand, merging tools are likely to generate incorrectly auto-merged results and thus introduce potential bugs. 

Therefore, we implement IntelliMerge, a a graph-based refactoring-aware three-way merging tool for Java programs and repositories. It can reduce the number of false positive conflicts comparing with git-merge and jFSTMerge without sacrificing the precision of auto-merging parts. Besides, by representing programs as a graph, it allows for building interesting applications that consume the intermediate data, for example, a GUI client to assist developers in manually resolving conflicts, which can visualize refactoring changes and connections between conflict blocks.

- Three-way merging

When merging two branches, they are compared with their nearest common ancestor (NCA) in the commit history to determine what changes and what stays untouched. This scenario is called "three-way merging", which involves the two versions to be merged and the _base_ version at their NCA.    

- Graph-based

Instead of merging files one by one like most merging tools, IntelliMerge represents each version as graphs (named Program Element Graphs (PEG)) and performs merging by aligning the _program elements_ correctly according to their semantics. The vertex set of the PEG consists of _program elements_ of object-oriented programs (e.g. classes, methods, fields), while the edge set consists of the relations between program elements (e.g. method invocation, field access).
​                                                                                                    

- Refactoring-aware

With the 3 program element graphs, IntelliMerge tries to align program elements involved in refactorings across them. Instead of detecting every refactoring types, IntelliMerge categories refactorings into 1-to-1 and m-to-n according to their effects, and employs a list of heuristic rules to align program elements before and after refactorings. 
​                                                                                                                                                                                       
We choose this approach for the following reasons: (1) one program element might be involved in multiple refactorings and edits between the base version and the merging version, therefore the target of our work is not refactoring detection but program merging with enhanced ability in handling refactoring changes, (2) refactoring is a general summary of best practices that constantly changes over time, there are multiple versions of refactoring types (we follow the one proposed by Martin Fowler), our formulation supports refactorings in a broader sense.          

---

## As a User

## Requirements

- Windows (Recommended) /Linux
- Java 8
- Git 2.18.0

## Usage

### JAR Usage

Download the latest jar from: https://github.com/Symbolk/IntelliMerge/releases, and run the following command to use it:

```sh
java -jar IntelliMerge-VERSION.jar [OPTIONS]
```

Available options will be printed if no options are given:

```bash
Please specify ONE of the following options: -r, -d.
Usage: IntelliMerge [options]
  Options:
    -b, --branches
      Names of branches to be merged. The order should be <left> <right> to
      merge <right> branch to <left>.
      Default: []
    -d, --directories
      Absolute path of three directories with Java files inside to be merged.
      The order should be <left> <base> <right>.
      Default: []
    -s, --hasSubModule
      Whether the repository has sub module.
      Default: true
    -o, --output
      Absolute path of an empty directory to save the merging results.
      Default: <empty string>
    -r, --repo
      Absolute path of the target Git repository.
      Default: <empty string>
    -t, --threshold
      [Optional] The threshold value for heuristic rules, default: 0.618.
      Default: 0.618
```

### Input

- Merging branches: 

1. The absolute path of a cloned repository.

2. Names of the two **local** branches to be merged, make sure they are local branches with the following command:

   ```bash
   D:\github\repos\fastjson (android -> origin)
   $ git branch
   * android
     master
   ```

- Merging directories: 

1. The absolute paths of 3 directories that contains Java files to be merged, in the order `ours base theirs`.

2. The absolute path of the output directory.


### Output

1. Merged Java files.

2. A csv file with the alignment information of program elements affected by refactorings, which can be consumed by the [IntelliMerge-UI] (under development) for developers to verify resolved conflicts and manually resolve remained conflicts.

   Example:

```json
refactoring_type;node_type;confidence;before_location;before_node;after_location;after_node
Change Method Signature;method;0.8035;9-11;String getDirector();23-25;String getDirector()
Change Method Signature;method;0.9747;13-15;void setDirector(String director);27-29;void setDirector(String director)
Change Method Signature;method;0.8065;15-20;int getFrequentRenterPoints(int daysRented);15-20;int getPointsOfFrequentRenters(int daysRented)
Change Method Signature;method;0.8027;8-10;int getFrequentRenterPoints(int daysRented);8-10;int getPointsOfFrequentRenters(int daysRented)
Change Field Signature;field;1.0;4-4;private String director;10-10;public String director
```

> [IntelliMerge-UI]: https://github.com/Symbolk/IntelliMerge-UI

### Example

We provide a sample repository as the example input data, so you can have a quick try.

1. Clone the sample repository from: https://github.com/Symbolk/intellimerge-sample-input, suppose that  it is cloned into: `D:\github\intellimerge-sample-input\`

2. Checkout local branches from remote ones with the following command under the cloned repository:
```
git checkout ours
git checkout theirs
```

3. Merge branches with the following command:

   ```bash
   java -jar IntelliMerge-VERSION.jar -r D:\github\intellimerge-sample-input -s true -b ours theirs -o D:\github\intellimerge-sample-input\result1
   ```

   > Remember to replace the arguments with the path where you clone the sample repository into.

4. Merge directories with the following command:

   ```bash
   git checkout master
   java -jar IntelliMerge-VERSION.jar -d D:\github\intellimerge-sample-input\src\main\java\bad\robot\refactoring\left D:\github\intellimerge-sample-input\src\main\java\bad\robot\refactoring\base D:\github\intellimerge-sample-input\src\main\java\bad\robot\refactoring\right -o D:\github\intellimerge-sample-input\result2
   ```

### API Usage

IntelliMerge provides the following APIs to use programmatically:

```java
List<String> mergeBranches(
      String repoPath, List<String> branchNames, String outputPath, boolean hasSubModule)
```

Merge two local branches of a Git repository.

#### Parameters

1. repoPath: Absolute path of the target Git repository.
2. branchNames: Names of two **local** branches to be merged. The order should be \<left\> \<right\> to merge \<right\> branch to \<left\>.
3. outputPath: Absolute path of an empty directory to save the merging results.
4. hasSubModule: Whether the Git repository has submodules.

#### Return Value

File paths of the merging results.



```java
List<String> mergeDirectories(List<String> directoryPaths, String outputPath)
```

Merge three directories that contains Java files.

#### Parameters

1. directoryPaths: Absolute paths of three directories with Java files inside to be merged. The order should be \<left\> \<base\> \<right\>.
2. outputPath: Absolute path of an empty directory to save the merging results.

#### Return Value

File paths of the merging results.



### Example

We provide two sample projects to demonstrate the API usage, which can serve as the scaffold to build applications upon:

- With Gradle: https://github.com/Symbolk/intellimerge-quickstart-gradle

- With Maven: https://github.com/Symbolk/intellimerge-quickstart-maven

Sample code snippets about API usage:

```java
// 1. merging branches
IntelliMerge merger = new IntelliMerge();
String outputPath = repoPath + "/results1";
boolean hasSubModule = false;
List<String> branchNames = new ArrayList<>();
branchNames.add("ours");
branchNames.add("theirs");

try {
    List<String> resultFilePaths = merger.mergeBranches(repoPath, branchNames, outputPath, hasSubModule);
    System.out.println("Merging results:");
    for (String path : resultFilePaths) {
        System.out.println(path);
    }
} catch (Exception e) {
    e.printStackTrace();
}
```

```java
// 2. merging directories
IntelliMerge merger = new IntelliMerge();
List<String> directoryPaths = new ArrayList<>();
String outputPath = repoPath + "/results2";

directoryPaths.add(repoPath + "/src/main/java/bad/robot/refactoring/left");
directoryPaths.add(repoPath + "/src/main/java/bad/robot/refactoring/base");
directoryPaths.add(repoPath + "/src/main/java/bad/robot/refactoring/right");
try {
    List<String> resultFilePaths = merger.mergeDirectories(directoryPaths, outputPath);
    System.out.println("Merging results:");
    for (String path : resultFilePaths) {
        System.out.println(path);
    }
} catch (Exception e) {
    e.printStackTrace();
}
```



## As a Developer

### Requirements

- Windows (Recommended) /Linux
- JDK 8
- Git 2.18.0
- Gradle 4.10.1
- IntelliJ IDEA (with Gradle integration)
- MongoDB (only when performing evaluation)

### Environment Setup

1. Open the cloned repository as a project with IntelliJ IDEA;

2. Download dependencies by clicking the refresh button in the Gradle tab of IDEA;

   ![gradle](https://github.com/Symbolk/IntelliMerge/blob/master/screenshots/gradle_refresh.png)

3. Run `IntelliMerge.main()` to see options available.

   ```bash
   Usage: IntelliMerge [options]
     Options:
       -b, --branches
         Names of branches to be merged. The order should be <left> <right> to
         merge <right> branch to <left>.
         Default: []
       -d, --directories
         Absolute path of three directories with Java files inside to be merged.
         The order should be <left> <base> <right>.
         Default: []
       -o, --output
         Absolute path of an empty directory to save the merging results.
         Default: <empty string>
       -r, --repo
         Absolute path of the target Git repository.
         Default: <empty string>
       -t, --threshold
         [Optional] The threshold value for heuristic rules, default: 0.618.
         Default: 0.618
   ```

### Build the JAR from source

Run the following command under the root of the cloned repository to build an executable jar with all dependencies packaged:

```sh
gradle fatJar
```

Packaged jar file will be generated in `build\libs`, with the name `IntelliMerge-VERSION-all.jar`.

### Project Structure

```
IntelliMerge   
   ├─client     
   ├─core
   │  ├─GraphBuilder
   │  ├─GraphMatcher
   │  └─GraphMerger
   ├─evaluation 
   ├─exception  
   ├─io
   │  └─GraphExporter         
   ├─model      
   │  ├─constant
   │     ├─EdgeType
   │     └─NodeType
   │  ├─mapping 
   │  └─node
   │     ├─SemanticNode
   │     └─SemanticEdge      
   └─util
       ├─GitService
       └─SimilarityAlg
          
```

> P.S. Major components are listed.
