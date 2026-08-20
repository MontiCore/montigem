import { AbstractGraph }  from "graphology-types";

export type HierarchicalLayoutOptionsInterface = {
  edgeLabels : string[],
  edgeGroups : string[]
  direction?: "vertical" | "horizontal"
  useDummyNodes?: boolean
}

type ReversedEdgeInfo = { originalKey: string, reversedKey: string };
type SelfLoopInfo = { key: string, source: string, attributes: Record<string, any> };
type StoredEdgeInfo = { key: string; source: string; target: string; attributes: Record<string, any> };

export class HierarchicalLayout  {

  protected levelSeparation : number = 150;
  protected nodeSeparation : number = 100;
  protected dummyPathSeparation : number = 40;

  /*
  * Step 1:
  * Remove self-loops and reverse DFS back edges so rank assignment can run on a DAG
  */
  private removeCycles(g: AbstractGraph): { reversedEdges: ReversedEdgeInfo[]; selfLoops: SelfLoopInfo[] } {
    const selfLoops: SelfLoopInfo[] = [];
    const selfLoopKeys: string[] = [];

    g.forEachEdge(edgeKey => {
      if (g.source(edgeKey) === g.target(edgeKey)) {
        selfLoops.push({ key: edgeKey, source: g.source(edgeKey), attributes: { ...g.getEdgeAttributes(edgeKey) } });
        selfLoopKeys.push(edgeKey);
      }
    });
    for (const edgeKey of selfLoopKeys) {
      g.dropEdge(edgeKey);
    }

    const reversedEdges: ReversedEdgeInfo[] = [];
    const leftOrder: string[] = [];
    const rightOrder: string[] = [];
    const remainingNodes = new Set<string>(g.nodes());

    const inDeg = new Map<string, number>();
    const outDeg = new Map<string, number>();
    for (const n of remainingNodes) { inDeg.set(n, 0); outDeg.set(n, 0); }
    g.forEachEdge(edgeKey => {
      const s = g.source(edgeKey), t = g.target(edgeKey);
      outDeg.set(s, (outDeg.get(s) ?? 0) + 1);
      inDeg.set(t, (inDeg.get(t) ?? 0) + 1);
    });

    const removeNode = (nodeKey: string) => {
      remainingNodes.delete(nodeKey);
      for (const edgeKey of g.outboundEdges(nodeKey) ?? []) {
        const t = g.target(edgeKey);
        if (remainingNodes.has(t)) inDeg.set(t, (inDeg.get(t) ?? 1) - 1);
      }
      for (const edgeKey of g.inboundEdges(nodeKey) ?? []) {
        const s = g.source(edgeKey);
        if (remainingNodes.has(s)) outDeg.set(s, (outDeg.get(s) ?? 1) - 1);
      }
    };

    while (remainingNodes.size > 0) {
      let changed = true;
      while (changed) {
        changed = false;
        for (const nodeKey of Array.from(remainingNodes)) {
          if (outDeg.get(nodeKey) === 0) { rightOrder.push(nodeKey); removeNode(nodeKey); changed = true; }
        }
        for (const nodeKey of Array.from(remainingNodes)) {
          if (inDeg.get(nodeKey) === 0) { leftOrder.push(nodeKey); removeNode(nodeKey); changed = true; }
        }
      }

      if (remainingNodes.size === 0) break;

      let bestNode: string = null;
      let bestScore = Number.NEGATIVE_INFINITY;
      for (const nodeKey of remainingNodes) {
        const score = (outDeg.get(nodeKey) ?? 0) - (inDeg.get(nodeKey) ?? 0);
        if (score > bestScore) { bestScore = score; bestNode = nodeKey; }
      }
      leftOrder.push(bestNode);
      removeNode(bestNode);
    }

    const nodeOrder = leftOrder.concat(rightOrder.reverse());
    const positions = new Map<string, number>();
    nodeOrder.forEach((nodeKey, index) => positions.set(nodeKey, index));

    for (const edgeKey of Array.from(g.edges())) {
      const source = g.source(edgeKey);
      const target = g.target(edgeKey);
      if ((positions.get(source) ?? 0) <= (positions.get(target) ?? 0)) {
        continue;
      }

      const attributes = { ...g.getEdgeAttributes(edgeKey) };
      g.dropEdge(edgeKey);
      let reversedKey = `rev_${edgeKey}`;
      let suffix = 0;
      while (g.hasEdge(reversedKey)) {
        suffix += 1;
        reversedKey = `rev_${edgeKey}_${suffix}`;
      }
      g.addEdgeWithKey(reversedKey, target, source, attributes);
      reversedEdges.push({ originalKey: edgeKey, reversedKey });
    }

    return { reversedEdges, selfLoops };
  }

