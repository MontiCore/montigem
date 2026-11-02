package aas.decorator;

import aas.AAS2UMLPModes;
import de.monticore.ast.Comment;
import de.monticore.cd.facade.CDModifier;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._ast.ASTCDCardinality;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._ast.ASTCDElement;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import de.se_rwth.commons.logging.Log;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.Qualifier;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEntity;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultRange;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementList;
import aas.decorator.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static aas.AAS2UMLPGenerator.SEMANTICIDTOCLASS;
import static de.monticore.cd4code.CD4CodeMill.cDAssocLeftSideBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDAssocRightSideBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDAssocTypeAssocBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDAssociationBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDAttributeBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDCardAtLeastOneBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDCardMultBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDCardOneBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDCardOptBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDClassBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDExtendUsageBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDLeftToRightDirBuilder;
import static de.monticore.cd4code.CD4CodeMill.cDRoleBuilder;
import static de.monticore.cd4code.CD4CodeMill.mCBasicTypeArgumentBuilder;
import static de.monticore.cd4code.CD4CodeMill.mCImportStatementBuilder;
import static de.monticore.cd4code.CD4CodeMill.mCOptionalTypeBuilder;
import static de.monticore.cd4code.CD4CodeMill.mCQualifiedNameBuilder;
import static de.monticore.cd4code.CD4CodeMill.mCQualifiedTypeBuilder;
import static aas.AAS2UMLPGenerator.NAMETRANSFORMATIONS;

public class AAS2UMLPDecorator {

  @FunctionalInterface
  private interface TriPredicate<A, B, C> {
    boolean test(A a, B b, C c);
  }

  public record IdentifiedAASElement(SubmodelElement sme, Referable parent, String fullId, String parentFullId) {
    @Override
    public String toString() {
      return fullId;
    }
  }

  private GlobalExtensionManagement glex;
  private final AAS2UMLPModes mode;

  private Map<String, String> seenIdShorts = new HashMap<>();
  private Map<String, String> seenSemanticIds = new HashMap<>();
  private List<String> generatedAssociations = new ArrayList<>();
  private List<String> metamodelOwnedNames = new ArrayList<>();
  private List<ASTCDElement> astcdElements = new ArrayList<>();

  private String METAMODELPATH = "src/main/resources/aas/decorator/aasMetamodel.cd";

  private ASTCDDefinition cd = new ASTCDDefinition();

  private final Predicate<SubmodelElement> subclassCreationCheck;
  private final TriPredicate<IdentifiedAASElement, List<String>, List<String>> associationCreationCheck;
  private final BiConsumer<IdentifiedAASElement, String>  updateMaps;


