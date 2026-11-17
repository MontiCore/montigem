<#-- (c) https://github.com/MontiCore/monticore -->
<#-- puts value under key into a map attribute with simple values; null keys and values and long keys a browser cannot
     represent exactly are rejected like by the owner (methods/mapattrs/Put) -->
${tc.signature("name", "keyType", "valueType", "builder", "isLongKey")}
${cd4c.method("public ${builder} ${name}Put(${keyType} key, ${valueType} value)")}

if (key == null || value == null) {
  Log.error("0xA7331 The ${name} cannot hold the key " + key + " with the value " + value
      + ": null keys and values are not allowed");
  return this.realBuilder;
}
<#if isLongKey>
if (key > GemValueMap.MAX_SAFE_KEY || key < -GemValueMap.MAX_SAFE_KEY) {
  Log.error("0xA7332 The ${name} cannot hold the key " + key + ", which a browser cannot represent exactly");
  return this.realBuilder;
}
</#if>
this._${name}.put(key, value);
return this.realBuilder;
