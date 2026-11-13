/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui;

import com.google.common.base.CaseFormat;
import de.monticore.ast.ASTNode;
import de.monticore.cd.codegen.TopDecorator;
import de.monticore.cd4code._prettyprint.CD4CodeFullPrettyPrinter;
import de.monticore.cd4codebasis._ast.ASTCDParameter;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTLiteralExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.generating.templateengine.reporting.Reporting;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUIAdapt;
import de.monticore.guidsl._ast.ASTGUIBlockContent;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIGuard;
import de.monticore.guidsl._ast.ASTGUIIterate;
import de.monticore.guidsl._ast.ASTGUINamedArg;
import de.monticore.guidsl._ast.ASTGUIParam;
import de.monticore.guidsl._ast.ASTGUIRemove;
import de.monticore.guidsl._ast.ASTGUIShortIterateControl;
import de.monticore.guidsl._prettyprint.GUIDSLFullPrettyPrinter;
import de.monticore.guidsl._symboltable.BuiltInTypes;
import de.monticore.guidsl._symboltable.GUIComponentDeclarationSymbol;
import de.monticore.guidsl._symboltable.GUIParamSymbol;
import de.monticore.guidsl._symboltable.IGUIDSLArtifactScope;
import de.monticore.guidsl._symboltable.IGUIDSLScope;
import de.monticore.io.paths.MCPath;
import de.monticore.literals.mccommonliterals._ast.ASTStringLiteral;
import de.monticore.ocl.setexpressions._ast.ASTSetCollectionItem;
import de.monticore.ocl.setexpressions._ast.ASTSetEnumeration;
import de.monticore.ocl.setexpressions._ast.ASTSetValueItem;
import de.monticore.prettyprint.IndentPrinter;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbolSurrogate;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.symbols.oosymbols._symboltable.IOOSymbolsScope;
import de.monticore.symbols.oosymbols._symboltable.OOTypeSymbol;
import de.monticore.symboltable.IGlobalScope;
import de.monticore.symboltable.IScope;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeOfGenerics;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import de.monticore.types.mcbasictypes._ast.ASTMCPackageDeclaration;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types3.TypeCheck3;
import de.se_rwth.commons.Names;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.TagLookUp;
import umlp.generator.domain.decorator.ManagerDecorator;
import umlp.generator.gui.genjava.decorator.GUI2JavaClassDecorator;
import umlp.generator.gui.genjava.prettyprinter.Expression2NullCheckAccessorPrinter;
import umlp.generator.gui.genjava.prettyprinter.Expression2SetterPrinter;
import umlp.generator.gui.genjava.prettyprinter.GUIViewElementsFinder;
import umlp.generator.gui.genjava.prettyprinter.IterationItemsFinder;
import umlp.generator.gui.gentypescript.config.GUI2ConfigGenerator;
import umlp.generator.gui.gentypescript.printer.TSBackendMap;
import umlp.generator.gui.gentypescript.printer.TSImportMap;
import umlp.generator.gui.gentypescript.printer.TSUtilsPrinter;
import umlp.generator.gui.gentypescript.printer.TypeScriptTypes;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GUIGeneratorUtil {

  public static final String SYNCHRONIZED_SUFFIX_FOR_HTML_TEMPL = "ForAngular__ChangeDetection";
  public static final String NG_TEMPLATE_SUFFIX = "Tpl";
  public static final String NG_TEMPLATE_ELSE_BLOCK_SUFFIX = "Else";
  public static final String HTML_ITERATE_INDEX_PREFIX = "c";

  protected static GUIGeneratorUtil instance;
  protected GUIDSLFullPrettyPrinter printer;
  protected static CD4CodeFullPrettyPrinter cdPrinter;
  // Fully qualified Name of the classdiagram, e.g. "hardy.Flensburg"
  private List<String> DOMAIN_AND_MODULE_MODELS;
  // Fully qualified Name of a gui model, e.g. "stanley.PersonCreate"
  private List<String> CREATE_MODELS;

  private GUIGeneratorUtil() {
    printer = new GUIDSLFullPrettyPrinter(new IndentPrinter());
    cdPrinter = new CD4CodeFullPrettyPrinter(new IndentPrinter());
  }

  public static GUIGeneratorUtil getInstance() {
    if (instance == null) {
      instance = new GUIGeneratorUtil();
    }
    return instance;
  }

  public String replaceDots(String input) {
    Pattern pattern = Pattern.compile("(?<!\\bthis)\\.");
    Matcher matcher = pattern.matcher(input);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      matcher.appendReplacement(result, "?\\.");
    }
    matcher.appendTail(result);

    return result.toString();
  }

  public boolean isComponent(ASTCDClass clazz) {
    return clazz.getName().endsWith("Component");
  }

  public boolean isCommandCall(ASTGUICompilationUnit unit, ASTCallExpression methodCall) {
    if (methodCall.getExpression() instanceof ASTFieldAccessExpression) {
      ASTFieldAccessExpression fieldAccessExpression = (ASTFieldAccessExpression) methodCall.getExpression();
      if (fieldAccessExpression.getExpression() instanceof ASTNameExpression) {
        String variableName = ((ASTNameExpression) fieldAccessExpression.getExpression()).getName();
        Optional<GUIParamSymbol> guiParamOpt = unit.getGUIComponentDeclaration().getSpannedScope().resolveGUIParam(variableName);
        if (guiParamOpt.isPresent()) {
          if (guiParamOpt.get().getType().getTypeInfo() instanceof OOTypeSymbol) {
            OOTypeSymbol typeSymbol = (OOTypeSymbol) guiParamOpt.get().getType().getTypeInfo();
             /*
             For now: Assume that every method call of a singleton is a Command Execution Call
             since taggin look up does not allow to search for method symbols
             */
            return TagLookUp.isSingleton(typeSymbol);
          }
        }

      }
    }
    return false;
  }

  public boolean isCommand(ASTCDClass clazz) {
    return !clazz.getSuperclassList().isEmpty()
        && (cdPrinter.prettyprint(clazz.getSuperclassList().get(0)).startsWith("Command")
        || cdPrinter.prettyprint(clazz.getSuperclassList().get(0)).startsWith("Request"));
  }

  public String getOriginalName(ASTCDClass clazz) {
    return isComponent(clazz) ?
        clazz.getName()
            .substring(0, clazz.getName().lastIndexOf("Component")) :
        clazz.getName();
  }

  public String getPackageName(ASTCDCompilationUnit origCD) {
    String origPackage = Names.constructQualifiedName(
        origCD.getCDPackageList(), origCD.getCDDefinition().getName()
    );
    return origPackage;
  }

  /**
   * Check whether a handwritten version of this generated file already exists and shall be integrated
   * (e.g. to apply TOP mechanism)
   */
  public boolean existsHandwrittenFile(MCPath targetPath, String hwFile) {
    Optional<URL> hwFilePath = targetPath.find(hwFile);
    boolean result = hwFilePath.isPresent();
    if (result) {
      Optional<Path> hwPath = MCPath.toPath(hwFilePath.get());
      hwPath.ifPresent(path -> Reporting.reportUseHandwrittenCodeFile(path,
          Paths.get(hwFile)));
    }
    Reporting.reportHWCExistenceCheck(targetPath, Paths.get(hwFile), hwFilePath);
    return result;
  }

  public boolean existsHandwrittenClass(MCPath targetPath, String qualifiedName) {
    return existsHandwrittenFile(targetPath, Names.getPathFromPackage(qualifiedName) + ".ts") | // Use bitwise to report on both checks
        existsHandwrittenFile(targetPath, Names.getPathFromPackage(qualifiedName) + ".java");
  }

  public boolean isInitialized() {
    return DOMAIN_AND_MODULE_MODELS != null;
  }

  public void init(List<String> domainModels, List<String> createModels) {
    DOMAIN_AND_MODULE_MODELS = domainModels;
    CREATE_MODELS = createModels;
  }

  public static void cleanUp() {
    // keep the instance, several generators hold it in static fields
    if (instance != null) {
      instance.DOMAIN_AND_MODULE_MODELS = null;
      instance.CREATE_MODELS = null;
    }
  }

  public final String print(ASTNode node) {
    printer.setPrintComments(false);
    return printer.prettyprint(node);
  }

  // At some places there must be generated the __synchronized suffix for JSweet generated code
  public final String print(ASTNode node, boolean syncedOpt) {
    printer.setPrintComments(false);
    String res = printer.prettyprint(node);
    if (syncedOpt) {
      res += SYNCHRONIZED_SUFFIX_FOR_HTML_TEMPL;
    }
    return res;
  }

  // TODO AGe: unreliable
  public final boolean isTop(String name) {
    return name.endsWith(TopDecorator.TOP_SUFFIX);
  }

  public final boolean isInputGui(ASTGUICompilationUnit guiAst, List<ASTGUIComponent> inputComponents) {
    return !inputComponents.isEmpty() &&
        guiAst.getGUIComponentDeclaration().getGUIParamList().stream()
            .filter(p -> isDomainType(p)).findAny().isPresent();
  }

  public final boolean isDomainType(ASTGUIParam param) {
    if (!param.isPresentSymbol()) {
      Log.error("0x0000E Unable to find symbol for '" + param.getName()
          + "' at " + param.get_SourcePositionStart().toString()
          + " - " + param.get_SourcePositionEnd().toString()
      );
    }
    return isDomainType(param.getSymbol().getType());
  }

  public final boolean isDomainType(SymTypeExpression type) {
    if (!type.hasTypeInfo()) {
      return false;
    }
    TypeSymbol ts = type.getTypeInfo();
    if (!(ts.getEnclosingScope() instanceof IGUIDSLArtifactScope)) {
      return false;
    }
    IGUIDSLArtifactScope as = (IGUIDSLArtifactScope) ts.getEnclosingScope();
    return DOMAIN_AND_MODULE_MODELS.contains(as.getFullName());
  }

  public final boolean isDomainListType(ASTGUIParam param) {
    SymTypeExpression type = param.getSymbol().getType();
    return isDomainListType(type);
  }

  public final boolean isDomainListType(SymTypeExpression type) {
    return getDomainListInnerType(type).isPresent();
  }

  public Optional<SymTypeExpression> getDomainListInnerType(SymTypeExpression type) {
    if (type.hasTypeInfo() && GenService.isList(type)) {
      if (((SymTypeOfGenerics) type).sizeArguments() == 1) {
        SymTypeExpression inner = ((SymTypeOfGenerics) type).getArgument(0);
        TypeSymbol ts = inner.getTypeInfo();
        if (ts.getEnclosingScope() instanceof IGUIDSLArtifactScope) {
          IGUIDSLArtifactScope as = (IGUIDSLArtifactScope) ts.getEnclosingScope();
          if (DOMAIN_AND_MODULE_MODELS.contains(as.getFullName())) {
            return Optional.of(inner);
          }
        }
      }
    }
    return Optional.empty();
  }

  public boolean isEnum(String name, IOOSymbolsScope scope) {
    Optional<OOTypeSymbol> sym = scope.resolveOOType(name);
    return sym.isPresent() && sym.get().isIsEnum();
  }

  public final boolean isEnumType(SymTypeExpression type) {
    if (!type.hasTypeInfo()) {
      return false;
    }
    Optional<TypeSymbol> symb = type.getTypeInfo().getEnclosingScope().resolveType(type.getTypeInfo().getName());
    if (symb.isPresent()) {
      if (symb.get() instanceof OOTypeSymbol ooTypeSymbol) {
        return ooTypeSymbol.isIsEnum();
      }
    }
    return false;
  }

  public boolean isEnumMember(ASTFieldAccessExpression node) {
    GUIGeneratorUtil util = GUIGeneratorUtil.getInstance();
    SymTypeExpression ste = util.getType(node);
    boolean res = false;

    if (ste.hasTypeInfo()
        && ste.getTypeInfo() instanceof TypeSymbolSurrogate stesurr
        && stesurr.lazyLoadDelegate() instanceof OOTypeSymbol ooTypeSymbol) {
      res = ooTypeSymbol.isIsEnum();
    }
    if (node.getEnclosingScope() instanceof IOOSymbolsScope scope) {
      res = res && util.isEnum(util.print(node.getExpression()), scope);
    }
    else {
      Log.error("0x0000C Attempted to resolve type of '"
          + util.print(node) + "' from outside of an OOSymbolsScope,"
          + " but expression conversion is restricted to OOSymbols context.");
      return false;
    }

    return res;
  }

  public final boolean isDomainEnumType(SymTypeExpression type) {
    Optional<TypeSymbol> symb = type.getTypeInfo().getEnclosingScope().resolveType(type.getTypeInfo().getName());
    if (symb.isPresent()) {
      if (symb.get().getEnclosingScope() instanceof IGUIDSLArtifactScope as) {
        boolean isDomain = DOMAIN_AND_MODULE_MODELS.contains(as.getFullName());
        if (isDomain && symb.get() instanceof OOTypeSymbol ooTypeSymbol) {
          return ooTypeSymbol.isIsEnum();
        }
      }
    }
    return false;
  }

  public final boolean isOptionalEnumType(SymTypeExpression type) {
    if (type.hasTypeInfo() && GenService.isOptional(type) && ((SymTypeOfGenerics) type).sizeArguments() == 1) {
      SymTypeExpression inner = ((SymTypeOfGenerics) type).getArgument(0);
      return isEnumType(inner);
    }
    return false;
  }

  public final boolean isDomainOptionalEnumType(SymTypeExpression type) {
    if (type.hasTypeInfo() && GenService.isOptional(type) && ((SymTypeOfGenerics) type).sizeArguments() == 1) {
      SymTypeExpression inner = ((SymTypeOfGenerics) type).getArgument(0);
      return isDomainEnumType(inner);
    }
    return false;
  }

  public final boolean isTSPackageImport(String javaPackage) {
    if (!TSImportMap.INSTANCE.isTSPackageImport(javaPackage)) {
      return false;
    }
    return !TSBackendMap.isBackendRTEPackage(javaPackage);
  }

  public final boolean isListType(SymTypeExpression type) {
    return type.hasTypeInfo() && GenService.isList(type);
  }

  public final String getDomainListInnerType(ASTGUIParam astguiParam) {
    SymTypeExpression type = astguiParam.getSymbol().getType();
    if (type.hasTypeInfo() && GenService.isList(type)) {
      if (((SymTypeOfGenerics) type).sizeArguments() == 1) {
        SymTypeExpression inner = ((SymTypeOfGenerics) type).getArgument(0);
        TypeSymbol ts = inner.getTypeInfo();
        if (ts.getEnclosingScope() instanceof IGUIDSLArtifactScope) {
          return ts.getName();
        }
      }
    }
    String res = astguiParam.getMCType().printType();
    res = res.replaceAll("List<", "");
    res = res.replaceAll("", ">");
    return res;
  }

  public final Optional<SymTypeExpression> getListInnerType(SymTypeExpression type) {
    if (GenService.isList(type)) {
      return getFirstInnerType(type);
    }
    return Optional.empty();
  }

  public Optional<SymTypeExpression> getFirstInnerType(SymTypeExpression type) {
    if (type.isGenericType()) {
      if (type.asGenericType().sizeArguments() > 0) {
        return Optional.of(type.asGenericType().getArgument(0));
      }
    }
    return Optional.empty();
  }

  public final boolean isDomainImport(ASTMCImportStatement i) {
    return isDomainImport(i.getQName());
  }

  public final boolean isDomainImport(String qName) {
    for (String cd : DOMAIN_AND_MODULE_MODELS) {
      if (qName.startsWith(cd)) {
        return true;
      }
    }
    return false;
  }

  public final boolean isGuiImport(ASTMCImportStatement i) {
    return isGuiImport(i.getQName());
  }

  public final boolean isGuiImport(String qName) {
    return GUIDSLMill.globalScope().resolveGUIComponentDeclaration(qName).isPresent();
  }

  public final boolean isBuiltIn(String qName) {
    for (BuiltInTypes type : BuiltInTypes.values()) {
      if (type.getQName().equals(qName)) {
        return true;
      }
    }
    return false;
  }

  public final boolean isOptionalNumberType(SymTypeExpression type) {
    if (type.hasTypeInfo() && GenService.isOptional(type) && ((SymTypeOfGenerics) type).sizeArguments() == 1) {
      SymTypeExpression inner = ((SymTypeOfGenerics) type).getArgument(0);
      return TypeScriptTypes.isNumber(inner);
    }
    return false;
  }

  public final SymTypeExpression getOptionalTypeArgument(SymTypeExpression type) {
    if (type.hasTypeInfo() && GenService.isOptional(type) && ((SymTypeOfGenerics) type).sizeArguments() == 1) {
      SymTypeExpression inner = ((SymTypeOfGenerics) type).getArgument(0);
      return ((SymTypeOfGenerics) type).getArgument(0);
    }
    return type;
  }

  public final boolean isOptional(SymTypeExpression type) {
    return type.hasTypeInfo() && type.isGenericType() && GenService.isOptional(type);
  }

  public final boolean isBuiltIn(ASTMCImportStatement i) {
    return isBuiltIn(i.getQName());
  }

  // TODO AGe: indicator of "main" model after cdmerge is built in
  public final Optional<String> getMainDomainModelQName() {
    if (!DOMAIN_AND_MODULE_MODELS.isEmpty()) {
      return Optional.of(DOMAIN_AND_MODULE_MODELS.get(0));
    }
    return Optional.empty();
  }

  public final Optional<String> getMainDomainModelSimpleName() {
    Optional<String> qName = getMainDomainModelQName();
    if (qName.isPresent()) {
      String[] parts = qName.get().split("\\.");
      return Optional.of(parts[parts.length - 1]);
    }
    return Optional.empty();
  }


  public final boolean isCreateModel(String componentQName) {
    return CREATE_MODELS.contains(componentQName);
  }

  public String getBaseClassName(ASTGUIComponentDeclaration decl) {
    return decl.getName();
  }

  public boolean isInputComponent(ASTGUIComponent guiComponent) {
    Optional<GUIComponentDeclarationSymbol> guiComponentSymbol = guiComponent.getEnclosingScope()
        .resolveGUIComponentDeclaration(guiComponent.getNameSymbol().getFullName());
    return !guiComponentSymbol.isPresent() || guiComponent.getGUINamedArgs().getGUINamedArgList().stream()
        .anyMatch(a -> isInputArgument(guiComponentSymbol.get(), a));
  }

  public boolean isInputArgument(ASTGUIComponent guiComponent, ASTGUINamedArg arg) {
    Optional<GUIComponentDeclarationSymbol> guiComponentSymbol = guiComponent.getEnclosingScope()
        .resolveGUIComponentDeclaration(guiComponent.getNameSymbol().getFullName());
    return !guiComponentSymbol.isPresent() || isInputArgument(guiComponentSymbol.get(), arg);
  }

  public boolean isComponentArgument(ASTGUIComponent guiComponent, ASTExpression arg) {
    return arg instanceof ASTGUIComponent;
  }

