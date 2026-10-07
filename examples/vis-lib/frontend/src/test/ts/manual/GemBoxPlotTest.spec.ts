import { chartLayoutTest } from './AbstractChartLayout';
import { GemBoxPlotTest } from 'manual/GemBoxPlotTest';

chartLayoutTest(
  GemBoxPlotTest,
  'gem-box-plot',
  'text, .bg, .gridlayer, .zerolinelayer, .ytick, .xtick, .legend, .legend *, .modebar, .modebar *, .point'
);


