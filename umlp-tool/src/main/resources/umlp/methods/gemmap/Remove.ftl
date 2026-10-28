<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("name", "keyType", "isEntry")}

${cd4c.method("public boolean remove${name?cap_first}(${keyType} key, NotificationScope scope)")}
Long gemID = this._${name}.getID(<#if isEntry>Long.valueOf(key.getGemId())<#else>key</#if>);
return gemID != null && _internal_remove${name?cap_first}Id(gemID, scope);
