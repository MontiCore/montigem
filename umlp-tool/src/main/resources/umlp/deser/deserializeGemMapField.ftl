<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the pairs of key and gemId of the value, see serializeGemMapField; keyJson reads a key from a JsonElement -->
${tc.signature("className", "fieldName", "keyType", "keyJson")}
${cd4c.method("protected Map<${keyType}, Long> deserialize${fieldName?cap_first} (JsonObject json)")}
  // insertion order
  Map<${keyType}, Long> res = new LinkedHashMap<>();
  if(!json.hasMember("${fieldName}")){
    return res;
  }
  for(JsonElement e : json.getMember("${fieldName}").getAsJsonArray().getValues()){
    JsonObject pair = e.getAsJsonObject();
    res.put(pair.hasMember("key") ? pair.getMember("key").${keyJson} : null,
        pair.getMember("gemId").getAsJsonNumber().getNumberAsLong());
  }
  return res;
