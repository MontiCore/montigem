package umlp.py;

import de.monticore.cd.codegen.decorators.data.AbstractDecorator;
import de.monticore.cd.codegen.decorators.data.DecoratorData;
import de.monticore.cd.facade.CDAttributeFacade;
import de.monticore.cd.facade.CDMethodFacade;
import de.monticore.cd.facade.CDParameterFacade;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis.CDBasisMill;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeExpressionFactory;
import de.monticore.types.mcbasictypes._ast.ASTMCQualifiedType;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types.mccollectiontypes._ast.ASTMCTypeArgument;
import de.monticore.types.mcsimplegenerictypes._ast.ASTMCBasicGenericType;
import de.monticore.types3.SymTypeRelations;
import de.se_rwth.commons.logging.Log;
import umlp.py.utily.Pyutils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GemBaseDecorator extends AbstractDecorator<GemBaseDecorator.Data> implements CDBasisVisitor2 {

  protected SymTypeExpression _stringSymTypeExpression;
  protected GemBaseDecorator.Data data;
  protected String domainName;

  protected SymTypeExpression getString() {
    if (_stringSymTypeExpression == null) {
      _stringSymTypeExpression = SymTypeExpressionFactory.createStringType();
    }
    return _stringSymTypeExpression;
  }

  @Override
  public void init(DecoratorData util, Optional<GlobalExtensionManagement> glexOpt) {
    super.init(util, glexOpt);
    this.data = this.decoratorData.createDataIfAbsent(this.getClass(), GemBaseDecorator.Data::new);
  }

  @Override
  public void visit(ASTCDDefinition node) {
    this.domainName = node.getName();
  }

  @Override
  public void visit(ASTCDClass node) {
    // TODO: check if should decorate
    var decNode = decoratorData.getAsDecorated(node);
    if (node.getSuperclassList().isEmpty()) {
      if (!decNode.isPresentCDExtendUsage()) {
        decNode.setCDExtendUsage(CD4CodeMill.cDExtendUsageBuilder().uncheckedBuild());
      }

      decNode.getCDExtendUsage().addSuperclass(
          MCTypeFacade.getInstance().createQualifiedType("montigem_runtime.gembase.GemBase"));
    } else {
      // Fix for multi-files:
      for (int i = 0; i < decNode.getCDExtendUsage().getSuperclassList().size(); i++) {
        var x = decNode.getCDExtendUsage().getSuperclass(i);
        if (x instanceof ASTMCQualifiedType) {
          if (((ASTMCQualifiedType) x).getNameList().size() == 1) {
            // A => A.A
            String superClassName = ((ASTMCQualifiedType) x).getNameList().get(0);
            ((ASTMCQualifiedType) x).getNameList().add(0, superClassName);
            // => MyDomain.A.A
            ((ASTMCQualifiedType) x).getNameList().add(0, domainName);
            CD4C.getInstance().addImport(
                decoratorData.getAsDecorated(node),
                domainName + "." + superClassName);
          } else {
            Log.error("NYI: Multi imports a", node.get_SourcePositionStart());
          }
        } else {
          Log.error("NYI: Multi imports b", node.get_SourcePositionStart());
        }
      }
    }

    decNode.addCDMember(CDAttributeFacade.getInstance()
        .createAttribute(CDBasisMill.modifierBuilder().build(),
            createClassVar("str"), "TYPE",
            stringLiteralExpr(node.getName())));

    decNode.addCDMember(CDAttributeFacade.getInstance()
        .createAttribute(CDBasisMill.modifierBuilder().build(),
            createClassVar("str"), "PACKAGE",
            stringLiteralExpr(domainName.toLowerCase()))); // TODO

    var initFunction = CDMethodFacade.getInstance().createMethod(CDBasisMill.modifierBuilder().build(),
        "__init__",
        CDParameterFacade.getInstance().createParameter("typing.Any", "*args"),
        CDParameterFacade.getInstance().createParameter("typing.Any", "**kwargs")
    );
    decNode.addCDMember(initFunction);

    glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE,
        initFunction,
        new TemplateHookPoint("cd2python.gem.Init")));

    data.origClass2Init.put(node, initFunction);
  }

  @Override
  public void visit(ASTCDAttribute originalNode) {
    var info = UmlpAttrHelper.getFromSymTypeExpr(originalNode.getSymbol().getType());
    ASTCDAttribute decNode = decoratorData.getAsDecorated(originalNode);
    if (info.getMultiplicity() == UmlpAttrHelper.Multiplicity.OPTIONAL) {
      // Attribute is an Optional (field or assoc)
      var inner = info.getSymTypeExpression();

      ASTMCTypeArgument typeArgument;

      if (SymTypeRelations.isCompatible(inner, getString())) {
        // Attribute is a String -> turn into typing.Optional[str]
        typeArgument = MCTypeFacade.getInstance().createBasicTypeArgumentOf("str");
        decNode.setMCType(MCTypeFacade.getInstance()
            .createBasicGenericTypeOf(Arrays.asList("typing", "Optional"),
                Collections.singletonList(typeArgument)));
        data.attrDataMap.computeIfAbsent(originalNode, x -> new AttrData("str"));
        decNode.setInitial(CD4CodeMill.literalExpressionBuilder().setLiteral(CD4CodeMill.nullLiteralBuilder().build()).build());
      } else if (inner.isPrimitive()) {
        // Attribute is a primitive -> turn into typing.Optional[...]
        typeArgument = MCTypeFacade.getInstance().createBasicTypeArgumentOf(inner.print());
        decNode.setMCType(MCTypeFacade.getInstance()
            .createBasicGenericTypeOf(Arrays.asList("typing", "Optional"),
                Collections.singletonList(typeArgument)));
        data.attrDataMap.computeIfAbsent(originalNode, x -> new AttrData(inner.print()));
        decNode.setInitial(CD4CodeMill.literalExpressionBuilder().setLiteral(CD4CodeMill.nullLiteralBuilder().build()).build());
      } else if (info.getTypeClass() == UmlpAttrHelper.RealType.GEM) {
        // Attribute is an optional domain class -> turn into Gem
        CD4C.getInstance().addImport(
            decoratorData.getAsDecorated((ASTCDClass) decoratorData.getParent(originalNode).get()),
            inner.printFullName());
        decNode.setMCType(createGemType(inner.printFullName() + "." + inner.print()));
        decNode.setInitial(createInitialEmptyGem(inner.printFullName() + "." + inner.print()));
        data.attrDataMap.computeIfAbsent(originalNode, x -> new AttrData(inner.printFullName()));
      } else {
        Log.error("Unhandled " + decNode.getMCType().printType(), decNode.get_SourcePositionStart());
      }


    } else if (info.getMultiplicity() == UmlpAttrHelper.Multiplicity.SET) {

      // Annotated[
      //    gembase.GemList, BeforeValidator(deserialize_gem_list), PlainSerializer(serialize_gem_list,
      //                                                                            return_type=typing.List[
      //                                                                              int])] = GemList()
      if (info.getTypeClass() == UmlpAttrHelper.RealType.GEM) {
        var inner = info.getSymTypeExpression();
        CD4C.getInstance().addImport(
            decoratorData.getAsDecorated((ASTCDClass) decoratorData.getParent(originalNode).get()),
            inner.printFullName());
        decNode.setMCType(createGemListType(inner.printFullName() + "." + inner.print()));
        decNode.setInitial(createInitialGemList());
//        data.attrDataMap.computeIfAbsent(originalNode, x -> new AttrData(inner.printFullName()));
      } else if (info.getTypeClass() == UmlpAttrHelper.RealType.BUILD_IN) {
        var inner = info.getSymTypeExpression();
        CD4C.getInstance().addImport(
            decoratorData.getAsDecorated((ASTCDClass) decoratorData.getParent(originalNode).get()),
            inner.printFullName());

        decNode.setMCType(MCTypeFacade.getInstance().createBasicGenericTypeOf("typing.List", inner.printFullName()));
        if (originalNode.isPresentInitial())
          Log.warn("TODO: initial value of", originalNode.get_SourcePositionStart());
      } else {
        Log.warn("TODO set " + info.getTypeClass(), originalNode.get_SourcePositionStart());
      }

    } else if (SymTypeRelations.isCompatible(originalNode.getSymbol().getType(), getString())) {
      // Change String to "str"
      decNode.setMCType(MCTypeFacade.getInstance().createQualifiedType("str"));
    } else if (info.getTypeClass() == UmlpAttrHelper.RealType.GEM) {
      CD4C.getInstance().addImport(
          decoratorData.getAsDecorated((ASTCDClass) decoratorData.getParent(originalNode).get()),
          originalNode.getSymbol().getType().printFullName());
      decNode.setMCType(createGemType(originalNode.getSymbol().getType().printFullName() + "." + originalNode.getSymbol().getType().print()));
      decNode.setInitial(createInitialEmptyGem(originalNode.getSymbol().getType().printFullName() + "." + originalNode.getSymbol().getType().print()));
      data.attrDataMap.computeIfAbsent(originalNode, x -> new AttrData(originalNode.getSymbol().getType().printFullName()));
    } else if (info.getTypeClass() == UmlpAttrHelper.RealType.BUILD_IN) {
      // do nothing, keep default
    } else if (info.getTypeClass() == UmlpAttrHelper.RealType.ENUM) {
      //  enumState : MyDomain.MyEnum.MyEnum   = MyDomain.MyEnum.MyEnum.A
      // keep default attribute, but add an import
      CD4C.getInstance().addImport(
          decoratorData.getAsDecorated((ASTCDClass) decoratorData.getParent(originalNode).get()),
          originalNode.getSymbol().getType().printFullName());
      decNode.setMCType(createTypeFrom(originalNode.getSymbol().getType()));
      if (originalNode.isPresentInitial()) {
        // EnumCl.EnumVal --> MyDomain.EnumC1
        // (we should probably find a better way)
        ASTFieldAccessExpression expr = (ASTFieldAccessExpression) decNode.getInitial();
        var domain1 = CD4CodeMill.nameExpressionBuilder().setName(domainName).build();
        var name = ((ASTNameExpression) expr.getExpression()).getName();
        var fae1 = CD4CodeMill.fieldAccessExpressionBuilder().setExpression(domain1)
            .setName(name)
            .build();
        var fae2 = CD4CodeMill.fieldAccessExpressionBuilder().setExpression(fae1)
            .setName(name)
            .build();
        expr.setExpression(fae2);
      }
    } else {
      Log.error("Unhandled type kind " + info.getTypeClass(), originalNode.get_SourcePositionStart());
    }
  }

  protected ASTMCQualifiedType createTypeFrom(SymTypeExpression symTypeExpression) {
    return (MCTypeFacade.getInstance().createQualifiedType(symTypeExpression.printFullName() + "." + symTypeExpression.print()));
  }


  private ASTExpression createInitialEmptyGem(String classname) {
    // empty_gem("pck.Type") instead of Gem() -> the default value knows its target type (required for lazy loading)
    return CD4CodeMill.callExpressionBuilder()
        .setExpression(CD4CodeMill.nameExpressionBuilder().setName("montigem_runtime.gembase.empty_gem").build())
        .setArguments(CD4CodeMill.argumentsBuilder().addExpression(stringLiteralExpr(classname)).build()).build();
  }

  private ASTExpression createInitialGemList() {
    return CD4CodeMill.callExpressionBuilder()
        .setExpression(CD4CodeMill.nameExpressionBuilder().setName("montigem_runtime.gembase.GemList").build())
        .setArguments(CD4CodeMill.argumentsBuilder().build()).build();
  }

  protected ASTMCType createGemType(String classname) {
    return MCTypeFacade.getInstance()
        .createBasicGenericTypeOf("typing.Annotated", createGemTypeAs(classname));
  }

  protected ASTMCType createGemListType(String classname) {
    return MCTypeFacade.getInstance()
        .createBasicGenericTypeOf("typing.Annotated", createGemListTypeAs(classname));
  }

  protected String[] createGemTypeAs(String classname) {
    classname = "\"" + classname + "\"";
    // [" + classname + "]
    return new String[]{"montigem_runtime.gembase.Gem", "pydantic.BeforeValidator(montigem_runtime.gembase.create_gem_deserializer(" + classname + "))", "pydantic.PlainSerializer(montigem_runtime.gembase.serialize_gem,return_type=int)"};
  }


  protected String[] createGemListTypeAs(String classname) {
    classname = "\"" + classname + "\"";
    // [" + classname + "]
    return new String[]{"montigem_runtime.gembase.GemList", "pydantic.BeforeValidator(montigem_runtime.gembase.create_gem_list_deserializer(" + classname + "))", "pydantic.PlainSerializer(montigem_runtime.gembase.serialize_gem_list,return_type=typing.List[int])"};
  }

  protected ASTMCBasicGenericType createClassVar(String t) {
    return MCTypeFacade.getInstance().createBasicGenericTypeOf("typing.ClassVar",
        MCTypeFacade.getInstance()
            .createBasicTypeArgumentOf(t));
  }


  protected ASTExpression stringLiteralExpr(String v) {
    return CD4CodeMill.literalExpressionBuilder()
        .setLiteral(CD4CodeMill.stringLiteralBuilder().setSource(v).build()).build();
  }

  @Override
  public void addToTraverser(CD4CodeTraverser traverser) {
    traverser.add4CDBasis(this);
  }

  public static class Data {
    protected final Map<ASTCDAttribute, AttrData> attrDataMap = new HashMap<>();
    protected final Map<ASTCDClass, ASTCDMethod> origClass2Init = new HashMap<>();

    public AttrData getData(ASTCDAttribute astcdAttribute) {
      return attrDataMap.get(astcdAttribute);
    }

    public ASTCDMethod getInitMethod(ASTCDClass origClass) {
      return origClass2Init.get(origClass);
    }
  }

  public static class AttrData {
    protected final String printedType;

    public AttrData(String printedType) {
      this.printedType = printedType;
    }

    public String getPrintedType() {
      return printedType;
    }
  }


}
