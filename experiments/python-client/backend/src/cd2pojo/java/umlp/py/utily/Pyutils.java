package umlp.py.utily;

import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.umlmodifier._ast.ASTModifier;
import de.monticore.umlstereotype.UMLStereotypeMill;

import java.util.Optional;

public class Pyutils {
  public static final String EMPTY_TEMPLATE = "cd2python.EmptyBody";

  public static void markAsAsync(ASTModifier modifier) {
    if (!modifier.isPresentStereotype())
      modifier.setStereotype(UMLStereotypeMill.stereotypeBuilder().uncheckedBuild());
    modifier.getStereotype().addValues(UMLStereotypeMill.stereoValueBuilder().setName("py__async").build());
  }

  public static void markAsAsync(ASTCDMethod method) {
    markAsAsync(method.getModifier());
  }

  public static void markAsOverride(Optional<GlobalExtensionManagement> glexOpt, ASTCDMethod method) {
    glexOpt.ifPresent(glex -> glex.addAfterTemplate("cd2java.Annotations", method, new StringHookPoint("@typing.override")));

  }
}
