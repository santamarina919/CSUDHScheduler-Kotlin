
## What is this project
This project is an implementation of a course scheduler for California State University Dominguez
Hills. It includes the following features
### 1. Prerequisite Tracking
The application uses a graph data structure to track relationships between courses. This graph provides all 
the information needed to correctly determine whether a course can be taken during a certain semester. 
Users update the graph state by adding or removing courses from their schedule. This graph is constructed based on information from 
the CSUDH website. The website contained unstructured data listing prerequisites required for certain courses which was then transformed
into a graph data structure.  

#### 1a. How Nodes are structured in the graph
I will only discuss how course and prerequisite nodes in this section. There are other node types
but the explanation is not relevant here.  

Each course the university offers is turned into a graph node. Any information regarding id, name and units are 
turned into properties of the node. On the university website prerequisites are list in the following ways:  
CSC 300 Prerequisites: CSC101, CSC201, CSC301  
CSC 200 Prerequisites: MAT101 AND CSC311 OR CSC121 AND MAT211  
CSC 111 Prerequisites: MAT101 OR CSC115 OR CSC121  
For the first example, the comma between course ids is taken as an AND between them. 
Notice that each conjunction between prerequisites connects them. For example, to take CSC200 you either take 
MAT101 and CSC311 or you take the pair CSC121 and MAT211. Each conjunction between prerequisites is turned into a graph node which 
each contains the attribute AND or OR (the original conjunction between courses). The prerequisite node then is given an edge for 
each course it connects. For example, in the graph for CSC300 The successive ands between CSC101,201, and 301 are
turned into a single AND node. The AND node is then given an edge that points to CSC101,201 and 301. The course that the prerequisites belong to 
is designated as the root course and it is always connected to an AND node that then connects all other conjunctions that are listed.   
NOTE: some interpretation is needed to create the graph as the data provided by the CSUDH website could be ambiguous at times. Also, 
prerequisite nodes can point to other prerequisite nodes. 

Then how do we use the graph to schedule courses?

First, for each Prerequisite type we will define a function that belongs to each of them.  
The function that belongs to AND nodes has to ensure that ALL courses it has edges with are completed. It must also ensure all other nodes it has edges to are complete 
but with their respective function.

The function that belongs to OR nodes must ensure AT LEAST 1 course or requirement it has an edge with is completed.  

If an arbitrary course can have all of their prerequisite node's function return true, then the course can be taken. 



## How is it different from the previous version




## Ktor Features

A list of features included in from the Ktor framework

| Name                                                                   | Description                                                                        |
| ------------------------------------------------------------------------|------------------------------------------------------------------------------------ |
| [Routing](https://start.ktor.io/p/routing)                             | Provides a structured routing DSL                                                  |
| [Authentication](https://start.ktor.io/p/auth)                         | Provides extension point for handling the Authorization header                     |
| [kotlinx.serialization](https://start.ktor.io/p/kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library                     |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation)     | Provides automatic content conversion according to Content-Type and Accept headers |
| [Postgres](https://start.ktor.io/p/postgres)                           | Adds Postgres database to your application                                         |
| [Resources](https://start.ktor.io/p/resources)                         | Provides type-safe routing                                                         |
| [Sessions](https://start.ktor.io/p/ktor-sessions)                      | Adds support for persistent sessions through cookies or headers                    |
| [CORS](https://start.ktor.io/p/cors)                                   | Enables Cross-Origin Resource Sharing (CORS)                                       |
