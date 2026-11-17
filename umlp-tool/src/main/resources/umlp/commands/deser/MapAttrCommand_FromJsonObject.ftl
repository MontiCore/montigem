<#-- (c) https://github.com/MontiCore/monticore -->
<#-- see MapAttrCommand_BuildJson; valueEnum: the enum type of the values, empty otherwise; keyUseDeSer, useDeSer: keys,
     values with a DeSer of the object mapper (date/time types) -->
${tc.signature("command", "keyJson", "keyUseDeSer", "keyType", "hasValue", "valueJson", "valueEnum", "useDeSer",
  "valueType", "objectMapperName")}
${cd4c.method("protected Optional<${command}> fromJsonObject(JsonObject jsonObject)")}
  return Optional.of(new ${command}(
    jsonObject.getMember("gemId").getAsJsonNumber().getNumberAsLong(),
    <#if keyUseDeSer>
    jsonObject.hasMember("key") ? ${objectMapperName}.<${keyType}>deserialize("\"" + jsonObject.getMember("key").getAsJsonString().getValue() + "\"", "${keyType}").orElse(null) : null<#if hasValue>,</#if>
    <#else>
    jsonObject.hasMember("key") ? jsonObject.getMember("key").${keyJson} : null<#if hasValue>,</#if>
    </#if>
<#if hasValue>
    <#if useDeSer>
    jsonObject.hasMember("value") ? ${objectMapperName}.<${valueType}>deserialize("\"" + jsonObject.getMember("value").getAsJsonString().getValue() + "\"", "${valueType}").orElse(null) : null
    <#elseif valueEnum?has_content>
    jsonObject.hasMember("value") ? ${valueEnum}.valueOf(jsonObject.getMember("value").${valueJson}) : null
    <#else>
    jsonObject.hasMember("value") ? jsonObject.getMember("value").${valueJson} : null
    </#if>
</#if>
  ));
