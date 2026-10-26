/* (c) https://github.com/MontiCore/monticore */
package umlp._cocos;

import de.monticore.cd4codebasis._ast.ASTCDClass;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cd4codebasis._cocos.CD4CodeBasisASTCDClassCoCo;
import de.se_rwth.commons.StringTransformations;
import de.se_rwth.commons.logging.Log;
import umlp.generator.Filter;

import java.util.HashSet;
import java.util.Set;

/**
 * Is the command unique within the domain?
 */
public class UMLPCheckCommandUnique implements CD4CodeBasisASTCDClassCoCo {

  public static final String ERROR_CODE = "0xD0012";

  public static final String ERROR_MSG_FORMAT = " The command `%s` already exists within the domain.";

  protected Set<String> methodNames = new HashSet<>();
  @Override
  public void check(ASTCDClass node) {
    var wrapped = Filter.Wrapper.wrap(node);
    if (!Filter.DERIVED.negate().and(Filter.SINGLETON).test(wrapped)) return;

    for (ASTCDMethod method : node.getCDMethodList()) {
      if (!methodNames.add(StringTransformations.capitalize(method.getName()))) {
        Log.error(ERROR_CODE + ERROR_MSG_FORMAT.formatted(method.getName()), method.get_SourcePositionStart());
      }
    }
  }

}