  public AAS2UMLPDecorator(GlobalExtensionManagement glex, AAS2UMLPModes mode) {
    this.glex = glex;
    this.mode = mode;
    switch (mode) {
      case SEMANTICID_ONLY -> {
        subclassCreationCheck = element -> isSemanticIdPresent(element)
            && !seenSemanticIds.containsKey(getSemIdFromSE(element));
        updateMaps = (element, name) -> {
          if (name.equals("not generated")) {
            getNameTransformationsHashMap().put(element.fullId(), name);
          } else {
            if (!getSemanticIdToClassHashMap().containsKey(getSemIdFromSE(element.sme()))) {
              getSemanticIdToClassHashMap().put(getSemIdFromSE(element.sme()), name);
              getNameTransformationsHashMap().put(element.fullId(), name);
            } else {
              getNameTransformationsHashMap().put(element.fullId(),
                  getSemanticIdToClassHashMap().get(getSemIdFromSE(element.sme())));
            }
          }
        };
        associationCreationCheck = (element,
                                    seenSemIdsInNamespace,
                                    seenIdShortsInNamespace) ->
            (isSemanticIdPresent(element.sme()) && !seenSemanticIds.containsKey(getSemIdFromSE(element.sme())))
            || (isSemanticIdPresent(element.sme()) && seenSemanticIds.containsKey(getSemIdFromSE(element.sme()))
                && !seenSemIdsInNamespace.contains(getSemIdFromSE(element.sme())));
      }
      case HYBRID -> {
        subclassCreationCheck = element -> {
          if (isSemanticIdPresent(element)) {
            return !seenSemanticIds.containsKey(getSemIdFromSE(element));
          } else {
            return !seenIdShorts.containsKey(element.getIdShort());
          }
        };
        updateMaps = (element, name) -> {
          if (isSemanticIdPresent(element.sme())) {
            if (!getSemanticIdToClassHashMap().containsKey(getSemIdFromSE(element.sme()))) {
              getSemanticIdToClassHashMap().put(getSemIdFromSE(element.sme()), name);
              getNameTransformationsHashMap().put(element.fullId(), name);
            } else {
              getNameTransformationsHashMap().put(element.fullId(),
                  getSemanticIdToClassHashMap().get(getSemIdFromSE(element.sme())));
            }
          } else {
            if (seenIdShorts.containsKey(element.sme().getIdShort())) {
              getNameTransformationsHashMap().put(element.fullId(), getNameTransformationsHashMap()
                  .get(seenIdShorts.get(element.sme().getIdShort())));
            } else {
              getNameTransformationsHashMap().put(element.fullId(), name);
            }
          }
        };
        associationCreationCheck = (element,
                                    seenSemIdsInNamespace,
                                    seenIdShortsInNamespace) ->
            (isSemanticIdPresent(element.sme()) && !seenSemanticIds.containsKey(getSemIdFromSE(element.sme())))
            || (isSemanticIdPresent(element.sme()) && seenSemanticIds.containsKey(getSemIdFromSE(element.sme()))
                && !seenSemIdsInNamespace.contains(getSemIdFromSE(element.sme())))
            || (!isSemanticIdPresent(element.sme()) && !seenIdShorts.containsKey(element.sme().getIdShort()))
            || (!isSemanticIdPresent(element.sme()) && seenIdShorts.containsKey(element.sme().getIdShort())
                &&  !seenIdShortsInNamespace.contains(element.sme().getIdShort()));
      }
      default -> {
        subclassCreationCheck = element -> !seenIdShorts.containsKey(element.getIdShort());
        updateMaps = (element, name) -> {

          if (seenIdShorts.containsKey(element.sme().getIdShort())) {
            getNameTransformationsHashMap().put(element.fullId(), getNameTransformationsHashMap()
                .get(seenIdShorts.get(element.sme().getIdShort())));
          } else {
            getNameTransformationsHashMap().put(element.fullId(), name);
          }

        };
        associationCreationCheck = (element,
                                    seenSemIdsInNamespace,
                                    seenIdShortsInNamespace) ->
            true;
      }
    }
  }

  private String getSemIdFromReferable(Referable referable) {
    if (referable instanceof SubmodelElement) {
      return getSemIdFromSE((SubmodelElement) referable);
    } else if (referable instanceof Submodel element) {
      if (element.getSemanticId() == null ) {
        Log.error("Submodel " + element.getIdShort() + " has no reference to a semantic id.");
        return "";
      }

      if (element.getSemanticId().getKeys().isEmpty()) {
        Log.error("Submodel " + element.getIdShort() + " is missing keys in the reference" +
            " to the semantic id.");
        return "";
      }

      StringBuilder id = new StringBuilder();
      for (Key key : element.getSemanticId().getKeys()) {
        id.append(key.getValue());
      }

      return id.toString();
    } else {
      Log.error("Element is neither a submodel nor a submodel element");
      return "";
    }
  }

  /**
   * Decorates the given class diagram AST with imports, AAS metamodel elements, submodel classes,
   * associations, and generated subclasses for the supplied AAS submodels.
   *
   * @param aasName the name of the AAS being transformed
   * @param submodels the submodels to transform into CD elements
   * @param conceptDescriptions concept descriptions used to resolve additional data specification details
   * @param decoratedCD the class diagram compilation unit to decorate
   */
  public void decorate(String aasName, List<Submodel> submodels, List<ConceptDescription> conceptDescriptions, ASTCDCompilationUnit decoratedCD) {
    if (decoratedCD.getCDDefinition() == null) {
      Log.error("Decorated CD definition is null");
    }
    cd = decoratedCD.getCDDefinition();

    decoratedCD.addAllMCImportStatements(createImports());

    cd.addAllCDElements(createMetaModelElements());

    List<List<IdentifiedAASElement>> namespaces = new ArrayList<>();

    submodels.forEach(submodel -> {
      cd.addCDElement(createSubmodelClass(submodel));
      getAllAASNamespaces(namespaces, submodel);
      });

    for (List<IdentifiedAASElement> namespace : namespaces) {
      createCDElementsFromSubmodelElements(namespace);
    }

    cd.addAllCDElements(astcdElements);
  }

