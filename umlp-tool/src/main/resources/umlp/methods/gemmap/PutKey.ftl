<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts o under the chosen key (type qualified association) by their ids, see _internal_put<Role>Id, which checks the
     key. keyType: the key type (a domain class for GemEntryMaps, stored by its gemId) -->
${tc.signature("name", "keyType", "targetName", "isEntry")}

${cd4c.method("public void put${name?cap_first}(${keyType} key, ${targetName} o, NotificationScope scope)")}
_internal_put${name?cap_first}Id(<#if isEntry>key == null ? null : Long.valueOf(key.getGemId())<#else>key</#if>, o.getGemId(), scope);