  // Restores the original graph semantics after layout
  private restoreCycles(g: AbstractGraph, reversedEdges: ReversedEdgeInfo[], selfLoops: SelfLoopInfo[]): void {
    for (const edgeInfo of reversedEdges) {
      if (g.hasEdge(edgeInfo.reversedKey)) {
        const source = g.source(edgeInfo.reversedKey);
        const target = g.target(edgeInfo.reversedKey);
        const attributes = { ...g.getEdgeAttributes(edgeInfo.reversedKey) };
        g.dropEdge(edgeInfo.reversedKey);
        g.addEdgeWithKey(edgeInfo.originalKey, target, source, attributes);
      }
    }

    for (const loop of selfLoops) {
      g.addEdgeWithKey(loop.key, loop.source, loop.source, loop.attributes);
    }
  }

  // Replaces long-span edges with chains of dummy nodes, one per intermediate layer.
  private insertDummyNodes(g: AbstractGraph, levelNodes: string[][], rankMinimum: number): StoredEdgeInfo[] {
    const originalEdges: StoredEdgeInfo[] = [];
    const edgesToProcess: string[] = [];

    g.forEachEdge(edgeKey => {
      const sourceRank = g.getNodeAttribute(g.source(edgeKey), "rank");
      const targetRank = g.getNodeAttribute(g.target(edgeKey), "rank");
      if (Math.abs(targetRank - sourceRank) > 1) {
        edgesToProcess.push(edgeKey);
      }
    });

    let dummyCounter = 0;
    for (const edgeKey of edgesToProcess) {
      const source = g.source(edgeKey);
      const target = g.target(edgeKey);
      const sourceRank = g.getNodeAttribute(source, "rank");
      const targetRank = g.getNodeAttribute(target, "rank");
      const edgeAttributes = g.getEdgeAttributes(edgeKey);

      const chainId = `chain_${dummyCounter}`;
      originalEdges.push({ key: edgeKey, source, target, attributes: { ...edgeAttributes } });

      g.dropEdge(edgeKey);

      const step = targetRank > sourceRank ? 1 : -1;
      let previousNode = source;

      for (let rank = sourceRank + step; rank !== targetRank; rank += step) {
        const dummyKey = `dummy_${dummyCounter++}`;
        g.addNode(dummyKey, { rank, isDummy: true, chainId, originalEdgeKey: edgeKey, size: 0, x: 0, y: 0 });

        const layerIndex = rankMinimum < 0 ? rank - rankMinimum : rank;
        while (levelNodes.length <= layerIndex) {
          levelNodes.push([]);
        }
        levelNodes[layerIndex].push(dummyKey);

        g.addEdge(previousNode, dummyKey, { minlen: 1 });
        previousNode = dummyKey;
      }

      g.addEdge(previousNode, target, { minlen: 1 });
    }

    return originalEdges;
  }

  // Drops dummy chains and restores original edges, attaching waypoints for polyline routing.
  private removeDummyNodes(g: AbstractGraph, originalEdges: StoredEdgeInfo[], levelNodes: string[][]): void {
    const waypointsByEdge = new Map<string, { x: number; y: number; rank: number }[]>();
    for (const nodeKey of g.nodes()) {
      if (!g.getNodeAttribute(nodeKey, "isDummy")) continue;
      const origKey = g.getNodeAttribute(nodeKey, "originalEdgeKey") as string;
      if (!origKey) continue;
      if (!waypointsByEdge.has(origKey)) waypointsByEdge.set(origKey, []);
      waypointsByEdge.get(origKey)!.push({
        x: g.getNodeAttribute(nodeKey, "x") as number,
        y: g.getNodeAttribute(nodeKey, "y") as number,
        rank: g.getNodeAttribute(nodeKey, "rank") as number,
      });
    }
    for (const wps of waypointsByEdge.values()) wps.sort((a, b) => a.rank - b.rank);

    const dummyNodes = new Set(g.nodes().filter(nodeKey => g.getNodeAttribute(nodeKey, "isDummy")));
    for (const dummyKey of dummyNodes) g.dropNode(dummyKey);

    for (let i = 0; i < levelNodes.length; i++) {
      levelNodes[i] = levelNodes[i].filter(nodeKey => !dummyNodes.has(nodeKey));
    }

    for (const edge of originalEdges) {
      if (g.hasEdge(edge.key)) continue;
      const wps = (waypointsByEdge.get(edge.key) ?? []).map(({ x, y }) => ({ x, y }));
      g.addEdgeWithKey(edge.key, edge.source, edge.target, {
        ...edge.attributes,
        ...(wps.length > 0 ? { waypoints: wps, type: "polyline" } : {}),
      });
    }
  }


