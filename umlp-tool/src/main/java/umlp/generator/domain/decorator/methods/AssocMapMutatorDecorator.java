/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.methods;

import com.google.common.collect.Lists;
import de.monticore.cd.codegen.CDGenService;
import de.monticore.cd4codebasis._ast.ASTCDMethod;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import umlp.generator.domain.decorator.DomainTypeDecorator;
import umlp.generator.domain.decorator.GenService;
import umlp.generator.util.PathUtil;

import java.util.List;
import java.util.Optional;

/**
 * Mutators of a GemMap of a qualified association, see GenService#isMap.
 * Attribute qualified: the key of a value is derived from its qualifying attribute, so values are put without a key
 * and rekeyed. Type qualified: values are put under a chosen key and never rekeyed.
 */
public class AssocMapMutatorDecorator extends AssocListMutatorDecorator {

  public AssocMapMutatorDecorator(GlobalExtensionManagement glex, CDGenService service,
                                  ASTCDType ast) {
    super(glex, service, ast);
  }

  @Override
  public List<ASTCDMethod> decorate(ASTCDAttribute ast) {
    List<ASTCDMethod> methodList = Lists.newArrayList();
    if (ast.getModifier().isDerived()) {
      return methodList;
    }
    String name = service.getNativeAttributeName(ast.getName());
    String keyType = AssocMapAccessorDecorator.getTypeArgument(ast, 0);
    String targetName = AssocMapAccessorDecorator.getTypeArgument(ast, 1);
    String sourceName = this.ast.getName();
    boolean isEntry = GenService.isGemEntryMap(ast);
    String storedKeyType = GenService.getGemMapStoredKeyType(ast);

    boolean isKeyDerived = GenService.isKeyDerivedGemMap(ast);
    // bidirectional: the role of the mirror on the value side, which the map's changes update
    Optional<String> mirror = GenService.getMirrorOfGemMap(ast);
    // a single owner ([1]/[0..1] owner side): a put moves the value out of its old owner's map
    boolean moves = mirror.isPresent() && !GenService.isMirrorOfGemMapList(ast);
    if (isKeyDerived) {
      // the server reports a put that replaces a value deriving the same key
      methodList.add(createMethod("umlp.methods.gemmap.Put", name, targetName, sourceName, isEntry, storedKeyType,
          isEntry ? "0xA7317" : "0xA7307", mirror.isPresent(), moves));
    } else {
      methodList.add(createMethod("umlp.methods.gemmap.PutKey", name, keyType, targetName, isEntry));
    }
    cd4c.addImport(this.ast, PathUtil.getBackendRTECommandPath("CommandManager"));
    methodList.add(createMethod("umlp.methods.gemmap.Remove", name, keyType, isEntry));
    // put/remove without scope
    methodList.addAll(createNotifyMethods(methodList, ast.getModifier().isLocal()));

    methodList.add(createMethod("umlp.methods.gemmap.InternalRemoveId", name, sourceName, mirror.isPresent()));
    // put by id: as sent by the server without loading the value (command _put), and the owner put of a type
    // qualified association (chosen keys); rekeying
    methodList.add(createMethod("umlp.methods.gemmap.InternalPutId", name, sourceName, storedKeyType, isEntry,
        !isKeyDerived, mirror.isPresent(), moves));
    if (mirror.isPresent()) {
      String targetManager = ast.getModifier().getStereotype().getValue(DomainTypeDecorator.STEREOTYPE_MANAGER);
      boolean mirrorIsList = GenService.isMirrorOfGemMapList(ast);
      methodList.add(createMethod("umlp.methods.gemmap.MirrorPut", name, mirror.get(), mirrorIsList, targetName,
          targetManager));
      methodList.add(createMethod("umlp.methods.gemmap.MirrorRemove", name, mirror.get(), mirrorIsList, targetName,
          targetManager));
      cd4c.addImport(this.ast, "java.util.Collections");
    }
    if (moves) {
      methodList.add(createMethod("umlp.methods.gemmap.MirrorMove", name, mirror.get(), targetName,
          ast.getModifier().getStereotype().getValue(DomainTypeDecorator.STEREOTYPE_MANAGER),
          ast.getModifier().getStereotype().getValue(DomainTypeDecorator.STEREOTYPE_MIRROR_OWNER),
          ast.getModifier().getStereotype().getValue(DomainTypeDecorator.STEREOTYPE_MIRROR_OWNER_MANAGER)));
      cd4c.addImport(this.ast, "java.util.Optional");
    }
    if (isKeyDerived) {
      methodList.add(createMethod("umlp.methods.gemmap.InternalRekey", name, sourceName, storedKeyType));
    }
    methodList.add(createMethod("umlp.methods.gemmap.RemoveByIdSure", name));
    methodList.add(createMethod("umlp.methods.assocs.RemoveByIdSureNoScope", name, ast.getModifier().isLocal() ? "LOCAL" : "ALL"));
    return methodList;
  }

  protected ASTCDMethod createMethod(String template, Object... args) {
    return (ASTCDMethod) cd4c.createMethod(this.ast, template, args).get();
  }
}
