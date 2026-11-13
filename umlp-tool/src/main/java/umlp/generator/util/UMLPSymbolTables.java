/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.util;

import de.monticore.ast.ASTNode;
import de.monticore.cd._symboltable.CDSymbolTables;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.ocl.ocl.OCLMill;
import de.monticore.ocl.util.SymbolTableUtil;
import de.monticore.symbols.basicsymbols._ast.ASTType;
import de.monticore.symbols.basicsymbols._symboltable.DiagramSymbol;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.symboltable.IScope;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types3.TypeCheck3;
import de.se_rwth.commons.Joiners;
import umlp.UMLPMill;
import umlp._symboltable.CDRoleSymbol2VariableSymbolAdapterDeSer;
import umlp._symboltable.IUMLPArtifactScope;
import umlp._visitor.UMLPTraverser;
import umlp.generator.domain.decorator.GenService;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UMLPSymbolTables {

  public static Set<ASTCDType> collectConcreteSubclassesInclSelf(ASTCDCompilationUnit origCD, ASTCDType domainType){
    return collectConcreteSubclassesInclSelfWithDist(origCD, domainType).keySet();
  }

  public static Map<ASTCDType, Integer> collectConcreteSubclassesInclSelfWithDist(ASTCDCompilationUnit origCD, ASTCDType domainType) {
    CDTypeSymbol symbol = domainType.getSymbol();
    Map<ASTCDType, Integer> concreteSubclasses = new LinkedHashMap<>();
    if(symbol.isIsClass() && !symbol.isIsAbstract()) {
      concreteSubclasses.put(domainType, 0);
    }

    CDBasisVisitor2 visitor = new CDBasisVisitor2() {
      @Override
      public void visit(ASTCDClass node) {
        if(node.getModifier().isAbstract()){
          return;
        }

        Map<ASTCDType, Integer> transitiveSuperTypes = getTransitiveSuperTypesWithDist(node, 1);
        if(transitiveSuperTypes.containsKey(domainType)) {
          concreteSubclasses.put(node, transitiveSuperTypes.get(domainType));
        }
      }
    };
    UMLPTraverser traverser = UMLPMill.inheritanceTraverser();
    traverser.add4CDBasis(visitor);
    origCD.accept(traverser);

    return concreteSubclasses;
  }

  public static Map<ASTCDInterface, Integer> getTransitiveSuperInterfacesWithDistance(ASTCDType ast, int curDist) {
    Map<ASTCDInterface, Integer> interfaces = new LinkedHashMap<>();
    for (SymTypeExpression s : ast.getSymbol().getInterfaceList()) {
      ASTType node = s.getTypeInfo().getAstNode();
      if (node instanceof ASTCDInterface) {
        ASTCDInterface interf = (ASTCDInterface) node;
        int dist = Math.min(curDist, interfaces.getOrDefault(interf, Integer.MAX_VALUE));
        interfaces.put(interf, dist);
        getTransitiveSuperInterfacesWithDistance(interf, curDist + 1).forEach((t, d) -> {
          int tmp = Math.min(d, interfaces.getOrDefault(t, Integer.MAX_VALUE));
          interfaces.put(t, tmp);
        });
      }
    }
    return interfaces;
  }

  public static Map<ASTCDType, Integer> getTransitiveSuperTypesWithDist(ASTCDType ast, int curDist) {
    Map<ASTCDType, Integer> types = new LinkedHashMap<>();
    if (ast instanceof ASTCDClass) {
      if (ast.getSymbol().isPresentSuperClass()) {
        ASTType superClass = ast.getSymbol().getSuperClass().getTypeInfo().getAstNode();
        if (superClass instanceof ASTCDType) {
          int dist = Math.min(curDist, types.getOrDefault(superClass, Integer.MAX_VALUE));
          types.put((ASTCDType) superClass, dist);
          getTransitiveSuperTypesWithDist((ASTCDType) superClass, curDist + 1).forEach((t, d) ->{
            int tmp = Math.min(d, types.getOrDefault(t, Integer.MAX_VALUE));
            types.put(t, tmp);
          });
        }
      }
    }
    getTransitiveSuperInterfacesWithDistance(ast, curDist + 1).forEach((t, d) -> {
      int tmp = Math.min(d, types.getOrDefault(t, Integer.MAX_VALUE));
      types.put(t, tmp);
    });
    return types;
  }

  public static Map<ASTCDAttribute, ASTCDType> getAttributesInHierarchyWithDefiningType(ASTCDType type){
    Map<ASTCDAttribute, ASTCDType> res = new LinkedHashMap<>();
    for (ASTCDAttribute a : type.getCDAttributeList()) {
      res.put(a, type);
    }

    for (Map.Entry<ASTCDType, Integer> e : getTransitiveSuperTypesWithDist(type, 1).entrySet()) {
      for(ASTCDAttribute a : e.getKey().getCDAttributeList()){
        res.put(a, e.getKey());
      }
    }

    return res;
  }

  public static ASTCDCompilationUnit getDiagramFor(ASTMCType type){
    return getDiagramFor(getTypeSymbol(type).get().getAstNode());
  }

  /**
   * Type symbol of {@code type}, e.g. List for List&lt;Foo&gt;.
   * Replaces {@link ASTMCType#getDefiningSymbol()}, which TypeCheck3 leaves empty.
   */
  public static Optional<TypeSymbol> getTypeSymbol(ASTMCType type) {
    SymTypeExpression symType = TypeCheck3.symTypeFromAST(type);
    return symType.hasTypeInfo() ? Optional.of(symType.getTypeInfo()) : Optional.empty();
  }

  public static ASTCDCompilationUnit getDiagramFor(ASTCDType type){
    return getDiagramFor((ASTNode) type);
  }

  public static ASTCDCompilationUnit getDiagramFor(ASTCDAttribute a) {
    return getDiagramFor((ASTNode) a);
  }

  public static ASTCDCompilationUnit getDiagramFor(ASTCDAssocSide a) {
    return getDiagramFor((ASTNode) a);
  }

  public static ASTCDCompilationUnit getDiagramFor(ASTCDAssociation a) {
    return getDiagramFor((ASTNode) a);
  }

  public static ASTCDCompilationUnit getDiagramFor(ASTType type) {
    return getDiagramFor((ASTNode) type);
  }


  public static ASTCDCompilationUnit getDiagramFor(ASTCDDefinition type) {
    return getDiagramFor((ASTNode) type);
  }

  private static ASTCDCompilationUnit getDiagramFor(ASTNode node){
    IScope cur = node.getEnclosingScope();
    while(!(cur instanceof IUMLPArtifactScope)){
      cur = cur.getEnclosingScope();
    }
    IUMLPArtifactScope asArtifactScope = (IUMLPArtifactScope) cur;
    return (ASTCDCompilationUnit) asArtifactScope.getAstNode();
  }

  public static void addRoleSymbol(String symbolFqn) {
    OCLMill.globalScope().putSymbolDeSer(symbolFqn, new CDRoleSymbol2VariableSymbolAdapterDeSer());
  }

  public static void addCd4cSymbols() {
    addRoleSymbol("de.monticore.cdassociation._symboltable.CDRoleSymbol");
  }

  public static Map<DiagramSymbol, LinkedHashSet<DiagramSymbol>> getExtensionDiamond(DiagramSymbol currentDiagram){
    Map<DiagramSymbol, LinkedHashSet<DiagramSymbol>> res = new LinkedHashMap<>();
    getExtensionDiamond(currentDiagram, res);
    return res;
  }

  protected static void getExtensionDiamond(DiagramSymbol currentDiagram, Map<DiagramSymbol, LinkedHashSet<DiagramSymbol>> extensionDiamond) {
    if(extensionDiamond.containsKey(currentDiagram)) {
      return;
    }
    ASTCDDefinition currentCdDefinition = (ASTCDDefinition) currentDiagram.getAstNode();

    LinkedHashSet<DiagramSymbol> currentExtensions = getExtendedCDs(getDiagramFor(currentCdDefinition));
    extensionDiamond.put(currentDiagram, currentExtensions);
    currentExtensions.forEach(diagramSymbol -> getExtensionDiamond(diagramSymbol, extensionDiamond));
  }

  /**
   * A class diagram extends every other UMLP class diagram it imports from, either by a star import
   * of the diagram ({@code import CD.*;}) or by importing one of its types ({@code import CD.Type;}).
   * Imports of Java types or of class diagrams only available as symbols (e.g., GUI component types)
   * are no extensions.
   */
  public static LinkedHashSet<DiagramSymbol> getExtendedCDs(ASTCDCompilationUnit cd) {
    String ownName = cd.getCDDefinition().getSymbol().getFullName();
    LinkedHashSet<DiagramSymbol> extendedCDs = new LinkedHashSet<>();
    for (ASTMCImportStatement importStatement : cd.getMCImportStatementList()) {
      List<String> parts = importStatement.getMCQualifiedName().getPartsList();
      List<String> diagramParts = importStatement.isStar() ? parts : parts.subList(0, parts.size() - 1);
      if (diagramParts.isEmpty()) {
        continue;
      }
      cd.getEnclosingScope().resolveDiagram(Joiners.DOT.join(diagramParts))
          .filter(DiagramSymbol::isPresentAstNode)
          .filter(diagram -> !diagram.getFullName().equals(ownName))
          .ifPresent(extendedCDs::add);
    }
    return extendedCDs;
  }

  public static Map<ASTCDAssocSide, ASTCDAssociation> getOppositeSideToAssociationMap(ASTCDClass domainClass) {
    List<ASTCDAssocSide> assocSides = CDSymbolTables.getAssociationsInHierarchy(domainClass).stream()
        .filter(GenService::isVisible)
        .toList();
    Map<ASTCDAssocSide, ASTCDAssociation> assocSideToAssociation = new LinkedHashMap<>();
    for (ASTCDAssocSide assocSide : assocSides) {
      ASTCDAssociation assocOfAssocSide = assocSide.getSymbol().getAssoc().getAssociation().getAstNode();
      assocSideToAssociation.put(assocSide, assocOfAssocSide);
    }
    return assocSideToAssociation;
  }
}
