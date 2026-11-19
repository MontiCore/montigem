/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDType;
import umlp.generator.util.TagLookUp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A map role of a concrete owner class: the GemMap of an attribute qualified association that the owner's objects
 * hold. Deleting an object removes it from the maps that may hold it, see {@link #holding}.
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
    Set<String> valueTypes = new HashSet<>();
    valueTypes.add(valueType.getSymbol().getFullName());
    CDSymbolTables.getTransitiveSuperTypes(valueType).forEach(t -> valueTypes.add(t.getSymbol().getFullName()));

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
}
