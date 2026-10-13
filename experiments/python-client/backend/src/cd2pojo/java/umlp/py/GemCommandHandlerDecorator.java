package umlp.py;

import com.google.common.collect.Iterables;
import de.monticore.cd.codegen.decorators.IDecorator;
import de.monticore.cd.codegen.decorators.data.AbstractDecorator;
import de.monticore.cd.facade.CDMethodFacade;
import de.monticore.cd.facade.CDParameterFacade;
import de.monticore.cd.facade.CDTypeFacade;
import de.monticore.cd.methodtemplates.CD4C;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdbasis.CDBasisMill;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.py.utily.PyGenService;
import umlp.py.utily.Pyutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class GemCommandHandlerDecorator extends AbstractDecorator<AbstractDecorator.NoData>
    implements CDBasisVisitor2 {

  public static final String SUFFIX = "CommandHandler";

  protected Stack<String> classNameStack = new Stack<>();

  protected List<Map<String, Object>> commandList = new ArrayList<>();
  protected ASTCDClass commandObserverClass;
  protected String domainName;

  @Override
  public void visit(ASTCDDefinition node) {
    this.domainName = node.getName();
  }

  @Override
  public void visit(ASTCDCompilationUnit node) {
    commandList.clear();
    this.commandObserverClass = CDTypeFacade.getInstance()
        .createCDClass(node.getCDDefinition().getName() + SUFFIX);
  }

  @Override
  public void endVisit(ASTCDCompilationUnit node) {
    decoratorData.getAsDecorated(node).getCDDefinition().addCDElement(commandObserverClass);

    var handleMethod = CDMethodFacade.getInstance()
        .createMethod(CDBasisMill.modifierBuilder().PUBLIC()
                .build(), "handleCommand",
            CDParameterFacade.getInstance().createParameter("typing.Any", "message"),
            CDParameterFacade.getInstance()
                .createParameter("typing.Any", "domain_manager")
        );
    Pyutils.markAsAsync(handleMethod);
    glexOpt.ifPresent(glex -> glex.replaceTemplate(Pyutils.EMPTY_TEMPLATE,
        handleMethod,
        new TemplateHookPoint(
            "cd2python.commandconsumer.handle",
            this.commandList)));
    addToClass(commandObserverClass, handleMethod);
    commandObserverClass = null;
  }

  @Override
  public void visit(ASTCDClass node) {
    classNameStack.add(node.getName());
    CD4C.getInstance().addImport(commandObserverClass, this.domainName + "." + node.getName());
  }

  @Override
  public void visit(ASTCDAttribute node) {
    if (classNameStack.isEmpty()) return;
    var info = UmlpAttrHelper.getFromSymTypeExpr(node.getSymbol().getType());

    String clazzName = classNameStack.peek();

    var role = decoratorData.fieldToRoles.get(node.getSymbol());
    boolean isBidirectional = role != null && role.isIsDefinitiveNavigable() && role.getOtherSide().isIsDefinitiveNavigable();

    switch (info.getMultiplicity()) {
      case MANDATORY:
        switch (info.getTypeClass()) {
          case GEM:
            addCommand(
                domainName.toLowerCase() + ".commands." + clazzName + "_set" + StringTransformations.capitalize(
                    node.getName()),
                "cd2python.commandconsumer.handleSetGem", node.getName());
            break;
          case BUILD_IN:
            addCommand(
                domainName.toLowerCase() + ".commands." + clazzName + "_set" + StringTransformations.capitalize(
                    node.getName()),
                "cd2python.commandconsumer.handleSet", node.getName());
            break;
          case ENUM:
            addCommand(
                domainName.toLowerCase() + ".commands." + clazzName + "_set" + StringTransformations.capitalize(
                    node.getName()),
                "cd2python.commandconsumer.handleSetEnum", node.getName(),
                node.getSymbol().getType().printFullName() + "." + node.getSymbol().getType().print());
            break;
          default:
            Log.warn("Unhandled type kind for handler", node.get_SourcePositionStart());
        }
        break;
      case SET:
        switch (info.getTypeClass()) {
          case GEM:
            if (isBidirectional) {
              if (role.getAssoc().getLeft() == role) {
                return; // do not add duplicate
              }

              String assocName = role.getAssoc().getRight().getEnclosingScope().getName() + "_" + role.getAssoc().getLeft().getName() + "_" + role.getAssoc().getRight().getName() + "_" + StringTransformations.uncapitalize(role.getAssoc().getLeft().getEnclosingScope().getName());
              List<Object> data = Arrays.asList(role.getAssoc().getRight().getEnclosingScope().getName(),
                  role.getAssoc().getRight().getName(),
                  role.getAssoc().getLeft().getEnclosingScope().getName(),
                  role.getAssoc().getLeft().getName(),
                  domainName,
                  PyGenService.isList(role.getAssoc().getRight().getAssocSide()),
                  PyGenService.isList(role.getAssoc().getLeft().getAssocSide())
              );
              addCommand(domainName.toLowerCase() + ".commands.associations." + StringTransformations.capitalize(assocName) + "_add", "cd2python.commandconsumer.handleAddGemBi", data);
              addCommand(domainName.toLowerCase() + ".commands.associations." + StringTransformations.capitalize(assocName) + "_remove", "cd2python.commandconsumer.handleRemGemBi", data);
            } else {
              addCommand(domainName.toLowerCase() + ".commands." + clazzName + "_add" + StringTransformations.capitalize(node.getName()), "cd2python.commandconsumer.handleAddGem", node.getName());
              addCommand(domainName.toLowerCase() + ".commands." + clazzName + "_remove" + StringTransformations.capitalize(node.getName()), "cd2python.commandconsumer.handleRemGem", node.getName());
            }
            break;
          case BUILD_IN:
            addCommand(
                domainName.toLowerCase() + ".commands." + clazzName + "_add" + StringTransformations.capitalize(
                    node.getName()),
                "cd2python.commandconsumer.handleAdd", node.getName());
            addCommand(
                domainName.toLowerCase() + ".commands." + clazzName + "_remove" + StringTransformations.capitalize(
                    node.getName()),
                "cd2python.commandconsumer.handleRem", node.getName());
            break;
          default:
            Log.warn("Unknown multiplicity: " + info.getMultiplicity(), node.get_SourcePositionStart());
        }
      case OPTIONAL:
        String cmdName = domainName.toLowerCase() + ".commands." + clazzName + "_set" + StringTransformations.capitalize(node.getName());
        if (isBidirectional) {
          String assocName = role.getAssoc().getRight().getEnclosingScope().getName() + "_" + role.getAssoc().getLeft().getName() + "_" + role.getAssoc().getRight().getName() + "_" + StringTransformations.uncapitalize(role.getAssoc().getLeft().getEnclosingScope().getName());
          cmdName = domainName.toLowerCase() + ".commands.associations." + StringTransformations.capitalize(assocName) + "_set";
        }

        switch (info.getTypeClass()) {
          case GEM:
            if (isBidirectional) {
              if (role.getAssoc().getLeft() == role) {
                return; // do not add duplicate
              }
              addCommand(cmdName, "cd2python.commandconsumer.handleSetGemBi", Arrays.asList(
                  // clazzL", "attrL", "classR", "attrR", "domain
                  role.getAssoc().getRight().getEnclosingScope().getName(),
                  role.getAssoc().getRight().getName(),
                  role.getAssoc().getLeft().getEnclosingScope().getName(),
                  role.getAssoc().getLeft().getName(),
                  domainName,
                  PyGenService.isList(role.getAssoc().getRight().getAssocSide()),
                  PyGenService.isList(role.getAssoc().getLeft().getAssocSide())
              ));
            } else {
              addCommand(cmdName, "cd2python.commandconsumer.handleSetGem", node.getName());
            }
            break;
          case BUILD_IN:
            if (isBidirectional) {
              Log.error("0xTODO: Bidirectional assoc with built-in " + info.getSymTypeExpression().printFullName() + " not supported", node.get_SourcePositionStart());
            }
            addCommand(cmdName, "cd2python.commandconsumer.handleSet", node.getName());
            break;
        }
        break;
      default:
        Log.warn("Unknown multiplicity: " + info.getMultiplicity(), node.get_SourcePositionStart());
    }

  }

  protected void addCommand(String command, String template, String attr) {
    this.addCommand(command, template, attr, "##unset##");
  }


  protected void addCommand(String command, String template, String attr, String type) {
    addCommand(command, template, Arrays.asList(classNameStack.peek(), attr, domainName, type));
  }

  protected void addCommand(String command, String template, List<Object> data) {
    var m = new HashMap<String, Object>();
    m.put("command", command);
    m.put("template", template);
    m.put("data", data);
    this.commandList.add(m);
  }

  @Override
  public void endVisit(ASTCDClass node) {
    classNameStack.pop();
  }

  @Override
  public void addToTraverser(CD4CodeTraverser traverser) {
    traverser.add4CDBasis(this);
  }

  @Override
  public Iterable<Class<? extends IDecorator>> getMustRunAfter() {
    // TODO: Setter
    return Iterables.concat(super.getMustRunAfter(),
        Arrays.asList(GemSetterDecorator.class));
  }


}
