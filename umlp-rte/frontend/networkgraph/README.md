# Graph Component

The standalone Graph Component, built on top of SigmaJS, facilitates the
creation of interactive graphs with enhanced data syntax options and features
like hover renderers, group filtering, and node search.

- [Graph Component](#graph-component)
  * [Features](#features)
  * [Required Libraries](#required-libraries)
  * [Typing / Available Syntax](#typing---available-syntax)
  * [Usage](#usage)
    + [Importing the Network Graph
      Component](#importing-the-network-graph-component)
    + [Utilizing the Network Graph
      Component](#utilizing-the-network-graph-component)
    + [Input/Output Properties](#input-output-properties)
    + [Extending Functionality](#extending-functionality)
  * [Additional Resources](#additional-resources)
  * [Detailed Explanation of supported Graph Types](#supported-data-types)
  * [Authors](#authors)

## Features

- Simplified graph creation with extended data syntax options compared to
  SigmaJS, including:
  - Use `from` or `source` for the source node and `to` or `target` for the
    target node.
  - Specify `id` or `key` for the node id (or leave it blank for an
    automatically generated `id`).
  - Utilize `group`, `category`, `type` or `cluster` for the node group.
- Enhanced hover renderers for displaying label, information, and optional
  neighboring node highlighting.
- Group filtering and node search capabilities for efficient data navigation.
- Comprehensive input and output events for interaction handling.
- Many additional configurable properties for a tailored user experience.

## Required Libraries

Tested with [Angular](https://angular.io/) (`v16.2.7`) and the following
dependencies:

```json
"dependencies": {
  "canvas": "^2.11.2",
  "graphology": "^0.25.4",
  "graphology-layout": "^0.6.1",
  "graphology-layout-force": "^0.2.4",
  "graphology-layout-forceatlas2": "^0.10.1",
  "graphology-layout-noverlap": "^0.4.2",
  "graphology-metrics": "^2.2.0",
  "primeicons": "^6.0.1",
  "sigma": "3.0.0-alpha3",
  // Utility dependencies that might be necessary when using the entire "shared" folder
  "xlsx": "^1.2.0",
  "dom-to-image": "^2.6.0",
  "lodash-es": "^4.17.0",
}
```

## Typing / Available Syntax

A comprehensive set of types and interfaces are provided to ensure robust
interactions with the Network Graph Component. Some key types include:

- `MetricValueType`: Represents a value that can be a number, string, or
  boolean.
- `CustomMetricsGenerator`: Function type to generate custom metrics.
- `RawNodeAttributes` and `RawEdgeAttributes`: Attributes directly sourced from
  raw data for nodes and edges respectively.
- `NodeAttributes` and `EdgeAttributes`: Processed attributes used for rendering
  or other operations, derived from `RawNodeAttributes` and `RawEdgeAttributes`
  respectively.
- And many more...

Import these types from the `./types/graph.types` module. Detailed descriptions
and typings can be found in the module.

```typescript
// Import example:
import { CustomMetricsGenerator, NodeAttributes, EdgeAttributes } from "./types/graph.types";
```

## Usage

### Importing the Network Graph Component

```typescript
import { NetworkGraphComponent } from 'path-to-network-graph-component';
```

### Utilizing the Network Graph Component

Insert `<network-graph></network-graph>` in the desired location within your
HTML file and bind the necessary inputs and outputs for your use case.

```html

<network-graph
  [nodes]="nodeData"
  [edges]="edgeData"
  [cameraState]="storedCameraStateToLoad"
  (nodeSelectionChange)="handleNodeSelectionChange($event)"
></network-graph>
```

### Input/Output Properties

The `NetworkGraphComponent` provides a variety of input properties for
configuring the graph, and output events for handling user interactions. These
include but are not limited to:

- Input Properties:
  - `showSearchNode`: Toggle the visibility of the `SearchNodeComponent`.
  - `showFilterGroup`: Toggle the visibility of the `FilterGroupComponent`.
  - `serializedGraph`: Serialized representation of the graph to be displayed.
  - And many more to look up within `NetworkGraphComponent` with extensive
    documentation...

- Output Events:
  - `nodeSelectionChange`: Event emitted when the selection of a node changes.
  - `graphClickNode`: Event emitted when a node on the graph is clicked.
  - `graphWheelNode`: Event emitted when the mouse wheel is used on a node on
    the graph.
  - And many more to look up within `NetworkGraphComponent` with extensive
    documentation...

A detailed list of all available input and output properties, along with their
descriptions, can be found within the `NetworkGraphComponent` class in the
provided code snippet.

### Extending Functionality

Extend the functionality of the `NetworkGraphComponent` by utilizing the various
services, like `GraphDataService`, `GraphManagerService`,
`GraphProcessorService`, and more, provided within the component.

## Additional Resources

- [SigmaJS Documentation](https://www.sigmajs.org/)
- [Graphology Documentation](https://graphology.github.io/)


# Supported Data Types
The following section provides a detailed explanation of the supported data
types for nodes and edges. The Graph Component supports both the original
Graphology data types and newly implemented data types. The newly implemented
data types are mapped to the original data types via the `NodeProcessorService`
and `EdgeProcessorService` respectively. The original data types are provided
for compatibility with Graphology and SigmaJS. The newly implemented data types
are provided for convenience and to simplify the creation of graphs.

## Node Data
In the following first a list of all supported node data types is provided,
followed by an example definition of some nodes.

### Supported Node Data Types
The following table provides an overview of both original and newly implemented
properties for node definitions. These properties are mapped via the
NodeProcessorService.

| Category     | Original Property | Mapped Properties       |
|--------------|-------------------|-------------------------|
| Graphology   | key*              | key, id, or none        |
| SigmaJS      | color             | color                   |
|              | forceLabel        | forceLabel              |
|              | highlighted       | highlighted             |
|              | hidden            | hidden                  |
|              | label             | label, name             |
|              | size              | size                    |
|              | type              | type                    |
|              | x                 | x                       |
|              | y                 | y                       |
|              | zIndex            | zIndex                  |
| Custom       | group             | group, cluster, type, tag|
|              | image             | image                   |
|              | tooltip           | tooltip, hover, hoverLabel|
|              | type              | nodeProgram (circle, image) |
(* required property)


### Example Node Definition
```json
[{ 
  "id": "1", 
  "x": 0.001, 
  "attributes": { 
    "label": "CEO", 
    "tooltip": "Chief Executive Officer",
    "group": "Management",
    "size": 100 
  } 
}, 
{ 
  "id": "2", 
  "label": "CTO", 
  "tooltip": "Chief Technology Officer", 
  "group": "Tech Leadership", 
  "size": 50 
}, 
{ 
  "size": 50, 
  "name": "HR Manager", 
  "attributes": { 
    "label": "HRM", 
    "tooltip": "Human Resources Manager", 
    "group": "HR", 
    "size": 50 
  } 
}]
```


## Edge Data
In the following first a list of all supported edge data types is provided,
followed by an example definition of some edges.

### Supported Edge Data Types
The following table outlines the original and newly implemented properties for
edge definitions. These properties are mapped via the EdgeProcessorService.

| Category     | Original Property | Mapped Properties       |
|--------------|-------------------|-------------------------|
| Graphology   | key               | key, id, or none        |
|              | source*           | source, from           |
|              | target*           | target, to             |
|              | undirected        | directed, undirected   |
| SigmaJS      | color             | color                   |
|              | forceLabel        | forceLabel              |
|              | hidden            | hidden                  |
|              | label             | label, name             |
|              | size              | weight, size            |
|              | type              | type                    |
|              | zIndex            | zIndex                  |
| Custom       | group             | (automatically determined)|
|              | tooltip           | tooltip, hover, hoverLabel|
|              | type              | edgeProgram (line, arrow, fastLine) |
(* required property)



### Example Edge Definition
```json
[{ 
  "source": 1, 
  "target": 2,
  "attributes": { 
    "label": "Directs" 
  } 
}, 
{ 
  "source": 2, 
  "target": "node_auto_id_0", 
  "attributes": { 
    "label": "Collaborates With"
  } 
}, 
{ 
  "source": "node_auto_id_0", 
  "target": "1", 
  "attributes": { 
    "weight": 20, 
    "label": "Reports To"
  }
}]
```

# Authors

- Niklas Büchel

See also the list of
[contributors](https://git.rwth-aachen.de/monticore/artifacts/artviz/artviz-dashboard/-/graphs/main?ref_type=heads)
who participated in this project.
