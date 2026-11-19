<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the values by their gemIds under their keys, as received (see builder/AssocGemMapIds) -->
${tc.signature("className", "attributeName", "keyType")}
${cd4c.method("public ${className} ${attributeName}GemMapIds(Map<${keyType}, Long> ids)")}
  this._${attributeName}._internal_putIDs(ids);
  return this;
