package umlp.generator.domain.decorator;

import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterfaceBuilder;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.StringTransformations;
import umlp.UMLPMill;
import umlp.generator.AbstractDecorator;
import umlp.generator.util.PathUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static de.monticore.cd.facade.CDModifier.PUBLIC;
import static umlp.generator.util.UMLPSymbolTables.getOppositeSideToAssociationMap;

public class DAOInterfaceDecorator extends AbstractDecorator {

  public static final String INTERFACE_SUFFIX = "Interface";

  protected String cdName = "";

  public DAOInterfaceDecorator(GlobalExtensionManagement glex) {
    super(glex);
  }

  @Override
  public void transform(ASTCDCompilationUnit origCD, ASTCDCompilationUnit decoratedCD) {
    cdName = origCD.getCDDefinition().getName();
    ASTCDPackage astcdPackage = getPackage(origCD, decoratedCD, DEFAULT_PACKAGE);
    getConcreteDomainClasses(origCD).forEach(c ->
        astcdPackage.addCDElement(withOrigin(
            dAOForDomainClass(c))));

    origCD.getCDDefinition().getCDAssociationsList().stream()
        .filter(a -> !a.getModifier().isDerived())
        .forEach(assoc -> astcdPackage.addCDElement(withOrigin(
            dAOForAssoc(assoc))));
  }

