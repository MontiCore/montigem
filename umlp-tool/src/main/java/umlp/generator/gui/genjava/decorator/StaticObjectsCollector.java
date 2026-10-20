package umlp.generator.gui.genjava.decorator;

import de.monticore.ast.ASTNode;
import de.monticore.cd.facade.CDAttributeFacade;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd.facade.CDParameterFacade;
import de.monticore.cd4codebasis._ast.ASTCDParameter;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.commonexpressions._visitor.CommonExpressionsVisitor2;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.expressions.expressionsbasis._visitor.ExpressionsBasisVisitor2;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIComponentDeclaration;
import de.monticore.guidsl._ast.ASTGUIGuard;
import de.monticore.guidsl._ast.ASTGUIIterate;
import de.monticore.guidsl._ast.ASTGUINamedArg;
import de.monticore.guidsl._ast.ASTGUIShortIterateControl;
import de.monticore.guidsl._symboltable.BuiltInTypes;
import de.monticore.guidsl._visitor.GUIDSLTraverser;
import de.monticore.guidsl._visitor.GUIDSLVisitor2;
import de.monticore.ocl.setexpressions._ast.ASTSetCollectionItem;
import de.monticore.ocl.setexpressions._ast.ASTSetEnumeration;
import de.monticore.ocl.setexpressions._ast.ASTSetValueItem;
import de.monticore.ocl.setexpressions._visitor.SetExpressionsVisitor2;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.symbols.oosymbols._symboltable.IOOSymbolsScope;
import de.monticore.symbols.oosymbols._symboltable.OOTypeSymbol;
import de.monticore.symboltable.IScope;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types.check.SymTypeOfGenerics;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types.mcfullgenerictypes.MCFullGenericTypesMill;
import de.monticore.types.mcsimplegenerictypes.MCSimpleGenericTypesMill;
import de.monticore.types.mcsimplegenerictypes._ast.ASTMCBasicGenericType;
import de.monticore.types.mcsimplegenerictypes._ast.ASTMCBasicGenericTypeBuilder;
import de.monticore.types.mcsimplegenerictypes._ast.ASTMCCustomTypeArgument;
import de.se_rwth.commons.Joiners;
import de.se_rwth.commons.Splitters;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.generator.gui.GUIGeneratorUtil;
import umlp.generator.gui.gentypescript.printer.TypeScriptTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StaticObjectsCollector implements GUIDSLVisitor2, CommonExpressionsVisitor2, ExpressionsBasisVisitor2,
    SetExpressionsVisitor2 {

  protected GUIDSLTraverser traverser;

  protected final GUIGeneratorUtil util = GUIGeneratorUtil.getInstance();

  protected static StaticObjectsCollector instance;

  protected int counter = 0;
  protected String curComp = "";
  protected String curArg = "";
  protected Optional<ASTGUINamedArg> lastArg = Optional.empty();
  protected Map<ASTSetEnumeration, SymTypeExpression> listTypes = new LinkedHashMap<>();

  protected Map<ASTNode, Attribute> node2Attr = new LinkedHashMap<>();
  protected Map<ASTNode, Method> node2Method = new LinkedHashMap<>();

  boolean insideIterateControl = false;

  public static StaticObjectsCollector getInstance() {
    if (instance == null) {
      instance = new StaticObjectsCollector();
    }
    return instance;
  }

  public static void unsetInstance() {
    instance = null;
  }

  public GUIDSLTraverser getTraverser() {
    return traverser;
  }

  private StaticObjectsCollector() {
    traverser = GUIDSLMill.traverser();
    traverser.add4GUIDSL(this);
    traverser.add4CommonExpressions(this);
    traverser.add4ExpressionsBasis(this);
    traverser.add4SetExpressions(this);
  }

  public void init(ASTGUIComponentDeclaration node) {
    reset();
    CallTypeDeriver.getInstance().init(node);
    node.accept(getTraverser());
  }

  public void reset() {
    counter = 0;
    curComp = "";
    curArg = "";
    lastArg = Optional.empty();
    listTypes = new LinkedHashMap<>();
    node2Attr = new LinkedHashMap<>();
    node2Method = new LinkedHashMap<>();
  }

  public Map<ASTNode, Attribute> getNode2Attr() {
    return node2Attr;
  }

  public Map<ASTNode, Method> getNode2Method() {
    return node2Method;
  }

  @Override
  public void visit(ASTGUIComponent node) {
    curComp = node.getId().getName();
  }

  @Override
  public void visit(ASTGUIIterate node) {
    curComp = node.getId().getName();
  }

  @Override
  public void visit(ASTGUIShortIterateControl node) {
    curArg = GUI2JavaClassDecorator.ITERATE_ARG;
    insideIterateControl = true;
  }

  @Override
  public void endVisit(ASTGUIShortIterateControl node) {
    insideIterateControl = false;
  }

  @Override
  public void visit(ASTGUIGuard node) {
    curComp = node.getId().getName();
    curArg = GUI2JavaClassDecorator.GUARD_ARG;
  }

  @Override
  public void visit(ASTGUINamedArg node) {
    curArg = node.getName();
    lastArg = Optional.of(node);
  }

  @Override
  public void visit(ASTSetEnumeration node) {
    // given an argument with a nested list type, e.g., val = [[1]], "peel off" and record types
    // of the list layers one by one.
    if (!listTypes.containsKey(node) && lastArg.isPresent()) {
      if (!lastArg.get().isPresentNameSymbol()) {
        Log.error("0x00012 Unable to find defining symbol for argument '"
            + lastArg.get().getName() + "'.");
        return;
      }
      SymTypeExpression ste = lastArg.get().getNameSymbol().getType();
      if (GUIGeneratorUtil.getInstance().isViewElementsCollection(ste)) {
        return;
      }
      if (lastArg.get().getExpression() instanceof ASTSetEnumeration setEnumeration &&
          setEnumeration.equals(node)) {
        recordTypes(node, ste);
      }
    }
    if (listTypes.containsKey(node)) {
      String name = curComp + StringTransformations.capitalize(curArg) + "_" + counter++;
      Attribute attr = createAttribute(node, listTypes.get(node), name);
      node2Attr.put(node, attr);
    }
  }

  @Override
  public void visit(ASTFieldAccessExpression node) {
    SymTypeExpression ste = util.getType(node);
    // single object assoc, e.g., @AddressForm(address = person.address)
    if ((util.isDomainType(ste) || util.isDomainListType(ste)) && !util.isEnumType(ste)) {
      String name = curComp + StringTransformations.capitalize(curArg) + "_" + counter++;
      Attribute attr = createAttribute(node, ste, name);
      node2Attr.put(node, attr);
    }
  }

  @Override
  public void visit(ASTCallExpression node) {
    if (node.getExpression() instanceof ASTNameExpression) {
      ASTNameExpression nameExp = (ASTNameExpression) node.getExpression();
      addMethodExpressionToMap(nameExp, node, nameExp.getName());
    }
    else if (node.getExpression() instanceof  ASTFieldAccessExpression) {
      // Method Call, i.e. Command Call of a Singleton for now "app.doSomething()"
      ASTFieldAccessExpression fieldAccessExpression = (ASTFieldAccessExpression) node.getExpression();
      if(fieldAccessExpression.getExpression() instanceof ASTNameExpression) {
        addMethodExpressionToMap((ASTNameExpression) fieldAccessExpression.getExpression(), node,
            fieldAccessExpression.getName());
      }
    }
  }

  private void addMethodExpressionToMap(ASTExpression expression, ASTCallExpression node, String expressionName) {
    if (!(expression.getEnclosingScope() instanceof IOOSymbolsScope)) {
      Log.error("0x0000C Attempted to resolve type of '"
          + GUIGeneratorUtil.getInstance().print(expression) + "' from outside of an OOSymbolsScope,"
          + " but expression conversion is restricted to OOSymbols context.");
      return;
    }
    IOOSymbolsScope scope = (IOOSymbolsScope) expression.getEnclosingScope();
    Optional<OOTypeSymbol> sym = scope.resolveOOType(expressionName);

    boolean isConstructor = false;
    // TODO AGe: use this code after bug in type check is fixed
    /*
    SymTypeExpression ste = util.getType(node);
    // those conditions should always be both true or false
    if (sym.isPresent() && ste.isFunctionType()) {
      isConstructor = !sym.get().getMethodList(node.getName()).isEmpty();
      SymTypeOfFunction stof = ste.asFunctionType();
      ste = stof.getType();
    }
    /**/

    // those conditions should always be both true or false
    if (sym.isPresent()) {
      isConstructor = !sym.get().getMethodList(expressionName).isEmpty();
    }
    if (isConstructor) {
      String name = curComp + StringTransformations.capitalize(curArg) + "_" + counter++;
      SymTypeExpression ste = SymTypeExpressionFactory.createTypeObject(sym.get());
      Attribute attr = createAttribute(expression, ste, name);
      node2Attr.put(node, attr);
    }
    else if (CallTypeDeriver.getInstance().hasReturnType(node)) {
      SymTypeExpression returnType = CallTypeDeriver.getInstance().getReturnType(node);
      List<SymTypeExpression> paramTypes = new ArrayList<>();
      for (ASTExpression exp : node.getArguments().getExpressionList()) {
        SymTypeExpression argSTE = util.getType(exp);
        if (argSTE.isObscureType()) {
          Log.error("0x00015 Unable to derive the type of expression '"
              + util.print(expression)
              + "' at " + expression.get_SourcePositionStart().toString()
              + " - " + expression.get_SourcePositionEnd().toString()
          );
        }
        paramTypes.add(argSTE);
      }

      Method guiMethod = new Method(returnType, expressionName, paramTypes);
      boolean isPredefined = false;
      for (PredefinedFunction fun : PredefinedFunction.values()) {
        if (fun.getName().equals(expressionName)) {
          guiMethod.setPredefined(fun, expressionName + "_" + counter++);
          isPredefined = true;
          break;
        }
      }
      for (ASTNode n : node2Method.keySet()) {
        Method m = node2Method.get(n);
        if (guiMethod.getName().equals(m.getName())) {
          if (!guiMethod.compatibleParamTypes(m) && guiMethod.sameParamTypesErasure(m)) {
            Log.error("0x00018 Found methods with incompatible parameter types"
                + " but the same type erasure: '"
                + util.print(n) + "' at " + n.get_SourcePositionStart().toString()
                + " - " + expression.get_SourcePositionEnd().toString()
                + " and "
                + util.print(node)
                + "' at " + node.get_SourcePositionStart().toString()
                + " - " + node.get_SourcePositionEnd().toString()
                + ". Please rename one of the methods."
            );
          }
          else if (guiMethod.compatibleReturnType(m) && guiMethod.compatibleParamTypes(m)) {
            if (!isPredefined && !(guiMethod.sameReturnType(m) && guiMethod.sameParamTypes(m))) {
              Log.error("0x0001D Method overloading is not yet supported for non-predefined"
                  + " functions: "
                  + util.print(n) + "' at " + n.get_SourcePositionStart().toString()
                  + " - " + expression.get_SourcePositionEnd().toString()
                  + " and "
                  + util.print(node)
                  + "' at " + node.get_SourcePositionStart().toString()
                  + " - " + node.get_SourcePositionEnd().toString()
                  + ". Please rename one of the methods."
              );
            }
            if (!isPredefined) {
              guiMethod = m;
            }
            break;
          }
          else if (!guiMethod.compatibleReturnType(m) && guiMethod.compatibleParamTypes(m)) {
            Log.error("0x00019 Found methods with compatible parameter types"
                + " but different return types: '"
                + util.print(n) + "' at " + n.get_SourcePositionStart().toString()
                + " - " + expression.get_SourcePositionEnd().toString()
                + " and "
                + util.print(node)
                + "' at " + node.get_SourcePositionStart().toString()
                + " - " + node.get_SourcePositionEnd().toString()
                + ". Please rename one of the methods."
            );
          }
        }
      }
      node2Method.put(node, guiMethod);
    }
    else {
      Log.error("0x00014 Unable to derive the type of call "
          + util.print(node)
          + "' at " + node.get_SourcePositionStart().toString()
          + " - " + node.get_SourcePositionEnd().toString()
          + ". Please make sure that the call is simple, e.g., doStuff(),"
          + " a direct value of"
          + " a GUI component parameter, "
          + " a GUI component argument, "
          + " a guard, "
          + " an iterable item, "
          + " or is inside a block."
      );
    }
  }

  protected Attribute createAttribute(ASTNode node, SymTypeExpression ste, String name) {
    IScope scope = node.getEnclosingScope();
    if (insideIterateControl) {
      scope = scope.getEnclosingScope();
    }
    Set<String> importSet = new LinkedHashSet<>();
    List<ASTCDParameter> params = new ArrayList<>();
    for (VariableSymbol vs : util.findIterationItemSymbols(scope)) {
      ASTMCType paramType = createTypeAndAddImports(vs.getType(), importSet);
      params.add(CDParameterFacade.getInstance().createParameter(paramType, vs.getName()));
    }

    ASTMCType baseType = createTypeAndAddImports(ste, importSet);
    ASTMCType type = baseType;

    // given @iterate(Person p ...) { @iterate(Address a ...) { } }
    // build: Map<Person, Map<Address, SomeType>>
    if (!params.isEmpty()) {
      // SomeType
      ASTMCCustomTypeArgument typeArgument = MCSimpleGenericTypesMill.mCCustomTypeArgumentBuilder()
          .setMCType(type)
          .build();
      // first iteration -> second iteration:
      // Map<Address, SomeType> -> Map<Person, Map<Address, SomeType>>
      for (int i = params.size() - 1; i >= 0; --i) {
        ASTCDParameter param = params.get(i);
        ASTMCBasicGenericType genType = MCFullGenericTypesMill.mCBasicGenericTypeBuilder()
            .setNamesList(new ArrayList<>(List.of("Map")))
            .setMCTypeArgumentsList(new ArrayList<>(List.of(
                MCSimpleGenericTypesMill.mCCustomTypeArgumentBuilder()
                    .setMCType(MCTypeFacade.getInstance()
                        // Address -> Person
                        .createQualifiedType(param.getMCType().printType())).build(),
                // SomeType -> Map<Address, SomeType>
                typeArgument
            )))
            .build();
        if (i > 0) {
          typeArgument = MCSimpleGenericTypesMill.mCCustomTypeArgumentBuilder()
              .setMCType(genType)
              .build();
        }
        else {
          type = genType;
        }
      }
      importSet.add("java.util.Map");
      importSet.add("java.util.HashMap");
    }

    ASTCDAttribute attr = CDAttributeFacade.getInstance()
        .createAttribute(CDModifier.PROTECTED.build(), type, name);
    return new Attribute(attr, baseType, params, importSet, ste);
  }

  protected void recordTypes(ASTSetEnumeration node, SymTypeExpression ste) {
    if (!listTypes.containsKey(node)) {
      listTypes.put(node, ste);
    }
    for (ASTSetCollectionItem val : node.getSetCollectionItemList()) {
      if (val instanceof ASTSetValueItem setValueItem &&
      setValueItem.getExpression() instanceof ASTSetEnumeration setEnumeration) {
        if (!ste.isGenericType()
            || !ste.asGenericType().getTypeConstructorFullName().equals(BuiltInTypes.LIST.getQName())) {
          Log.error("0x00013 Unexpected non-generic type for a list '" + util.print(node) + "'.");
          return;
        }
        recordTypes(setEnumeration, ste.asGenericType().getArgument(0));
      }
    }
  }

  // TODO AGe: should be removed after introducing block scopes in GUI DSL
  @Deprecated
  protected ASTMCType createTypeAndAddImports(SymTypeExpression ste, Set<String> importSet) {
    if (!ste.hasTypeInfo()) {
      Log.error("0x0001B Encountered a SymTypeExpression disconnected from a type symbol: "
          + ste.printFullName());
    }
    if (ste.isObjectType()) {
      // given in GUI:
      // import hardy.Domain.Person;
      // the resolved type is `hardy.Person`
      // but the CD Generator produces `hardy.domain.Person`, so calculate that instead
      //IBasicSymbolsScope scope = ste.getTypeInfo().getEnclosingScope();

      String qType = ste.asObjectType().printFullName();
      String type = ste.asObjectType().print();

      List<String> parts = new ArrayList<>(Splitters.DOT.splitToList(qType));
      if (parts.size() > 1) {
        String domainPackage = parts.get(parts.size() - 2).toLowerCase();
        parts.set(parts.size() - 2, domainPackage);
        qType = Joiners.DOT.join(parts);
      }
      importSet.add(qType);
      return MCTypeFacade.getInstance().createQualifiedType(type);

    }
    else if (ste.isGenericType()) {
      SymTypeOfGenerics stog = ste.asGenericType();
      String qType = stog.getTypeConstructorFullName();
      String type = stog.getTypeInfo().getName();
      if (qType.equals(BuiltInTypes.EVENT.getQName())) {
        qType = TypeScriptTypes.CONSUMER.getJavaQName().get();
        type = TypeScriptTypes.CONSUMER.getMCType();
      }
      importSet.add(qType);

      ASTMCBasicGenericTypeBuilder genTypeBuilder = MCFullGenericTypesMill.mCBasicGenericTypeBuilder()
          .setNamesList(new ArrayList<>(Arrays.asList(type)))
          .setMCTypeArgumentsList(new ArrayList<>());
      for (SymTypeExpression steArg : stog.getArgumentList()) {
        genTypeBuilder.addMCTypeArgument(
            MCSimpleGenericTypesMill.mCCustomTypeArgumentBuilder()
                .setMCType(createTypeAndAddImports(steArg, importSet))
                .build());
      }
      return genTypeBuilder.build();
    }
    else if (ste.isPrimitive()) {
      return MCTypeFacade.getInstance().createQualifiedType(
          ste.asPrimitive().getBoxedPrimitiveName());
    }
    else if (!(ste.isNullType() || ste.isVoidType() || ste.isWildcard() || ste.isTypeVariable())) {
      Log.error("0x00010 Encountered unsupported type in a parameter: '"
          + ste.printFullName() + "'");
    }
    return MCTypeFacade.getInstance().createQualifiedType(ste.printFullName());
  }

}
