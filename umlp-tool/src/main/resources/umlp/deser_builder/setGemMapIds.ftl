<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the keys of the values are derived on first access -->
${tc.signature("className", "attributeName")}
${cd4c.method("public ${className} ${attributeName}GemMapIds(List<Long> ids)")}
  this._${attributeName}._internal_addValueIDs(ids);
  return this;
