<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the values by their gemIds under their keys (as received from another participant), without loading them;
     keyType: the stored key type (the key, or the gemId of the key object for GemEntryMaps) -->
${tc.signature("name", "type", "builder", "keyType")}
${cd4c.method("public ${builder} ${name}GemMapIds(Map<${keyType}, Long> ids)")}

this._${name}._internal_putIDs(ids);
return this.realBuilder;
