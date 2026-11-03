<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("typeName", "cdName", "attributes", "optionalAttributes", "listAttributes", "listAssocs", "gemAssocs", "assocToDAOFactoryDiagramMap", "assocToManagerDiagramMap", "neo4j")}
${cd4c.method("public Optional<${typeName}> unpack${typeName}(org.neo4j.driver.Record res)")}
  int gemId = res.get("id").asInt();
  ${typeName}Builder builder = ${cdName}Manager.${typeName?uncap_first}BuilderFromId(gemId);
  builder.setINTERNAL_deser_mode(true);
<#if attributes?has_content>
  builder
</#if>
<#list attributes as attr, dbName>
    .${attr.getName()}(${includeArgs("umlp.dao.GetNeo4j", "res", attr, dbName, neo4j)})<#sep>${'\n'}</#sep><#rt>
</#list><#lt><#if attributes?has_content>;</#if>
<#if optionalAttributes?has_content>
  builder
</#if>
<#list optionalAttributes as attr, dbName>
  .${attr.getName()}(Optional.ofNullable(${includeArgs("umlp.dao.GetNeo4j", "res", attr, dbName, neo4j)}))<#sep>${'\n'}</#sep><#rt>
</#list><#lt><#if optionalAttributes?has_content>;</#if><#rt>
<#if listAttributes?has_content>
  builder
</#if>
<#list listAttributes as attr>
  .${attr.getName()}(this.select${attr.getName()?cap_first}(gemId))<#sep>${'\n'}</#sep><#rt>
</#list><#lt><#if listAttributes?has_content>;</#if><#rt>

<#lt>
<#list listAssocs as assocSide, assoc><#assign assocType = assocSide.getSymbol().getType().print()>
    <#if assocSide.isLeft()>
        <#assign otherSide=assoc.getRight().getName()>
    <#else>
        <#assign otherSide=assoc.getLeft().getName()>
    </#if>
  builder.${assocSide.getName()}${genService.isMap(assocSide)?then("GemMapValueIds","GemListIds")}(${assocToDAOFactoryDiagramMap[assoc.getName()]}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().select${assocSide.getName()?cap_first}IdsFor${otherSide?cap_first}(gemId));
</#list>
<#list gemAssocs as assocSide, assoc><#assign assocType = assocSide.getSymbol().getType().print()>
  <#if assocSide.isLeft()>
      <#assign otherSide=assoc.getRight().getName()>
  <#else>
      <#assign otherSide=assoc.getLeft().getName()>
  </#if>
  Optional<Long> ${assocSide.getName()}IdOpt = ${assocToDAOFactoryDiagramMap[assoc.getName()]}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().select${assocSide.getName()?cap_first}IdFor${otherSide?cap_first}(gemId);
  if(${assocSide.getName()}IdOpt.isPresent()) {
    builder.${assocSide.getName()}Gem(new Gem<>(${assocSide.getName()}IdOpt.get(), () -> {
        Optional<${assocType}> opt = ${assocToManagerDiagramMap[assoc.getName()]}Manager.${assocType?uncap_first}Opt(${assocSide.getName()}IdOpt.get());
        if(opt.isPresent()){
          return new LoadResult<>(opt.get());
        } else {
          return new LoadResult<>(LoadingState.FAILED_TO_LOAD);
        }
    }));
  }
</#list><#lt>
  <#rt>
  return builder.build();