  public void getAllAASNamespaces(List<List<IdentifiedAASElement>> namespaces, Submodel submodel) {

    if (submodel.getId() != null && !submodel.getId().isEmpty() ) {
      List<IdentifiedAASElement> elements = new ArrayList<>();
      String fullIdSm = submodel.getId();

      for (SubmodelElement child : submodel.getSubmodelElements()) {
        String fullIdChild = fullIdSm + "/" + child.getIdShort();
        elements.add(new IdentifiedAASElement(child, submodel, fullIdChild, fullIdSm ));
      }
      namespaces.add(elements);

      for (SubmodelElement child : submodel.getSubmodelElements()) {
        getAllAASNamespaces(namespaces, child, submodel, fullIdSm);
      }
    } else {
      Log.error("Submodel " + submodel.getIdShort() + " has no id or id is empty!");
    }

  }

  private void getAllAASNamespaces(List<List<IdentifiedAASElement>> namespaces, SubmodelElement submodelElement, Referable parentElement, String fullIdParent) {
    if (submodelElement.getIdShort() == null) {
      submodelElement.setIdShort(parentElement.getIdShort() + "_Element");
    }
    switch (submodelElement.getClass().getSimpleName()) {
      case "DefaultSubmodelElementCollection" -> {
        List<IdentifiedAASElement> elements = new ArrayList<>();
        String fullIdSme = fullIdParent + "/" + submodelElement.getIdShort();

        for (SubmodelElement child : ((DefaultSubmodelElementCollection) submodelElement).getValue()) {
          String fullIdChild = fullIdSme + "/" + child.getIdShort();
          elements.add(new IdentifiedAASElement(child, submodelElement, fullIdChild, fullIdSme ));
        }
        namespaces.add(elements);

        for (SubmodelElement child : ((DefaultSubmodelElementCollection) submodelElement).getValue()) {
          getAllAASNamespaces(namespaces, child, submodelElement, fullIdSme);
        }
      }
      case "DefaultSubmodelElementList" -> {
        List<IdentifiedAASElement> elements = new ArrayList<>();
        String fullIdSme = fullIdParent + "/" + submodelElement.getIdShort();

        for (SubmodelElement child : ((DefaultSubmodelElementList) submodelElement).getValue()) {
          if (child.getIdShort() == null) {
            child.setIdShort(submodelElement.getIdShort() + "_Element");
          }
          String fullIdChild = fullIdSme + "/" + child.getIdShort();
          elements.add(new IdentifiedAASElement(child, submodelElement, fullIdChild, fullIdSme ));
        }
        namespaces.add(elements);

        for (SubmodelElement child : ((DefaultSubmodelElementList) submodelElement).getValue()) {
          getAllAASNamespaces(namespaces, child, submodelElement, fullIdSme);
        }
      }
      default -> {}
    }
  }

  private void createCDElementsFromSubmodelElements(List<IdentifiedAASElement> values) {

    List<String> seenIdShortsInNamespace = new ArrayList<>();
    List<String> seenSemIdsInNamespace = new ArrayList<>();

    for (IdentifiedAASElement element : values) {
      SubmodelElement submodelElement = element.sme;

      String cdName = determineCDName(submodelElement.getIdShort());

      boolean classCreation = false;
      boolean associationCreation = false;

      if (isComplexValueClass(element)) {
        if (!getNameTransformationsHashMap().get(element.parentFullId()).equals("not generated")) {
          if (subclassCreationCheck.test(submodelElement)) {
            classCreation = true;

            astcdElements.add(createSubclass(submodelElement, cdName));
          }
        }
      }

      if (associationCreationCheck.test(element, seenSemIdsInNamespace, seenIdShortsInNamespace)) {
        if (!getNameTransformationsHashMap().get(element.parentFullId()).equals("not generated")){
          associationCreation = true;
          String leftType = getNameTransformationsHashMap().get(element.parentFullId());
          String rightType = getRightType(element, cdName);
          String rightCardinality = getCardinality(submodelElement);
          String rightRoleName = getRightRole(element, cdName);
          ASTCDAssociation association = createAssociation(leftType, rightType, rightCardinality, rightRoleName);
          if (!generatedAssociations.contains(association.getPrintableName())) {
            astcdElements.add(association);
            generatedAssociations.add(association.getPrintableName());
            seenIdShortsInNamespace.add(submodelElement.getIdShort());
            if (isSemanticIdPresent(submodelElement))
              seenSemIdsInNamespace.add(getSemIdFromSE(submodelElement));
          }
        }
      }

      if (classCreation || associationCreation) {
        updateMaps.accept(element, cdName);
        updateSeenMaps(element);
      } else {
        if (isSemanticIdPresent(submodelElement)) {
          updateMaps.accept(element, getSemanticIdToClassHashMap().getOrDefault(getSemIdFromSE(submodelElement), "not generated"));
        } else {
          updateMaps.accept(element, "not generated");
        }

      }
    }
  }

