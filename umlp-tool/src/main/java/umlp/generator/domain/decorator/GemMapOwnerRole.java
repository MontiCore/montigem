/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDType;
import umlp.generator.util.TagLookUp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A map role of a concrete owner class: the GemMap of a qualified association that the owner's objects
 * hold. Deleting an object removes it from the maps that may hold it as a value (see {@link #holding}) or as a key
 * (see {@link #keyedBy}).
 */
public class GemMapOwnerRole {

  protected final String owner;

  protected final String role;

  protected final boolean singleton;

  public GemMapOwnerRole(String owner, String role, boolean singleton) {
    this.owner = owner;
    this.role = role;
    this.singleton = singleton;
  }

  /**
   * @return the name of the concrete owner class
   */
  public String getOwner() {
    return owner;
  }

  /**
   * @return the name of the map role
   */
  public String getRole() {
    return role;
  }

  /**
   * @return whether the owner class is a singleton (its manager keeps one instance instead of a cache)
   */
  public boolean isSingleton() {
    return singleton;
  }

  /**
   * the map roles of the concrete classes of cd (declared or inherited) whose values may be objects of type valueType,
   * i.e. whose value type is valueType or one of its supertypes
   */
  public static List<GemMapOwnerRole> holding(ASTCDCompilationUnit cd, ASTCDType valueType) {
    Set<String> valueTypes = typeAndSuperTypes(valueType);
    List<GemMapOwnerRole> result = new ArrayList<>();
    for (ASTCDClass owner : cd.getCDDefinition().getCDClassesList()) {
      if (owner.getModifier().isAbstract() || owner.getModifier().isDerived()) {
        continue;
      }
      for (ASTCDAssocSide side : CDSymbolTables.getAssociationsInHierarchy(owner)) {
        if (GenService.isMap(side) && !GenService.isSymbolDerived(side)
            && valueTypes.contains(side.getSymbol().getType().getTypeInfo().getFullName())) {
          result.add(new GemMapOwnerRole(owner.getName(), side.getName(), TagLookUp.isSingleton(owner)));
        }
      }
    }
    return result;
  }

  /**
   * the map roles of the concrete classes of cd (declared or inherited) whose keys may be objects of type keyType,
   * i.e. GemEntryMaps of type qualified associations whose key type is keyType or one of its supertypes: deleting a key
   * object removes its entry, like deleting a value. Their keys are stored, so the database deletes the entries of the
   * owners that are not loaded as well (see {@link #typeQualifiedKeyedBy}). Attribute qualified maps derive their keys
   * from the values, their entries follow the references of the values.
   */
  public static List<GemMapOwnerRole> keyedBy(ASTCDCompilationUnit cd, ASTCDType keyType) {
    Set<String> keyTypes = typeAndSuperTypes(keyType);
    List<GemMapOwnerRole> result = new ArrayList<>();
    for (ASTCDClass owner : cd.getCDDefinition().getCDClassesList()) {
      if (owner.getModifier().isAbstract() || owner.getModifier().isDerived()) {
        continue;
      }
      for (ASTCDAssocSide side : CDSymbolTables.getAssociationsInHierarchy(owner)) {
        if (isKeyedBy(side, keyTypes)) {
          result.add(new GemMapOwnerRole(owner.getName(), side.getName(), TagLookUp.isSingleton(owner)));
        }
      }
    }
    return result;
  }

  /**
   * the type qualified associations of cd whose keys (stored in the database) may be objects of type keyType: the
   * database deletes their rows with a deleted key object, also of the owners that are not loaded
   */
  public static List<ASTCDAssociation> typeQualifiedKeyedBy(ASTCDCompilationUnit cd, ASTCDType keyType) {
    Set<String> keyTypes = typeAndSuperTypes(keyType);
    List<ASTCDAssociation> result = new ArrayList<>();
    for (ASTCDAssociation assoc : cd.getCDDefinition().getCDAssociationsList()) {
      if (!assoc.getModifier().isDerived()
          && GenService.getTypeQualifiedMapSide(assoc).filter(side -> isKeyedBy(side, keyTypes)).isPresent()) {
        result.add(assoc);
      }
    }
    return result;
  }

  protected static boolean isKeyedBy(ASTCDAssocSide side, Set<String> keyTypes) {
    if (!GenService.isMap(side) || GenService.isKeyDerived(side) || GenService.isSymbolDerived(side)) {
      return false;
    }
    QualifierKey key = QualifierKey.of(side);
    return key.isEntryKey() && key.getKeySymbol() != null && keyTypes.contains(key.getKeySymbol().getFullName());
  }

  protected static Set<String> typeAndSuperTypes(ASTCDType type) {
    Set<String> types = new HashSet<>();
    types.add(type.getSymbol().getFullName());
    CDSymbolTables.getTransitiveSuperTypes(type).forEach(t -> types.add(t.getSymbol().getFullName()));
    return types;
  }
}
