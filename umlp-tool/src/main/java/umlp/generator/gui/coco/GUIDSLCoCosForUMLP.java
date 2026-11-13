/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.gui.coco;

import com.google.common.collect.Lists;
import de.monticore.expressions.cocos.ExpressionValid;
import de.monticore.guidsl._cocos.GUIComponentDeclNameFits;
import de.monticore.guidsl._cocos.GUIComponentDeclarationIsPresent;
import de.monticore.guidsl._cocos.GUIDSLCoCoChecker;
import de.monticore.guidsl._cocos.GUIDSLCoCos;
import de.monticore.guidsl._cocos.GUIMultipleNameArgs;
import de.monticore.guidsl._cocos.GUINameArgExists;
import de.monticore.guidsl._cocos.GUIParamDefaultFitsType;
import de.monticore.guidsl._cocos.GUIParameterNamesFit;
import de.monticore.guidsl._cocos.GUIRangeIterateControlStartAndEndAreInt;
import de.monticore.guidsl._cocos.GUIShortIterateCheckIterable;
import de.monticore.types.check.TypeCalculator;
import de.monticore.types.check.types3wrapper.TypeCheck3AsTypeCalculator;
import de.monticore.types.mcbasictypes.cocos.QualifiedTypeHasNoTypeParameters;

public class GUIDSLCoCosForUMLP extends GUIDSLCoCos {

  public GUIDSLCoCoChecker createChecker() {
    TypeCalculator tc = new TypeCheck3AsTypeCalculator();
    GUIDSLCoCoChecker checker = new GUIDSLCoCoChecker();
    checker.addCoCo(new ExpressionValid(tc));
    checker.addCoCo(new GUIParamDefaultFitsType());
    checker.addCoCo(new EnhancedGuardConditionIsBoolean());
    checker.addCoCo(new GUIRangeIterateControlStartAndEndAreInt());
    checker.addCoCo(new GUINameArgExists());
    checker.addCoCo(new GUIMultipleNameArgs());
    checker.addCoCo(new GUIComponentDeclarationIsPresent());
    checker.addCoCo(new GUIShortIterateCheckIterable());
    checker.addCoCo(new GUIComponentDeclNameFits(Lists.newArrayList("forms", "form", "template")));
    checker.addCoCo(new GUIParameterNamesFit(Lists.newArrayList("class")));
    checker.addCoCo(new GuiNoSingletonList());
    checker.addCoCo(new GUIParamDefaultOnlyLiteralCoCo());
    checker.addCoCo(new GUIParamNoUnderspecifiedGenericsCoCo());
    checker.addCoCo(new NameExpressionsSymbolBoundExceptCallsCoCo());
    checker.addCoCo(new RefGUIArgIncompatibleWithDerivedCoCo());
    checker.addCoCo(new GuiNoAttributeQualifiedRoleCoCo());
    checker.addCoCo(new QualifiedTypeHasNoTypeParameters());
    return checker;
  }
}