  private String getRightRole(IdentifiedAASElement element, String cdName) {
    if (isComplexValueClass(element)) {
      return StringUtils.uncapitalize(getRightType(element, cdName));
    } else {
      switch (mode) {
        case SEMANTICID_ONLY -> {
          if (seenSemanticIds.containsKey(getSemIdFromSE(element.sme()))) {
            return getNameTransformationsHashMap().get(seenSemanticIds.get(getSemIdFromSE(element.sme())));
          }
          return cdName;
        }
        case HYBRID -> {
          if (isSemanticIdPresent(element.sme())) {
            if (seenSemanticIds.containsKey(getSemIdFromSE(element.sme()))) {
              return getNameTransformationsHashMap().get(seenSemanticIds.get(getSemIdFromSE(element.sme())));
            }
            return cdName;
          }
          else {
            if (seenIdShorts.containsKey(element.sme().getIdShort())) {
              return getNameTransformationsHashMap().get(seenIdShorts.get(element.sme().getIdShort()));
            }
            return cdName;
          }
        }
        default -> {
          if (seenIdShorts.containsKey(element.sme().getIdShort())) {
            return getNameTransformationsHashMap().get(seenIdShorts.get(element.sme().getIdShort()));
          }
        }
      }
      return cdName;
    }
  }

  private void updateSeenMaps(IdentifiedAASElement element) {
    switch (mode) {
      case SEMANTICID_ONLY -> {
        if (!seenSemanticIds.containsKey(getSemIdFromSE(element.sme()))) {
          seenSemanticIds.put(getSemIdFromSE(element.sme()), element.fullId());
        }
      }
      case HYBRID -> {
        if (isSemanticIdPresent(element.sme())) {
          if (!seenSemanticIds.containsKey(getSemIdFromSE(element.sme()))) {
            seenSemanticIds.put(getSemIdFromSE(element.sme()), element.fullId());
          }
        } else {
          if (!seenIdShorts.containsKey(element.sme().getIdShort())) {
            seenIdShorts.put(element.sme().getIdShort(), element.fullId());
          }
        }
      }
      default -> {
        if (!seenIdShorts.containsKey(element.sme().getIdShort())) {
          seenIdShorts.put(element.sme().getIdShort(), element.fullId());
        }
      }
    }
  }


  private String getRightType(IdentifiedAASElement element, String cdName) {
    switch (mode) {
      case SEMANTICID_ONLY -> {

        if (isComplexValueClass(element)) {
          if (seenSemanticIds.containsKey(getSemIdFromSE(element.sme()))) {
            return StringUtils.capitalize(getNameTransformationsHashMap()
                .get(seenSemanticIds.get(getSemIdFromSE(element.sme()))));
          } else {
            return StringUtils.capitalize(cdName);
          }
        } else {
          if (element.sme() instanceof DefaultEntity) {
            return "AASEntity";
          } else {
            return element.sme().getClass().getSimpleName().replace("Default", "");
          }
        }
      }
      case HYBRID -> {
        if (isComplexValueClass(element)) {
          if (isSemanticIdPresent(element.sme())) {
            if (seenSemanticIds.containsKey(getSemIdFromSE(element.sme()))) {
              return StringUtils.capitalize(getNameTransformationsHashMap()
                  .get(seenSemanticIds.get(getSemIdFromSE(element.sme()))));
            } else {
              return StringUtils.capitalize(cdName);
            }
          } else {
            if (seenIdShorts.containsKey(element.sme().getIdShort())) {
              return StringUtils.capitalize(getNameTransformationsHashMap()
                  .get(seenIdShorts.get(element.sme().getIdShort())));
            } else {
              return StringUtils.capitalize(cdName);
            }
          }
        } else {
          if (element.sme() instanceof DefaultEntity) {
            return "AASEntity";
          } else {
            return element.sme().getClass().getSimpleName().replace("Default", "");
          }
        }
      }
      default -> {
        if (isComplexValueClass(element)) {
          if (seenIdShorts.containsKey(element.sme().getIdShort())) {
            return StringUtils.capitalize(getNameTransformationsHashMap()
                .get(seenIdShorts.get(element.sme().getIdShort())));
          } else {
            return StringUtils.capitalize(cdName);
          }
        } else {
          if (element.sme() instanceof DefaultEntity) {
            return "AASEntity";
          } else {
            return element.sme().getClass().getSimpleName().replace("Default", "");
          }
        }
      }
    }
  }

