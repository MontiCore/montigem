<#-- (c) https://github.com/MontiCore/monticore -->
<#-- initial value of the keyObserver attribute: rekeys a value when its qualifying attribute changes,
     locally and by changes of other participants (NotificationScope.LOCAL) -->
${tc.signature("className", "declaringType", "attrName", "notifyType")}
 = new ${declaringType}Observer() {
  @Override
  public NotificationScope getScope() {
    return NotificationScope.LOCAL;
  }

  @Override
  public void notifySet${attrName?cap_first}(${declaringType} o, ${notifyType} oldValue, ${notifyType} newValue) {
    ${className}.this._internal_rekey(oldValue, newValue, o.getGemId());
  }
}
