<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the keys of the values are derived on first access -->
${tc.signature("name", "type", "builder")}
${cd4c.method("public ${builder} ${name}GemMapIds(List<Long> ids)")}

this._${name}._internal_addValueIDs(ids);
return this.realBuilder;
