<#-- (c) https://github.com/MontiCore/monticore -->
  ${tc.signature("className", "attr", "fieldType", "jsonStr", "isList", "innerType", "isOptional", "useDeSer", "objectMapperName")}
<#assign fieldName=attr.getName()>
<#assign contextParameter=(objectMapperName == "context" && useDeSer)?then(", PojoGraphContext context", "")>
<#assign contextSuffix=(objectMapperName == "context" && useDeSer)?then("WithContext", "")>
<#if !jsonStr?has_content && !useDeSer>
<#--    TODO Kommentar einfuegen, wie das zu fixen waere-->
    ${cd4c.method("abstract protected void serialize" + fieldName?cap_first + "(" + className + " obj, JsonPrinter printer)")}
<#else>
    ${cd4c.method("protected void serialize" + fieldName?cap_first + contextSuffix + "(" + className + " obj, JsonPrinter printer" + contextParameter + ")")}

    <#if isList>
      printer.beginArray("${fieldName}");
      <#if useDeSer>
      for (${innerType} e: obj.get${fieldName?cap_first}List()) {
        Optional<String> serialized = ${objectMapperName}.serialize(e, "${innerType}");
        <#if objectMapperName == "context">
        printer.valueJson(serialized.orElseThrow());
        <#else>
        printer.valueJson(serialized.orElse(""));
        </#if>
      }
      <#elseif genService.isInnerCDEnum(attr.getSymbol().getType())>
      for (${innerType?keep_after_last(".")} e: obj.get${fieldName?cap_first}List()) {
        printer.value(e.name());
      }
      <#else>
      for (${innerType} e: obj.get${fieldName?cap_first}List()) {
        printer.value(e);
      }
      </#if>
      printer.endArray();
    <#elseif isOptional>
      if (obj.get${fieldName?cap_first}().isPresent()) {
        <#if useDeSer>
        printer.memberJson("${fieldName}", ${objectMapperName}.serialize(obj.get${fieldName?cap_first}().get(), "${innerType}").get());
        <#elseif genService.isInnerCDEnum(attr.getSymbol().getType())>
        printer.member("${fieldName}", obj.get${fieldName?cap_first}().get().name());
        <#else>
        printer.member("${fieldName}", obj.get${fieldName?cap_first}().get());
        </#if>
      }
    <#else>
      <#if useDeSer>
      printer.memberJson("${fieldName}", ${objectMapperName}.serialize(obj.${genService.getGetterName(fieldName, attr.getSymbol().getType())}(), "${fieldType}").orElseThrow());
      <#else>
      printer.member("${fieldName}", obj.${genService.getGetterName(fieldName, attr.getSymbol().getType())}()<#if genService.isCDEnum(attr.getSymbol().getType())>.name()</#if>);
      </#if>
    </#if>
</#if>
