<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("attrName")}
${cd4c.method("private List<String> get${attrName?cap_first}(JsonObject jsonObject)")}
  // Only String types supported -> Cf. LoadingTests - TestCommand_PrimitiveWList_iCT
  List<String> list = new ArrayList<>();
  for (JsonElement e : jsonObject.getMember("${attrName}").getAsJsonArray().getValues()) {
    String value = e.getAsJsonString().getValue();
    list.add(value);
  }
  return list;