  private static boolean isComplexValueClass(IdentifiedAASElement element) {
    return element.sme() instanceof DefaultProperty
        || element.sme() instanceof DefaultRange
        || element.sme() instanceof DefaultSubmodelElementCollection
        || element.sme() instanceof DefaultSubmodelElementList;
  }

  /**
   * Parses the AAS metamodel CD and returns its contained CD elements.
   * Also records metamodel-owned class names so generated names do not collide with them.
   *
   * @return the CD elements declared by the AAS metamodel
   */
  private List<ASTCDElement> createMetaModelElements() {
    ASTCDCompilationUnit cd = new ASTCDCompilationUnit();
    try {
      Optional<ASTCDCompilationUnit> cdOpt = CD4CodeMill.parser().parse(METAMODELPATH);
      if (cdOpt.isPresent()) {
        cd = cdOpt.get();
      }
      else {
        throw new IOException();
      }
    } catch (IOException e) {
      Log.error("Error parsing the AAS metamodel located at " + METAMODELPATH);
    }
    if (cd.getCDDefinition() == null) {
      Log.error("The metamodel has no cd definition");
    }
    metamodelOwnedNames.addAll(cd.getCDDefinition().getCDElementList().stream()
        .filter(elem -> elem instanceof ASTCDClass)
        .map(elem ->
            StringUtils.uncapitalize(((ASTCDClass) elem).getName())
        )
        .toList());
    return cd.getCDDefinition().getCDElementList();
  }

  /**
   * Creates a CD class representing the given AAS submodel.
   * The generated class extends the metamodel class {@code Submodel}.
   *
   * @param submodel the AAS submodel to transform
   * @return the generated CD class for the submodel
   */
  public ASTCDClass createSubmodelClass(Submodel submodel) {
    String name = determineCDName(submodel.getIdShort());
    getNameTransformationsHashMap().put(submodel.getId(), name);
    return cDClassBuilder()
        .add_PreComment(new Comment("\n/* ~~~~~~~~~~~~~~~~~~~~~~~~ Submodel " + name + " ~~~~~~~~~~~~~~~~~~~~~~~~ */"))
        .setCDExtendUsage(cDExtendUsageBuilder()
            .addSuperclass(mCQualifiedTypeBuilder()
                .setMCQualifiedName(mCQualifiedNameBuilder()
                    .setPartsList(List.of("Submodel"))
                    .build())
                .build())
            .build())
        .setName(StringUtils.capitalize(name))
        .setCDInterfaceUsageAbsent()
        .setModifier(CDModifier.PACKAGE_PRIVATE.build())
        .build();
  }

