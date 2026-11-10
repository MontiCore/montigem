package umlp.generator.domain.decorator;

import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDClassBuilder;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDElement;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.StringHookPoint;
import de.se_rwth.commons.logging.Log;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.db.sql.SQLSchemaGenerator;
import umlp.generator.util.PathUtil;

import java.util.ArrayList;
import java.util.List;

import static de.monticore.cd.facade.CDModifier.PUBLIC;

public class QueryBuilderDecorator extends AbstractDecorator {

  public static final String SUFFIX = "QueryBuilder";
  public static final String SUPER_CLASS = "QueryBuilder";
  protected String cdName = "";

  public QueryBuilderDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    cdName = origCD.getCDDefinition().getName();
    List<ASTCDClass> qbList = new ArrayList<>();
    for (ASTCDClass c : getConcreteNonPojoDomainClasses(origCD)) {
      ASTCDClass qb = createQueryBuilderClassForDomainClass(c);
      astcdPackage.addCDElement(withOrigin(qb));
      qbList.add(qb);
    }
    QueryBuilderMapDecorator qbmDecorator = new QueryBuilderMapDecorator(glex, qbList);
    qbmDecorator.transform(origCD, decoratedCD);
    Log.info("After decorator: QueryBuilderMapDecorator", BEDecorator.class.getName());
  }

  /**
   * Creates QueryBuilderClass from a DomainClass.
   * `class Person` -> `class PersonQueryBuilder`.
   *
   * @param domainClass Transformed class.
   * @return Query builder class.
   */
  private ASTCDClass createQueryBuilderClassForDomainClass(ASTCDClass domainClass) {
    String className = getQueryBuilderClassName(domainClass);
    ASTCDClassBuilder builder = UMLPMill.cDClassBuilder()
        .setModifier(PUBLIC.build())
        .setName(className);
    builder.setCDExtendUsage(getExtendUsageFacade()
        .createCDExtendUsage(SUPER_CLASS));

    ASTCDClass clazz = builder.build();

    createBuildMethod(clazz, domainClass);

    // Add @jsweet.lang.Erased annotation
    glex.replaceTemplate("cd2java.Annotations", clazz,
        new StringHookPoint("@jsweet.lang.Erased"));

    addStaticImports(clazz);
    return clazz;
  }

  protected String getQueryBuilderClassName(ASTCDClass domainClass) {
    return domainClass.getName() + SUFFIX;
  }

  protected void createBuildMethod(ASTCDClass queryBuilderClass, ASTCDClass domainClass) {
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);

    String className = SQLSchemaGenerator.convertClassName(domainClass.getName(), SQLSchemaGenerator.AttributeType.PLAIN);
    List<String> attrs = new ArrayList<>();
    for (ASTCDAttribute a : attributes) {
      String s = SQLSchemaGenerator.convertAttributeName(a.getName(), SQLSchemaGenerator.AttributeType.PLAIN);
      attrs.add(s);
    }
    List<String> optAttrs = new ArrayList<>();
    for (ASTCDAttribute a : getOptionalAttributes(domainClass)) {
      String s = SQLSchemaGenerator.convertAttributeName(a.getName(), SQLSchemaGenerator.AttributeType.PLAIN);
      optAttrs.add(s);
    }

    getCD4C().addMethod(queryBuilderClass, "umlp.querybuilder.QueryBuilderBuild",
        className, attrs, optAttrs);
  }

  private void addStaticImports(ASTCDType clazz) {
    // UMLP RTE
    getCD4C().addImport(clazz, PathUtil.getBackendRTEOCLPath(SUPER_CLASS));
  }

}
