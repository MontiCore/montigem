package umlp.py;

import com.google.common.collect.Iterables;
import de.monticore.cd.codegen.decorators.IDecorator;
import de.monticore.cd.codegen.decorators.data.AbstractDecorator;
import de.monticore.cd.facade.CDMethodFacade;
import de.monticore.cd.facade.CDParameterFacade;
import de.monticore.cd.facade.CDTypeFacade;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._visitor.CDAssociationVisitor2;
import de.monticore.cdbasis.CDBasisMill;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.se_rwth.commons.logging.Log;
import umlp.py.utily.PyGenService;
import umlp.py.utily.Pyutils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Add observer to gem classes if:
 * - an association is bidirectional
 * This BiObserver updates the other side of the association
 */
public class GemBiAssocObserverDecorator extends AbstractDecorator<AbstractDecorator.NoData>
    implements CDBasisVisitor2, CDAssociationVisitor2 {

  public static final String SUFFIX = "BiObserver";

  protected Map<ASTCDClass, ASTCDClass> domaiNClass2BiObclass = new LinkedHashMap<>();

  protected String domainName;

  @Override
  public void visit(ASTCDDefinition node) {
    this.domainName = node.getName();
  }

  protected ASTCDClass getBiObserverClass(ASTCDClass originalClass) {
    if (this.domaiNClass2BiObclass.containsKey(originalClass)) {
      return this.domaiNClass2BiObclass.get(originalClass);
    }
    var decNode = decoratorData.getAsDecorated(originalClass);

    var biObserverClass = CDTypeFacade.getInstance().createCDClass(decNode.getName() + SUFFIX, domainName + "." + decNode.getName() + GemObserverDecorator.SUFFIX + "." + decNode.getName() + GemObserverDecorator.SUFFIX);
    CD4C.getInstance().addImport(biObserverClass, domainName + "." + decNode.getName() + GemObserverDecorator.SUFFIX);

    addElementToParent(decoratorData.getAsDecorated(decoratorData.getParent(originalClass).get()), biObserverClass);

    // getScope -> LOCAL
    var getScopeFunction = CDMethodFacade.getInstance().createMethod(CDBasisMill.modifierBuilder().build(), CdPyUtilsPrinter.NotificationScope, "getScope");
    glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE, getScopeFunction, new StringHookPoint("return montigem_runtime.gembase.NotificationScope.LOCAL")));
    biObserverClass.addCDMember(getScopeFunction);

    // ABiObserver.INSTANCE = ABiObserver()
    glexOpt.ifPresent(glex -> glex.addAfterTemplate("cd2java.Class", biObserverClass, new StringHookPoint(biObserverClass.getName() + ".INSTANCE = " + biObserverClass.getName() + "()")));

    // We have to avoid the circular import by adding a local import to the init function
    var gemBaseInit = decoratorData.getDecoratorData(GemBaseDecorator.class).getInitMethod(originalClass);

    glexOpt.ifPresent(glex -> glex.addAfterTemplate("Init:After", gemBaseInit, new StringHookPoint("import " + domainName + "." + originalClass.getName() + SUFFIX + "\n" + "self._observer += [" + domainName + "." + originalClass.getName() + SUFFIX + "." + originalClass.getName() + SUFFIX + ".INSTANCE] \n")));

    this.domaiNClass2BiObclass.put(originalClass, biObserverClass);
    return biObserverClass;
  }

  @Override
  public void visit(ASTCDAssociation node) {
    if (!node.getCDAssocDir().isBidirectional()) {
      return;
    }

    handleAssoc(node.getLeft(), node.getRight());
    handleAssoc(node.getRight(), node.getLeft());

  }

  protected void handleAssoc(ASTCDAssocSide side, ASTCDAssocSide otherSide) {
    var thisSideClass = side.getMCQualifiedType().getDefiningSymbol();
    if (thisSideClass.isEmpty()) {
      Log.error("Failed to resolve " + side.getMCQualifiedType().printType(), side.get_SourcePositionStart());
      return;
    }

    var biObserverClass = getBiObserverClass((ASTCDClass) thisSideClass.get().getAstNode());


    //
    String managerPath = domainName + "." + domainName + "Manager" + "." + domainName + "Manager";
    CD4C.getInstance().addImport(biObserverClass, domainName + "." + domainName + "Manager");
    String otherSideClassName =
        side.getSymbol().getOtherSide().getType().getTypeInfo().getEnclosingScope().getName() + "." +
            side.getSymbol().getOtherSide().getType().print() + "." +
            side.getSymbol().getOtherSide().getType().print();

    if (!otherSide.isPresentCDCardinality() || otherSide.getCDCardinality().isOne() || otherSide.getCDCardinality().isOpt()) {
      // mandatory or opt
      String methodName = "Set" + otherSide.getName();


      var notify = CDMethodFacade.getInstance()
          .createMethod(CDBasisMill.modifierBuilder().PUBLIC()
                  .build(), "notify" + methodName,
              CDParameterFacade.getInstance().createParameter("typing.Any", "_update_obj"),
              CDParameterFacade.getInstance().createParameter("montigem_runtime.gembase.GEM_ID", "_old_value"),
              CDParameterFacade.getInstance().createParameter("montigem_runtime.gembase.GEM_ID", "_new_value")
          );
      Pyutils.markAsAsync(notify);
      Pyutils.markAsOverride(glexOpt, notify);
      glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE,
          notify,
          new TemplateHookPoint(
              "cd2python.biobserver.NotifyMan",
              managerPath,
              otherSideClassName,
              side.getName(),
              PyGenService.isList(side)
          )));
      addToClass(biObserverClass, notify);
    } else { // mult
      {
        String methodName = "Add" + otherSide.getName();

        var notify = CDMethodFacade.getInstance()
            .createMethod(CDBasisMill.modifierBuilder().PUBLIC()
                    .build(), "notify" + methodName,
                CDParameterFacade.getInstance().createParameter("typing.Any", "_update_obj"),
                CDParameterFacade.getInstance().createParameter("montigem_runtime.gembase.GEM_ID", "new_gem_id"),
                CDParameterFacade.getInstance().createParameter("int", "index_in_list")
            );
        Pyutils.markAsAsync(notify);
        Pyutils.markAsOverride(glexOpt, notify);
        glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE,
            notify,
            new TemplateHookPoint(
                "cd2python.biobserver.NotifyAdd",
                managerPath,
                otherSideClassName,
                side.getName(),
                PyGenService.isList(side)
            )));
        addToClass(biObserverClass, notify);
      }
      {
        String methodName = "Remove" + otherSide.getName();

        var notify = CDMethodFacade.getInstance()
            .createMethod(CDBasisMill.modifierBuilder().PUBLIC()
                    .build(), "notify" + methodName,
                CDParameterFacade.getInstance().createParameter("typing.Any", "_update_obj"),
                CDParameterFacade.getInstance().createParameter("montigem_runtime.gembase.GEM_ID", "removed_gem_id"),
                CDParameterFacade.getInstance().createParameter("int", "index_in_list")
            );
        Pyutils.markAsAsync(notify);
        Pyutils.markAsOverride(glexOpt, notify);
        glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE,
            notify,
            new TemplateHookPoint(
                "cd2python.biobserver.NotifyRem",
                managerPath,
                otherSideClassName,
                side.getName(),
                PyGenService.isList(side)
            )));
        addToClass(biObserverClass, notify);
      }
    }
  }

  @Override
  public void addToTraverser(CD4CodeTraverser traverser) {
    traverser.add4CDBasis(this);
    traverser.add4CDAssociation(this);
  }

  @Override
  public Iterable<Class<? extends IDecorator>> getMustRunAfter() {
    return Iterables.concat(super.getMustRunAfter(), List.of(GemSetterDecorator.class, GemObserverDecorator.class));
  }

}
