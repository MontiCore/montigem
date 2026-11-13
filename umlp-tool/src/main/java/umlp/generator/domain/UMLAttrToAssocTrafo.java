/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.monticore.cd4code.CD4CodeMill;
import de.monticore.cd4code._visitor.CD4CodeTraverser;
import de.monticore.cdassociation._ast.ASTCDAssocLeftSide;
import de.monticore.cdassociation._ast.ASTCDAssocRightSide;
import de.monticore.cdassociation._ast.ASTCDAssociation;
import de.monticore.cdassociation._ast.ASTCDCardinality;
import de.monticore.cdbasis._ast.*;
import de.monticore.cdbasis._visitor.CDBasisVisitor2;
import de.monticore.cdinterfaceandenum._ast.ASTCDInterface;
import de.monticore.cdinterfaceandenum._visitor.CDInterfaceAndEnumVisitor2;
import de.monticore.types.MCTypeFacade;
import de.monticore.types.mcbasictypes._ast.ASTMCType;
import de.monticore.types.mccollectiontypes._ast.ASTMCGenericType;
import de.monticore.types.check.SymTypeExpression;
import de.monticore.types.check.SymTypeOfGenerics;
import umlp.generator.domain.decorator.GenService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;

import static de.monticore.cd.facade.CDModifier.PUBLIC;

/*
 * This transformation converts attributes with "CDType" (except enums) into a suitable association:
 * T -> [1], Optional<T> -> [0..1], List<T> -> [*], and Map<K, T> -> the type qualified association [K] -> T [1].
 * It is assumed that the symbol table is constructed
 *
 **/
public class UMLAttrToAssocTrafo implements CDBasisVisitor2, CDInterfaceAndEnumVisitor2 {

  protected Optional<ASTCDPackage> astcdPackage = Optional.empty();

  protected ASTCDType astcdType;

  protected Multimap<ASTCDType, ASTCDAttribute> attrMap = Multimaps.newMultimap(new LinkedHashMap<>(), ArrayList::new);

  protected Multimap<Optional<ASTCDPackage>, ASTCDAssociation> assocMap = Multimaps.newMultimap(new LinkedHashMap<>(), ArrayList::new);

  protected CD4CodeTraverser traverser;

