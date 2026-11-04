<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts o under the chosen key (type qualified association) -->
${tc.signature("name", "keyType", "type", "builder")}
${cd4c.method("public ${builder} ${name}Put(${keyType} key, ${type} o)")}

this._${name}._internal_putKeyValue(key, o);
return this.realBuilder;
