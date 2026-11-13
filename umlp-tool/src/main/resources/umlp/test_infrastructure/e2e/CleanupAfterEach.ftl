<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature()}
${cd4c.method("protected void cleanupBackend(Page page, BrowserContext browserContext, TestInfo testInfo)")}

  if(coverageTracker != null){
    coverageTracker.stopJSCoverage(testInfo.getDisplayName());
  }
  page.close();
  browserContext.close();

  CommandWebSocketService service = this.authWebsocketService!= null ?
      this.authWebsocketService : this.noAuthWebsocketService;

  long timeoutMillis = 30_000; // 30 seconds
  long waitIntervalMillis = 200;
  long startTime = System.currentTimeMillis();

  // Check in the Backend if there is still an open connection and if so wait
  while(!service.getClientIds().isEmpty()) {
    if (System.currentTimeMillis() - startTime >= timeoutMillis) {
      Log.warn("Timeout after "+timeoutMillis+"ms. Close all opened pages properly in your E2E to speed it up.");
      break; // Timed out
    }
    try {
      Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException("Failed to wait for backend", e);
    }
  }
  Log.info("Clean up of Backend", this.getClass().getName());
  DatabaseAccessManager.executeStatement("DROP ALL OBJECTS");

