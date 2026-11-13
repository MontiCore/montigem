<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("attrName", "type", "typeName", "isCDEnum", "json", "useDeSer", "objectMapperName", "isCollectionType")}
<#if useDeSer>
  jsonPrinter.memberJson("${attrName}", ${objectMapperName}.serialize(command.${genService.getGetterName(attrName, type)}(), "${typeName}").orElseThrow());
<#else>
  <#if genService.isListOfGemIdentifiable(type)>
  jsonPrinter.beginArray("${attrName}");
  for(long gemId : command.${genService.getGetterName(attrName, type)}()) {
    jsonPrinter.value(gemId);
  }
    jsonPrinter.endArray();
  <#elseif genService.isList(type)>
    jsonPrinter.beginArray("${attrName}");
    for(var val : command.${genService.getGetterName(attrName, type)}<#if isCDEnum || isCollectionType>List</#if>()) { <#-- Do I/ALu even want to know why getGetterName is not returning the List (only required for CollectionTypes"List", not java.util.List? - and why are enums always with List?-->
      jsonPrinter.value(val<#if isCDEnum>.name()</#if>);
    }
    jsonPrinter.endArray();
  <#elseif genService.isOptional(type)>
  if (command.isPresent${attrName?cap_first}()) {
    jsonPrinter.member("${attrName}", command.${genService.getGetterName(attrName, type)}()<#if isCDEnum>.name()</#if>);
  }
  <#else>
    jsonPrinter.member("${attrName}", command.${genService.getGetterName(attrName, type)}<#if genService.isList(type)>List</#if>()<#if isCDEnum>.name()</#if>);
  </#if>
</#if>