/*  public  List<ASTGUINamedArg> getComponentArguments(ASTGUIComponent guiComponent) {
    return guiComponent.getGUINamedArgs().getGUINamedArgList().stream().filter(a -> isComponentArgument(guiComponent, a))
       .collect(Collectors.toList());
  }*/

  public List<ASTGUIComponent> getComponentAsArguments(ASTGUIComponent guiComponent) {
    return guiComponent.getGUINamedArgs().getGUINamedArgList().stream().map(
        ASTGUINamedArg::getExpression).filter(a -> isComponentArgument(guiComponent, a))
        .map(a -> (ASTGUIComponent) a).collect(Collectors.toList());
  }

  public List<ASTExpression> getComponentArguments(ASTGUIComponent guiComponent) {
    List<ASTExpression> values = new ArrayList<>();
    values.addAll(guiComponent.getGUINamedArgs().getGUINamedArgList().stream().map(a -> a.getExpression())
        .filter(a -> isComponentArgument(guiComponent, a)).collect(Collectors.toList()));
    values.addAll(guiComponent.getGUINamedArgs().getGUINamedArgList().stream().map(a -> a.getExpression())
        .filter(a -> a instanceof ASTSetEnumeration)
        .flatMap(a -> ((ASTSetEnumeration) a).getSetCollectionItemList().stream())
        .map(v -> (ASTSetValueItem) v)
        .filter(v -> isComponentArgument(guiComponent, v.getExpression()))
        .map(ASTSetValueItem::getExpression).toList());
    return values;
  }

  public List<ASTExpression> getComponentListArguments(ASTGUIComponent guiComponent, ASTExpression arg) {
    List<ASTExpression> values = new ArrayList<>();
    if (!(arg instanceof ASTSetEnumeration)) {
      return values;
    }
    values.addAll(((ASTSetEnumeration) arg)
        .getSetCollectionItemList().stream()
        .filter(v -> v instanceof ASTSetValueItem)
        .map(v -> ((ASTSetValueItem) v).getExpression())
        .filter(v -> isComponentArgument(guiComponent, v)).collect(Collectors.toList()));
    return values;
  }

  public Optional<GUIParamSymbol> resolveGUIParamSym(
      IGUIDSLScope scope, String compName, String argName
  ) {
    Optional<GUIComponentDeclarationSymbol> declSym = scope.resolveGUIComponentDeclaration(compName);
    if (declSym.isPresent()) {
      return declSym.get().getSpannedScope().resolveGUIParam(argName);
    }
    return Optional.empty();
  }

  public String gui2cdAttrName(String componentId, String argName) {
    return componentId + StringTransformations.capitalize(argName);
  }

  public String gui2cdAttrGetterName(String domainParamName) {
    return "get" + StringTransformations.capitalize(domainParamName);
  }

  public String gui2cdAttrBooleanGetterName(String domainParamName) {
    return "is" + StringTransformations.capitalize(domainParamName);
  }

  public String gui2cdAttrSetterName(String domainParamName) {
    return "set" + StringTransformations.capitalize(domainParamName);
  }

  public String gui2cdAttrBuilderSetterName(String domainParamName) {
    return domainParamName;
  }

  public String gui2cdAttrGetterName(String componentId, String argName) {
    return "get"
        + StringTransformations.capitalize(componentId)
        + StringTransformations.capitalize(argName);
  }

  public String gui2cdAttrGetterNameTS(String componentId, ASTGUINamedArg namedArg) {
    return "get"
        + StringTransformations.capitalize(componentId)
        + StringTransformations.capitalize(namedArg.getName())
        + SYNCHRONIZED_SUFFIX_FOR_HTML_TEMPL;
  }

  public boolean isList(ASTGUINamedArg namedArg) {
    IGUIDSLScope enclosingScope = namedArg.getEnclosingScope();
    if (enclosingScope == null) {
      return false;
    }
    Optional<GUIParamSymbol> guiParamSymbolOpt = enclosingScope.resolveGUIParam(namedArg.getName());
    if (guiParamSymbolOpt.isEmpty() || guiParamSymbolOpt.get().getType() == null) {
      return false;
    }
    return GenService.isList(guiParamSymbolOpt.get().getType());
  }

  @SuppressWarnings("unused") // Component.ftl
  @Deprecated(forRemoval = true)
  public String getIsSynchronizedSuffix() {
    return SYNCHRONIZED_SUFFIX_FOR_HTML_TEMPL;
    /*
    if (true) return ""; // no longer required due to | async
    return UMLPGeneratorConfig.getInstance().isGenerateTypescript() ? SYNCHRONIZED_SUFFIX : "";
    /**/
  }

  public String gui2cdAttrSetterName(String componentId, String argName) {
    return "set"
        + StringTransformations.capitalize(componentId)
        + StringTransformations.capitalize(argName);
  }

  public String gui2cdEventCallName(ASTGUIComponent guiComponent, String eventName) {
    return StringTransformations.uncapitalize(guiComponent.getId().getName())
        + StringTransformations.capitalize(eventName);
  }

  public String getShortIteratorItemName(ASTGUIIterate iterate) {
    if (iterate.getIterateControl() instanceof ASTGUIShortIterateControl) {
      return ((ASTGUIShortIterateControl) iterate.getIterateControl()).getItem().getName();
    }
    Log.error("0x00004 iterate construct " + iterate.getId().getName()
        + " was not processed: range iterate control not supported yet");
    return "";
  }

  public String getIteratorCounterName(ASTGUIIterate iterate) {
    return HTML_ITERATE_INDEX_PREFIX + StringTransformations.capitalize(iterate.getId().getName());
  }

  public String printShortIteratorIterable(ASTGUIIterate iterate) {
    ASTExpression e = getShortIteratorIterable(iterate);
    IterationItemsFinder finder = new IterationItemsFinder();
    Collection<ASTExpression> iterationItems = new LinkedHashSet<>(finder.getItems(iterate));

    GUIViewElementsFinder gveFinder = new GUIViewElementsFinder();
    Set<ASTExpression> ignore = new LinkedHashSet<>(iterationItems);
    ignore.addAll(gveFinder.getGUIViewElements(iterate));

    Expression2NullCheckAccessorPrinter printer = new Expression2NullCheckAccessorPrinter(
        new IndentPrinter(), true, ignore, iterationItems
    );

    return printer.prettyprint(e);
  }

  public ASTExpression getShortIteratorIterable(ASTGUIIterate iterate) {
    if (iterate.getIterateControl() instanceof ASTGUIShortIterateControl) {
      ASTGUIShortIterateControl ic = ((ASTGUIShortIterateControl) iterate.getIterateControl());
      return ic.getIterable();
    }
    Log.error("0x00004 iterate construct " + iterate.getId().getName()
        + " was not processed: range iterate control not supported yet");
    return null;
  }

  public String getIterableGetter(ASTGUIIterate iterate) {
    String name = iterate.getId().getName() + StringTransformations.capitalize(
        GUI2JavaClassDecorator.ITERATE_ARG);
    String args = findIterationItemSymbols(iterate.getEnclosingScope()).stream()
        .map(v -> v.getName()).collect(
            Collectors.joining(", "));
    String getter = "get" + StringTransformations.capitalize(name);
    getter = getter + SYNCHRONIZED_SUFFIX_FOR_HTML_TEMPL + "(" + args + ")";
    return getter;
  }

  public String gui2tsGeneralImport(String forComponent, String postfix) {
    Optional<String> domainQName = getMainDomainModelQName();
    if (domainQName.isPresent()) {
      String name = domainQName.get();
      String simpleName = name.substring(name.lastIndexOf(".") + 1);
      if (name.lastIndexOf(".") > 0) {
        String part1 = name.substring(0, name.lastIndexOf("."));
        return part1.toLowerCase() + "." + StringTransformations.uncapitalize(simpleName) + "." + postfix;
      }
      else {
        return name.toLowerCase() + "." + postfix;
      }
    }
    else {
      Log.error("0x00000 GUI Model " + forComponent
          + " is missing a connection to the domain model."
          + " Please specify the domain model using '-dm' flag.");
      return "";
    }
  }

  public String gui2tsManagerImport(String forComponent) {
    Optional<String> domainQName = getMainDomainModelQName();
    if (domainQName.isPresent()) {
      String name = domainQName.get();
      String simpleName = name.substring(name.lastIndexOf(".") + 1);
      return gui2tsGeneralImport(forComponent, simpleName + ManagerDecorator.SUFFIX);
    }
    else {
      Log.error("0x00000 GUI Model " + forComponent
          + " is missing a connection to the domain model."
          + " Please specify the domain model using '-dm' flag.");
      return "";
    }
  }

  public boolean isComponentListArgument(ASTGUIComponent guiComponent, ASTExpression arg) {
    return arg instanceof ASTSetEnumeration &&
        ((ASTSetEnumeration) arg).getSetCollectionItemList().stream()
            .filter(v -> v instanceof ASTSetValueItem)
            .map(v -> (ASTSetValueItem) v)
            .anyMatch(v -> v.getExpression() instanceof ASTGUIComponent);
  }

  public boolean isEventComponent(ASTGUIComponent guiComponent) {
    return guiComponent.getGUINamedArgs().getGUINamedArgList().stream()
        .anyMatch(a -> isEventArgument(guiComponent, a));
  }

  public boolean isCreateClass(ASTGUICompilationUnit ast) {
    return CREATE_MODELS.contains(ast.getComponentQName());
  }

  public boolean isInputArgument(GUIComponentDeclarationSymbol guiComponent, ASTGUINamedArg arg) {
    Optional<GUIParamSymbol> argSymbol = guiComponent.getSpannedScope().resolveGUIParam(arg.getName());
    return argSymbol.isPresent() && argSymbol.get().isIsReference();
  }

  public boolean isEventArgument(ASTGUINamedArg arg) {
    return arg.isPresentNameSymbol() && isEventArgument(arg.getNameSymbol());
  }

  @Deprecated
  public boolean isEventArgument(ASTGUIComponent guiComponent, ASTGUINamedArg arg) {
    return isEventArgument(arg);
  }

  public boolean isEventArgument(GUIParamSymbol paramSym) {
    return paramSym.getType().getTypeInfo().getFullName().equals(BuiltInTypes.EVENT.getQName());
  }

  public boolean isStringLiteral(ASTExpression argValue) {
    return argValue instanceof ASTLiteralExpression
        && ((ASTLiteralExpression) argValue).getLiteral() instanceof ASTStringLiteral;
  }

  public List<ASTGUINamedArg> getInputArguments(ASTGUIComponent guiComponent) {
    Optional<GUIComponentDeclarationSymbol> guiComponentSymbol = guiComponent.getEnclosingScope().resolveGUIComponentDeclaration(guiComponent.getNameSymbol().getFullName());
    if (guiComponentSymbol.isEmpty()) {
      return new ArrayList<>();
    }
    return guiComponent.getGUINamedArgs().getGUINamedArgList().stream().filter(a -> isInputArgument(guiComponentSymbol.get(), a)).collect(Collectors.toList());
  }

  public List<ASTGUINamedArg> getEventArguments(ASTGUIComponent guiComponent) {
    return guiComponent.getGUINamedArgs().getGUINamedArgList().stream()
        .filter(a -> isEventArgument(guiComponent, a)).collect(Collectors.toList());
  }

  public List<ASTGUIParam> getGUIViewElementParameters(ASTGUICompilationUnit gui) {
    return gui.getGUIComponentDeclaration().getGUIParamList().stream()
        .filter(p -> isViewElementsCollection(p)).collect(Collectors.toList());
  }

  public boolean isViewElementsCollection(ASTGUIParam param) {
    return param.isPresentSymbol() && isViewElementsCollection(param.getSymbol().getType());
  }

  public boolean hasGetter(ASTGUIComponent comp, ASTGUINamedArg arg) {
    Optional<GUIParamSymbol> paramSym = resolveGUIParamSym(
        arg.getEnclosingScope(), comp.getNameSymbol().getFullName(), arg.getName());
    return paramSym.isPresent() && hasGetter(paramSym.get().getType());
  }

  public Optional<AutomaticDomainValidityBinding> getAutomaticDomainValidityBinding(ASTGUIComponent comp) {
    Optional<GUIParamSymbol> targetParam = getDomainValidationTargetParam(comp);
    if (targetParam.isEmpty() || hasNamedArg(comp, targetParam.get().getName())) {
      return Optional.empty();
    }

    return comp.getGUINamedArgs().getGUINamedArgList().stream()
        .filter(arg -> arg.isPresentNameSymbol() && TagLookUp.isDomainValidationEntry(arg.getNameSymbol()))
        .map(ASTGUINamedArg::getExpression)
        .filter(ASTFieldAccessExpression.class::isInstance)
        .map(ASTFieldAccessExpression.class::cast)
        .findFirst()
        .map(entry -> new AutomaticDomainValidityBinding(targetParam.get(), entry));
  }

  public boolean shouldGenerateAutomaticDomainValidity(ASTGUIComponent comp) {
    return getAutomaticDomainValidityBinding(comp).isPresent();
  }

  public String getAutomaticDomainValidityGetterNameTS(ASTGUIComponent comp) {
    return getAutomaticDomainValidityBinding(comp)
        .map(binding -> gui2cdAttrGetterName(comp.getId().getName(), binding.getTargetArgName()) + SYNCHRONIZED_SUFFIX_FOR_HTML_TEMPL)
        .orElse("");
  }

  public String getAutomaticDomainValidityTargetArgName(ASTGUIComponent comp) {
    return getAutomaticDomainValidityBinding(comp)
        .map(AutomaticDomainValidityBinding::getTargetArgName)
        .orElse("");
  }

  protected boolean hasNamedArg(ASTGUIComponent comp, String name) {
    return comp.getGUINamedArgs().getGUINamedArgList().stream()
        .anyMatch(arg -> name.equals(arg.getName()));
  }

  protected Optional<GUIParamSymbol> getDomainValidationTargetParam(ASTGUIComponent comp) {
    if (!comp.isPresentNameSymbol()) {
      return Optional.empty();
    }
    return comp.getNameSymbol().getGUIParamList().stream()
        .filter(TagLookUp::isDomainValidationTarget)
        .findFirst();
  }

  public static class AutomaticDomainValidityBinding {
    protected String targetArgName;
    protected SymTypeExpression targetType;
    protected ASTFieldAccessExpression entryExpression;

    public AutomaticDomainValidityBinding(GUIParamSymbol targetParam, ASTFieldAccessExpression entryExpression) {
      this.targetArgName = targetParam.getName();
      this.targetType = targetParam.getType();
      this.entryExpression = entryExpression;
    }

    public String getTargetArgName() {
      return targetArgName;
    }

    public ASTFieldAccessExpression getEntryExpression() {
      return entryExpression;
    }

    public SymTypeExpression getTargetType() {
      return targetType;
    }
  }

  public boolean hasGetter(SymTypeExpression ste) {
    String type = ste.getTypeInfo().getFullName();
    return !isViewElement(ste)
        && !isViewElementsCollection(ste)
        && !type.equals(BuiltInTypes.EVENT.getQName());
  }

  public boolean hasSetter(ASTGUIComponent comp, ASTGUINamedArg arg) {
    Optional<GUIParamSymbol> paramSym = resolveGUIParamSym(
        arg.getEnclosingScope(), comp.getNameSymbol().getFullName(), arg.getName());
    return paramSym.isPresent() && hasGetter(paramSym.get().getType()) && paramSym.get().isIsReference();
  }

  public boolean isNameExpression(ASTExpression expression) {
    return expression instanceof ASTNameExpression;
  }

  public String getNameOfNameExpression(ASTExpression expression) {
    return isNameExpression(expression) ? ((ASTNameExpression) expression).getName() : "";
  }

  public boolean isViewElementsCollection(ASTGUIComponent comp, ASTGUINamedArg arg) {
    Optional<GUIParamSymbol> paramSym = resolveGUIParamSym(
        arg.getEnclosingScope(), comp.getNameSymbol().getFullName(), arg.getName());
    return paramSym.isPresent() && isViewElementsCollection(paramSym.get().getType());
  }

  public boolean isViewElement(ASTGUIComponent comp, ASTGUINamedArg arg) {
    Optional<GUIParamSymbol> paramSym = resolveGUIParamSym(
        arg.getEnclosingScope(), comp.getNameSymbol().getFullName(), arg.getName());
    return paramSym.isPresent() && isViewElement(paramSym.get().getType());
  }

  public boolean isViewElementsCollection(ASTExpression e) {
    boolean res = false;
    SymTypeExpression ste = TypeCheck3.typeOf(e);
    if (ste.isGenericType()) {
      List<SymTypeExpression> arguments = ((SymTypeOfGenerics) ste).getArgumentList();
      for (SymTypeExpression arg : arguments) {
        res = res
            || isViewElement(arg)
            || isViewElementsCollection(arg);
      }
    }
    return res;
  }

  public boolean isViewElementsCollection(SymTypeExpression ste) {
    boolean res = false;
    if (ste.isGenericType()) {
      List<SymTypeExpression> arguments = ((SymTypeOfGenerics) ste).getArgumentList();
      for (SymTypeExpression arg : arguments) {
        res = res
            || isViewElement(arg)
            || isViewElementsCollection(arg);
      }
    }
    return res;
  }

  public boolean isViewElement(SymTypeExpression ste) {
    return ste.hasTypeInfo()
        && ste.getTypeInfo().getFullName().equals(BuiltInTypes.GUI_VIEW_ELEMENT.getQName());
  }

  public String getBoxedType(String type) {
    if (TypeScriptTypes.INT.getMCType().equals(type)) {
      return TypeScriptTypes.INT_BOXED.getMCType();
    }
    if (TypeScriptTypes.LONG.getMCType().equals(type)) {
      return TypeScriptTypes.LONG_BOXED.getMCType();
    }
    if (TypeScriptTypes.DOUBLE.getMCType().equals(type)) {
      return TypeScriptTypes.DOUBLE_BOXED.getMCType();
    }
    if (TypeScriptTypes.FLOAT.getMCType().equals(type)) {
      return TypeScriptTypes.FLOAT_BOXED.getMCType();
    }
    return type;
  }

  public String pascal2KebabCase(String pascalCaseName) {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, pascalCaseName);
  }

  public String getModuleName(String qualifiedModulName) {
    String moduleName = qualifiedModulName;
    if (qualifiedModulName.lastIndexOf(".") > 0) {
      moduleName = qualifiedModulName.substring(qualifiedModulName.lastIndexOf(".") + 1);
    }
    return moduleName;
  }

  public String getPackageModuleName(ASTMCImportStatement i) {
    List<String> temp = List.of(java2tsModule(i.getQName()).split("/"));
    return temp.get(temp.size() - 2).substring(0, 1).toUpperCase() + temp.get(temp.size() - 2).substring(1) + "GemModule";
  }

  public String getPackageModulePath(ASTMCImportStatement i) {
    List<String> temp = List.of(java2tsModule(i.getQName()).split("/"));
    String moduleName = temp.get(temp.size() - 2).substring(0, 1).toUpperCase() + temp.get(temp.size() - 2).substring(1);
    String path = "";
    for (int index = 0; index < temp.size() - 1; index++) {
      path += temp.get(index) + "/";
    }
    path += moduleName + GUI2ConfigGenerator.MODULE_FILE_SUFFIX;
    return path;
  }

  public String getModuleClassName(String compName) {
    return StringTransformations.capitalize(compName.toLowerCase()) + "Module";
  }

  public String getRoutingId(ASTGUIParam p) {
    if (p.isPresentSymbol()) {
      return p.getSymbol().getFullName().replace(".", "_") + "_RoutingId";
    }
    return p.getName() + "_RoutingId";
  }

  public List<String> getRoutingIds(ASTGUICompilationUnit decoratedAst) {
    List<String> ids = new ArrayList<>();
    if (!isCreateModel(decoratedAst.getComponentQName())) {
      for (ASTGUIParam p : decoratedAst.getGUIComponentDeclaration().getGUIParamList()) {
        if (p.isPresentSymbol()) {
          if (!GenService.isList(p.getSymbol().getType()) && isDomainType(p) && !isDerived(p)
              && !TagLookUp.isSingleton(p.getSymbol().getType().getTypeInfo())
              && !TagLookUp.isDerived(p.getSymbol().getType().getTypeInfo())) {
            ids.add(getRoutingId(p));
          }
        }
        else {
          Log.warn("0x00008 parameter " + p.getName()
              + " of component " + decoratedAst.getComponentName()
              + " was not found. Routing information may be incomplete.");
        }
      }
    }
    return ids;
  }

  private boolean isDerived(ASTGUIParam p) {
    return false;
  }

  public String getGenFileFolder(ASTMCPackageDeclaration pDecl) {
    return String.join("/", pDecl.getMCQualifiedName().getPartsList());
  }

  public String getIterateName(String iterateCompName) {
    return "getIterate" + StringTransformations.capitalize(iterateCompName) + "It()" + ".get(idx)";
  }

  public String getIterateIndex() {
    return "int idx";
  }

  public String getQualifiedNameForModule(ASTGUIComponent node) {
    String qualifiedModuleName = node.getName();
    Optional<GUIComponentDeclarationSymbol> symbol = node.getEnclosingScope().resolveGUIComponentDeclaration(node.getName());
    if (symbol.isPresent() && symbol.get().getFullName().lastIndexOf(".") > 0) {
      String fullName = symbol.get().getFullName();
      qualifiedModuleName = fullName.substring(0, fullName.lastIndexOf("."));
    }
    return qualifiedModuleName;
  }

  public String gui2htmlArgValue(ASTExpression node) {
    if (node instanceof ASTGUIComponent guiComponent) {
      return getNgTemplateName(guiComponent.getId().getName());
    }
    if (node instanceof ASTGUIGuard guiGuard) {
      return getNgTemplateName(guiGuard.getId().getName());
    }
    if (node instanceof ASTGUIIterate guiIterate) {
      return getNgTemplateName(guiIterate.getId().getName());
    }
    if (node instanceof ASTSetEnumeration setEnumeration) {
      StringBuilder res = new StringBuilder("[");
      for (ASTSetCollectionItem v : setEnumeration.getSetCollectionItemList()) {
        res.append(gui2htmlArgValue(((ASTSetValueItem) v).getExpression())).append(", ");
      }
      res.append("]");
      return res.toString();
    }
    if (node instanceof ASTExpression expression) {
      return print(expression);
    }

    Log.error("0x00005 corrupt ASTGUIArgValue node " + node
        + ": missing actual value");
    return "";
  }

  public String getComponentPath(String javaPackage) {
    return qName2path(javaPackage);
  }

  public String java2tsModule(String javaPackage) {
    String res = qName2path(javaPackage);

    Optional<String> tsPackage = TSImportMap.INSTANCE.getTSPackage(javaPackage);
    if (tsPackage.isPresent()) {
      res = tsPackage.get();
    }
    if (TSBackendMap.isBackendRTEPackage(javaPackage)) {
      res = TSBackendMap.getTSRTEPackage();
    }

    return res;
  }

  public String getRelativeImportPathForAnnotation(
      String path, String base, String qualifiedName) {
    path = path + "/" + Names.getPathFromPackage(qualifiedName) + ".html";
    base = base + "/" + Names.getPathFromPackage(qualifiedName) + ".ts";
    String relative = Paths.get(base).getParent()
        .toAbsolutePath()
        .normalize()
        .relativize(Paths.get(path))
        .toString()
        .replaceAll("\\\\", "/");

    return relative;
  }

  public Optional<String> getRelativeImportPath(
      MCPath imported, Path importer, String qualifiedName, String fileExtension) {
    String hwFile = Names.getPathFromPackage(qualifiedName) + "." + fileExtension;
    return getRelativeImportPath(imported, importer, hwFile);
  }

  public Optional<String> getRelativeImportPath(
      MCPath hwPath, Path outPath, String hwFile) {
    Optional<URL> hwFilePath = hwPath.find(hwFile);
    Optional<String> res = Optional.empty();
    if (hwFilePath.isPresent()) {
      Optional<Path> hw = MCPath.toPath(hwFilePath.get());
      if (hw.isPresent()) {
        res = Optional.of(outPath.resolve(hwFile).getParent()
            .toAbsolutePath()
            .normalize()
            .relativize(hw.get().toAbsolutePath())
            .toString()
            .replaceAll("\\\\", "/"));

        Reporting.reportUseHandwrittenCodeFile(hw.get(), Paths.get(hwFile));
      }
    }
    Reporting.reportHWCExistenceCheck(hwPath, Paths.get(hwFile), hwFilePath);
    return res;
  }

  public String predictRelativeImportPath(
      Path importedFolder, Path importer, String file) {
    Path imported = importedFolder.resolve(file);
    return importer.getParent()
        .toAbsolutePath()
        .normalize()
        .relativize(imported.toAbsolutePath())
        .toString()
        .replaceAll("\\\\", "/");
  }

  public Optional<String> getRelativeImportPathForClass(MCPath imported, Path importer, String qualifiedName) {
    Optional<String> result = getRelativeImportPath(imported, importer, qualifiedName, "ts");
    if (result.isEmpty()) {
      result = getRelativeImportPath(imported, importer, qualifiedName, "java");
      if (result.isPresent()) {
        return Optional.of("./" + Names.getSimpleName(qualifiedName));
      }
    }
    return result;
  }

  public Optional<String> getRelativeImportPathForAppConfigFile(
      MCPath imported, Path importer, String qualifiedName, String fileExtension) {
    String hwFile = Names.getPathFromPackage(qualifiedName) + "." + fileExtension;
    Optional<URL> hwFilePath = imported.find(hwFile);
    Optional<String> res = Optional.empty();
    if (hwFilePath.isPresent()) {
      Optional<Path> hwPath = MCPath.toPath(hwFilePath.get());
      res = Optional.of(importer
          .toAbsolutePath()
          .normalize()
          // TODO this is bullshit
          .relativize(importer.resolve(hwFile).toAbsolutePath())
          .toString()
          .replaceAll("\\\\", "/"));

      hwPath.ifPresent(path -> Reporting.reportUseHandwrittenCodeFile(path,
          Paths.get(hwFile)));
    }
    Reporting.reportHWCExistenceCheck(imported, Paths.get(hwFile), hwFilePath);
    return res;
  }

  /*
  public Optional<String> getRelativeHandwrittenPath(
      MCPath hwPath, Path outPath, ASTMCPackageDeclaration packageDecl, String fileName) {
    Collection<Path> hwFilePaths = new ArrayList<>();
    for (Path p : hwPath.getEntries()) {
      hwFilePaths.add(getFilePath(p, packageDecl));
    }
    String localOutPath = getGenFileFolder(packageDecl);
    Path outFolder = outPath.resolve(localOutPath);
    return getRelativeHandwrittenPath(hwPath, hwFilePaths, outFolder, fileName);
  }

  public Path getFilePath(Path input, ASTMCPackageDeclaration packageDecl) {
    String localPath = String.join("/", packageDecl.getMCQualifiedName().getPartsList());
    return Paths.get(input.toString(), localPath);
  }

  public Optional<String> getRelativeHandwrittenPath(
      MCPath hwMCPath,
      Collection<Path> hwPath,
      Path outputFolder,
      String fileName
  ) {
    Iterator<Path> targetPathIt = hwPath.iterator();
    while (targetPathIt.hasNext()) {
      Path it = targetPathIt.next();
      Path hwFile = it.resolve(fileName);
      if (Files.exists(hwFile)) {
        Reporting.reportHWCExistenceCheck(hwMCPath, Paths.get(hwHtmlPath.get()).toAbsolutePath(), hwFilePath);
        String path = outputFolder
            .toAbsolutePath()
            .normalize()
            .relativize(hwFile.toAbsolutePath())
            .toString();
        path = path.replace("\\", "/");
        return Optional.of(path);
      }
    }
    return Optional.empty();
  }
  /**/

  public String removeExtension(String filePath) {
    int index = filePath.lastIndexOf(".");
    if (index > 0) {
      return filePath.substring(0, index);
    }
    return filePath;
  }

  public String qName2path(String parts) {
    return parts.replaceAll("\\.", "/");
  }

  // TODO: remove this method after package names fixed!!
  public String removeDiagramName(String name) {
    int index1 = name.lastIndexOf(".");
    if (index1 == -1) {
      return name;
    }
    String res = name.substring(0, index1);
    int index2 = res.lastIndexOf(".");
    if (index2 == -1) {
      return name;
    }
    return name.substring(0, index2) + name.substring(index1);
  }

  public SymTypeExpression getType(ASTExpression node) {
    return TypeCheck3.typeOf(node);
  }

  public String getNgTemplateName(ASTGUIBlockContent node) {
    String name;
    if (node.isPresentGUIGuard()) {
      name = node.getGUIGuard().getId().getName();
    }
    else if (node.isPresentGUIIterate()) {
      name = node.getGUIIterate().getId().getName();
    }
    else if (node.isPresentExpression() && node.getExpression() instanceof ASTGUIComponent guiComponent) {
      name = guiComponent.getId().getName();
    }
    else {
      String received = "Unknown";
      if (node.isPresentExpression()) {
        received = ASTExpression.class.getSimpleName();
      }
      if (node.isPresentGUIAdapt()) {
        received = ASTGUIAdapt.class.getSimpleName();
      }
      if (node.isPresentGUIRemove()) {
        received = ASTGUIRemove.class.getSimpleName();
      }
      if (node.isPresentSemicolon()) {
        received = "Semicolon";
      }
      Log.error("0x0000F Unable to transform ASTGUIBlockContent to an ng-template name."
          + "Expected template to contain an ASTGUIComponent, ASTGUIGuard or ASTGUIIterate,"
          + " but got " + received + "."
      );
      return null;
    }
    return getNgTemplateName(name);
  }

  public String getNgTemplateName(String name) {
    return name + NG_TEMPLATE_SUFFIX;
  }

  public String getNgTemplateElseBlockName(String name) {
    return name + NG_TEMPLATE_ELSE_BLOCK_SUFFIX;
  }

  /**
   * Search from bottom to top in a scope tree for an IGUIDSLScope instance.
   *
   * @param scope Scope from which the search starts.
   * @return Optional.empty() if no IGUIDSLScope instance could be found, otherwise optional of the
   * first encountered IGUIDSLScope instance.
   */
  public Optional<IGUIDSLScope> findClosestGUIDSLScope(IScope scope) {
    if (scope instanceof IGUIDSLScope) {
      return Optional.of((IGUIDSLScope) scope);
    }
    if (scope instanceof IGlobalScope) {
      return Optional.empty();
    }
    return findClosestGUIDSLScope(scope.getEnclosingScope());
  }

  /**
   * Search from bottom to top in a scope tree for a VariableSymbol instance representing an
   * iteration item.
   *
   * @param name  Name of the variable (iteration item).
   * @param scope Scope from which the search starts.
   * @return Optional.empty() if no iteration item with given name was found, otherwise optional of
   * VariableSymbol instance representing the iteration item.
   */
  public Optional<VariableSymbol> findIterationItemSymbols(String name, IScope scope) {
    Optional<IGUIDSLScope> guiScope = findClosestGUIDSLScope(scope);
    if (guiScope.isPresent()) {
      Optional<VariableSymbol> vs = guiScope.get().resolveVariable(name);
      if (vs.isPresent() && vs.get().getEnclosingScope().getAstNode() instanceof ASTGUIIterate) {
        return vs;
      }
    }
    return Optional.empty();
  }

  /**
   * Search from bottom to top in a scope tree for all VariableSymbol instances representing
   * iteration items.
   *
   * @param scope Scope from which the search starts.
   * @return List of VariableSymbol instances representing the iteration items.
   */
  public List<VariableSymbol> findIterationItemSymbols(IScope scope) {
    List<VariableSymbol> symbols = new ArrayList<>();
    Optional<IGUIDSLScope> guiScope = findClosestGUIDSLScope(scope);
    if (guiScope.isEmpty()) {
      return symbols;
    }
    return findIterationItemSymbols(guiScope.get(), symbols);
  }

  protected List<VariableSymbol> findIterationItemSymbols(
      IGUIDSLScope scope, List<VariableSymbol> symbols
  ) {
    if (scope instanceof IGlobalScope) {
      return symbols;
    }
    ASTNode node = scope.getAstNode();
    if (node instanceof ASTGUIIterate) {
      for (VariableSymbol vs : scope.getLocalVariableSymbols()) {
        if (((ASTGUIIterate) node).getIterateControl() instanceof ASTGUIShortIterateControl
            && ((ASTGUIShortIterateControl) ((ASTGUIIterate) node).getIterateControl()).getItem()
            .equals(vs.getAstNode())
        ) {
          symbols.add(vs);
        }
      }
    }
    return findIterationItemSymbols(scope.getEnclosingScope(), symbols);
  }

  public String types2ArgsString(Collection<String> types) {
    StringBuilder res = new StringBuilder();
    Iterator<String> iter = types.iterator();
    int i = 0;
    while (iter.hasNext()) {
      String type = iter.next();
      res.append(type);
      res.append(" ");
      res.append(Expression2SetterPrinter.SETTER_PARAM + i);
      if (iter.hasNext()) {
        res.append(",");
      }
    }
    return res.toString();
  }

  public String variableSymbols2ArgsString(Collection<VariableSymbol> variableSymbols) {
    StringBuilder res = new StringBuilder();
    Iterator<VariableSymbol> iter = variableSymbols.iterator();
    while (iter.hasNext()) {
      VariableSymbol vs = iter.next();
      res.append(vs.getType().printFullName());
      res.append(" ");
      res.append(vs.getName());
      if (iter.hasNext()) {
        res.append(",");
      }
    }
    return res.toString();
  }

  public String variableSymbols2ArgsString(
      String setterType, String arg, Collection<VariableSymbol> variableSymbols
  ) {
    StringBuilder res = new StringBuilder(setterType + " " + arg);
    Iterator<VariableSymbol> iter = variableSymbols.iterator();
    while (iter.hasNext()) {
      res.append(",");
      VariableSymbol vs = iter.next();
      res.append(vs.getType().printFullName());
      res.append(" ");
      res.append(vs.getName());
    }
    return res.toString();
  }

  public String params2String(Map<ASTCDParameter, SymTypeExpression> params) {
    if (params == null) {
      return "";
    }
    StringBuilder res = new StringBuilder();
    Iterator<ASTCDParameter> iter = params.keySet().iterator();
    while (iter.hasNext()) {
      ASTCDParameter param = iter.next();
      res.append(param.getMCType().printType());
      res.append(" ");
      res.append(param.getName());
      if (iter.hasNext()) {
        res.append(",");
      }
    }
    return res.toString();
  }

  public String params2String(Collection<ASTCDParameter> params) {
    StringBuilder res = new StringBuilder();
    Iterator<ASTCDParameter> iter = params.iterator();
    while (iter.hasNext()) {
      ASTCDParameter param = iter.next();
      res.append(param.getMCType().printType());
      res.append(" ");
      res.append(param.getName());
      if (iter.hasNext()) {
        res.append(",");
      }
    }
    return res.toString();
  }

  public String params2StringVariableNames(Collection<ASTCDParameter> params) {
    StringBuilder res = new StringBuilder();
    Iterator<ASTCDParameter> iter = params.iterator();
    while (iter.hasNext()) {
      ASTCDParameter param = iter.next();
      res.append(param.getName());
      if (iter.hasNext()) {
        res.append(",");
      }
    }
    return res.toString();
  }

  public String params2String(String setterType, String arg, Collection<ASTCDParameter> params) {
    StringBuilder res = new StringBuilder(setterType + " " + arg);
    Iterator<ASTCDParameter> iter = params.iterator();
    while (iter.hasNext()) {
      res.append(",");
      ASTCDParameter param = iter.next();
      res.append(param.getMCType().printType());
      res.append(" ");
      res.append(param.getName());
    }
    return res.toString();
  }

  public String tsParams2String(ASTMCType setterType, String arg, Collection<ASTCDParameter> params) {
    TSUtilsPrinter printer = new TSUtilsPrinter();

    StringBuilder res = new StringBuilder(arg + ": " + printer.printType(setterType));
    Iterator<ASTCDParameter> iter = params.iterator();
    while (iter.hasNext()) {
      res.append(",");
      ASTCDParameter param = iter.next();
      res.append(param.getName());
      res.append(": ");
      res.append(printer.printType(param.getMCType()));
    }
    return res.toString();
  }

  public String params2ArgsString(String arg, Collection<ASTCDParameter> params) {
    StringBuilder res = new StringBuilder(arg);
    Iterator<ASTCDParameter> iter = params.iterator();
    while (iter.hasNext()) {
      res.append(",");
      ASTCDParameter param = iter.next();
      res.append(param.getName());
    }
    return res.toString();
  }

  public boolean isAbstractDomainType(SymTypeExpression typeSTE) {
    boolean isAbstract = false;
    if (isDomainType(typeSTE)) {
      TypeSymbol typeSymbol = typeSTE.asObjectType().getTypeInfo();
      if (typeSymbol instanceof OOTypeSymbol ooTypeSymbol) {
        isAbstract = ooTypeSymbol.isIsAbstract();
      }
    }
    return isAbstract;
  }

  public Optional<ASTGUIComponent> getAsGuiComponent(ASTExpression node) {
    if (node instanceof ASTGUIComponent guiComponent) {
      return Optional.of(guiComponent);
    }
    return Optional.empty();
  }
}