  // Reorders one layer by 2-sided barycenter; isolated nodes sort to the end.
  private orderLayer(currentLayer: string[], prevLayer: string[] | null, nextLayer: string[] | null, g: AbstractGraph): string[] {
    const prevPos = prevLayer ? new Map(prevLayer.map((k, i) => [k, i])) : null;
    const nextPos = nextLayer ? new Map(nextLayer.map((k, i) => [k, i])) : null;

    const bary = currentLayer.map((nodeKey, origIdx) => {
      const positions: number[] = [];
      if (prevPos) {
        for (const edgeKey of g.inboundEdges(nodeKey) ?? []) {
          const idx = prevPos.get(g.source(edgeKey));
          if (idx !== undefined) positions.push(idx);
        }
      }
      if (nextPos) {
        for (const edgeKey of g.outboundEdges(nodeKey) ?? []) {
          const idx = nextPos.get(g.target(edgeKey));
          if (idx !== undefined) positions.push(idx);
        }
      }
      const value = positions.length > 0
        ? positions.reduce((a, b) => a + b, 0) / positions.length
        : Number.POSITIVE_INFINITY;
      return { nodeKey, value, origIdx };
    });

    bary.sort((a, b) => (a.value - b.value) || (a.origIdx - b.origIdx));
    return bary.map(e => e.nodeKey);
  }

  // Pre-sorts dummies toward the outer edge of their layer, proportional to chain span.
  private initializeDummyOrder(levelNodes: string[][], originalEdges: StoredEdgeInfo[], g: AbstractGraph, rankMinimum: number): void {
    const totalLayers = levelNodes.length;
    const maxSpan = totalLayers > 1 ? totalLayers - 1 : 1;
    const dummyToFraction = new Map<string, number>();

    const dummiesByEdge = new Map<string, string[]>();
    for (const layer of levelNodes) {
      for (const k of layer) {
        if (!g.getNodeAttribute(k, "isDummy")) continue;
        const origKey = g.getNodeAttribute(k, "originalEdgeKey") as string;
        if (!origKey) continue;
        if (!dummiesByEdge.has(origKey)) dummiesByEdge.set(origKey, []);
        dummiesByEdge.get(origKey)!.push(k);
      }
    }

    for (const edge of originalEdges) {
      if (!g.hasNode(edge.source)) continue;
      const sourceRank = g.getNodeAttribute(edge.source, "rank") as number;
      const layerIdx = rankMinimum < 0 ? sourceRank - rankMinimum : sourceRank;
      const sourceLayer = levelNodes[layerIdx];
      if (!sourceLayer) continue;
      const realInSource = sourceLayer.filter(k => !g.getNodeAttribute(k, "isDummy"));
      const pos = realInSource.indexOf(edge.source);
      const sourceFraction = realInSource.length > 1 ? pos / (realInSource.length - 1) : 0.5;

      const dummies = dummiesByEdge.get(edge.key) ?? [];
      const spanFactor = dummies.length / maxSpan;
      const nearestExtreme = sourceFraction <= 0.5 ? 0 : 1;
      const fraction = sourceFraction * (1 - spanFactor) + nearestExtreme * spanFactor;

      for (const dummy of dummies) {
        dummyToFraction.set(dummy, fraction);
      }
    }

    for (const layer of levelNodes) {
      const real = layer.filter(k => !g.getNodeAttribute(k, "isDummy"));
      const dummies = layer.filter(k => g.getNodeAttribute(k, "isDummy"));
      if (dummies.length === 0) continue;

      const withPos: { key: string; pos: number }[] = [];
      real.forEach((k, i) => withPos.push({ key: k, pos: real.length > 1 ? i / (real.length - 1) : 0.5 }));
      dummies.forEach(k => withPos.push({ key: k, pos: dummyToFraction.get(k) ?? 0.5 }));
      withPos.sort((a, b) => a.pos - b.pos);

      layer.length = 0;
      layer.push(...withPos.map(e => e.key));
    }
  }

