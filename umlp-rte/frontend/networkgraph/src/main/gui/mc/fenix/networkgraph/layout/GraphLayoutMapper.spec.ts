import { AsyncList } from "@umlp/commonj2ts";
import { ChannelHierarchicalLayoutOptionsBuilder } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/ChannelHierarchicalLayoutOptionsBuilder";
import { CirclePackLayoutOptionsBuilder } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/CirclePackLayoutOptionsBuilder";
import { CircularLayoutOptionsBuilder } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/CircularLayoutOptionsBuilder";
import { HierarchicalLayoutDirection } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/HierarchicalLayoutDirection";
import { InitialGraphLayoutBuilder } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/InitialGraphLayoutBuilder";
import { InitialGraphLayoutMode } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/InitialGraphLayoutMode";
import { ComplexGraphLayoutBuilder } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/ComplexGraphLayoutBuilder";
import { ComplexGraphLayoutMode } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/ComplexGraphLayoutMode";
import { ForceAtlas2LayoutOptionsBuilder } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/ForceAtlas2LayoutOptionsBuilder";
import { ForceLayoutOptionsBuilder } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/ForceLayoutOptionsBuilder";
import { NoOverlapLayoutOptionsBuilder } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/NoOverlapLayoutOptionsBuilder";
import { ComplexGraphLayouts, InitialGraphLayouts } from "../types/graph-layout.types";
import { mapComplexGraphLayout, mapInitialGraphLayout } from "./GraphLayoutMapper";

describe("mapInitialGraphLayout", () => {

  async function layout(mode: InitialGraphLayoutMode): Promise<InitialGraphLayoutBuilder> {
    return (await new InitialGraphLayoutBuilder().__InitialGraphLayoutBuilderConstructor()).mode(mode);
  }

  it("maps no layout to no configuration", () => {
    expect(mapInitialGraphLayout(null)).toEqual({ configuration: null });
    expect(mapInitialGraphLayout(undefined)).toEqual({ configuration: null });
  });

  it("uses plain configurations as they are", () => {
    const configuration = { mode: InitialGraphLayouts.RANDOM };
    expect(mapInitialGraphLayout(configuration)).toEqual({ configuration });
  });

  it("maps the options of the circular layout", async () => {
    const options = (await new CircularLayoutOptionsBuilder().__CircularLayoutOptionsBuilderConstructor())
      .center(0.2).scale(3).build();
    const modeled = (await layout(InitialGraphLayoutMode.CIRCULAR)).circular(options).build();

    expect(mapInitialGraphLayout(modeled)).toEqual({
      configuration: { mode: InitialGraphLayouts.CIRCULAR, parameters: { center: 0.2, scale: 3 } },
    });
  });

  it("leaves out absent options, so that the layout uses its defaults", async () => {
    const modeled = (await layout(InitialGraphLayoutMode.RANDOM)).build();

    expect(mapInitialGraphLayout(modeled)).toEqual({
      configuration: { mode: InitialGraphLayouts.RANDOM, parameters: {} },
    });
  });

  it("maps the hierarchy attributes of the circle pack layout", async () => {
    const builder = await new CirclePackLayoutOptionsBuilder().__CirclePackLayoutOptionsBuilderConstructor();
    await builder.hierarchyAttributes(await new AsyncList<string>().__AsyncListConstructor(["group"]));
    const modeled = (await layout(InitialGraphLayoutMode.CIRCLE_PACK)).circlePack(builder.build()).build();

    expect(mapInitialGraphLayout(modeled)).toEqual({
      configuration: { mode: InitialGraphLayouts.CIRCLE_PACK, parameters: { hierarchyAttributes: ["group"] } },
    });
  });

  it("sets the settings the graph manager takes the hierarchical layout from", async () => {
    const modeled = (await layout(InitialGraphLayoutMode.HIERARCHICAL)).build();

    expect(mapInitialGraphLayout(modeled)).toEqual({
      configuration: { mode: InitialGraphLayouts.HIERARCHICAL },
      settings: { hierarchicalLayoutMode: "HIERARCHICAL", hierarchicalDirection: "vertical", useDummyNodes: false },
    });
  });

  it("maps the options of the channel hierarchical layout", async () => {
    const options = (await new ChannelHierarchicalLayoutOptionsBuilder().__ChannelHierarchicalLayoutOptionsBuilderConstructor())
      .direction(HierarchicalLayoutDirection.HORIZONTAL).useDummyNodes(true).levelSeparation(100).build();
    const modeled = (await layout(InitialGraphLayoutMode.CHANNEL_HIERARCHICAL)).channelHierarchical(options).build();

    expect(mapInitialGraphLayout(modeled)).toEqual({
      configuration: { mode: InitialGraphLayouts.CHANNEL_HIERARCHICAL, parameters: { levelSeparation: 100 } },
      settings: { hierarchicalLayoutMode: "CHANNEL_HIERARCHICAL", hierarchicalDirection: "horizontal", useDummyNodes: true },
    });
  });
});

