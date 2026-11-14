/* (c) https://github.com/MontiCore/monticore */

package umlp.generator.domain.decorator;

import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4codebasis._ast.ASTCDMethodSignature;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.monticore.io.paths.MCPath;
import umlp.UMLPMill;
import umlp.generator.util.PathUtil;

import java.util.List;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;

// TODO: collapse Builder and DeSerBuilder?
public class DeSerBuilderDecorator extends DomainClassDecorator {

  public static final String DESER_BUILDER_SUFFIX = "DeSerBuilder";

  public DeSerBuilderDecorator(final GlobalExtensionManagement glex, MCPath hwPath) {
    super(glex, hwPath, List.of());
  }

  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    String name = origCD.getCDDefinition().getName();
    getConcreteDomainClasses(origCD).forEach(c -> astcdPackage.addCDElement(withOrigin(createClass(c, name, origCD))));
  }

  protected ASTCDClass createClass(ASTCDClass domainClass, String name, ASTCDCompilationUnit origCD) {
    String className = domainClass.getName() + DESER_BUILDER_SUFFIX;

    ASTCDClass deserBuilderClass = UMLPMill.cDClassBuilder()
            .setModifier(PUBLIC.build())
            .setCDExtendUsage(getExtendUsageFacade().createCDExtendUsage(domainClass.getName() + BuilderDecorator.BUILDER_SUFFIX))
            .setName(className).build();

    // Constructors
    getCD4C().addConstructor(deserBuilderClass, getConstructorTemplate(), domainClass.getName());

    // Attributes
    List<ASTCDAttribute> attributes = getAllAttributes(domainClass, false, origCD).attrList.stream()
            .filter(a -> !GenService.isDerived(a)).collect(Collectors.toList());;

    // Methods
    for (ASTCDAttribute a : attributes) {
      if (GenService.isGemMap(a)) {
        getCD4C().addMethod(
            deserBuilderClass,
            "umlp.deser_builder.setGemMapIds",
            deserBuilderClass.getName(),
            GenService.getNativeName(a.getName()),
            GenService.getGemMapStoredKeyType(a)
        );
        getCD4C().addImport(deserBuilderClass, "java.util.Map");
      } else if (GenService.isGemList(a)) {
        getCD4C().addMethod(
            deserBuilderClass,
            getsetGemListIdsTemplate(),
            deserBuilderClass.getName(),
            GenService.getNativeName(a.getName())
        );
      }else if(GenService.isGem(a)){
        getCD4C().addMethod(
            deserBuilderClass,
            getsetGemTemplate(),
            deserBuilderClass.getName(),
            CD4CodeMill.prettyPrint(a.getMCType(), false),
                GenService.getNativeName(a.getName())
        );
      }else{
        ASTCDMethodSignature m = getCD4C().addMethod(
            deserBuilderClass,
            getsetSimpleTemplate(),
            deserBuilderClass.getName(),
            a.printType(),
                GenService.getNativeName(a.getName())
        );
        addOverride(m);
      }
    }


    // Imports
    addImports(domainClass, deserBuilderClass);

    return deserBuilderClass;
  }

  protected void addOverride(ASTCDMethodSignature m) {
    glex.replaceTemplate("cd2java.Annotations", m, new StringHookPoint("@Override"));
  }

  protected String getConstructorTemplate() {
    return "umlp.deser_builder.Constructor";
  }

  protected String getsetSimpleTemplate() {
    return "umlp.deser_builder.setSimple";
  }

  protected String getsetGemTemplate() {
    return "umlp.deser_builder.setGem";
  }

  protected String getsetGemListIdsTemplate() {
    return "umlp.deser_builder.setGemListIds";
  }

  protected void addImports(ASTCDType domainType, ASTCDType clazz) {
    super.addImports(domainType, clazz);
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
  }

}
