<#-- (c) https://github.com/MontiCore/monticore -->
<#-- rekeys a value when its qualifying attribute changes, locally and by changes of other participants (the
     observer's scope is NotificationScope.LOCAL): the manager rekeys the maps of its loaded owners, through the
     owners, so that the server tells the participants that loaded an owner (see managerimpl/DoRekey) -->
${tc.signature("declaringType", "attrName", "notifyType", "managerName", "rekeyName")}
${cd4c.method("public void notifySet${attrName?cap_first}(${declaringType} o, ${notifyType} oldValue, ${notifyType} newValue)")}
${managerName}.rekey${rekeyName}(oldValue, newValue, o.getGemId());
