package umlp.test_rte.playwright;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Tracks JavaScript coverage for a given browser context.
 */
public final class CoverageTracker {

  private final BrowserContext context;
  private final Map<CDPSession, Consumer<JsonObject>> cdpSessions;
  private final Map<CDPSession, Set<String>> scriptsIdBySession;
  private final Map<CDPSession, Map<String, JsonElement>> scriptsSourcesBySession;
  private File frontendDistDirectory;
  private Consumer<Page> pageListener;


  public CoverageTracker(BrowserContext context, File frontendDistDirectory){
    this.context = context;
    this.cdpSessions = new LinkedHashMap<>();
    this.scriptsIdBySession = new HashMap<>();
    this.scriptsSourcesBySession = new HashMap<>();
    this.frontendDistDirectory = frontendDistDirectory;
  }

  public void startJSCoverage(){
    scriptsIdBySession.clear();
    scriptsSourcesBySession.clear();
    cdpSessions.clear();

    // Attach coverage to any pages that already exist
    for(Page page : context.pages()){
      attachToPage(page);
    }

    // Attach to any new pages created in this context
    this.pageListener = this::attachToPage;
    context.onPage(this.pageListener);
  }

  public void stopJSCoverage(String outputFileName){
    try{
      if(this.pageListener != null){
        context.offPage(this.pageListener);
      }

      JsonArray allEntries = new JsonArray();

      for(Map.Entry<CDPSession, Consumer<JsonObject>> entry : cdpSessions.entrySet()) {
        CDPSession cdpSession = entry.getKey();
        Consumer<JsonObject> listener = entry.getValue();

        JsonObject result;
        try {
          result = cdpSession.send("Profiler.takePreciseCoverage");

          cdpSession.send("Profiler.stopPreciseCoverage");
          cdpSession.send("Profiler.disable");
          cdpSession.send("Debugger.disable");
          cdpSession.off("Debugger.scriptParsed", listener);
          cdpSession.detach();
        } catch (Exception e) {
          // Session or page was likely closed during the test run, skip it.
          continue;
        }
        if (result != null && result.has("result") && result.get("result").isJsonArray()) {
          JsonArray scripts = result.getAsJsonArray("result");
          Map<String, JsonElement> sessionSources = scriptsSourcesBySession.getOrDefault(cdpSession, Map.of());
          if (scripts != null && !scripts.isEmpty()) {
            for (JsonElement element : scripts) {
              JsonObject scriptEntry = element.getAsJsonObject();
              String scriptId = scriptEntry.get("scriptId").getAsString();

              if (!sessionSources.containsKey(scriptId)) continue;

              scriptEntry.add("source", sessionSources.get(scriptId));
              allEntries.add(scriptEntry);
            }
          }
        }
      }

      if(allEntries.size() > 0) {
        File outDir = new File("build/reports/coverage");
        if (!outDir.exists()) outDir.mkdirs();

        File outFile = new File(outDir, sanitizeFileName(outputFileName) + ".json");
        sanitizeAndSaveContent(allEntries, outFile);
      }

      scriptsIdBySession.clear();
      scriptsSourcesBySession.clear();
      cdpSessions.clear();
    }catch(Exception e){
      throw new RuntimeException(e);
    }
  }

  private void attachToPage(Page page){
    CDPSession cdpSession = context.newCDPSession(page);

    scriptsIdBySession.put(cdpSession, new HashSet<>());
    scriptsSourcesBySession.put(cdpSession, new HashMap<>());

    cdpSession.send("Debugger.enable");
    JsonObject debuggerParam = new JsonObject();
    debuggerParam.addProperty("skip", true);
    cdpSession.send("Debugger.setSkipAllPauses", debuggerParam);
    Consumer<JsonObject> listener = event -> onScriptParsed(cdpSession, event);
    cdpSessions.put(cdpSession, listener);
    cdpSession.on("Debugger.scriptParsed", listener);

    cdpSession.send("Profiler.enable");
    JsonObject profilerParams = new JsonObject();
    profilerParams.addProperty("callCount", true);
    profilerParams.addProperty("detailed", true);
    cdpSession.send("Profiler.startPreciseCoverage", profilerParams);
  }


  private void onScriptParsed(CDPSession session, JsonObject event){
    String scriptId =  event.get("scriptId").getAsString();
    String scriptUrl = event.has("url") ? event.get("url").getAsString() : "";

    Set<String> sessionScriptIds = this.scriptsIdBySession.get(session);
    Map<String, JsonElement> sessionSources = this.scriptsSourcesBySession.get(session);

    if(sessionScriptIds != null && !sessionScriptIds.contains(scriptId) && !scriptUrl.isEmpty()){
      sessionScriptIds.add(scriptId);
      JsonObject scriptParams = new JsonObject();
      scriptParams.addProperty("scriptId", scriptId);

      try {
        JsonObject scriptResult = session.send("Debugger.getScriptSource", scriptParams);
        if (scriptResult != null && scriptResult.has("scriptSource")) {
          sessionSources.put(scriptId, scriptResult.get("scriptSource"));
        }
      }catch (Exception e){
        // Script was unloaded, eval was discarded, or the target closed during navigation.
        // Safe to ignore since it won't appear in final coverage.
      }
    }
  }

  private String sanitizeFileName(String fileName){
    if (fileName == null || fileName.trim().isEmpty()) {
      return "unknown_test_coverage";
    }
    // Replaces any character that is NOT a letter, number, hyphen, or underscore with an underscore.
    return fileName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
  }

  private void sanitizeAndSaveContent(JsonArray content, File outFile) throws IOException {
    String fileUriPrefix = frontendDistDirectory.toURI().toString();
    if(!fileUriPrefix.endsWith("/")){
      fileUriPrefix += "/";
    }
    // For Windows, 'file:/' is valid, but this is not accepted by c8, convert to 'file:///'
    if(fileUriPrefix.startsWith("file:/") && !fileUriPrefix.startsWith("file:///")){
      fileUriPrefix = fileUriPrefix.replaceFirst("file:/", "file:///");
    }


    // Replace any localhost occurrences to ensure proper handling by later tools that cannot
    // resolve a localhost URI
    for(JsonElement element : content){
      if(element.isJsonObject()){
        JsonObject jsonObject = element.getAsJsonObject();
        if(jsonObject.has("url")){
          String oldUrl = jsonObject.get("url").getAsString();
          // Replace localhost:<port>/ with fileUriPrefix
          String newUrl = oldUrl.replaceAll("^https?://(localhost|127\\.0\\.0\\.1):\\d+/", fileUriPrefix);
          jsonObject.addProperty("url", newUrl);
        }
      }
    }

    // Wrap in expected {"result": [...]}
    JsonObject rootObject =  new JsonObject();
    rootObject.add("result", content);

    Files.write(outFile.toPath(), rootObject.toString().getBytes(StandardCharsets.UTF_8));

  }
}
