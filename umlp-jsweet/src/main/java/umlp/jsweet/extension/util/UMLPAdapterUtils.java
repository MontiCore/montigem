/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.util;

import com.google.gson.Gson;
import de.se_rwth.commons.logging.Log;
import jsweet.lang.Erased;
import org.jsweet.JSweetConfig;
import org.jsweet.transpiler.JSweetContext;
import org.jsweet.transpiler.Java2TypeScriptTranslator;
import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.OverloadScanner;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.util.Util;
import standalone.com.sun.tools.javac.code.Symbol;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UMLPAdapterUtils {

  protected final PrinterAdapter adapter;

  protected final Types types;

  protected final JSweetContext context;

  protected final Util util;


  public UMLPAdapterUtils(PrinterAdapter adapter, Types types, JSweetContext context, Util util) {
    this.adapter = adapter;
    this.types = types;
    this.context = context;
    this.util = util;
  }

  public void printMacroName(String macroName) {
    adapter.print("/* " + macroName + " */");
  }

  public static String getSimpleNameOfFQN(String importedName) {
    int index = 0;
    if (importedName.lastIndexOf("/") > 0) {
      index = importedName.lastIndexOf("/");
    }
    if (importedName.lastIndexOf("\\") > 0) {
      index = importedName.lastIndexOf("\\");
    }
    if (importedName.lastIndexOf(".") > 0) {
      index = importedName.lastIndexOf(".");
    }
    if (index > 0) {
      return importedName.substring(index + 1);
    }
    return importedName;
  }

  private List<String> j4tsExcluded = Arrays.asList(
      "java.lang.Boolean",
      "java.lang.Byte",
      "java.lang.Character",
      "java.lang.Double",
      "java.lang.Float",
      "java.lang.Integer",
      "java.lang.Long",
      "java.lang.Short",
      "java.lang.String",
      "java.lang.Override"
      );
  private boolean isJ4TSExcluded(String qualifiedName) {
    return j4tsExcluded.stream().anyMatch(e -> e.equals(qualifiedName));
  }

  private List<String> j4tsPackages = Arrays.asList(
      "java.beans",
      "java.io",
      "java.lang",
      "java.net",
      "java.nio",
      "java.security",
      "java.sql",
      "java.text",
      "java.util"
  );

  private boolean isJ4TSPackage(String qualifiedName) {
    return j4tsPackages.stream().anyMatch(p -> qualifiedName.startsWith(p + "."));
  }

  public boolean isSubTypeOfTypeMirror(TypeMirror type,
      String superClass) {
    TypeElement typeElement = getTypeElement(type);
    return isSubTypeOf(typeElement, superClass);
  }

  public boolean isSubTypeOf(TypeElement type,
      String superClass) {
    if (type == null) {
      return false;
    }
    if (types.erasure(type.asType()).toString().equals(superClass)) {
      return true;
    }
    for (TypeElement anInterface : util.getInterfaces(type)) {
      if (isSubTypeOf(anInterface, superClass)) {
        return true;
      }
    }
    if (type.getSuperclass() != null && !type.getSuperclass().toString().equals("none")) {
      if (isSubTypeOf(util.getSuperclass(type), superClass)) {
        return true;
      }
    }
    return false;
  }

  public Set<TypeElement> listSuperTypes(TypeElement type) {
    Set<TypeElement> result = new LinkedHashSet<>();

    if (type.getAnnotationsByType(Erased.class).length > 0) {
      // Do not inspect @Erased classes (because they might be async)
      return result;
    }
    result.add(type);

    for (TypeElement anInterface : util.getInterfaces(type)) {
      result.addAll(listSuperTypes(anInterface));
    }
    if (type.getSuperclass() != null && !type.getSuperclass().toString().equals("none")) {
      result.addAll(listSuperTypes((TypeElement) types.asElement(type.getSuperclass())));
    }

    return result;
  }

  public Set<String> listSuperTypesNames(TypeElement type) {
    return listSuperTypes(type).stream()
        .map(t -> t.getQualifiedName().toString())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public Set<ExecutableElement> listMatchingSuperMethods(TypeElement typeElement,
      ExecutableElement methodElement) {
    Set<ExecutableElement> result = new LinkedHashSet<>();
    for (TypeElement superType : listSuperTypes(typeElement)) {
      OverloadScanner.Overload overload = context.getOverload(superType, methodElement);
      if (overload != null) {
        for (OverloadScanner.OverloadMethodEntry overloadEntry : overload.getEntries()) {
          result.add(overloadEntry.methodElement);
        }
      }
    }
    return result;
  }

  public TypeElement getTypeElement(TypeMirror type) {
    TypeElement typeElementByName = util.getTypeElementByName(context,types.erasure(type).toString());
    return typeElementByName;
  }

  public String getOverloadMethodName(ExecutableElement method) {
    if (method.getKind() == ElementKind.CONSTRUCTOR) {
      return "constructor";
    }
    StringBuilder nameBuilder = new StringBuilder(method.getSimpleName().toString());
    nameBuilder.append("$");
    for (VariableElement paramElement : method.getParameters()) {
      nameBuilder.append(
          types.erasure(paramElement.asType()).toString().replace('.', '_').replace("[]", "_A"));
      nameBuilder.append("$");
    }
    if (!method.getParameters().isEmpty()) {
      nameBuilder.deleteCharAt(nameBuilder.length() - 1);
    }
    return nameBuilder.toString();
  }

  public String getTypeNameWithoutBuilderAndTOP(TypeElement type, boolean printPackage) {
    String name = printPackage ? type.getQualifiedName().toString() : type.getSimpleName().toString();
    if (name.endsWith("TOP")) {
      boolean isActuallyTOP = type.getModifiers().stream().anyMatch(m -> m.toString().equals("abstract"));
      if (isActuallyTOP) {
        name = name.substring(0, name.lastIndexOf("TOP"));
      }
    }
    if (name.endsWith("Builder")) {
      ArrayList<ExecutableElement> collector = new ArrayList<>();
      util.collectMatchingMethodDeclarationsInType(type, "isINTERNAL_deser_mode", null, true, collector);
      boolean isActuallyBuilder = !collector.isEmpty();
      if (isActuallyBuilder) {
        name = name.substring(0, name.lastIndexOf("Builder"));
      }
    }
    return name;
  }

  public Java2TypeScriptTranslator getPrinter() {
    if (adapter.getPrinter() instanceof Java2TypeScriptTranslator) {
      return (Java2TypeScriptTranslator) adapter.getPrinter();
    }
    else {
      Log.error("0JSC001 Java2TypeScriptTranslater must be core Adapter");
      return null;
    }
  }

  public void importFromWithModule(String importedName, String pathToImportedClass) {
    getPrinter().useModule(
        new ModuleImportDescriptor(importedName, pathToImportedClass));
  }

  public void importFromWithoutModule(String importedName, String pathToImportedClass) {
    Set<String> importedNames = context.getImportedNames(context.currentCompilationUnit.get().getSourceFile().getName());
    if (!importedNames.contains(importedName)) {
      context.addHeader("import." + importedName, "import " + importedName + " from '" + pathToImportedClass + "';");
    }
  }

  public String getJSweetParameterName(VariableElement parameter) {
    return getJSweetVariableName(context.getActualName(parameter));
  }

  public String getJSweetExecutableName(ExecutableElement executable) {
    return getJSweetVariableName(context.getActualName(executable));
  }

  public String getJSweetVariableName(String variableName) {
    if (JSweetConfig.JS_KEYWORDS.contains(variableName)) {
      return "__" + variableName;
    } else {
      return variableName;
    }
  }

  public static boolean isRTEIncluded(String fullyQualifiedName) {
    if (isSpecificRTEClass(fullyQualifiedName)) {
      return true;
    }
    if (!fullyQualifiedName.startsWith(RTEPathUtil.getBackendRTEPath())) {
      return false;
    }
    for (Pattern exclude : getExcludes()) {
      if (exclude.matcher(fullyQualifiedName).matches()) {
        return false;
      }
    }
    return true;
  }

  private static boolean isSpecificRTEClass(String fullyQualifiedName) {
    switch (fullyQualifiedName) {
      case "mc.fenix.commonj2ts.deser.DeSerRepository":
        return true;
      default:
        return false;
    }
  }

  protected static List<Pattern> getExcludes() {
    return excludes;
  }

  private static final List<Pattern> excludes = Arrays.asList(
      Pattern.compile("umlp\\.backendrte\\.service\\.websocket\\.WebSocketConfig")
  );

  public static boolean isJdkType(TypeElement type) {
    String qualifiedName = type.getQualifiedName().toString();
    return qualifiedName.startsWith("java.")
        || qualifiedName.startsWith("javax.")
        || qualifiedName.startsWith("sun.")
        || qualifiedName.startsWith("com.sun.")
        || qualifiedName.startsWith("jdk.");
  }

  /**
   * Without the JVM-wide jar cache: in the Gradle daemon, closing another class loader on the
   * same jar would otherwise close this stream ("Stream closed").
   */
  protected static InputStream openUncached(URL resource) throws IOException {
    URLConnection connection = resource.openConnection();
    connection.setUseCaches(false);
    return connection.getInputStream();
  }

  public static void addAsyncAnnotationsForJREToContext(JSweetContext context) {
    try (
      InputStream is = openUncached(UMLPAdapterUtils.class.getResource("/asyncjre.json"))) {
      Map<String, Object> config = new Gson().fromJson(
          new String(is.readAllBytes(), StandardCharsets.UTF_8)
          , Map.class);
      if (config != null) {
        Log.info("The current transpilation will assume an asynchronous JavaRTE annotated by >asyncjre.json<.", UMLPAdapterUtils.class.getName());

        Map<String,Object> asyncConfig = (Map<String,Object>) config.get("@Async");
        Object allOverloadsByName = asyncConfig.get("allOverloadsByName");
        Map<String, Set<String>> asyncMethods = new HashMap<>();
        if (allOverloadsByName != null) {
          Collection<?> names = allOverloadsByName instanceof Collection ? (Collection<?>) allOverloadsByName
              : Collections.singletonList(allOverloadsByName);
          for (Object o : names) {
            String qualifiedMethodName = o.toString();
            int lastDot = qualifiedMethodName.lastIndexOf('.');
            if (lastDot < 0) {
              Log.warn("invalid entry '" + qualifiedMethodName + "' for allOverloadsByName"
                  + ".allOverloadsByName (expected '<className>.<methodName>')");
              continue;
            }
            String className = qualifiedMethodName.substring(0, lastDot);
            String methodName = qualifiedMethodName.substring(lastDot + 1);
            if(asyncMethods.containsKey(className)) {
              asyncMethods.get(className).add(methodName);
            } else {
              asyncMethods.put(className, new HashSet<>(List.of(methodName)));
            }
          }
        }
        Elements elements = context.elements;
        for (var entry : asyncMethods.entrySet()) {
          String className = entry.getKey();

          TypeElement typeElement = elements.getTypeElement(className);
          if (typeElement == null) {
            continue;
          }

          for (Element e : elements.getAllMembers(typeElement)) {
            if(e.getKind() == ElementKind.METHOD && asyncMethods.get(className).contains(e.getSimpleName().toString())) {
              context.addExtraAnnotationType(e, JSweetConfig.ANNOTATION_ASYNC);
            }
          }
        }
      }
    } catch (
        IOException e) {
      throw new RuntimeException(e);
    }
  }
/*
  private static final Map<String, Set<String>> ASYNC_METHODS = Map.ofEntries(
     // ------------------ Java RTE ----------------------

      Map.entry("java.util.List", Set.of(
          "size",
          "isEmpty",
          "add",
          "addAll",
          "contains",
          "containsAll",
          "equals",
          "get",
          "hashCode",
          "indexOf",
          "lastIndexOf",
          "remove",
          "clear",
          "removeAll",
          "retainAll",
          "set",
          "subList",
          "toArray",
          "of",
          "sort"
      )),

      Map.entry("java.util.ArrayList", Set.of(
          "new"
      )),

      Map.entry("java.util.Set", Set.of(
          "size",
          "isEmpty",
          "add",
          "addAll",
          "contains",
          "containsAll",
          "equals",
          "hashCode",
          "remove",
          "removeAll",
          "retainAll",
          "toArray"
      )),

      Map.entry("java.util.HashSet", Set.of(
          "new"
      )),

      Map.entry("com.google.common.collect.Sets", Set.of(
          "newHashSet"
      )),

      Map.entry("java.util.Collections", Set.of(
          "singleton",
          "singletonList",
          "unmodifiableList"
      )),

      Map.entry("java.util.stream.Stream", Set.of(
          "toList",
          "toArray",
          "forEach",
          "forEachOrdered",
          "count",
          "anyMatch",
          "allMatch",
          "noneMatch",
          "min",
          "max",
          "findFirst",
          "findAny",
          "reduce",
          "collect"
      )),

      Map.entry("java.util.Map", Set.of(
          "keySet",
          "entrySet"
      )),

      Map.entry("java.lang.String", Set.of(
          "join"
      )),

      Map.entry("java.lang.Iterable", Set.of(
          "forEach"
      )),

      Map.entry("java.util.Arrays", Set.of(
          "asList"
      ))
  );*/
}
