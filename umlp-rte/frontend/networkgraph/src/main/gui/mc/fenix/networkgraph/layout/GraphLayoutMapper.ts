import { ComplexGraphLayout } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/ComplexGraphLayout";
import { ComplexGraphLayoutMode } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/ComplexGraphLayoutMode";
import { InitialGraphLayout } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/InitialGraphLayout";
import { InitialGraphLayoutMode } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/InitialGraphLayoutMode";
import { HierarchicalLayoutDirection } from "../../../../../../../build/gui2java2ts/mc/fenix/networkgraph/networkgraphtypes/HierarchicalLayoutDirection";
import { DEFAULT_LAYOUT_OPTIONS } from "../consts/graph-layout.const";
import {
  ComplexGraphLayoutConfiguration,
  ComplexGraphLayouts,
  InitialGraphLayoutConfiguration,
  InitialGraphLayouts
} from "../types/graph-layout.types";
import { GraphSettings } from "../types/graph-settings.types";

/** The initial layout configuration of a modeled {@link InitialGraphLayout} and the settings it requires. */
export interface MappedInitialGraphLayout {
  configuration: InitialGraphLayoutConfiguration | null;

  /**
   * The graph manager takes the hierarchical variant, the dummy nodes and the direction from the settings (see
   * GraphManagerService.applyGraphLayout), so these are set for the hierarchical modes.
   */
  settings?: Partial<GraphSettings>;
}

/**
 * Maps the initial layout modeled in the GUI (NetworkGraphTypes.cd) to the configuration of the layout library.
 * Plain configurations, e.g., of TypeScript components, are used as they are.
 */
export function mapInitialGraphLayout(
  layout: InitialGraphLayout | InitialGraphLayoutConfiguration | null | undefined
): MappedInitialGraphLayout {
  if (!layout) return { configuration: null };
  if (!isModeled<InitialGraphLayout>(layout)) return { configuration: layout };

  switch (layout.getMode()) {
    case InitialGraphLayoutMode.CIRCULAR: {
      const options = layout.getCircular();
      return {
        configuration: {
          mode: InitialGraphLayouts.CIRCULAR,
          parameters: present({ center: options?.getCenter(), scale: options?.getScale() }),
        },
      };
    }
    case InitialGraphLayoutMode.RANDOM: {
      const options = layout.getRandom();
      return {
        configuration: {
          mode: InitialGraphLayouts.RANDOM,
          parameters: present({ center: options?.getCenter(), scale: options?.getScale() }),
        },
      };
    }
    case InitialGraphLayoutMode.CIRCLE_PACK: {
      const options = layout.getCirclePack();
      const hierarchyAttributes: string[] = options?.getHierarchyAttributesList()?.__internalArray() ?? [];
      return {
        configuration: {
          mode: InitialGraphLayouts.CIRCLE_PACK,
          parameters: present({
            center: options?.getCenter(),
            scale: options?.getScale(),
            hierarchyAttributes: hierarchyAttributes.length > 0 ? hierarchyAttributes : undefined,
          }),
        },
      };
    }
    case InitialGraphLayoutMode.HIERARCHICAL: {
      const options = layout.getHierarchical();
      return {
        configuration: { mode: InitialGraphLayouts.HIERARCHICAL },
        settings: hierarchicalSettings(InitialGraphLayouts.HIERARCHICAL, options?.getDirection(),
          options?.getUseDummyNodes()),
      };
    }
    case InitialGraphLayoutMode.CHANNEL_HIERARCHICAL: {
      const options = layout.getChannelHierarchical();
      return {
        configuration: {
          mode: InitialGraphLayouts.CHANNEL_HIERARCHICAL,
          parameters: present({ levelSeparation: options?.getLevelSeparation(), channelWidth: options?.getChannelWidth() }),
        },
        settings: hierarchicalSettings(InitialGraphLayouts.CHANNEL_HIERARCHICAL, options?.getDirection(),
          options?.getUseDummyNodes()),
      };
    }
    default:
      return { configuration: null };
  }
}

/**
 * Maps the complex layout modeled in the GUI (NetworkGraphTypes.cd) to the configuration of the layout library.
 * Plain configurations, e.g., of TypeScript components, are used as they are.
 */
export function mapComplexGraphLayout(
  layout: ComplexGraphLayout | ComplexGraphLayoutConfiguration | null | undefined
): ComplexGraphLayoutConfiguration | null {
  if (!layout) return null;
  if (!isModeled<ComplexGraphLayout>(layout)) return layout;

  const iterationSettings = present({ maxIterations: layout.getMaxIterations(), maxLayoutTime: layout.getMaxLayoutTime() });
  switch (layout.getMode()) {
    case ComplexGraphLayoutMode.FORCE: {
      const options = layout.getForce();
      return {
        ...iterationSettings,
        mode: ComplexGraphLayouts.FORCE,
        parameters: {
          settings: present({
            attraction: options?.getAttraction(),
            repulsion: options?.getRepulsion(),
            gravity: options?.getGravity(),
            inertia: options?.getInertia(),
            maxMove: options?.getMaxMove(),
          }),
        },
      };
    }
    case ComplexGraphLayoutMode.FORCEATLAS2: {
      const options = layout.getForceAtlas2();
      return {
        ...iterationSettings,
        mode: ComplexGraphLayouts.FORCEATLAS2,
        parameters: {
          // the settings inferred from the graph are the defaults (see COMPLEX_LAYOUT_MAPPING)
          settings: present({
            linLogMode: options?.getLinLogMode(),
            outboundAttractionDistribution: options?.getOutboundAttractionDistribution(),
            adjustSizes: options?.getAdjustSizes(),
            edgeWeightInfluence: options?.getEdgeWeightInfluence(),
            scalingRatio: options?.getScalingRatio(),
            strongGravityMode: options?.getStrongGravityMode(),
            gravity: options?.getGravity(),
            slowDown: options?.getSlowDown(),
            barnesHutOptimize: options?.getBarnesHutOptimize(),
            barnesHutTheta: options?.getBarnesHutTheta(),
          }),
        },
      };
    }
    case ComplexGraphLayoutMode.NO_OVERLAP: {
      const options = layout.getNoOverlap();
      return {
        ...iterationSettings,
        mode: ComplexGraphLayouts.NO_OVERLAP,
        parameters: {
          settings: present({
            gridSize: options?.getGridSize(),
            margin: options?.getMargin(),
            expansion: options?.getExpansion(),
            ratio: options?.getRatio(),
            speed: options?.getSpeed(),
          }),
        },
      };
    }
    default:
      return null;
  }
}

function isModeled<T extends InitialGraphLayout | ComplexGraphLayout>(layout: T | object): layout is T {
  // duck typing, as the app and the library may each load their own copy of the generated class
  return typeof (layout as T).getMode === "function";
}

function hierarchicalSettings(
  mode: InitialGraphLayouts.HIERARCHICAL | InitialGraphLayouts.CHANNEL_HIERARCHICAL,
  direction: HierarchicalLayoutDirection | null | undefined,
  useDummyNodes: boolean | null | undefined
): Partial<GraphSettings> {
  return {
    hierarchicalLayoutMode: mode,
    hierarchicalDirection: direction === HierarchicalLayoutDirection.HORIZONTAL ? "horizontal" : "vertical",
    useDummyNodes: useDummyNodes ?? DEFAULT_LAYOUT_OPTIONS.useDummyNodes,
  };
}

/** Absent optionals are null, they are left out so that the layout uses its defaults. */
function present<T extends object>(parameters: T): any {
  return Object.fromEntries(Object.entries(parameters).filter(([, value]) => value !== null && value !== undefined));
}