  public UMLAttrToAssocTrafo(CD4CodeTraverser traverser) {
    this.traverser = traverser;
    this.traverser.add4CDBasis(this);
    this.traverser.add4CDInterfaceAndEnum(this);
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
  public void visit(ASTCDClass node) {
    astcdType = node;
  }

  @Override
  public void visit(ASTCDInterface node) {
    astcdType = node;
  }

  @Override
  public void visit(ASTCDAttribute attr) {
    SymTypeExpression attrType = attr.getSymbol().getType();
    if (GenService.isDomainType(attrType) && !GenService.isCDEnum(attrType)) {
      assocMap.put(astcdPackage,
              createAssoc(attr.getName(), astcdType.getSymbol().getName(),
                      CD4CodeMill.cDCardOneBuilder().build(),
                      attrType.print()));
      attrMap.put(astcdType, attr);
    } else if (GenService.isMap(attrType)) {
      // Map<K, T>: the type qualified association Owner [K] -> (attr) T [1]; other maps are rejected by the CoCos
      // UMLPMapAttributeValueType and UMLPMapAttributeKeyType
      Optional<SymTypeExpression> valueType = GenService.getMapTypeArgument(attrType, 1);
      Optional<ASTMCType> keyType = getMapKeyType(attr);
      if (valueType.isPresent() && GenService.isAssociationTarget(valueType.get()) && keyType.isPresent()) {
        ASTCDAssociation assoc = createAssoc(attr.getName(), astcdType.getSymbol().getName(),
            CD4CodeMill.cDCardOneBuilder().build(), valueType.get().print());
        assoc.getLeft().setCDQualifier(CD4CodeMill.cDQualifierBuilder().setByType(keyType.get()).build());
        // later findings (e.g. of the CD4A CoCos after the transformation) point to the attribute
        assoc.set_SourcePositionStart(attr.get_SourcePositionStart());
        assoc.set_SourcePositionEnd(attr.get_SourcePositionEnd());
        assoc.getLeft().getCDQualifier().set_SourcePositionStart(attr.get_SourcePositionStart());
        assoc.getLeft().getCDQualifier().set_SourcePositionEnd(attr.get_SourcePositionEnd());
        assocMap.put(astcdPackage, assoc);
        attrMap.put(astcdType, attr);
      }
    } else if (attrType.isGenericType()) {
      SymTypeExpression argType = ((SymTypeOfGenerics) attrType).getArgument(0);
      if (GenService.isDomainType(argType) && !GenService.isCDEnum(argType)) {
        if (GenService.isOptional(attrType)) {
          assocMap.put(astcdPackage, createAssoc(attr.getName(), astcdType.getSymbol().getName(),
                  CD4CodeMill.cDCardOptBuilder().build(),
                  argType.print()));
          attrMap.put(astcdType, attr);
        } else if (GenService.isList(attrType)) {
          assocMap.put(astcdPackage, createAssoc(attr.getName(), astcdType.getSymbol().getName(),
                  CD4CodeMill.cDCardMultBuilder().build(),
                  argType.print()));
          attrMap.put(astcdType, attr);
        }
      }
    }
  }

  @Override
  public void endVisit(ASTCDDefinition node) {
    attrMap.entries().forEach(e -> e.getKey().removeCDMember(e.getValue()));
    assocMap.entries().forEach(e -> {
      Optional<ASTCDPackage> key = e.getKey();
      if (key.isPresent()) {
        e.getKey().get().addCDElement(e.getValue());
      } else {
        node.addCDElement(e.getValue());
      }
    });
  }

  /**
   * the key type K of a {@code Map<K, V>} attribute, as written in the model
   */
  protected Optional<ASTMCType> getMapKeyType(ASTCDAttribute attr) {
    if (attr.getMCType() instanceof ASTMCGenericType) {
      ASTMCGenericType mapType = (ASTMCGenericType) attr.getMCType();
      if (mapType.getMCTypeArgumentList().size() == 2 && mapType.getMCTypeArgument(0).getMCTypeOpt().isPresent()) {
        return Optional.of(mapType.getMCTypeArgument(0).getMCTypeOpt().get().deepClone());
      }
    }
    return Optional.empty();
  }

  protected ASTCDAssociation createAssoc(String attrName, String sourceName, ASTCDCardinality card, String targetName) {
    ASTCDAssocLeftSide leftSide = CD4CodeMill.cDAssocLeftSideBuilder()
            .setModifier(PUBLIC.build())
            .setMCQualifiedType(MCTypeFacade.getInstance().createQualifiedType(sourceName))
            .setCDRole(CD4CodeMill.cDRoleBuilder().setName(attrName+sourceName).build())
            .build();
    ASTCDAssocRightSide rightSide = CD4CodeMill.cDAssocRightSideBuilder()
            .setModifier(PUBLIC.build())
            .setMCQualifiedType(MCTypeFacade.getInstance().createQualifiedType(targetName))
            .setCDRole(CD4CodeMill.cDRoleBuilder().setName(attrName).build())
            .setCDCardinality(card)
            .build();
    return CD4CodeMill.cDAssociationBuilder()
            .setModifier(PUBLIC.build())
            .setCDAssocType(CD4CodeMill.cDAssocTypeAssocBuilder().build())
            .setLeft(leftSide)
            .setCDAssocDir(CD4CodeMill.cDLeftToRightDirBuilder().build())
            .setRight(rightSide)
            .build();
  }

  public void transform(ASTCDCompilationUnit ast) {
    ast.accept(this.traverser);
  }

}
