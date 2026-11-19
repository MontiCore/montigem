<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("command", "parameters", "types", "useDeser", "objectMapperName", "makeAbstract", "isCollectionTypes")}
<#if makeAbstract>
    ${cd4c.method("public abstract void buildJson(${command} command, JsonPrinter jsonPrinter)")}
<#else>
    ${cd4c.method("public void buildJson(${command} command, JsonPrinter jsonPrinter)")}
  super.buildJson(command, jsonPrinter);
<#list parameters as param>
    <#assign type = types[param?index]>
    <#assign isCollectionType = isCollectionTypes[param?index]> <#--a workaround for https://git.rwth-aachen.de/monticore/monticore/-/work_items/5257 -->
    ${tc.includeArgs("umlp.commands.deser.OwnCommand_BuildJsonParameters", [param, type, genService.printTypeFullName(type), genService.isCDOrInnerCDEnum(type), genService.calculateJsonInclGenerics(type, param), useDeser[param?index], objectMapperName, isCollectionType])}
</#list>
</#if>