  private ASTCDElement createSubclass(SubmodelElement submodelElement, String cdName ) {
    return switch (submodelElement) {
      case DefaultSubmodelElementCollection defaultSubmodelElementCollection -> cDClassBuilder()
          .setCDExtendUsage(cDExtendUsageBuilder()
              .addSuperclass(mCQualifiedTypeBuilder()
                  .setMCQualifiedName(mCQualifiedNameBuilder()
                      .setPartsList(List.of("SubmodelElementCollection"))
                      .build())
                  .build())
              .build())
          .setName(StringUtils.capitalize(cdName))
          .setCDInterfaceUsageAbsent()
          .setModifier(CDModifier.PACKAGE_PRIVATE.build())
          .build();
      case DefaultSubmodelElementList defaultSubmodelElementList -> cDClassBuilder()
          .setCDExtendUsage(cDExtendUsageBuilder()
              .addSuperclass(mCQualifiedTypeBuilder()
                  .setMCQualifiedName(mCQualifiedNameBuilder()
                      .setPartsList(List.of("SubmodelElementList"))
                      .build())
                  .build())
              .build())
          .setName(StringUtils.capitalize(cdName))
          .setCDInterfaceUsageAbsent()
          .setModifier(CDModifier.PACKAGE_PRIVATE.build())
          .build();
      case DefaultProperty defaultProperty -> cDClassBuilder()
          .setCDExtendUsage(cDExtendUsageBuilder()
              .addSuperclass(mCQualifiedTypeBuilder()
                  .setMCQualifiedName(mCQualifiedNameBuilder()
                      .setPartsList(List.of("Property"))
                      .build())
                  .build())
              .build())
          .setName(StringUtils.capitalize(cdName))
          .setCDInterfaceUsageAbsent()
          .setModifier(CDModifier.PACKAGE_PRIVATE.build())
          .addCDMember(cDAttributeBuilder()
              .setName("value")
              .setMCType(mCOptionalTypeBuilder()
                  .setMCTypeArgument(mCBasicTypeArgumentBuilder()
                      .setMCQualifiedType(mCQualifiedTypeBuilder()
                          .setMCQualifiedName(
                              mCQualifiedNameBuilder()
                                  .setPartsList(List.of(determineValueType(submodelElement)))
                                  .build())
                          .build())
                      .build())
                  .build())
              .setModifier(CDModifier.PACKAGE_PRIVATE.build())
              .build())
          .build();
      case null, default -> cDClassBuilder()
          .setCDExtendUsage(cDExtendUsageBuilder()
              .addSuperclass(mCQualifiedTypeBuilder()
                  .setMCQualifiedName(mCQualifiedNameBuilder()
                      .setPartsList(List.of("Range"))
                      .build())
                  .build())
              .build())
          .setName(StringUtils.capitalize(cdName))
          .setCDInterfaceUsageAbsent()
          .setModifier(CDModifier.PACKAGE_PRIVATE.build())
          .addCDMember(cDAttributeBuilder()
              .setName("min")
              .setMCType(mCOptionalTypeBuilder()
                  .setMCTypeArgument(mCBasicTypeArgumentBuilder()
                      .setMCQualifiedType(mCQualifiedTypeBuilder()
                          .setMCQualifiedName(
                              mCQualifiedNameBuilder()
                                  .setPartsList(List.of(determineValueType(submodelElement)))
                                  .build())
                          .build())
                      .build())
                  .build())
              .setModifier(CDModifier.PACKAGE_PRIVATE.build())
              .build())
          .addCDMember(cDAttributeBuilder()
              .setName("max")
              .setMCType(mCOptionalTypeBuilder()
                  .setMCTypeArgument(mCBasicTypeArgumentBuilder()
                      .setMCQualifiedType(mCQualifiedTypeBuilder()
                          .setMCQualifiedName(
                              mCQualifiedNameBuilder()
                                  .setPartsList(List.of(determineValueType(submodelElement)))
                                  .build())
                          .build())
                      .build())
                  .build())
              .setModifier(CDModifier.PACKAGE_PRIVATE.build())
              .build())
          .build();
    };
  }


  /**
   * Determines a valid and unique CD name for an AAS idShort.
   * Hyphens are replaced with underscores, the first character is lowercased, and collisions with
   * previously generated names or metamodel-owned names are resolved by prefixing {@code aas_}.
   *
   * @param idShort the original AAS idShort
   * @return the normalized CD name
   */
  public String determineCDName(String idShort) {

    if (idShort.isEmpty()) {
      Log.error("IdShort is empty.");
      return "";
    }

    String normalizedName = idShort.replace("-", "_");

    normalizedName = _determineCDName(StringUtils.uncapitalize(normalizedName), getNameTransformationsHashMap());
    return normalizedName;
  }

  /**
   * Recursively resolves name collisions by prefixing the candidate name with {@code aas_}.
   *
   * @param s the candidate normalized name
   * @param map the current name transformation map
   * @return a collision-free normalized name
   */
  private String _determineCDName(String s, HashMap<String, String> map) {
    if (map.containsValue(s) || metamodelOwnedNames.contains(s)) {
      return _determineCDName("aas_" + s, map);
    }
    return s;
  }

