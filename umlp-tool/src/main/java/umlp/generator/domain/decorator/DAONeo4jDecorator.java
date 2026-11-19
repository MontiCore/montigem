package umlp.generator.domain.decorator;

import com.google.common.collect.Maps;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._ast.ASTCDTypeTOP;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.StringTransformations;
import umlp.generator.db.sql.SQLSchemaGenerator;
import umlp.generator.util.PathUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.monticore.cd.facade.CDModifier.PRIVATE;

public class DAONeo4jDecorator extends DAODecorator {

  public static final String neo4J_SUFFIX = "Neo4J";
  private static final String neo4J_Template_SUFFIX="_Neo4j";

  public DAONeo4jDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  protected String getDAOClassName(ASTCDClass domainClass) {
    return domainClass.getName() + "DAO"+ neo4J_SUFFIX;
  }

  @Override
  protected String getDAOSpecificNameForAssoc(String assocName) {
    return "Assoc" + StringTransformations.capitalize(assocName) + "DAO"+neo4J_SUFFIX;
  }

  @Override
  protected void removeAllMethodForAssocDAO(ASTCDType daoClass, ASTCDAssociation assoc,
                                            ASTCDAssocSide start, ASTCDAssocSide end) {
    boolean startIsLeft = assoc.getLeft().equals(start);
    String startType = CD4CodeMill.prettyPrint(start.getMCQualifiedType(), false);
    String endType = CD4CodeMill.prettyPrint(end.getMCQualifiedType(), false);
    getCD4C().addMethod(daoClass, "umlp.dao.associations.RemoveAllInitNeo4jForAssocDAO",
        start.getName(), end.getName(), startType, endType,
        startIsLeft ? assoc.getCDAssocDir().isDefinitiveNavigableRight()
            : assoc.getCDAssocDir().isDefinitiveNavigableLeft(),
        startIsLeft ? assoc.getCDAssocDir().isDefinitiveNavigableLeft()
            : assoc.getCDAssocDir().isDefinitiveNavigableRight());
    getCD4C().addMethod(daoClass, "umlp.dao.associations.RemoveAllForAssocDAONeo4j", start.getName());
  }

  @Override
  protected void removeMethodForAssociationDAO(ASTCDType daoClass, ASTCDAssociation assoc) {
    ASTCDAssocSide assocStart = assoc.getLeft();
    ASTCDAssocSide assocEnd = assoc.getRight();

    getCD4C().addMethod(daoClass, "umlp.dao.associations.RemoveInitNeo4jForAssocDAO", assoc.getName(),
        assocStart.getName(),
        assocEnd.getName(),
        CD4CodeMill.prettyPrint(assocStart.getMCQualifiedType(), false),
        CD4CodeMill.prettyPrint(assocEnd.getMCQualifiedType(), false),
        assoc.getCDAssocDir().isDefinitiveNavigableRight(),
        assoc.getCDAssocDir().isDefinitiveNavigableLeft()
        );
    getCD4C().addMethod(daoClass, "umlp.dao.associations.RemoveForAssocDAONeo4j", assoc);
  }