  // Iterative forward/backward barycenter sweeps; routes long-span chains to the outer side.
  private minimizeCrossings(levelNodes: string[][], g: AbstractGraph, maxIterations?: number): void {
    const defaultMax = Math.max(4, 2 * levelNodes.length);
    const effectiveMaxIterations = maxIterations ?? defaultMax;
    const maxNoImprovementSteps = 100;
    let bestCrossings = this.countAllCrossings(levelNodes, g);
    let bestOrder = levelNodes.map(layer => [...layer]);
    let currentOrder = bestOrder.map(layer => [...layer]);
    let noImprovementSteps = 0;
    let iteration = 0;

    const chainNodeAtLayer = new Map<string, Map<number, string>>();
    for (let li = 0; li < levelNodes.length; li++) {
      for (const k of levelNodes[li]) {
        if (!g.getNodeAttribute(k, "isDummy")) continue;
        const cid = g.getNodeAttribute(k, "chainId") as string;
        if (!cid) continue;
        if (!chainNodeAtLayer.has(cid)) chainNodeAtLayer.set(cid, new Map());
        chainNodeAtLayer.get(cid)!.set(li, k);
      }
    }

    const tryChainAtSide = (order: string[][], chainId: string, side: "left" | "right"): string[][] => {
      const result = order.map(layer => [...layer]);
      const perLayer = chainNodeAtLayer.get(chainId);
      if (!perLayer) return result;
      for (const [li, nodeKey] of perLayer.entries()) {
        const layer = result[li];
        const idx = layer.indexOf(nodeKey);
        if (idx < 0) continue;
        const node = layer.splice(idx, 1)[0];
        if (side === "left") layer.unshift(node);
        else layer.push(node);
      }
      return result;
    };

    while (iteration < effectiveMaxIterations && noImprovementSteps <= maxNoImprovementSteps) {
      let candidate = currentOrder.map(layer => [...layer]);

      // Forward sweep
      for (let i = 0; i < candidate.length; i++) {
        candidate[i] = this.orderLayer(
          candidate[i],
          i > 0 ? candidate[i - 1] : null,
          i < candidate.length - 1 ? candidate[i + 1] : null,
          g,
        );
      }

      // Backward sweep
      for (let i = candidate.length - 1; i >= 0; i--) {
        candidate[i] = this.orderLayer(
          candidate[i],
          i > 0 ? candidate[i - 1] : null,
          i < candidate.length - 1 ? candidate[i + 1] : null,
          g,
        );
      }

      let currentC = this.countAllCrossings(candidate, g);
      for (const chainId of chainNodeAtLayer.keys()) {
        const left = tryChainAtSide(candidate, chainId, "left");
        const right = tryChainAtSide(candidate, chainId, "right");
        const leftC = this.countAllCrossings(left, g);
        const rightC = this.countAllCrossings(right, g);
        if (leftC < currentC && leftC <= rightC) { candidate = left; currentC = leftC; }
        else if (rightC < currentC) { candidate = right; currentC = rightC; }
      }

      const candidateCrossings = currentC;
      if (candidateCrossings < bestCrossings) {
        bestCrossings = candidateCrossings;
        bestOrder = candidate;
        currentOrder = candidate;
        noImprovementSteps = 0;
      } else if (candidateCrossings === bestCrossings) {
        currentOrder = candidate;
        noImprovementSteps++;
      } else {
        noImprovementSteps++;
      }
      iteration++;
    }

    for (let i = 0; i < levelNodes.length; i++) {
      levelNodes[i] = bestOrder[i];
    }
  }

  private countCrossings(layer1: string[], layer2: string[], g: AbstractGraph): number {
    const pos2Map = new Map(layer2.map((k, i) => [k, i]));
    const edges: { pos1: number, pos2: number }[] = [];

    for (let pos1 = 0; pos1 < layer1.length; pos1++) {
      const outgoing = g.outboundEdges(layer1[pos1]);
      if (!outgoing) continue;
      for (const edgeKey of outgoing) {
        const pos2 = pos2Map.get(g.target(edgeKey));
        if (pos2 !== undefined) edges.push({ pos1, pos2 });
      }
    }

    let crossings = 0;
    for (let i = 0; i < edges.length; i++) {
      for (let j = i + 1; j < edges.length; j++) {
        if ((edges[i].pos1 - edges[j].pos1) * (edges[i].pos2 - edges[j].pos2) < 0) {
          crossings++;
        }
      }
    }
    return crossings;
  }

