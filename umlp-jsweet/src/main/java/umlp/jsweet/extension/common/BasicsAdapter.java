/* (c) https://github.com/MontiCore/monticore */
package umlp.jsweet.extension.common;

import org.jsweet.transpiler.ModuleImportDescriptor;
import org.jsweet.transpiler.extension.PrinterAdapter;
import org.jsweet.transpiler.model.CompilationUnitElement;
import org.jsweet.transpiler.model.ExtendedElement;
import org.jsweet.transpiler.model.ImportElement;
import org.jsweet.transpiler.model.MethodInvocationElement;
import org.jsweet.transpiler.model.NewClassElement;
import org.jsweet.transpiler.model.TypeCastElement;
import umlp.jsweet.extension.util.RTEPathUtil;
import umlp.jsweet.extension.util.UMLPAdapterUtils;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.function.Function;

/**
 * This adapter is responsible to correctly map Elements from the MontiCore world to correct
 * TS implementaitons.
 * This is done mainly for de.monticore.symboltable.serialization.json.* and de.se_rwth.commons.logging.Log
 */
public class BasicsAdapter extends AbstractAdapter {

  private final UMLPAdapterUtils adapterUtils;

  public BasicsAdapter(PrinterAdapter parentAdapter) {
    super(parentAdapter);
    initTypeMappings();
    this.adapterUtils = new UMLPAdapterUtils(this, types(), context, util());
  }

  private void initTypeMappings() {
    getTypeMirrorMappings().add(0, computeMCJSonTypeMapping());
    // also by name: getMappedType (e.g. of type arguments, as in Optional<List<JsonElement>>) only applies these
    addTypeMapping("de.monticore.symboltable.serialization.json.JsonObject", "any");
    addTypeMapping("de.monticore.symboltable.serialization.json.JsonElement", "any");
  }

  public Function<TypeMirror, String> computeMCJSonTypeMapping() {
    return (TypeMirror type) -> {
      if (type.toString().equals("de.monticore.symboltable.serialization.json.JsonObject")) {
        return "any";
      }
      if (type.toString().equals("de.monticore.symboltable.serialization.json.JsonElement")) {
        return "any";
      }
      return null;
    };
  }

  @Override
  protected void initStaticLibrary() {
    addToStaticLibrary("de.se_rwth.commons.logging.Log");
    addToStaticLibrary("de.monticore.symboltable.serialization.JsonPrinter");
    addToStaticLibrary("de.se_rwth.commons.StringTransformations");
  }

  @Override
  public boolean substituteMethodInvocation(MethodInvocationElement invocation) {
    if (invocation.getTargetType() == null) {
      return super.substituteMethodInvocation(invocation);
    }

    String targetType = invocation.getTargetType().toString();
    String methodName = invocation.getMethodName();
    switch (targetType) {
      case "de.monticore.symboltable.serialization.JsonParser":
        if (substituteJsonParserMethods(invocation, methodName)) {
          return true;
        }
        break;
      case "de.monticore.symboltable.serialization.json.JsonObject":
      case "de.monticore.symboltable.serialization.json.JsonElement":
      case "de.monticore.symboltable.serialization.json.JsonString":
      case "de.monticore.symboltable.serialization.json.JsonArray":
      case "de.monticore.symboltable.serialization.json.JsonBoolean":
      case "de.monticore.symboltable.serialization.json.JsonNull":
      case "de.monticore.symboltable.serialization.json.JsonNumber":
      case "de.monticore.symboltable.serialization.json.JsonTraceableJsonObject":
      case "de.monticore.symboltable.serialization.json.JsonUserJsonString":
        if (substituteJsonElementMethods(invocation, methodName)) {
          return true;
        }
        break;
    }

    return super.substituteMethodInvocation(invocation);
  }

  // Replace with JSON.parse
  private boolean substituteJsonParserMethods(MethodInvocationElement invocation,
                                              String methodName) {
    switch (methodName) {
      case "parseJsonObject":
      case "parse":
        this.print("JSON.parse(").printArgList(invocation.getArguments()).print(")");
        return true;
    }
    return false;
  }

