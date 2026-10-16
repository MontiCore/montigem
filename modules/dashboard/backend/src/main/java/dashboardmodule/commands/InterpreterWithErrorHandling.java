package dashboardmodule.commands;

import dashboardmodule.DashboardModuleManager;
import de.monticore.symboltable.ImportStatement;
import de.monticore.values.MCValue;
import de.se_rwth.commons.logging.Finding;
import de.se_rwth.commons.logging.Log;
import expressionwrapper.Interpreter;
import expressionwrapper.InterpreterFailedException;
import jsweet.lang.Erased;
import umlp.backendrte.common.GemList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Erased
public class InterpreterWithErrorHandling {

  // Packages (e.g., the domain of the application using this module) that are imported by default in data scripts
  protected static final List<String> additionalDomainPackages = new ArrayList<>();
  // Classes whose class path entries are made available to the data scripts
  protected static final List<Class<?>> additionalClassPathEntries = new ArrayList<>();

  /**
   * Makes a domain available to the data scripts of the dashboard cells,
   * e.g., {@code addDomain("mydomain", MyDomainManager.class)} enables {@code MyDomainManager.getPersonList()}.
   * Usually called once by the application using this module during startup.
   *
   * @param packageName   package of the domain, imported by default within the data scripts
   * @param classPathEntry any class of the domain, its class path entry is made available to the interpreter
   */
  public static void addDomain(String packageName, Class<?> classPathEntry) {
    additionalDomainPackages.add(packageName);
    additionalClassPathEntries.add(classPathEntry);
  }

  public static ValueOrError interpretString(String interpreterModel) {
    Log.info("Starting interpretation of model: "+interpreterModel, InterpreterWithErrorHandling.class.getName());

    Interpreter interpreter = new Interpreter();
    interpreter.setup();
    interpreter.addDefaultImportStatement(new ImportStatement("dashboardmodule", true));
    interpreter.addDefaultImportStatement(new ImportStatement("umlp.backendrte.common.GemList", false));
    interpreter.addClassPathEntry(DashboardModuleManager.class);
    interpreter.addClassPathEntry(GemList.class);
    additionalDomainPackages.forEach(p -> interpreter.addDefaultImportStatement(new ImportStatement(p, true)));
    additionalClassPathEntries.forEach(interpreter::addClassPathEntry);
    Collection<Finding> oldFindings = new ArrayList<>(Log.getFindings());
    boolean previousFailQuickSetting = Log.isFailQuickEnabled();
    Log.enableFailQuick(false);
    Optional<MCValue> val = Optional.empty();
    try {
      val = Optional.of(interpreter.interpret(interpreterModel));
    }catch (InterpreterFailedException e) {
      Log.info("Interpretation failed with exception. "+e.getMessage(), InterpreterWithErrorHandling.class.getName());
      e.printStackTrace();
    }
    Log.enableFailQuick(previousFailQuickSetting);

    Collection<Finding> newFindings = new ArrayList<>(Log.getFindings());
    newFindings.removeAll(oldFindings);

    if(!newFindings.isEmpty()) {
      return new ValueOrError(newFindings.stream().map(Object::toString).collect(Collectors.joining("\n")));
    } else if(val.isEmpty()) {
      return new ValueOrError("Interpretation failed but cannot retrieve the reason why.");
    } else if(val.get().isError()) {
      return new ValueOrError(val.get().asError());
    } else {
      return new ValueOrError(val.get());
    }
  }

  /**
   * Only for use in BackendOnly Code
   */
  @Erased
  public static class ValueOrError{
    private final String findings;
    private final MCValue value;
    final boolean hasErrors;

    protected ValueOrError(String findings) {
      this.findings = findings;
      this.value = null;
      this.hasErrors = true;
    }

    protected ValueOrError(MCValue value) {
      this.value = value;
      this.findings = null;
      this.hasErrors = false;
    }

    public boolean isValue() {
      return !this.hasErrors;
    }

    public boolean isError() {
      return this.hasErrors;
    }

    public String getFindings() {
      return findings;
    }

    public MCValue getValue() {
      return value;
    }
  }
}
