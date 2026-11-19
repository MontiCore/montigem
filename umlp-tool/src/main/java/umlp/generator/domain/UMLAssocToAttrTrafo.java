/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdassociation._ast.ASTCDAssocSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._visitor.CDAssociationVisitor2;
import de.monticore.cdbasis._ast.ASTCDAttribute;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.cdbasis._ast.ASTCDDefinition;
import de.monticore.cdbasis._ast.ASTCDPackage;
import de.monticore.cdbasis._ast.ASTCDType;
import de.monticore.cdbasis._symboltable.CDTypeSymbol;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import umlp.generator.domain.decorator.GenService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;

/*
 * This transformation converts associations with "EnumType" into a suitable attribute.
 * It is assumed that the symbol table is constructed
 *
 **/
public class UMLAssocToAttrTrafo implements CDBasisVisitor2, CDAssociationVisitor2 {

  protected Optional<ASTCDPackage> astcdPackage = Optional.empty();

  protected Multimap<Optional<ASTCDPackage>, ASTCDAssociation> assocMap = Multimaps.newMultimap(new LinkedHashMap<>(), ArrayList::new);

  protected CD4CodeTraverser traverser;

  public UMLAssocToAttrTrafo(CD4CodeTraverser traverser) {
    this.traverser = traverser;
    this.traverser.add4CDBasis(this);
    this.traverser.add4CDAssociation(this);
  }

  @Override
  public void visit(ASTCDPackage node) {
    astcdPackage = Optional.of(node);
  }

  @Override
  public void endVisit(ASTCDPackage node) {
    astcdPackage= Optional.empty();
  }

  @Override
  public void visit(ASTCDAssociation assoc) {
    if (!assoc.getLeft().isPresentSymbol() || !assoc.getRight().isPresentSymbol()) {
      // new created association (see UMLAttrToAssocTrafo)
      return;
    }
    // Modeled POJOs have generated association APIs on both ends. They must not
    // take the external-value conversion below, which retains only one end.
    if (GenService.isPojoCDType(assoc.getLeft().getSymbol().getType())
        && GenService.isPojoCDType(assoc.getRight().getSymbol().getType())) {
      return;
    }
    ASTCDAssocSide assocSide;
    SymTypeExpression classType;
    if (GenService.isCDEnum(assoc.getLeft().getSymbol().getType()) ||
        GenService.isNotADomainType(assoc.getLeft().getSymbol().getType())) {
      assocSide = assoc.getLeft();
      classType = assoc.getRight().getSymbol().getType();
      SymTypeExpression enumType = assocSide.getSymbol().getType();
      ASTMCType attrType;
      if (assocSide.isPresentCDCardinality() && assocSide.getCDCardinality().isOpt()) {
        attrType = MCTypeFacade.getInstance().createOptionalTypeOf(enumType.print());
      } else if (assocSide.isPresentCDCardinality() && GenService.isList(assocSide.getCDCardinality())) {
        attrType = MCTypeFacade.getInstance().createListTypeOf(enumType.print());
      } else {
        attrType = MCTypeFacade.getInstance().createQualifiedType(enumType.print());
      }
      ASTCDAttribute attr = CD4CodeMill.cDAttributeBuilder().
          setModifier(assoc.getModifier().deepClone()).
          setName(assocSide.getCDRole().getName()).
          setMCType(attrType).build();
      Optional<CDTypeSymbol> cdType = assoc.getEnclosingScope().resolveCDType(classType.printFullName());
      if (cdType.isPresent() && cdType.get().isPresentAstNode()) {
        ASTCDType astCDType = cdType.get().getAstNode();
        astCDType.addCDMember(attr);
        assocMap.put(astcdPackage, assoc);
      }
    } else if (GenService.isCDEnum(assoc.getRight().getSymbol().getType()) ||
        GenService.isNotADomainType(assoc.getRight().getSymbol().getType())) {
      assocSide = assoc.getRight();
      classType = assoc.getLeft().getSymbol().getType();
      SymTypeExpression enumType = assocSide.getSymbol().getType();
      ASTMCType attrType;
      if (assocSide.isPresentCDCardinality() && assocSide.getCDCardinality().isOpt()) {
        attrType = MCTypeFacade.getInstance().createOptionalTypeOf(enumType.print());
      } else if (assocSide.isPresentCDCardinality() && GenService.isList(assocSide.getCDCardinality())) {
        attrType = MCTypeFacade.getInstance().createListTypeOf(enumType.print());
      } else {
        attrType = MCTypeFacade.getInstance().createQualifiedType(enumType.print());
      }
      ASTCDAttribute attr = CD4CodeMill.cDAttributeBuilder().
          setModifier(assoc.getModifier().deepClone()).
          setName(assocSide.getCDRole().getName()).
          setMCType(attrType).build();
      Optional<CDTypeSymbol> cdType = assoc.getEnclosingScope().resolveCDType(classType.printFullName());
      if (cdType.isPresent() && cdType.get().isPresentAstNode()) {
        ASTCDType astCDType = cdType.get().getAstNode();
        astCDType.addCDMember(attr);
        assocMap.put(astcdPackage, assoc);
      }
    }
  }

  @Override
  public void endVisit(ASTCDDefinition node) {
    assocMap.entries().forEach(e -> {
      Optional<ASTCDPackage> key = e.getKey();
      if (key.isPresent()) {
        e.getKey().get().removeCDElement(e.getValue());
      } else {
        node.removeCDElement(e.getValue());
      }
    });
  }

  public void transform(ASTCDCompilationUnit ast) {
    ast.accept(this.traverser);
  }

}
