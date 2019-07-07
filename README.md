# IntelliMerge

#### IntelliMerge is a graph-based refactoring-aware three-way merging tool for Java projects with Git as the version control system.

Refactoring is a popular practice in object-oriented programming, especially with the support of automatic refactoring tools. However, it brings trouble to existing merging tools, both text-based ones (like Git) or tree-based ones (like jFSTMerge). On one hand, it tends to cause more merge conflicts that are hard to understand and resolve. On the other hand, merging tools are likely to generate incorrectly auto-merged results and thus introduce potential bugs.     

## Introduction

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
- Gradle 4.10.1

## Usage

### JAR Usage

Run the following command to use the jar:

```sh
java -jar IntelliMerge.jar [OPTIONS]
```

Available options:

```
-h,--help                    print this help message
-c,--clonepath <file>        directory to temporarily download repositories (default=projects)
-d,--dbproperties <file>     database properties file (default=database.properties)
```



### Input

- Merging branches: 

1. The absolute path of a cloned repository.

2. Names of the two branches to be merged, make sure they are local branches with the following command:

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

Merged Java files and a file with the alignment of program elements affected by refactorings, which will be consumed by a GUI client  * to assist the developer in manual conflict resolving.

> * under development

### Example

1. Merging branches:
2. Merging files:
3. Merging directories:

### API Usage

#### Input

#### Output

IntelliMerge provides the following APIs to use programmatically:

```java
mergeBranches()
```

```java
mergeDirectories()
```





### Example

1. Copy the jar file into `lib` folder of your project, and import it:

   - For Gradle projects: 

     1. Edit build.gradle to add the dependency:

     ```groovy
     dependencies {
         compile fileTree(dir: 'lib', include: ['IntelliMerge.jar'])
     }
     ```

     2. Refresh dependencies to take effect.

   - For Maven projects: 

     1. Edit pom.xml to add  the dependency:

     ```xml
      <executions>
          <execution>
              <id>make-assembly</id>
              <phase>initialize</phase>
              <goals>
                  <goal>install-file</goal>
              </goals>
              <configuration>
                  <groupId>edu.pku.InteliMerge</groupId>
                  <artifactId>intellimerge</artifactId>
                  <version>1.0</version>
                  <packaging>jar</packaging>
                  <file>lib/IntelliMerge.jar</file>
              </configuration>
          </execution>
     </executions>
     ...
     <dependencies>
         <dependency>
             <groupId>edu.pku.InteliMerge</groupId>
             <artifactId>intellimerge</artifactId>
             <version>1.0</version>
         </dependency>
     <dependencies>
     ```

     2. Refresh dependencies to take effect, with command `mvn initialize`.

   > Currently the lib is not published on the Maven Repository, so import it through the local jar file.

2. Import the client classes in your code:

   ```java
   import edu.pku.intellimerge.client.*;
   ```

3. Initialize an object to invoke API methods:

   ```java
   
   ```



## As a Developer

### Requirements

- Windows (Recommended) /Linux
- JDK 8
- Git 2.18.0
- Gradle 4.10.1
- MongoDB
- InteliJ IDEA (with Gradle integration)

### Environment Setup

1. Open the cloned repository as a project with InteliJ IDEA;

2. Download dependencies by clicking the refresh button in the Gradle tab of IDEA;

   ![gradle](https://github.com/Symbolk/IntelliMerge/blob/master/screenshots/gradle_refresh.png)

3. Run CLIClient.main() to see options available.

   [screenshot here]

### Build the JAR from source

Run the following command under the root of the cloned repository  to build the jar:

```sh
gradle build
```

### Project Structure

```
IntelliMerge   
   ├─client     
   ├─core
   │  ├─SemanticGraphBuilder
   │  ├─TwowayGraphMatcher
   │  └─ThreewayGraphMerger
   ├─evaluation 
   ├─exception  
   ├─io         
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