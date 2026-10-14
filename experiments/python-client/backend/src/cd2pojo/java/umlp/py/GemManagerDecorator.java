package umlp.py;

import de.monticore.cd.codegen.decorators.data.AbstractDecorator;
import de.monticore.cd.facade.CDMethodFacade;
import de.monticore.cd.facade.CDParameterFacade;
import de.monticore.cd.facade.CDTypeFacade;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdbasis.CDBasisMill;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.generating.templateengine.TemplateHookPoint;
import umlp.py.utily.Pyutils;

/**
 * The manager knows the package & holds a static instance
 */
public class GemManagerDecorator extends AbstractDecorator<AbstractDecorator.NoData>
    implements CDBasisVisitor2 {

  protected String domainName;

  @Override
  public void visit(ASTCDDefinition node) {
    this.domainName = node.getName();

    var managerClass = CDTypeFacade.getInstance().createCDClass(node.getName() + "Manager",
        "montigem_runtime.gembase.DomainManager");


    addElementToParent(decoratorData.getAsDecorated(node), managerClass);

    var initFunction = CDMethodFacade.getInstance().createMethod(CDBasisMill.modifierBuilder().build(),
        "__init__",
        CDParameterFacade.getInstance().createParameter("montigem_runtime.commandmanager.CommandManager", "command_manager"));
    managerClass.addCDMember(initFunction);

    glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE,
        initFunction,
        new TemplateHookPoint("cd2python.manager.Init", node.getName().toLowerCase(), managerClass.getName())));

  }

  @Override
  public void addToTraverser(CD4CodeTraverser traverser) {
    traverser.add4CDBasis(this);
  }

}
