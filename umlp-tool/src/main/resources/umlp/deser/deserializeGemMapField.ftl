<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the pairs of key and gemId of the value, see serializeGemMapField; keyJson reads a key from a JsonElement,
     keyDeSerType: a time key type, read by the DeSer of the object mapper -->
${tc.signature("className", "fieldName", "keyType", "keyJson", "keyDeSerType", "objectMapperName")}
${cd4c.method("protected Map<${keyType}, Long> deserialize${fieldName?cap_first} (JsonObject json)")}
  // insertion order
  Map<${keyType}, Long> res = new LinkedHashMap<>();
  if(!json.hasMember("${fieldName}")){
    return res;
  }
  for(JsonElement e : json.getMember("${fieldName}").getAsJsonArray().getValues()){
    JsonObject pair = e.getAsJsonObject();
    <#if keyDeSerType?has_content>
    res.put(pair.hasMember("key") ? ${objectMapperName}.<${keyDeSerType}>deserialize('"' + pair.getMember("key").getAsJsonString().getValue() + '"', "${keyDeSerType}").orElse(null) : null,
    <#else>
    res.put(pair.hasMember("key") ? pair.getMember("key").${keyJson} : null,
    </#if>
        pair.getMember("gemId").getAsJsonNumber().getNumberAsLong());
  }
  return res;