  private countAllCrossings(levelNodes: string[][], g: AbstractGraph): number {
    let total = 0;
    for (let i = 0; i < levelNodes.length - 1; i++) {
      total += this.countCrossings(levelNodes[i], levelNodes[i + 1], g);
    }
    return total;
  }

  public tightTree(graph: AbstractGraph, relevantEdgeLabels? : string[], relevantEdgeGroups?: string[]) {
    // Choose arbitrary node from which to start our tree
    var start = graph.nodes()[0];
    var size = graph.nodes().length;

    const tree : string[] = []
    tree.push(start)

    var edge, delta;
    while (this.tightTreeHelper(tree, graph) < size) {
      edge = this.findMinSlackEdge(tree, graph);
      delta = edge != null && tree.includes(graph.source(edge)) ? HierarchicalLayout.slack(graph, edge) : -HierarchicalLayout.slack(graph, edge);
      this.shiftRanks(tree, graph, delta);
    }
    return graph;
  }

   /*
   * Finds a maximal tree of tight edges and returns the number of nodes in the
   * tree.
   */
  public tightTreeHelper(t : string[], g : AbstractGraph) {

    function dfs(nodeKey : string) {
      g.edges(nodeKey).forEach(edgeKey => {
        var edgeSource = g.source(edgeKey),
          edgeTarget = (nodeKey === edgeSource) ? g.target(edgeKey) : edgeSource;
        if (!t.includes(edgeTarget) && !HierarchicalLayout.slack(g, edgeKey)) {
          t.push(edgeTarget);
          dfs(edgeTarget);
        }
      });
    }

    t.forEach(dfs, this);
    return t.length;
  }

  public findMinSlackEdge(treeNodes : string[], g : AbstractGraph) {
    //console.warn("Treenodes: ",treeNodes)

    let minSlack = Number.POSITIVE_INFINITY;
    let minSlackEdge : string = null;
    g.forEachEdge(edgeKey => {
      if(treeNodes.includes(g.source(edgeKey)) !== treeNodes.includes(g.target(edgeKey))) {
        const currentSlack = HierarchicalLayout.slack(g, edgeKey);
        if(currentSlack < minSlack) {
          minSlack = currentSlack;
          minSlackEdge = edgeKey;
        }
      }
    })
    //console.warn(minSlack)
    //console.warn("MinSlackEdge:", minSlackEdge)
    return minSlackEdge;
  }

  /*
   * Returns the amount of slack for the given edge. The slack is defined as the
   * difference between the length of the edge and its minimum length.
   */
  public static slack(g : AbstractGraph, edgeKey : string) {
    const slack = g.getNodeAttribute(g.target(edgeKey), "rank") - g.getNodeAttribute(g.source(edgeKey), "rank") - g.getEdgeAttribute(edgeKey, "minlen");
    //console.warn("current slack: ",g.target(edgeKey), g.source(edgeKey), slack )
    return slack;
  }

  public longestPath(g : AbstractGraph) {
    const state: Record<string, 0 | 1 | 2> = {};
    const ranks: Record<string, number> = {};

    const dfs = (nodeKey: string): number => {
      const nodeState = state[nodeKey] ?? 0;

      if (nodeState === 2) {
        return ranks[nodeKey];
      }

      if (nodeState === 1) {
        throw new Error(`Cycle detected during longestPath at node ${nodeKey}`);
      }

      state[nodeKey] = 1;

      const outgoing = g.edges(nodeKey).filter(edgeKey => g.source(edgeKey) === nodeKey);
      let rank = 0;

      if (outgoing.length > 0) {
        rank = HierarchicalLayout.applyWithChunking(
          Math.min,
          outgoing.map(edgeKey => dfs(g.target(edgeKey)) - g.getEdgeAttribute(edgeKey, "minlen"))
        );
      }

      ranks[nodeKey] = rank;
      g.setNodeAttribute(nodeKey, "rank", rank)
      state[nodeKey] = 2;
      return rank;
    }

    g.nodes().forEach((nodeKey: string) => {
      if ((state[nodeKey] ?? 0) === 0) {
        dfs(nodeKey);
      }
    });
  }

