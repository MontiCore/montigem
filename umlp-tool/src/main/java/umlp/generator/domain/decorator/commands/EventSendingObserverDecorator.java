package umlp.generator.domain.decorator.commands;

import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDElement;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;

import java.util.List;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;

public class EventSendingObserverDecorator extends AbstractDecorator {
  public static String SUFFIX = "EventSendingObserver";

  public EventSendingObserverDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, COMMAND_PACKAGE);
    var concreteClasses = getConcreteNonPojoDomainClasses(origCD);
    astcdPackage.addCDElement(withOrigin(createClass(origCD, concreteClasses, astcdPackage)));
  }

  protected ASTCDElement createClass(ASTCDCompilationUnit origCD, List<ASTCDClass> classes, ASTCDPackage pack) {
    String name = origCD.getCDDefinition().getName();
    ASTCDClass ast = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setCDInterfaceUsage(getInterfaceUsageFacade().createCDInterfaceUsage(name + "Observer"))
        .setName(name + SUFFIX)
        .build();

    getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + name + "Observer");
    getCD4C().addImport(ast, PathUtil.getBackendRTECommandPath("CommandManager"));

    for (ASTCDClass aClass : classes) {
      String cName = aClass.getName();
      getCD4C().addMethod(ast, "umlp.eventSendingObserver.SendAddedEvent", cName);
      getCD4C().addMethod(ast, "umlp.eventSendingObserver.SendDeleteEvent", cName);
      getCD4C().addImport(ast, getPackageName(origCD, DEFAULT_PACKAGE) + "." + cName);
    }
    return ast;
  }
}
