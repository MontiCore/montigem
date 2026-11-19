<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the key of o is derived from its qualifying attribute -->
${tc.signature("name", "type", "builder")}
${cd4c.method("public ${builder} ${name}Put(${type} o)")}

this._${name}._internal_putValue(o);
return this.realBuilder;
