<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rekeys a value of the map when its qualifying attribute changes,
     locally and by changes of other participants (the observer's scope is NotificationScope.LOCAL);
     through the owner, so that the server tells the participants that loaded the owner (see RekeyAndSync) -->
${tc.signature("declaringType", "attrName", "notifyType")}
${cd4c.method("public void notifySet${attrName?cap_first}(${declaringType} o, ${notifyType} oldValue, ${notifyType} newValue)")}
map._internal_rekeyAndSync(oldValue, newValue, o.getGemId());
