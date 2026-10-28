<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("cdName", "className", "simpleAttributes", "gemAttributes", "gemListAttributes", "gemMapAttributes")}
${cd4c.method("public Optional<${className}> deserialize(JsonObject json)")}
  ${className}Builder builder = ${cdName}Manager.${className?uncap_first}BuilderFromId(json.getLongMember("gemId"));
  builder.setINTERNAL_deser_mode(true);
  <#if simpleAttributes?has_content || gemAttributes?has_content || gemListAttributes?has_content || gemMapAttributes?has_content>
  builder
    <#list simpleAttributes as attr>
      .${attr}(deserialize${attr?cap_first}(json))
    </#list>
    <#list gemAttributes as attr>
      .${attr}Gem(deserialize${attr?cap_first}(json))
    </#list>
    <#list gemListAttributes as attr>
      .${attr}GemListIds(deserialize${attr?cap_first}(json))
    </#list>
    <#list gemMapAttributes as attr>
      .${attr}GemMapIds(deserialize${attr?cap_first}(json))
    </#list>
  ;
  </#if>

  deserialize_Addons(builder, json);
  return builder.build();
