<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name","className", "keyType")}

${cd4c.method("void maybeNotifyPut${name?cap_first}(${className} ${className?uncap_first}, ${keyType} key, long valueGemId, NotificationScope scope)")}

if(scope.contains(this.getScope())) {
    this.notifyPut${name?cap_first}(${className?uncap_first}, key, valueGemId);
}
