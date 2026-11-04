<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts o under the chosen key (type qualified association); a null key is rejected like by the owner (PutKey) -->
${tc.signature("name", "keyType", "type", "builder")}
${cd4c.method("public ${builder} ${name}Put(${keyType} key, ${type} o)")}

if (key == null) {
  Log.error("0xA7323 The object with id " + o.getGemId() + " cannot be put under the key null");
  return this.realBuilder;
}
this._${name}._internal_putKeyValue(key, o);
return this.realBuilder;
