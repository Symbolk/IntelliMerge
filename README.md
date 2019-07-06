# IntelliMerge

#### IntelliMerge is a graph-based refactoring-aware three-way merging tool for Java projects with Git as the version control system.

Refactoring is a popular practice in object-oriented programming, especially with the support of automatic refactoring tools. However, it brings trouble to existing merging tools, both text-based ones (like Git) or tree-based ones (like jFSTMerge). On one hand, it tends to cause more merge conflicts that are hard to understand and resolve. On the other hand, merging tools are likely to generate incorrectly auto-merged results and thus introduce potential bugs.     

## Introduction

- Three-way merging

When merging two branches, they are compared with their nearest common ancestor (NCA) in the commit history to determine what changes and what stays untouched. This scenario is called "three-way merging", which involves the two versions to be merged and the _base_ version at their NCA.    

- Graph-based

Instead of merging files one by one like most merging tools, IntelliMerge represents each version as graphs (named Program Element Graphs (PEG)) and performs merging by aligning the _program elements_ correctly according to their semantics. The vertex set of the PEG consists of _program elements_ of object-oriented programs (e.g. classes, methods, fields), while the edge set consists of the relations between program elements (e.g. method invocation, field access).
                                                                                                    
- Refactoring-aware

With the 3 program element graphs, IntelliMerge tries to align program elements involved in refactorings across them. Instead of detecting every refactoring types, IntelliMerge categories refactorings into 1-to-1 and m-to-n according to their effects, and employs a list of heuristic rules to align program elements before and after refactorings. 
                                                                                                                                                                                       
We choose this approach for the following reasons: (1) one program element might be involved in multiple refactorings and edits between the base version and the merging version, therefore the target of our work is not refactoring detection but program merging with enhanced ability in handling refactoring changes, (2) refactoring is a general summary of best practices that constantly changes over time, there are multiple versions of refactoring types (we follow the one proposed by Martin Fowler), our formulation supports refactorings in a broader sense.          

---

## Getting Started

## System requirements

- Windows/Linux
- JRE 8
- Git 2.18.0

## Usage

### Input


### Output


### API usage


## Example

## Development

## System requirements

- Windows/Linux
- JDK 8
- Git 2.18.0
- MongoDB
- Intelij IDEA


## Project Structure

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