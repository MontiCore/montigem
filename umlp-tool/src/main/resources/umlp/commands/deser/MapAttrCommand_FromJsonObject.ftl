<#-- (c) https://github.com/MontiCore/monticore -->
<#-- see MapAttrCommand_BuildJson; valueEnum: the enum type of the values, empty otherwise -->
${tc.signature("command", "keyJson", "hasValue", "valueJson", "valueEnum")}
<#assign value="jsonObject.getMember(\"value\").${valueJson}">
<#if valueEnum?has_content><#assign value="${valueEnum}.valueOf(${value})"></#if>
${cd4c.method("protected Optional<${command}> fromJsonObject(JsonObject jsonObject)")}
  return Optional.of(new ${command}(
    jsonObject.getMember("gemId").getAsJsonNumber().getNumberAsLong(),
    jsonObject.hasMember("key") ? jsonObject.getMember("key").${keyJson} : null<#if hasValue>,
    jsonObject.hasMember("value") ? ${value} : null</#if>
  ));
