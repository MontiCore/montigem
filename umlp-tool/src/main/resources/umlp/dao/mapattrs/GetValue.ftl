<#-- (c) https://github.com/MontiCore/monticore -->
<#-- like Get and GetNeo4j, for a key or value of a map attribute -->
${signature("statement", "type", "columnName", "neo4j")}
<#assign value=neo4j?has_content?then("${statement}.get(${columnName}).asObject()", "${statement}.getObject(${columnName})")>
<#assign numberValueCall=genService.getNumberValueCall(type)>
<#if genService.isCDEnum(type)>
  ${includeArgs("umlp.dao.get.java_lang_Enum"+neo4j, statement, columnName, type.print())}
<#elseif numberValueCall?has_content>
  ${value} == null ? null : ((Number) ${value})${numberValueCall}
<#else>
  ${value} == null ? null : ((${genService.getDBType(type, neo4j)}) ${value})
</#if>
