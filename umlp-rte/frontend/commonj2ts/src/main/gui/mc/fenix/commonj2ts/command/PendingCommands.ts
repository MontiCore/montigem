/* (c) https://github.com/MontiCore/monticore */

/**
 * Counts the commands this client is busy with: commands executed via
 * {@link CommandManager.executeCommand} whose result has not arrived yet, and
 * messages received from the server whose processing has not finished.
 *
 * The count is published as the global variable {@link PendingCommands.GLOBAL_NAME}
 * (`window.umlp_pending_commands`), so that end-to-end tests can wait until the
 * client is idle (`PlaywrightUtils#waitForBackend` in umlp-test-rte).
 */
export class PendingCommands {
  public static readonly GLOBAL_NAME = "umlp_pending_commands";

  private static count: number = 0;

  public static getCount(): number {
    return PendingCommands.count;
  }

  public static begin(): void {
    PendingCommands.count++;
    PendingCommands.publish();
  }

  public static end(): void {
    PendingCommands.count = Math.max(0, PendingCommands.count - 1);
    PendingCommands.publish();
  }

  /**
   * Counts a command from now until the promise returned by `work` settles.
   */
  public static async track<T>(work: () => Promise<T>): Promise<T> {
    PendingCommands.begin();
    try {
      return await work();
    } finally {
      PendingCommands.end();
    }
  }

  /**
   * Writes the current count to the global variable.
   */
  public static publish(): void {
    (globalThis as any)[PendingCommands.GLOBAL_NAME] = PendingCommands.count;
  }
}
