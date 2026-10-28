<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the pairs of key and gemId of the value, in insertion order: [{"key": ..., "gemId": ...}, ...];
     keyType: the stored key type (the key, or the gemId of the key object for GemEntryMaps) -->
${tc.signature("className", "fieldName", "keyType")}
${cd4c.method("protected void serialize" + fieldName?cap_first + "(" + className + " obj, JsonPrinter printer)")}
  printer.beginArray("${fieldName}");
  // getIDMap derives pending keys first (values loaded from the database by id only)
  for (Map.Entry<${keyType}, Long> e : obj._internal_get${fieldName?cap_first}GemMap().getIDMap().entrySet()) {
    printer.beginObject();
    if (e.getKey() != null) {
      printer.member("key", e.getKey());
    }
    printer.member("gemId", e.getValue());
    printer.endObject();
  }
  printer.endArray();
