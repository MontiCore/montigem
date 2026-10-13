package umlp.py;

import com.google.common.collect.Iterables;
import de.monticore.cd.codegen.decorators.IDecorator;
import de.monticore.cd.codegen.decorators.data.AbstractDecorator;
import de.monticore.cd.facade.CDAttributeFacade;
import de.monticore.cd.facade.CDMethodFacade;
import de.monticore.cd.facade.CDParameterFacade;
import de.monticore.cd.facade.CDTypeFacade;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdbasis.CDBasisMill;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.expressions.commonexpressions.CommonExpressionsMill;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.types.MCTypeFacade;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.py.utily.Pyutils;

import java.util.Arrays;
import java.util.Stack;

public class GemCommandObserverDecorator extends AbstractDecorator<AbstractDecorator.NoData>
    implements CDBasisVisitor2 {

  public static final String SUFFIX = "CommandObserver";

  protected Stack<ASTCDClass> observerClassStack = new Stack<>();
  protected Stack<String> classNameStack = new Stack<>();

  protected String domainName;

  @Override
  public void visit(ASTCDDefinition node) {
    this.domainName = node.getName();
  }

  @Override
  public void visit(ASTCDClass node) {
    // TODO: check if should decorate
    var decNode = decoratorData.getAsDecorated(node);


    var commandObserverClass = CDTypeFacade.getInstance().createCDClass(node.getName() + SUFFIX,
                                                                        domainName + "." + node.getName() + GemObserverDecorator.SUFFIX
                                                                            + "." + node.getName() + GemObserverDecorator.SUFFIX);
    CD4C.getInstance().addImport(commandObserverClass,
                                 domainName + "." + node.getName() + GemObserverDecorator.SUFFIX);

    observerClassStack.add(commandObserverClass);
    classNameStack.add(node.getName());

    addElementToParent(decoratorData.getAsDecorated(decoratorData.getParent(node).get()),
                       commandObserverClass);


    var getScopeFunction = CDMethodFacade.getInstance()
        .createMethod(CDBasisMill.modifierBuilder().build(), CdPyUtilsPrinter.NotificationScope,
                      "getScope");
    glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE,
                                                   getScopeFunction,
                                                   new StringHookPoint(
                                                       "return montigem_runtime.gembase.NotificationScope.GLOBAL")));
    commandObserverClass.addCDMember(getScopeFunction);

    // ACommandObserver.INSTANCE = ACommandObserver()
    glexOpt.ifPresent(glex -> glex.addAfterTemplate("cd2java.Class", commandObserverClass,
                                                    new StringHookPoint(
                                                        commandObserverClass.getName() + ".INSTANCE = " + commandObserverClass.getName() + "()")));

    if (node.isPresentCDExtendUsage()) {
      // CommandObservers also require class hierarchy
      for (var superClass : node.getCDExtendUsage().getSuperclassList()) {
        // add to the left -> higher prio
        commandObserverClass.getCDExtendUsage().addSuperclass(0, MCTypeFacade.getInstance().createQualifiedType(domainName
            + "." + superClass.printType() + GemCommandObserverDecorator.SUFFIX
            + "." + superClass.printType() + GemCommandObserverDecorator.SUFFIX
        ));
        CD4C.getInstance().addImport(commandObserverClass,
            domainName + "." + superClass.printType() + GemCommandObserverDecorator.SUFFIX);
      }
    }
  }

  @Override
  public void visit(ASTCDAttribute node) {
    if (observerClassStack.isEmpty()) return;
    var info = UmlpAttrHelper.getFromSymTypeExpr(node.getSymbol().getType());

    var observerDecClass = observerClassStack.peek();

    var role = decoratorData.fieldToRoles.get(node.getSymbol());
    boolean isBidirectional = role != null && role.isIsDefinitiveNavigable() && role.getOtherSide().isIsDefinitiveNavigable();

    String methodName;
    switch (info.getMultiplicity()) {
      case MANDATORY:
      case OPTIONAL:
        // todo: use method signature from GemObserverDecorator
        methodName = "Set" + node.getName();

        var notify = CDMethodFacade.getInstance()
            .createMethod(CDBasisMill.modifierBuilder().PUBLIC()
                              .build(), "notify" + methodName,
                          CDParameterFacade.getInstance().createParameter("typing.Any",
                                                                          StringTransformations.uncapitalize(
                                                                              classNameStack.peek())),
                          CDParameterFacade.getInstance()
                              .createParameter("typing.Any", "_old_value"),
                          CDParameterFacade.getInstance()
                              .createParameter("typing.Any", node.getName())
                         );
        Pyutils.markAsAsync(notify);
        Pyutils.markAsOverride(glexOpt, notify);
        if (isBidirectional) {
          if (info.getTypeClass() != UmlpAttrHelper.RealType.GEM)
            Log.error("Bidirectional only supported to gem objects");
          String assocName = role.getAssoc().getRight().getEnclosingScope().getName() + "_" + role.getAssoc().getLeft().getName() + "_" + role.getAssoc().getRight().getName() + "_" + StringTransformations.uncapitalize(role.getAssoc().getLeft().getEnclosingScope().getName());
          String cmdName = domainName.toLowerCase() + ".commands.associations." + StringTransformations.capitalize(assocName) + "_set";


          glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE, notify, new TemplateHookPoint( "cd2python.gemcommandobserver.SetGemBi",
              cmdName,
              StringTransformations.uncapitalize(classNameStack.peek()),
              role.getOtherSide().getName(),
              role.getName())));
        } else {
          glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE, notify, new TemplateHookPoint(info.getTypeClass() == UmlpAttrHelper.RealType.GEM ? "cd2python.gemcommandobserver.SetGem" : "cd2python.gemcommandobserver.SetBuiltIn", domainName.toLowerCase(), StringTransformations.uncapitalize(classNameStack.peek()), node.getName())));
        }
        addToClass(observerDecClass, notify);
        break;
      case SET:
        // todo: use method signature from GemObserverDecorator
      {
        methodName = "Add" + node.getName();

        var notifyAdd = CDMethodFacade.getInstance()
            .createMethod(CDBasisMill.modifierBuilder().PUBLIC()
                    .build(), "notify" + methodName,
                CDParameterFacade.getInstance().createParameter("typing.Any",
                    StringTransformations.uncapitalize(
                        classNameStack.peek())),
                CDParameterFacade.getInstance()
                    .createParameter("typing.Any", node.getName()),
                CDParameterFacade.getInstance()
                    .createParameter("typing.Any", "index_in_list")
            );
        Pyutils.markAsAsync(notifyAdd);
        Pyutils.markAsOverride(glexOpt, notifyAdd);
        if (isBidirectional) {
          if (info.getTypeClass() != UmlpAttrHelper.RealType.GEM)
            Log.error("Bidirectional only supported to gem objects");
          String assocName = role.getAssoc().getRight().getEnclosingScope().getName() + "_" + role.getAssoc().getLeft().getName() + "_" + role.getAssoc().getRight().getName() + "_" + StringTransformations.uncapitalize(role.getAssoc().getLeft().getEnclosingScope().getName());
          String cmdName = domainName.toLowerCase() + ".commands.associations." + StringTransformations.capitalize(assocName) + "_add";


          glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE, notifyAdd, new TemplateHookPoint( "cd2python.gemcommandobserver.AddGemBi",
              cmdName,
              StringTransformations.uncapitalize(classNameStack.peek()),
              role.getOtherSide().getName(),
              role.getName())));
        } else {
          glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE, notifyAdd, new TemplateHookPoint(info.getTypeClass() == UmlpAttrHelper.RealType.GEM ? "cd2python.gemcommandobserver.AddGem" : "cd2python.gemcommandobserver.AddBuiltIn", domainName.toLowerCase(), StringTransformations.uncapitalize(classNameStack.peek()), node.getName())));
        }
        addToClass(observerDecClass, notifyAdd);
      }
      {
        methodName = "Remove" + node.getName();

        var notifyAdd = CDMethodFacade.getInstance()
            .createMethod(CDBasisMill.modifierBuilder().PUBLIC()
                    .build(), "notify" + methodName,
                CDParameterFacade.getInstance().createParameter("typing.Any",
                    StringTransformations.uncapitalize(
                        classNameStack.peek())),
                CDParameterFacade.getInstance()
                    .createParameter("typing.Any", node.getName()),
                CDParameterFacade.getInstance()
                    .createParameter("typing.Any", "index_in_list")
            );
        Pyutils.markAsAsync(notifyAdd);
        Pyutils.markAsOverride(glexOpt, notifyAdd);
        if (isBidirectional) {
          if (info.getTypeClass() != UmlpAttrHelper.RealType.GEM)
            Log.error("Bidirectional only supported to gem objects");
          String assocName = role.getAssoc().getRight().getEnclosingScope().getName() + "_" + role.getAssoc().getLeft().getName() + "_" + role.getAssoc().getRight().getName() + "_" + StringTransformations.uncapitalize(role.getAssoc().getLeft().getEnclosingScope().getName());
          String cmdName = domainName.toLowerCase() + ".commands.associations." + StringTransformations.capitalize(assocName) + "_remove";


          glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE, notifyAdd, new TemplateHookPoint( "cd2python.gemcommandobserver.RemGemBi",
              cmdName,
              StringTransformations.uncapitalize(classNameStack.peek()),
              role.getOtherSide().getName(),
              role.getName())));
        } else {
          glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE, notifyAdd, new TemplateHookPoint(info.getTypeClass() == UmlpAttrHelper.RealType.GEM ? "cd2python.gemcommandobserver.RemGem" : "cd2python.gemcommandobserver.RemBuiltIn", domainName.toLowerCase(), StringTransformations.uncapitalize(classNameStack.peek()), node.getName())));
        }
        addToClass(observerDecClass, notifyAdd);
      }

        break;
      default:
        Log.warn("Unknown multiplicity: " + info.getMultiplicity(), node.get_SourcePositionStart());
    }

  }

  @Override
  public void endVisit(ASTCDClass node) {
    classNameStack.pop();
    observerClassStack.pop();
  }

  @Override
  public void addToTraverser(CD4CodeTraverser traverser) {
    traverser.add4CDBasis(this);
  }

  @Override
  public Iterable<Class<? extends IDecorator>> getMustRunAfter() {
    // TODO: Setter
    return Iterables.concat(super.getMustRunAfter(),
                            Arrays.asList(GemSetterDecorator.class, GemObserverDecorator.class));
  }

  // TODO: Backport
  public <D> D getData(Class<? extends IDecorator<D>> decorator) {
    return (D) this.decoratorData.getData(decorator);
  }
}