  /**
   * Reads the AAS cardinality qualifier from a submodel element.
   * If no cardinality qualifier exists, {@code ZeroToMany} is used as default.
   *
   * @param it the submodel element whose cardinality should be read
   * @return the AAS cardinality value
   */
  private static String getCardinality(SubmodelElement it) {
    List<Qualifier> cardQualifiers = it.getQualifiers().stream()
        .filter(qualifier -> qualifier.getType().equals("Multiplicity")
            || qualifier.getType().equals("SMT/Cardinality")
            || qualifier.getType().equals("Cardinality"))
        .toList();
    return cardQualifiers.isEmpty() ? "ZeroToMany" : cardQualifiers.getFirst().getValue();
  }

  private static ASTCDCardinality createCardinality(String card) {
    switch (card) {
      case "One" -> {
        return cDCardOneBuilder().setDigits("1").build();
      }
      case "ZeroToOne" -> {
        return cDCardOptBuilder().setDigitssList(List.of("0", "1")).build();
      }
      case "OneToMany" -> {
        return cDCardAtLeastOneBuilder().setDigits("1").build();
      }
      default -> {
        return cDCardMultBuilder().build();
      }
    }
  }

  /**
   * Returns the global name transformation map, creating it if it does not yet exist.
   *
   * @return the map from original AAS names to generated CD names
   */
  private HashMap<String, String> getNameTransformationsHashMap() {
    if (!glex.hasGlobalVar(NAMETRANSFORMATIONS)) {
      glex.defineGlobalVar(NAMETRANSFORMATIONS, new LinkedHashMap<String, String>());
    }

    return ((HashMap<String, String>) glex.getGlobalVar(NAMETRANSFORMATIONS));
  }

  /**
   * Returns the semanticId to class transformation map, creating it if it does not yet exist.
   *
   * @return the map from AAS semantic Ids to the corresponding CD class
   */
  private HashMap<String, String> getSemanticIdToClassHashMap() {
    if (!glex.hasGlobalVar(SEMANTICIDTOCLASS)) {
      glex.defineGlobalVar(SEMANTICIDTOCLASS, new LinkedHashMap<String, String>());
    }

    return ((HashMap<String, String>) glex.getGlobalVar(SEMANTICIDTOCLASS));
  }

  /**
   * Maps AAS value type (from SubmodelElement) to CD type string.
   * Matches the ordering and behavior of the Freemarker macro determineValueType in AASX2UMLP.ftl.
   *
   * @param element the SubmodelElement containing valueType info
   * @return the CD type as a String (e.g., "String", "LocalDate", "Integer")
   */
  public String determineValueType(SubmodelElement element) {
    if (!(element instanceof DefaultRange || element instanceof DefaultProperty)) {
      Log.error("Element is not a range or property, so it has no ValueType attribute.");
      return "";
    }
    String valueType = "";
    if (element instanceof DefaultRange) {
      valueType = ((DefaultRange) element).getValueType().name();
    } else {
      valueType = ((DefaultProperty) element).getValueType().name();
    }

    String typeName;
    switch (valueType) {
      case "ANY_URI":
        typeName = "String";
        break;
      case "BASE64BINARY":
        typeName = "Base64";
        break;
      case "BOOLEAN":
        typeName = "Boolean";
        break;
      case "BYTE":
        typeName = "Byte";
        break;
      case "DATE":
        typeName = "LocalDate";
        break;
      case "DATE_TIME":
      case "DATE_TIME_UTC":
        typeName = "LocalDateTime";
        break;
      case "DECIMAL":
      case "DOUBLE":
        typeName = "Double";
        break;
      case "DURATION":
        typeName = "Duration";
        break;
      case "FLOAT":
        typeName = "Float";
        break;
      case "GDAY", "GMONTH_DAY":
        typeName = "MonthDay";
        break;
      case "GMONTH":
        typeName = "Month";
        break;
      case "GYEAR":
        typeName = "Year";
        break;
      case "GYEAR_MONTH":
        typeName = "YearMonth";
        break;
      case "HEX_BINARY":
        typeName = "DatatypeConverter";
        break;
      case "INT":
      case "INTEGER":
      case "NEGATIVE_INTEGER":
      case "NON_NEGATIVE_INTEGER":
      case "NON_POSITIVE_INTEGER":
      case "POSITIVE_INTEGER", "UNSIGNED_LONG", "UNSIGNED_SHORT":
        typeName = "Integer";
        break;
      case "LONG", "UNSIGNED_INT":
        typeName = "Long";
        break;
      case "SHORT", "UNSIGNED_BYTE":
        typeName = "Short";
        break;
      case "TIME":
        typeName = "LocalTime";
        break;
      case "STRING":
      default:
        typeName = "String";
        break;
    }

    return typeName;
  }

