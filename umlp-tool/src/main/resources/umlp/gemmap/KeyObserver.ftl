<#-- (c) https://github.com/MontiCore/monticore -->
<#-- initial value of a keyObserver attribute: rekeys a value when its qualifying attribute changes,
     locally and by changes of other participants (NotificationScope.LOCAL).
     observedType: the (runtime) type of the values observed; subclasses notify their own observers only -->
${tc.signature("className", "observedType", "declaringType", "attrName", "notifyType")}
 = new ${observedType}Observer() {
  @Override
  public NotificationScope getScope() {
    return NotificationScope.LOCAL;
  }

  @Override
  public void notifySet${attrName?cap_first}(${declaringType} o, ${notifyType} oldValue, ${notifyType} newValue) {
    ${className}.this._internal_rekey(oldValue, newValue, o.getGemId());
  }
}