  public static splitToChunks(array, chunkSize = this.CHUNKING_THRESHOLD) {
    const chunks = [];
    for (let i = 0; i < array.length; i += chunkSize) {
      const chunk = array.slice(i, i + chunkSize);
      chunks.push(chunk);
    }
    return chunks;
  }

  private static CHUNKING_THRESHOLD = 65535;

  public static applyWithChunking(fn, argsArray) {
    if(argsArray.length > this.CHUNKING_THRESHOLD) {
      const chunks = this.splitToChunks(argsArray);
      return fn.apply(null, chunks.map(chunk => fn.apply(null, chunk)));
    } else {
      return fn.apply(null, argsArray);
    }
  }

  public shiftRanks(t, g, delta) {
    t.forEach(v => g.setNodeAttribute(v, "rank", g.getNodeAttribute(v, "rank") + delta));
  }


  private static getComponents(g: AbstractGraph): string[][] {
    const visited = new Set<string>();
    const components: string[][] = [];
    for (const start of g.nodes()) {
      if (visited.has(start)) continue;
      const component: string[] = [];
      const queue = [start];
      while (queue.length > 0) {
        const node = queue.shift()!;
        if (visited.has(node)) continue;
        visited.add(node);
        component.push(node);
        for (const neighbor of g.neighbors(node)) {
          if (!visited.has(neighbor)) queue.push(neighbor);
        }
      }
      components.push(component);
    }
    return components;
  }

  public static assign(g: AbstractGraph, options? : HierarchicalLayoutOptionsInterface) {

    const hierarchicalLayout = new HierarchicalLayout();

    const components = HierarchicalLayout.getComponents(g);
    if (components.length > 1) {
      const horizontal = options?.direction === "horizontal";
      const freeAxis: "x" | "y" = horizontal ? "y" : "x";
      let freeAxisOffset = 0;
      for (const compNodes of components) {
        const nodeSet = new Set(compNodes);
        const sub = g.copy();
        for (const n of (sub as AbstractGraph).nodes()) {
          if (!nodeSet.has(n)) (sub as AbstractGraph).dropNode(n);
        }
        HierarchicalLayout.assign(sub as AbstractGraph, options);
        const coords = compNodes.map(n => (sub as AbstractGraph).getNodeAttribute(n, freeAxis) as number);
        const minCoord = Math.min(...coords);
        const maxCoord = Math.max(...coords);
        const shift = freeAxisOffset - minCoord;
        for (const node of compNodes) {
          g.setNodeAttribute(node, "x", ((sub as AbstractGraph).getNodeAttribute(node, "x") as number) + (freeAxis === "x" ? shift : 0));
          g.setNodeAttribute(node, "y", ((sub as AbstractGraph).getNodeAttribute(node, "y") as number) + (freeAxis === "y" ? shift : 0));
        }
        for (const edgeKey of g.edges()) {
          const src = g.source(edgeKey), tgt = g.target(edgeKey);
          if (!nodeSet.has(src) || !nodeSet.has(tgt)) continue;
          const subEdgeKey = (sub as AbstractGraph).edge(src, tgt);
          if (subEdgeKey == null) continue;
          const subAttrs = (sub as AbstractGraph).getEdgeAttributes(subEdgeKey);
          if (subAttrs["waypoints"]) {
            g.setEdgeAttribute(edgeKey, "waypoints", subAttrs["waypoints"].map((wp: { x: number; y: number }) => ({
              x: wp.x + (freeAxis === "x" ? shift : 0),
              y: wp.y + (freeAxis === "y" ? shift : 0),
            })));
            g.setEdgeAttribute(edgeKey, "type", "polyline");
          }
        }
        freeAxisOffset += (maxCoord - minCoord) + hierarchicalLayout.nodeSeparation * 3;
      }
      return;
    }

    // Step 1: Remove cycles
    const { reversedEdges, selfLoops } = hierarchicalLayout.removeCycles(g);

    g.forEachEdge(edgeKey =>
      g.setEdgeAttribute(edgeKey, "minlen", 1)
    )

    // Step 2: Layer assignment
    hierarchicalLayout.longestPath(g)
    hierarchicalLayout.tightTree(g)

    const levelNodes : string[][] = []
    let rankMinimum = Number.POSITIVE_INFINITY;
    g.forEachNode(nodeKey => {
      const rank = g.getNodeAttribute(nodeKey, "rank")
      if (rank < rankMinimum) rankMinimum = rank;
    });

    if (rankMinimum === Number.POSITIVE_INFINITY) return;

    g.forEachNode(nodeKey => {
      const r: number = g.getNodeAttribute(nodeKey, "rank");
      const index = rankMinimum < 0 ? r - rankMinimum : r;
      while (levelNodes.length <= index) levelNodes.push([]);
      levelNodes[index].push(nodeKey);
    })

    // Step 3: Crossing minimization, dummy nodes are optional
    let originalEdges: StoredEdgeInfo[] = [];
    if (options?.useDummyNodes !== false) {
      originalEdges = hierarchicalLayout.insertDummyNodes(g, levelNodes, rankMinimum);
      hierarchicalLayout.initializeDummyOrder(levelNodes, originalEdges, g, rankMinimum);
    }
    hierarchicalLayout.minimizeCrossings(levelNodes, g);

    // Restore original edge directions and self-loops
    hierarchicalLayout.restoreCycles(g, reversedEdges, selfLoops);

    // Step 4: Assign coordinates
    const PRIORITY_PASSES = 8;
    const horizontal = options?.direction === "horizontal";
    const freeAxis: "x" | "y" = horizontal ? "y" : "x";
    const fixedAxis: "x" | "y" = horizontal ? "x" : "y";

    levelNodes.forEach(layer => hierarchicalLayout.initializeLayerOnAxis(g, layer, freeAxis));

    for (let pass = 0; pass < PRIORITY_PASSES; pass++) {
      for (let i = 1; i < levelNodes.length; i++)
        hierarchicalLayout.priorityLayer(g, levelNodes[i], levelNodes[i - 1], true, freeAxis);
      for (let i = levelNodes.length - 2; i >= 0; i--)
        hierarchicalLayout.priorityLayer(g, levelNodes[i], levelNodes[i + 1], false, freeAxis);
    }

    levelNodes.forEach((layer, li) => hierarchicalLayout.alignOrthogonalAxis(g, layer, li, fixedAxis));

    // Step 5: Remove dummy nodes now that their coordinates are assigned
    if (options?.useDummyNodes !== false) {
      hierarchicalLayout.removeDummyNodes(g, originalEdges, levelNodes);
    }
  }

