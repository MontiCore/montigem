import { createServer } from 'node:net';
import { defineConfig } from 'vitest/config';

/**
 * Vitest's browser-mode dev server binds to a fixed default port (63315).
 * On Windows that port can fall inside a Hyper-V/WSL2 dynamic port exclusion
 * range, which fails with EACCES instead of the EADDRINUSE that Vite knows
 * how to retry around. Asking the OS for an ephemeral port up front avoids
 * excluded ranges (the OS won't hand one out) and works for concurrent runs
 * since every invocation gets its own port.
 */
function findFreePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = createServer();
    server.unref();
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const port = (server.address() as { port: number }).port;
      server.close(() => resolve(port));
    });
  });
}

export default defineConfig(async () => ({
  test: {
    browser: {
      api: {
        port: await findFreePort(),
      },
    },
  },
}));
