// Run after :umlp-rte:frontend:functiondisplay:gui2java2ts.
const path = require('node:path');
const {spawnSync} = require('node:child_process');
const root = path.resolve(__dirname, '../../../../..');
const frontend = path.join(root, 'umlp-rte/frontend');
const esbuild = require(require.resolve('esbuild', {
  paths: [path.join(frontend, 'node_modules/ng-packagr')]
}));
const output = path.join(root, 'products/ma2fustrumo-tool/build/typescript-tests/graph.cjs');
esbuild.buildSync({
  entryPoints: [path.join(__dirname, 'function-display-graph.test.ts')],
  outfile: output,
  bundle: true,
  platform: 'node',
  format: 'cjs',
  nodePaths: [path.join(frontend, 'node_modules')],
  alias: {'@umlp/commonj2ts': path.join(frontend, 'commonj2ts/index.ts')},
  logLevel: 'warning'
});
const result = spawnSync(process.execPath, ['--test', output], {stdio: 'inherit'});
process.exit(result.status ?? 1);