  private static readonly DEFAULT_NODE_SIZE = 20;

  private isDummy(g: AbstractGraph, nodeKey: string): boolean {
    return Boolean(g.getNodeAttribute(nodeKey, "isDummy"));
  }

  private getSize(g: AbstractGraph, nodeKey: string): number {
    return this.isDummy(g, nodeKey) ? 0 : (g.getNodeAttribute(nodeKey, "size") ?? HierarchicalLayout.DEFAULT_NODE_SIZE);
  }

  private isFixed(g: AbstractGraph, nodeKey: string): boolean {
    return Boolean(g.getNodeAttribute(nodeKey, "fixed"));
  }

  private getCoord(g: AbstractGraph, nodeKey: string, axis: "x" | "y"): number {
    return g.getNodeAttribute(nodeKey, axis) ?? 0;
  }

  private setCoord(g: AbstractGraph, nodeKey: string, axis: "x" | "y", value: number): void {
    g.setNodeAttribute(nodeKey, axis, value);
  }

  private sep(g: AbstractGraph, a: string, b: string): number {
    return !this.isDummy(g, a) && !this.isDummy(g, b)
      ? this.nodeSeparation
      : this.dummyPathSeparation;
  }

  private minGap(g: AbstractGraph, a: string, b: string): number {
    return this.getSize(g, a) / 2 + this.sep(g, a, b) + this.getSize(g, b) / 2;
  }

  private static mean(values: number[]): number {
    return values.reduce((sum, v) => sum + v, 0) / values.length;
  }

  private getNeighborCoords(g: AbstractGraph, nodeKey: string, layerLookup: Set<string>, inbound: boolean, axis: "x" | "y"): number[] {
    const edges = inbound ? g.inboundEdges(nodeKey) : g.outboundEdges(nodeKey);
    if (!edges) return [];
    const coords: number[] = [];
    for (const edgeKey of edges) {
      const neighbor = inbound ? g.source(edgeKey) : g.target(edgeKey);
      if (layerLookup.has(neighbor)) coords.push(this.getCoord(g, neighbor, axis));
    }
    return coords;
  }

