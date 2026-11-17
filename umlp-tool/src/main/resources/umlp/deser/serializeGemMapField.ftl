<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the pairs of key and gemId of the value, in insertion order: [{"key": ..., "gemId": ...}, ...];
     keyType: the stored key type (the key, or the gemId of the key object for GemEntryMaps); keyDeSerType: a time key
     type, written by the DeSer of the object mapper, empty otherwise -->
${tc.signature("className", "fieldName", "keyType", "keyDeSerType", "objectMapperName")}
${cd4c.method("protected void serialize" + fieldName?cap_first + "(" + className + " obj, JsonPrinter printer)")}
  printer.beginArray("${fieldName}");
  // getIDMap derives pending keys first (values loaded from the database by id only)
  for (Map.Entry<${keyType}, Long> e : obj._internal_get${fieldName?cap_first}GemMap().getIDMap().entrySet()) {
    printer.beginObject();
    if (e.getKey() != null) {
    <#if keyDeSerType?has_content>
      printer.memberJson("key", ${objectMapperName}.serialize(e.getKey(), "${keyDeSerType}").orElseThrow());
    <#else>
      printer.member("key", e.getKey());
    </#if>
    }
    printer.member("gemId", e.getValue());
    printer.endObject();
  }
  printer.endArray();
