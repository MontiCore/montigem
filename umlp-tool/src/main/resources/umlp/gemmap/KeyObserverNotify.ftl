<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rekeys a value of the map when its qualifying attribute changes,
     locally and by changes of other participants (the observer's scope is NotificationScope.LOCAL) -->
${tc.signature("declaringType", "attrName", "notifyType")}
${cd4c.method("public void notifySet${attrName?cap_first}(${declaringType} o, ${notifyType} oldValue, ${notifyType} newValue)")}
map._internal_rekey(oldValue, newValue, o.getGemId());
