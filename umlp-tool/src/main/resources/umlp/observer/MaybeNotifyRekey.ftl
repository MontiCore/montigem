<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name","className", "keyType")}

${cd4c.method("void maybeNotifyRekey${name?cap_first}(${className} ${className?uncap_first}, ${keyType} oldKey, ${keyType} newKey, long valueGemId, NotificationScope scope)")}

if(scope.contains(this.getScope())) {
    this.notifyRekey${name?cap_first}(${className?uncap_first}, oldKey, newKey, valueGemId);
}
