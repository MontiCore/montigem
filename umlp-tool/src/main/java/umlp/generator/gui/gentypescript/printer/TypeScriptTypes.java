/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.gentypescript.printer;

import de.monticore.guidsl._symboltable.BuiltInTypes;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types3.SymTypeRelations;
import umlp.generator.domain.UMLPGeneratorConfig;
import umlp.generator.gui.GUIGeneratorUtil;

import java.util.Optional;

public enum TypeScriptTypes {
  OBJECT("Object", "any", "java.lang.Object"),
  STRING("String", "string"),
  EVENT(BuiltInTypes.EVENT.getName(), "Event",
      BuiltInTypes.EVENT.getQName(), "@umlp/commonj2ts"),
  TEMPLATE_REF(BuiltInTypes.GUI_VIEW_ELEMENT.getName(), "TemplateRef",
      BuiltInTypes.GUI_VIEW_ELEMENT.getQName(), "@angular/core"),
  // TODO: set proper router service when implemented
  ROUTER("Router", "Router",
      "umlp.guiutil.Router", "@angular/router"),
  APP_ROUTER("AppRouter", "AppRouter", "app.AppRouter"),
  LIST("List", "Array", "java.util.List"),
  ASYNC_LIST("List", "IUMLPList", "java.util.List"),
  ARRAY_LIST("ArrayList", "Array", "java.util.ArrayList"),
  ASYNC_ARRAY_LIST("ArrayList", "IUMLPList", "java.util.ArrayList"),
  COLLECTION("Collection", "Array", "java.util.Collection"),
  MAP("Map", "Map", "java.util.Map"),
  ASYNC_MAP("Map", "IUMLPMap", "java.util.Map"),
  HASH_MAP("HashMap", "Map", "java.util.HashMap"),
  ASYNC_HASH_MAP("HashMap", "IUMLPMap", "java.util.HashMap"),
  LOCAL_DATE("LocalDate", "Date", "java.time.LocalDate"),
  LOCAL_DATE_TIME("LocalDateTime", "Date", "java.time.LocalDateTime"),
  ITERATOR("Iterator", "Iterator", "java.util.Iterator"),
  VOID("void", "void" ),
  VOID_BOXED("Void", "void", "java.lang.Void"),
  BOOLEAN("boolean", "boolean"),
  BOOLEAN_BOXED("Boolean", "boolean", "java.lang.Boolean"),
  INT("int", "number"),
  INT_BOXED("Integer", "number", "java.lang.Integer"),
  DOUBLE("double", "number"),
  DOUBLE_BOXED("Double", "number", "java.lang.Double"),
  LONG("long", "number"),
  LONG_BOXED("Long", "number", "java.lang.Long"),
  FLOAT("float", "number"),
  FLOAT_BOXED("Float", "number", "java.lang.Float"),
  RUNNABLE("Runnable", "() => void", "java.lang.Runnable"),
  CONSUMER("Consumer", "Event", "java.util.function.Consumer", "@umlp/commonj2ts"),
  DIRECTIVE("Directive", "Directive", "umlp.jsweet.extension.annotation.Directive", "@angular/core"),
  GEM_LIST("GemList", "GemList", "umlp.backendrte.common.GemList", "@umlp/commonj2ts");

  private final String tsType;
  private final String mcType;
  private final Optional<String> javaQName;
  private final Optional<String> tsImportPath;

  TypeScriptTypes(String mcType, String tsType, String javaQName, String tsImportPath) {
    this.mcType = mcType;
    this.tsType = tsType;
    this.javaQName = Optional.of(javaQName);
    this.tsImportPath = Optional.of(tsImportPath);
  }

  TypeScriptTypes(String mcType, String tsType, String javaQName) {
    this.mcType = mcType;
    this.tsType = tsType;
    this.javaQName = Optional.of(javaQName);
    this.tsImportPath = Optional.empty();
  }

  TypeScriptTypes(String mcType, String tsType) {
    this.mcType = mcType;
    this.tsType = tsType;
    this.javaQName = Optional.empty();
    this.tsImportPath = Optional.empty();
  }

  public static Optional<String> getTsImportPath(String mcType) {
    for (TypeScriptTypes type : TypeScriptTypes.values()) {
      if ((type.mcType.equals(mcType)
          || (type.javaQName.isPresent() && type.javaQName.get().equals(mcType)))) {
        return type.tsImportPath;
      }
    }
    return Optional.empty();
  }

  public static String toTSType(String mcType) {
    String res = mcType;

    for (TypeScriptTypes type : TypeScriptTypes.values()) {
      if ((type.mcType.equals(mcType)
          || (type.javaQName.isPresent() && type.javaQName.get().equals(mcType)))) {
        return type.tsType;
      }
    }

    return res;
  }

  public static String toAsyncTSType(String mcType) {
    String res = mcType;

    for (TypeScriptTypes type : TypeScriptTypes.values()) {

      if ((type.mcType.equals(mcType)
          || (type.javaQName.isPresent() && type.javaQName.get().equals(mcType)))) {
        if(type == LIST) {
          return ASYNC_LIST.tsType;
        } else if(type == ARRAY_LIST)  {
          return ASYNC_ARRAY_LIST.tsType;
        } else if(type == MAP)  {
          return ASYNC_MAP.tsType;
        } else if(type == HASH_MAP)  {
          return ASYNC_HASH_MAP.tsType;
        } else {
          return type.tsType;
        }
      }
    }

    return res;
  }

  public static boolean isDefault(String mcType) {
    for (TypeScriptTypes type : TypeScriptTypes.values()) {
      if ((type.mcType.equals(mcType)
          || (type.javaQName.isPresent() && type.javaQName.get().equals(mcType)))) {
        return true;
      }
    }
    return GUIGeneratorUtil.getInstance().isBuiltIn(mcType);
  }

  // TODO AGe: deprecated, unclean
  public static boolean isString(SymTypeExpression type) {
    if (toTSType(type.print()).equals(STRING.getTSType())) {
      return true;
    }
    /*
    TypeCalculator tc = new TypeCalculator(
        new FullSynthesizeFromGUIDSL(), new FullDeriveFromGUIDSL()
    );
    return tc.isString(type)
        || tc.isChar(type);
    /**/
    return SymTypeRelations.isString(type) || SymTypeRelations.isChar(type);
  }

  public static boolean isNumber(SymTypeExpression type) {
    return SymTypeRelations.isDouble(type)
        || SymTypeRelations.isInt(type)
        || SymTypeRelations.isFloat(type)
        || SymTypeRelations.isLong(type)
        || SymTypeRelations.isShort(type)
        || SymTypeRelations.isByte(type);
  }

  public String getMCType() {
    return mcType;
  }

  public String getTSType() {
    return tsType;
  }

  public Optional<String> getTsImportPath() {
    return tsImportPath;
  }

  public Optional<String> getJavaQName() {
    return javaQName;
  }
}

