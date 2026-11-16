/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.generator.domain.decorator.QualifierKey;

import java.util.List;
import java.util.Set;

/**
 * Generates the command {@code <Owner>_rekey<Role>} of the GemMap of an attribute qualified association: moves a
 * value from oldKey to newKey after its qualifying attribute changed. Only the server sends it, to the participants
 * that loaded the owner, so that also those that did not load the value (and thus do not receive its setter
 * commands) know the new key. The server never accepts it from a client.
 */
public class RekeyDecorator extends PutDecorator {

  public static final String REKEY_SUFFIX = "_rekey";

  public RekeyDecorator(GlobalExtensionManagement glex, Set<String> supportedDeSers) {
    super(glex, supportedDeSers);
  }

  @Override
  protected String getSuffix() {
    return REKEY_SUFFIX;
  }

  @Override
  protected List<String> getKeyFields() {
    return List.of("oldKey", "newKey");
  }

  @Override
  protected void addDoAction(ASTCDClass ast, ASTCDCompilationUnit origCD, ASTCDAssocSide a, ASTCDType domainType,
                             QualifierKey key) {
    getCD4C().addMethod(ast, "umlp.commands.rekey.DoRun", domainType.getName(), getCorrectManager(domainType),
        a.getName());
  }

  @Override
  protected void addCheckAccessPolicy(ASTCDClass ast, ASTCDType domainType) {
    getCD4C().addMethod(ast, "umlp.commands.rekey.CheckAccessPolicy");
  }
}