  private initializeLayerOnAxis(g: AbstractGraph, layer: string[], axis: "x" | "y"): void {
    const realNodes = layer.filter(k => !this.isDummy(g, k));
    const span = realNodes.reduce((sum, k) => sum + this.getSize(g, k), 0) +
      Math.max(0, realNodes.length - 1) * this.nodeSeparation;
    let cursor = -span / 2;
    for (const nodeKey of layer) {
      if (this.isDummy(g, nodeKey)) {
        if (!this.isFixed(g, nodeKey)) this.setCoord(g, nodeKey, axis, 0);
        continue;
      }
      const size = this.getSize(g, nodeKey);
      if (!this.isFixed(g, nodeKey)) this.setCoord(g, nodeKey, axis, cursor + size / 2);
      cursor += size + this.nodeSeparation;
    }
  }

  /*
   * Priority coordinate assignment for one layer. Dummies > real nodes by degree.
   * Each node targets the barycenter of its neighbours in the adjacent layer.
   * Higher-priority nodes lock first; lower-priority nodes fit into the remaining gaps.
   */
  private priorityLayer(g: AbstractGraph, layer: string[], adjacentLayer: string[] | null, inbound: boolean, axis: "x" | "y"): void {
    if (layer.length === 0) return;

    const maxDegree = layer
      .filter(k => !this.isDummy(g, k))
      .reduce((max, k) => Math.max(max, g.degree(k)), 0);
    const priority = (k: string) => this.isDummy(g, k) ? maxDegree + 1 : g.degree(k);

    const adjacentLookup = adjacentLayer ? new Set(adjacentLayer) : new Set<string>();
    const target = new Map<string, number>();
    for (const k of layer) {
      if (this.isFixed(g, k)) { target.set(k, this.getCoord(g, k, axis)); continue; }
      const coords = this.getNeighborCoords(g, k, adjacentLookup, inbound, axis);
      target.set(k, coords.length > 0 ? HierarchicalLayout.mean(coords) : this.getCoord(g, k, axis));
    }

    const pos = new Map<string, number>(layer.map(k => [k, this.getCoord(g, k, axis)]));
    const locked = new Set<string>();
    const order = [...layer].sort((a, b) => priority(b) - priority(a));

    for (const k of order) {
      if (this.isFixed(g, k)) { locked.add(k); continue; }

      const i = layer.indexOf(k);
      const t = target.get(k)!;

      // Hard bounds: walk outward to nearest locked anchor, accumulate
      // minGap through every unlocked node in between.
      let lo = -Infinity;
      for (let j = i - 1; j >= 0; j--) {
        if (locked.has(layer[j])) {
          let minPos = pos.get(layer[j])!;
          for (let jj = j; jj < i; jj++) minPos += this.minGap(g, layer[jj], layer[jj + 1]);
          lo = minPos;
          break;
        }
      }
      let hi = Infinity;
      for (let j = i + 1; j < layer.length; j++) {
        if (locked.has(layer[j])) {
          let maxPos = pos.get(layer[j])!;
          for (let jj = j; jj > i; jj--) maxPos -= this.minGap(g, layer[jj - 1], layer[jj]);
          hi = maxPos;
          break;
        }
      }

      pos.set(k, Math.max(lo, Math.min(hi, t)));
      locked.add(k);

      // Nudge unlocked neighbours to preserve order.
      for (let j = i - 1; j >= 0; j--) {
        if (locked.has(layer[j])) break;
        const maxAllowed = pos.get(layer[j + 1])! - this.minGap(g, layer[j], layer[j + 1]);
        if (pos.get(layer[j])! > maxAllowed) pos.set(layer[j], maxAllowed); else break;
      }
      for (let j = i + 1; j < layer.length; j++) {
        if (locked.has(layer[j])) break;
        const minAllowed = pos.get(layer[j - 1])! + this.minGap(g, layer[j - 1], layer[j]);
        if (pos.get(layer[j])! < minAllowed) pos.set(layer[j], minAllowed); else break;
      }
    }

    for (const k of layer) {
      if (!this.isFixed(g, k)) this.setCoord(g, k, axis, pos.get(k)!);
    }
  }

  private alignOrthogonalAxis(g: AbstractGraph, layer: string[], layerIndex: number, axis: "x" | "y"): void {
    const value = axis === "x"
      ? layerIndex * this.levelSeparation
      : -layerIndex * this.levelSeparation;
    for (const nodeKey of layer) {
      if (!this.isFixed(g, nodeKey)) {
        this.setCoord(g, nodeKey, axis, value);
      }
    }
  }

}