  /**
   * Creates DaoType from a DomainClass
   * @param domainClass
   * @return
   */
  protected ASTCDType dAOForDomainClass(ASTCDClass domainClass) {
    Map<ASTCDAssocSide, ASTCDAssociation> assocSideToAssociation = getOppositeSideToAssociationMap(domainClass)
        .entrySet().stream()
        .filter(entry -> !entry.getValue().getModifier().isDerived())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(l,r) ->l, LinkedHashMap::new));

    ASTCDType domainClassDAO = createDAOTypeForDomainClass(domainClass);

    this.addStaticImports(domainClassDAO);
    this.addDatabaseSpecificImports(domainClassDAO);
    this.addImports(domainClass, domainClassDAO);
    this.domainClassDAOMethods(domainClassDAO, domainClass, assocSideToAssociation);
    return domainClassDAO;
  }

  protected ASTCDType createDAOTypeForDomainClass(ASTCDClass domainClass) {
    return UMLPMill.cDInterfaceBuilder()
        .setModifier(PUBLIC.build())
        .setName(domainClass.getName() + "DAO"+INTERFACE_SUFFIX).build();
  }

  protected void domainClassDAOMethods(ASTCDType daoInterface, ASTCDClass domainClass,
                                     Map<ASTCDAssocSide, ASTCDAssociation> assocSideToAssoc) {
    Map<ASTCDAssocSide, ASTCDAssociation> nonDerivedAssocSideToAssoc =
        assocSideToAssoc.entrySet().stream().filter(e -> !e.getValue().getModifier().isDerived())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (l,r) -> l, LinkedHashMap::new));

    Map<ASTCDAssocSide, ASTCDAssociation> gemAssocSideToAssoc =
        nonDerivedAssocSideToAssoc.entrySet().stream().filter(e -> !GenService.isMultiValued(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (l,r) -> l, LinkedHashMap::new));

    Map<ASTCDAssocSide, ASTCDAssociation> listAssocSideToAssoc =
        nonDerivedAssocSideToAssoc.entrySet().stream().filter(e -> GenService.isMultiValued(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (l,r) -> l, LinkedHashMap::new));

    selectMethod(daoInterface, domainClass, listAssocSideToAssoc, gemAssocSideToAssoc);
    selectAllIdsMethod(daoInterface, domainClass,listAssocSideToAssoc, gemAssocSideToAssoc);
    selectFilteredMethod(daoInterface, domainClass);
    unpackMethod(daoInterface, domainClass,listAssocSideToAssoc, gemAssocSideToAssoc);
    insertMethods(daoInterface, domainClass,listAssocSideToAssoc, gemAssocSideToAssoc);
    deleteMethod(daoInterface, domainClass,nonDerivedAssocSideToAssoc);
    updateAttrMethod(daoInterface, domainClass);
    updateOptionalAttrMethod(daoInterface, domainClass);
    updateAndSelectListAttrMethods(daoInterface, domainClass);
  }

  protected void deleteMethod(ASTCDType daoInterface, ASTCDClass domainClass,
                              Map<ASTCDAssocSide, ASTCDAssociation> nonDerivedAssocs) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.Delete", domainClass.getName());
  }

  protected void insertMethods(ASTCDType daoInterface, ASTCDClass domainClass,
                               Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                               Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.Insert", domainClass.getName());
  }

  protected void unpackMethod(ASTCDType daoInterface, ASTCDClass domainClass,
                              Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                              Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap){
    // This method is only needed in its subclasses

    //getCD4C().addMethod(daoInterface, "umlp.daointerface.Unpack", domainClass.getName());
  }

  protected void selectMethod(ASTCDType daoInterface, ASTCDClass domainClass,
                              Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                              Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.Select", domainClass.getName());
  }

  protected void selectAllIdsMethod(ASTCDType daoInterface, ASTCDClass domainClass,
                                    Map<ASTCDAssocSide, ASTCDAssociation> listAssociationMap,
                                    Map<ASTCDAssocSide, ASTCDAssociation> gemAssociationMap){
    getCD4C().addMethod(daoInterface, "umlp.daointerface.SelectAllIds", domainClass.getName());
  }

  protected void selectFilteredMethod(ASTCDType daoInterface, ASTCDClass domainClass){
    getCD4C().addMethod(daoInterface, "umlp.daointerface.SelectFiltered", domainClass.getName());
  }

  protected void updateAttrMethod(ASTCDType daoInterface, ASTCDClass domainClass) {
    getNormalAttributes(domainClass).forEach(attr ->
        getCD4C().addMethod(daoInterface, "umlp.daointerface.UpdateAttr",
            attr.getName(), attr.getMCType().printType()));
  }

  protected void updateOptionalAttrMethod(ASTCDType daoInterface, ASTCDClass domainClass) {
    getOptionalAttributes(domainClass).forEach(attr ->
            getCD4C().addMethod(daoInterface, "umlp.daointerface.UpdateOptionalAttr",
                attr.getName(), GenService.createType(attr).printType()));
  }

  protected void updateAndSelectListAttrMethods(ASTCDType daoInterface, ASTCDClass domainClass) {
    getListAttributes(domainClass).forEach(attr -> {
              String innerType = GenService.getInnerType(GenService.createType(attr));
              getCD4C().addMethod(daoInterface, "umlp.daointerface.UpdateListAttr", attr.getName(), innerType);
              getCD4C().addMethod(daoInterface, "umlp.daointerface.SelectListAttr", attr, innerType);
            }
        );
  }

  // Interface of Assoc Daos for each domain class

  /**
   * Creates DaoClass from an Association
   *
   * @param assoc
   * @return
   */
  protected ASTCDType dAOForAssoc(ASTCDAssociation assoc) {
    ASTCDType associationDAO = createDAOTypeForAssoc(assoc);
    this.addStaticImports(associationDAO);
    this.addDatabaseSpecificImports(associationDAO);
    this.assocDAOMethods(associationDAO, assoc);
    return associationDAO;
  }

  protected ASTCDType createDAOTypeForAssoc(ASTCDAssociation assoc) {
    return UMLPMill.cDInterfaceBuilder()
        .setModifier(PUBLIC.build())
        .setName("Assoc" + StringTransformations.capitalize(assoc.getName()) + "DAO"+INTERFACE_SUFFIX).build();
  }

  protected void assocDAOMethods(ASTCDType daoInterface, ASTCDAssociation association) {
    addMethodsForAssociationDAO(daoInterface, association);

    removeMethodForAssociationDAO(daoInterface, association);
    if (association.getCDAssocDir().isDefinitiveNavigableRight()) {
      selectMethodForAssocDAO(daoInterface, association, association.getLeft(), association.getRight());
      removeAllMethodForAssocDAO(daoInterface, association, association.getLeft(), association.getRight());
      updateMethodForAssocDAO(daoInterface, association, association.getLeft(), association.getRight());
    }
    if (association.getCDAssocDir().isDefinitiveNavigableLeft()) {
      selectMethodForAssocDAO(daoInterface, association, association.getRight(), association.getLeft());
      removeAllMethodForAssocDAO(daoInterface,association, association.getRight(), association.getLeft());
      updateMethodForAssocDAO(daoInterface, association, association.getRight(), association.getLeft());
    }
  }

  protected void removeAllMethodForAssocDAO(ASTCDType daoInterface, ASTCDAssociation assoc,
                                            ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.associations.RemoveAllForAssocDAO",
        start.getName());
  }

  protected void selectMethodForAssocDAO(ASTCDType daoInterface, ASTCDAssociation association,
                                         ASTCDAssocSide assocStart, ASTCDAssocSide assocEnd) {
    if(GenService.isMultiValued(assocEnd)) {
      selectMethodForListAssocDAO(daoInterface, assocStart, assocEnd);
    } else {
      selectMethodForGemAssocDAO(daoInterface, assocStart, assocEnd);
    }
  }

  protected void selectMethodForListAssocDAO(ASTCDType daoInterface, ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.associations.SelectForAssocDAO",
        start.getName(), end.getName());
  }

  protected void selectMethodForGemAssocDAO(ASTCDType daoInterface, ASTCDAssocSide start, ASTCDAssocSide end) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.associations.SelectForGemAssocDAO",
        start.getName(), end.getName());
  }

  protected void addMethodsForAssociationDAO(ASTCDType daoInterface, ASTCDAssociation assoc) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.associations.AddForAssocDAO", assoc);
    getCD4C().addMethod(daoInterface, "umlp.daointerface.associations.AddManyForAssocDAO", assoc);
  }

  protected void updateMethodForAssocDAO(ASTCDType daoInterface, ASTCDAssociation assoc, ASTCDAssocSide start,
                                         ASTCDAssocSide end) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.associations.UpdateForAssocDAO",
        start.getName(), end.getName());
  }

  protected void removeMethodForAssociationDAO(ASTCDType daoInterface, ASTCDAssociation assoc) {
    getCD4C().addMethod(daoInterface, "umlp.daointerface.associations.RemoveForAssocDAO", assoc);
  }


  protected void addStaticImports(ASTCDType clazz) {
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("Gem"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("LoadResult"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTECommonPath("LoadingState"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTEOCLPath("OCL2SQL"));
    getCD4C().addImport(clazz, PathUtil.getBackendRTEOCLPath("OCLException"));
    // Java
    getCD4C().addImport(clazz, "java.util.List");
    getCD4C().addImport(clazz, "java.util.Optional");
    getCD4C().addImport(clazz, "java.util.Map;");
    getCD4C().addImport(clazz, "java.util.HashMap;");
  }

  protected void addDatabaseSpecificImports(ASTCDType clazz) {

  }

}
