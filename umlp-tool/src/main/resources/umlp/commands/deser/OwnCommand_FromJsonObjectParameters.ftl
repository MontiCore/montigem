<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("attrName", "type", "isOptional", "isCDEnum", "isList", "json", "useDeSer", "objectMapperName", "isGemOptional")}
<#if useDeSer>
  <#if isOptional>Optional.ofNullable(</#if>
    ${objectMapperName}.<${type}>deserialize(jsonObject.getMember("${attrName}").isJsonString() ? '"'
      + jsonObject.getMember("${attrName}").getAsJsonString().getValue() + '"' : jsonObject.getMember("${attrName}").toString(), "${type}").orElse(null)
  <#if isOptional>)</#if>
<#else>
    <#if isCDEnum>
      <#if isList>
        jsonObject.getMember("${attrName}").getAsJsonArray().getValues().stream().map(JsonElement::getAsJsonString)
        .map(v->${type?keep_after_last(".")}.valueOf(v.getValue())).collect(java.util.stream.Collectors.toList())
      <#elseif isOptional>
        jsonObject.hasMember("${attrName}") ?
          Optional.of(${type?keep_after_last(".")}.valueOf(jsonObject.getMember("${attrName}").${json}))
          : Optional.empty()
      <#else>
        ${type?keep_after_last(".")}.valueOf(jsonObject.getMember("${attrName}").${json})
      </#if>
    <#elseif isList>
    ${json}
    <#else>
      <#if isOptional || isGemOptional>
      jsonObject.getMember("${attrName}") != null ?
        Optional.of(jsonObject.getMember("${attrName}").${json})
          : Optional.empty()
      <#else>
      jsonObject.getMember("${attrName}").${json}
      </#if>
    </#if>
</#if>