  @Override
  protected void addMethodsForAssociationDAO(ASTCDType daoClass, ASTCDAssociation assoc) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.AddInitNeo4jForAssocDAO", assoc.getName(),
            assoc.getLeft().getSymbol().getType().print(),
            assoc.getRight().getSymbol().getType().print(),
            assoc.getLeft().getName(),
            assoc.getRight().getName(),
            assoc.getCDAssocDir().isDefinitiveNavigableLeft(),
            assoc.getCDAssocDir().isDefinitiveNavigableRight(),
            GenService.isMultiValued(assoc.getLeft()),
            GenService.isMultiValued(assoc.getRight()));
    getCD4C().addMethod(daoClass, "umlp.dao.associations.AddForAssocDAONeo4j", assoc);
    getCD4C().addMethod(daoClass, "umlp.dao.associations.AddManyForAssocDAONeo4j", assoc);
  }

  @Override
  protected void selectAllIdsMethod(ASTCDType daoClass, ASTCDClass domainClass,
                                    Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                                    Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap){
    List<ASTCDAttribute> listAttributes = getListAttributes(domainClass);
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);
    Map<ASTCDAttribute, String> optMap = Maps.newLinkedHashMap();
    getOptionalAttributes(domainClass).forEach(e -> optMap.put(e,
        SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));

    getCD4C().addMethod(daoClass, "umlp.dao.SelectAllIdsInitNeo4j",
        SQLSchemaGenerator.convertClassName(domainClass.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED), domainClass.getName(),
        attributes.stream().map(a -> SQLSchemaGenerator.convertAttributeName(a.getName(), SQLSchemaGenerator.AttributeType.ESCAPED_QUOTED)).collect(Collectors.toList()));

    Map<ASTCDAttribute, String> attributeMap = Maps.newLinkedHashMap();
    attributes.forEach(e -> attributeMap.put(e,
        SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));
    getCD4C().addMethod(daoClass, "umlp.dao.SelectAllIdsNeo4j", domainClass.getName(), cdName, attributeMap,
        optMap, listAttributes, listAssociationMap, gemAssociationMap);
  }

  @Override
  protected void selectMethod(ASTCDType daoClass, ASTCDClass domainClass,
                            Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                            Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap) {
    List<ASTCDAttribute> listAttributes = getListAttributes(domainClass);
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);
    Map<ASTCDAttribute, String> optMap = Maps.newLinkedHashMap();
    getOptionalAttributes(domainClass).forEach(e -> optMap.put(e,
                    SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));

    getCD4C().addMethod(daoClass, "umlp.dao.SelectInitNeo4j", domainClass.getName(), attributes, getOptionalAttributes(domainClass));

    Map<ASTCDAttribute, String> attributeMap = Maps.newLinkedHashMap();
    attributes.forEach(e -> attributeMap.put(e, SQLSchemaGenerator.convertAttributeName(e.getName(), SQLSchemaGenerator.AttributeType.QUOTED)));
    getCD4C().addMethod(daoClass, "umlp.dao.SelectNeo4j", domainClass.getName(), cdName, attributeMap,
        optMap, listAttributes, listAssociationMap, gemAssociationMap);
  }

  @Override
  protected void insertMethods(ASTCDType daoClass, ASTCDClass domainClass,
                            Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                            Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap) {
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);
    List<ASTCDAttribute> listAttributes = getListAttributes(domainClass);
    List<ASTCDAttribute> optionalAttributes = getOptionalAttributes(domainClass);
    List<String> superTypeNamesInclSelf = new ArrayList<>(CDSymbolTables.getTransitiveSuperTypes(domainClass).stream().map(ASTCDTypeTOP::getName).collect(Collectors.toList()));
    superTypeNamesInclSelf.add(domainClass.getName());

    Map<String, String> assocToCorrectManagerMap =
        getAssocToSourceDefinitionCD(Stream.of(gemAssociationMap.values(), listAssociationMap.values()).flatMap(Collection::stream));

    getCD4C().addMethod(daoClass, "umlp.dao.InsertInitNeo4j", domainClass.getName(),
        superTypeNamesInclSelf, attributes, optionalAttributes);
    getCD4C().addMethod(daoClass, "umlp.dao.InsertNeo4j", domainClass.getName(), cdName, attributes,
        optionalAttributes , listAttributes, listAssociationMap, gemAssociationMap, assocToCorrectManagerMap, neo4J_Template_SUFFIX);
    /*getCD4C().addMethod(daoClass, "umlp.dao.BulkInsert", domainClass.getName(), cdName, attributes,
        optionalAttributes , listAttributes, listAssociationMap, gemAssociationMap, neo4J_Template_SUFFIX);*/
  }

  @Override
  protected void selectInitMethodForListAssocDAO(ASTCDType daoClass, String assocName, ASTCDAssocSide start, ASTCDAssocSide end, String numerus) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.SelectInitNeo4jForAssocDAO", start.getName(),
        end.getName(), CD4CodeMill.prettyPrint(start.getMCQualifiedType(), false), assocName, numerus);
  }

  @Override
  protected void deleteMethod(ASTCDType daoClass, ASTCDClass domainClass, Map<ASTCDAssocSide, ASTCDAssociation> assocSideToAssoc) {
    getCD4C().addMethod(daoClass, "umlp.dao.DeleteNeo4j", domainClass.getName(), assocSideToAssoc);
  }

  @Override
  protected void updateAttrMethod(ASTCDType doaClass, ASTCDClass domainClass){
    CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived() && !GenService.isList(a) && !GenService.isOptional(a)).forEach(attr ->
            getCD4C().addMethod(doaClass, "umlp.dao.UpdateAttrNeo4j", domainClass.getName(), attr.getName(),
                    attr.getMCType().printType(), GenService.getDBType(attr.getSymbol().getType()), false));
  }

  @Override
  protected void updateOptionalAttrMethod(ASTCDType doaClass, ASTCDClass domainClass){
    CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived() && GenService.isOptional(a)).forEach(attr ->
            getCD4C().addMethod(doaClass, "umlp.dao.UpdateAttrNeo4j", domainClass.getName(), attr.getName(),
                GenService.createType(attr).printType(), GenService.getDBType(attr.getSymbol().getType()), true));
  }

  @Override
  protected void updateAndSelectListAttrMethods(ASTCDType doaClass, ASTCDClass domainClass) {
    CDSymbolTables.getAttributesInHierarchy(domainClass).stream()
        .filter(a -> !a.getModifier().isDerived() && GenService.isList(a)).forEach(attr -> {
              GenService.createType(attr);
              String innerType = GenService.getInnerType(GenService.createType(attr));
              String tableName = SQLSchemaGenerator.convertListAttributeTableName(domainClass.getName(), attr.getName());
              String dbType = GenService.getDBType(attr.getSymbol().getType());
              getCD4C().addMethod(doaClass, "umlp.dao.UpdateListAttrNeo4j", domainClass.getName(), tableName, attr, innerType, dbType);
              getCD4C().addMethod(doaClass, "umlp.dao.SelectListAttrNeo4j", domainClass.getName(), tableName, attr, innerType, dbType);
            }
        );
  }

  @Override
  protected void selectFilteredMethod(ASTCDType daoClass, ASTCDClass domainClass) {
    getCD4C().addMethod(daoClass, "umlp.dao.SelectFilteredInit", domainClass.getName());
    getCD4C().addMethod(daoClass, "umlp.dao.SelectFilteredNeo4j", domainClass.getName());
  }

  @Override
  protected void selectMethodForListAssocDAO(ASTCDType daoClass, ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.SelectForAssocDAONeo4j", start.getName(), end.getName());
  }

  @Override
  protected void selectMethodForGemAssocDAO(ASTCDType daoClass, ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoClass, "umlp.dao.associations.SelectForGemAssocDAONeo4j", start.getName(), end.getName());
  }

  @Override
  protected void updateMethodForAssocDAO(ASTCDType daoClass, ASTCDAssociation association,
        ASTCDAssocSide start, ASTCDAssocSide end) {
    boolean startIsLeft = association.getLeft().equals(start);
    String leftType = association.getLeft().getMCQualifiedType().printType();
    String rightType = association.getRight().getMCQualifiedType().printType();

    String leftName = association.getLeft().getName();
    String rightName = association.getRight().getName();

    boolean navigableRight = association.getCDAssocDir().isDefinitiveNavigableRight();
    boolean navigableLeft = association.getCDAssocDir().isDefinitiveNavigableLeft();

    if(!startIsLeft) {
      getCD4C().addMethod(daoClass, "umlp.dao.associations.UpdateInitNeo4jForAssocDAO", association.getName(),
          rightName, leftName, rightType, leftType, navigableLeft, navigableRight,
          GenService.isMultiValued(association.getRight()), GenService.isMultiValued(association.getLeft()));
      getCD4C().addMethod(daoClass, "umlp.dao.associations.UpdateNeo4jForAssocDAO", rightName, leftName);
    } else {
      getCD4C().addMethod(daoClass, "umlp.dao.associations.UpdateInitNeo4jForAssocDAO", association.getName(),
          leftName, rightName, leftType, rightType, navigableRight, navigableLeft,
          GenService.isMultiValued(association.getLeft()), GenService.isMultiValued(association.getRight()));
      getCD4C().addMethod(daoClass, "umlp.dao.associations.UpdateNeo4jForAssocDAO", leftName, rightName);
    }

  }

  /**
   * See method in super class. We do not want kebab-case for attributes as in the SQL variant
   */
  @Override
  protected void unpackMethod(ASTCDType daoClass, ASTCDClass domainClass,
                              Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                              Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap) {
    List<ASTCDAttribute> listAttributes = getListAttributes(domainClass);
    List<ASTCDAttribute> attributes = getNormalAttributes(domainClass);
    Map<ASTCDAttribute, String> optMap = Maps.newLinkedHashMap();
    getOptionalAttributes(domainClass).forEach(e -> optMap.put(e, "\"" + e.getName() + "\""));

    Map<String, String> assocToCorrectManagerMap =
        getAssocToSourceDefinitionCD(Stream.of(gemAssociationMap.values(), listAssociationMap.values()).flatMap(Collection::stream));

    Map<String, String> assocToCorrectDAOFactoryMap =
        getAssocToTargetDefinitionCD(Stream.of(gemAssociationMap.values(), listAssociationMap.values()).flatMap(Collection::stream));

    Map<ASTCDAttribute, String> attributeMap = Maps.newLinkedHashMap();
    attributes.forEach(e -> attributeMap.put(e, "\"" + e.getName() + "\""));
    getCD4C().addMethod(daoClass, "umlp.dao.UnpackNeo4j", domainClass.getName(), cdName, attributeMap,
        optMap, listAttributes, listAssociationMap, gemAssociationMap, assocToCorrectManagerMap, assocToCorrectDAOFactoryMap,neo4J_Template_SUFFIX);
  }

  @Override
  protected void addDatabaseSpecificImports(ASTCDType clazz) {
    // UMLP RTE
    getCD4C().addImport(clazz, "umlp.backendrte.common.DatabaseAccessManager4Neo4j");
    // Neo4j
    getCD4C().addImport(clazz, "org.neo4j.driver.Session");
    getCD4C().addImport(clazz, "org.neo4j.driver.Values");
    getCD4C().addImport(clazz, "org.neo4j.driver.Result");
    getCD4C().addImport(clazz, "org.neo4j.driver.Record");
  }
}