  // Replace with the TS access operator "[x]"
  private boolean substituteJsonElementMethods(MethodInvocationElement invocation,
                                               String methodName) {
    switch (methodName) {
      case "isJsonObject":
        adapterUtils.printMacroName(methodName);
        this.print("(typeof ").print(invocation.getTargetExpression()).print(" == \"object\")");
        return true;
      case "isJsonArray":
        adapterUtils.printMacroName(methodName);
        this.print("(Array.isArray(").print(invocation.getTargetExpression()).print("))");
        return true;
      case "isJsonBoolean":
        adapterUtils.printMacroName(methodName);
        this.print("(typeof ").print(invocation.getTargetExpression()).print(" == \"boolean\")");
        return true;
      case "isJsonString":
        adapterUtils.printMacroName(methodName);
        this.print("(typeof ").print(invocation.getTargetExpression()).print(" == \"string\")");
        return true;
      case "isJsonNumber":
        adapterUtils.printMacroName(methodName);
        this.print("(typeof ").print(invocation.getTargetExpression()).print(" == \"number\")");
        return true;
      //          case "isJsonNull":
      case "getAsJsonObject":
      case "getAsJsonArray":
      case "getAsJsonBoolean":
      case "getAsJsonString":
      case "getAsJsonNumber":
      case "getNumberAsFloat":
      case "getNumberAsDouble":
      case "getNumberAsInteger":
      case "getNumberAsLong":
      case "getValue":
        //          case "putMember":
        //          case "removeMember":
        //          case "getMemberNames":
      case "toString":
        adapterUtils.printMacroName(methodName);
        this.print(invocation.getTargetExpression());
        return true;
      //          case "getMembers":
      //          case "setMembers":
      //          case "sizeMembers":
      //          case "hasMembers":
      case "hasMember":
        adapterUtils.printMacroName(methodName);
        this.print("(").print(invocation.getTargetExpression()).print("[")
            .printArgList(invocation.getArguments()).print("] != undefined && ")
            .print(invocation.getTargetExpression()).print("[")
            .printArgList(invocation.getArguments()).print("] != null)");
        return true;
      case "hasStringMember":
        adapterUtils.printMacroName(methodName);
        this.print("(typeof ").print(invocation.getTargetExpression()).print("[").printArgList(
            invocation.getArguments()).print("] == \"string\")");
        return true;
      case "hasArrayMember":
        adapterUtils.printMacroName(methodName);
        this.print("(Array.isArray(").print(invocation.getTargetExpression()).print("[")
            .printArgList(
                invocation.getArguments()).print("]))");
        return true;
      case "hasBooleanMember":
        adapterUtils.printMacroName(methodName);
        this.print("(typeof ").print(invocation.getTargetExpression()).print("[").printArgList(
            invocation.getArguments()).print("] == \"boolean\")");
        return true;
      case "hasObjectMember":
        adapterUtils.printMacroName(methodName);
        this.print("(typeof ").print(invocation.getTargetExpression()).print("[").printArgList(
            invocation.getArguments()).print("] == \"object\")");
        return true;
      case "hasIntegerMember":
      case "hasDoubleMember":
      case "hasFloatMember":
      case "hasLongMember":
        adapterUtils.printMacroName(methodName);
        this.print("(typeof ").print(invocation.getTargetExpression()).print("[").printArgList(
            invocation.getArguments()).print("] == \"number\")");
        return true;
      case "getValues":
        // JsonArray#getValues: List<JsonElement> -> an AsyncList over the elements of the JSON array
        adapterUtils.printMacroName(methodName);
        this.print("(await new " + CollectionsAdapter.CustomAsyncListImplementationConstructor + "(")
            .print(invocation.getTargetExpression()).print("))");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName,
            getLibraryLocation()));
        return true;
      case "getArrayMember":
        // List<JsonElement> -> an AsyncList over the elements of the JSON array (not AsyncList.of, which would
        // make the whole array its only element)
        adapterUtils.printMacroName(methodName);
        this.print("(await new " + CollectionsAdapter.CustomAsyncListImplementationConstructor + "(")
            .print(invocation.getTargetExpression()).print("[")
            .printArgList(invocation.getArguments()).print("]))");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName,
            getLibraryLocation()));
        return true;
      case "getArrayMemberOpt":
        // Optional<List<JsonElement>> -> AsyncList | null (see OptionalAdapter): empty if the member is no JSON array
        adapterUtils.printMacroName(methodName);
        this.print("(await (async (array) => Array.isArray(array) ? await new "
                + CollectionsAdapter.CustomAsyncListImplementationConstructor + "(array) : null)(")
            .print(invocation.getTargetExpression()).print("[")
            .printArgList(invocation.getArguments()).print("]))");
        getPrinter().useModule(new ModuleImportDescriptor(CollectionsAdapter.CustomAsyncListImplementationName,
            getLibraryLocation()));
        return true;
      case "getStringMember":
      case "getStringMemberOpt":
      case "getBooleanMember":
      case "getBooleanMemberOpt":
      case "getObjectMember":
      case "getObjectMemberOpt":
      case "getIntegerMember":
      case "getIntegerMemberOpt":
      case "getDoubleMember":
      case "getDoubleMemberOpt":
      case "getFloatMember":
      case "getFloatMemberOpt":
      case "getLongMember":
      case "getLongMemberOpt":
      case "getMember":
        adapterUtils.printMacroName(methodName);
        this.print(invocation.getTargetExpression()).print("[")
            .printArgList(invocation.getArguments()).print("]");
        return true;
    }
    return false;
  }

  // Better overload
  @Override
  public boolean substituteNewClass(NewClassElement newClass) {
    String className = newClass.getTypeAsElement().toString();
    switch (className) {
      case "umlp.backendrte.command.SimpleResult":
        if (newClass.getArguments().size() == 3) {
          ExtendedElement argument = newClass.getArgument(1);
          if (argument.getTypeAsElement().toString()
              .equals("umlp.backendrte.common.LoadingState")) {
            print("new SimpleResult().createFromLoadingState(").printArgList(
                newClass.getArguments()).print(")");
            return true;
          }
          else {
            print("new SimpleResult().createFromObject(").printArgList(newClass.getArguments())
                .print(")");
            return true;
          }
        }
      case "input.GemTextInputValidity":
      case "mc.fenix.input.GemTextInputValidity":
      case "input.src.lib.gemtextinput.GemTextInputTypes.GemTextInputValidity":
      case "mc.fenix.input.GemTextInputTypes.GemTextInputValidity":
      case "mc.fenix.input.gemtextinputtypes.GemTextInputValidity":
        if (newClass.getArguments().size() == 0) {
          print("{}");
          return true;
        }
        else if (newClass.getArguments().size() == 2) {
          return super.substituteNewClass(newClass);
        }
        else {
          // ERROR
          return false;
        }
      default:
    }

    return super.substituteNewClass(newClass);
  }

  @Override
  public String needsImport(ImportElement importElement, String qualifiedName) {
    /*
    switch (qualifiedName) {
      case "input.GemTextInputValidity":
      case "mc.fenix.input.GemTextInputValidity":
      case "input.src.lib.gemtextinput.GemTextInputTypes.GemTextInputValidity":
      case "mc.fenix.input.GemTextInputTypes.GemTextInputValidity":
      case "mc.fenix.input.gemtextinputtypes.GemTextInputValidity":
        return getRootRelativeName(importElement.getImportedType());
    }
    /**/
    return super.needsImport(importElement, qualifiedName);
  }

  @Override
  public ModuleImportDescriptor getModuleImportDescriptor(
      CompilationUnitElement currentCompilationUnit, String importedName,
      TypeElement importedClass) {
    /*
    String fqn = importedClass.getQualifiedName().toString();
    switch (fqn) {
      case "input.GemTextInputValidity":
      case "mc.fenix.input.GemTextInputValidity":
      case "input.src.lib.gemtextinput.GemTextInputTypes.GemTextInputValidity":
      case "mc.fenix.input.GemTextInputTypes.GemTextInputValidity":
      case "mc.fenix.input.gemtextinputtypes.GemTextInputValidity":
        return new ModuleImportDescriptor(false, (PackageElement) importedClass.getEnclosingElement(),
            "GemTextInputValidity", "@umlp/input", importedClass);

    }
    /**/
    return super.getModuleImportDescriptor(currentCompilationUnit, importedName, importedClass);
  }

  @Override
  public boolean substituteTypeCast(TypeCastElement castExpression) {
    if (castExpression.getExpression().toString().equals("this")) {
      print("<").print(
          types().asElement(castExpression.getType()).getSimpleName().toString()).print(">");
      print("<unknown>").print(castExpression.getExpression());
      return true;
    }
    return super.substituteTypeCast(castExpression);
  }

  @Override
  public void afterTypeBody(TypeElement type) {
    super.afterTypeBody(type);
    if (adapterUtils.isSubTypeOf(type, RTEPathUtil.getBackendRTECommonPath("GemIdentifiable"))
        && !getContext().isInterface(type)) {
      boolean hasTOP = type.getSuperclass() == null ? false :
          types().asElement(type.getSuperclass()).getSimpleName().toString().equals(
              type.getSimpleName().toString() + "TOP");
      String override = hasTOP ? "override" : "";
      printIndent().print("public " + override + " __className: string = \"").print(
          adapterUtils.getTypeNameWithoutBuilderAndTOP(type,
              false)).print("\";").println();
    }
  }
}