describe("mapComplexGraphLayout", () => {

  async function layout(mode: ComplexGraphLayoutMode): Promise<ComplexGraphLayoutBuilder> {
    return (await new ComplexGraphLayoutBuilder().__ComplexGraphLayoutBuilderConstructor()).mode(mode);
  }

  it("maps no layout to no configuration", () => {
    expect(mapComplexGraphLayout(null)).toBeNull();
    expect(mapComplexGraphLayout(undefined)).toBeNull();
  });

  it("uses plain configurations as they are", () => {
    const configuration = { mode: ComplexGraphLayouts.FORCE, maxIterations: 5 };
    expect(mapComplexGraphLayout(configuration)).toBe(configuration);
  });

  it("maps the limits and the options of the force layout", async () => {
    const options = (await new ForceLayoutOptionsBuilder().__ForceLayoutOptionsBuilderConstructor())
      .gravity(0.01).maxMove(50).build();
    const modeled = (await layout(ComplexGraphLayoutMode.FORCE)).maxIterations(100).maxLayoutTime(2000)
      .force(options).build();

    expect(mapComplexGraphLayout(modeled)).toEqual({
      mode: ComplexGraphLayouts.FORCE,
      maxIterations: 100,
      maxLayoutTime: 2000,
      parameters: { settings: { gravity: 0.01, maxMove: 50 } },
    });
  });

  it("leaves out absent options and limits, so that the layout uses its defaults", async () => {
    const modeled = (await layout(ComplexGraphLayoutMode.FORCEATLAS2)).build();

    expect(mapComplexGraphLayout(modeled)).toEqual({
      mode: ComplexGraphLayouts.FORCEATLAS2,
      parameters: { settings: {} },
    });
  });

  it("maps the options of the ForceAtlas2 layout", async () => {
    const options = (await new ForceAtlas2LayoutOptionsBuilder().__ForceAtlas2LayoutOptionsBuilderConstructor())
      .linLogMode(true).slowDown(1.2).barnesHutOptimize(false).build();
    const modeled = (await layout(ComplexGraphLayoutMode.FORCEATLAS2)).forceAtlas2(options).build();

    expect(mapComplexGraphLayout(modeled)).toEqual({
      mode: ComplexGraphLayouts.FORCEATLAS2,
      parameters: { settings: { linLogMode: true, slowDown: 1.2, barnesHutOptimize: false } },
    });
  });

  it("maps the options of the no overlap layout", async () => {
    const options = (await new NoOverlapLayoutOptionsBuilder().__NoOverlapLayoutOptionsBuilderConstructor())
      .margin(10).gridSize(40).build();
    const modeled = (await layout(ComplexGraphLayoutMode.NO_OVERLAP)).noOverlap(options).build();

    expect(mapComplexGraphLayout(modeled)).toEqual({
      mode: ComplexGraphLayouts.NO_OVERLAP,
      parameters: { settings: { margin: 10, gridSize: 40 } },
    });
  });
});