  /**
   * Creates the import statements required by generated AAS-to-UMLP class diagrams.
   *
   * @return the list of generated import statements
   */
  private List<ASTMCImportStatement> createImports() {
    List<ASTMCImportStatement> imports = new ArrayList<>();

    imports.add(mCImportStatementBuilder()
        .setMCQualifiedName(
            mCQualifiedNameBuilder()
                .setPartsList(List.of("java","time","LocalDate"))
                .build()
        ).build());

    imports.add(mCImportStatementBuilder()
        .setMCQualifiedName(
            mCQualifiedNameBuilder()
                .setPartsList(List.of("java","time","LocalDateTime"))
                .build()
        ).build());

    imports.add(mCImportStatementBuilder()
        .setMCQualifiedName(
            mCQualifiedNameBuilder()
                .setPartsList(List.of("java","util","List"))
                .build()
        ).build());

    imports.add(mCImportStatementBuilder()
        .setMCQualifiedName(
            mCQualifiedNameBuilder()
                .setPartsList(List.of("java","util","Map"))
                .build()
        ).build());

    imports.add(mCImportStatementBuilder()
        .setMCQualifiedName(
            mCQualifiedNameBuilder()
                .setPartsList(List.of("java","util","Set"))
                .build()
        ).build());

    imports.add(mCImportStatementBuilder()
        .setMCQualifiedName(
            mCQualifiedNameBuilder()
                .setPartsList(List.of("java","util","Optional"))
                .build()
        ).build());

    imports.add(mCImportStatementBuilder()
        .setMCQualifiedName(
            mCQualifiedNameBuilder()
                .setPartsList(List.of("java","lang","String"))
                .build()
        ).build());

    return imports;
  }

  /**
   * Returns the value of a semanticId for a submodel element.
   * Logs an error if there is no semanticId.
   *
   * @param element the element for which the semanticId is desired.
   * @return the value of the semanticId as a String.
   */
  private static String getSemIdFromSE(SubmodelElement element) {
    if (element.getSemanticId() == null ) {
      Log.error("Submodel element " + element.getIdShort() + " has no reference to a semantic id.");
      return "";
    }

    if (element.getSemanticId().getKeys().isEmpty()) {
      Log.error("Submodel element " + element.getIdShort() + " is missing keys in the reference" +
          " to the semantic id.");
      return "";
    }

    StringBuilder id = new StringBuilder();
    for (Key key : element.getSemanticId().getKeys()) {
      id.append(key.getValue());
    }

    return id.toString();
  }

  private static boolean isSemanticIdPresent(SubmodelElement element) {
    return element.getSemanticId() != null && !element.getSemanticId().getKeys().isEmpty();
  }

  private ASTCDAssociation createAssociation(String leftType, String rightType, String rightCardinality, String rightRoleName) {

    ASTCDAssociation assoc = cDAssociationBuilder()
        .setNameAbsent()
        .setModifier(CDModifier.PACKAGE_PRIVATE.build())
        .setCDAssocType(cDAssocTypeAssocBuilder().build())
        .setCDAssocDir(cDLeftToRightDirBuilder().build())
        .setLeft(cDAssocLeftSideBuilder()
            .setModifier(CDModifier.PACKAGE_PRIVATE.build())
            .setCDCardinality(cDCardOneBuilder().setDigits("1").build())
            .setCDOrderedAbsent()
            .setCDRoleAbsent()
            .setCDQualifierAbsent()
            .setMCQualifiedType(mCQualifiedTypeBuilder()
                .setMCQualifiedName(
                    mCQualifiedNameBuilder()
                        .setPartsList(List.of(StringUtils.capitalize(leftType)))
                        .build())
                .build())
            .build())
        .setRight(cDAssocRightSideBuilder()
            .setModifier(CDModifier.PACKAGE_PRIVATE.build())
            .setCDCardinality(createCardinality(rightCardinality))
            .setCDOrderedAbsent()
            .setCDQualifierAbsent()
            .setMCQualifiedType(mCQualifiedTypeBuilder()
                .setMCQualifiedName(
                    mCQualifiedNameBuilder()
                        .setPartsList(List.of(StringUtils.capitalize(rightType)))
                        .build())
                .build())
            .build())
        .build();
    assoc.getRight().setCDRole(cDRoleBuilder().setName(rightRoleName).build());

    return assoc;
  }
}
