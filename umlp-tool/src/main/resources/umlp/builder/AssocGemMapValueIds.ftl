<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the values by their gemIds only, e.g. when loading from the database, which does not store the keys:
     the keys of the values are derived on first access -->
${tc.signature("name", "type", "builder")}
${cd4c.method("public ${builder} ${name}GemMapValueIds(List<Long> ids)")}

this._${name}._internal_addValueIDs(ids);
return this.realBuilder;
