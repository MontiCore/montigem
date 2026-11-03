<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("typeName", "cdName", "attributes", "optionalAttributes", "listAttributes", "listAssocs", "gemAssocs", "assocsToDefinitionDiagramMap", "neo4j")}
${cd4c.method("public void insert${typeName}(${typeName} o)")}
  <#assign statement="insert"+typeName>
  ensureInsert${typeName}Created();
  DatabaseAccessManager4Neo4j.executeWrite(
      ${statement},
      Values.parameters(
        "p1", o.getGemId()<#if (attributes?size > 0 || optionalAttributes?size > 0)>,</#if>
        <#assign index = 1>
        <#list attributes as attr><#assign index = index+1>
          ${includeArgs("umlp.dao.SetNeo4j", statement, "p"+index, genService.getDBType(attr.getSymbol().getType()), "o."+genService.getGetterName(attr.getName(), attr.getSymbol().getType())+"()", false, neo4j)}
          <#if attr_has_next>, </#if>
        </#list>
        <#if (index> 1 && optionalAttributes?size > 0)>,</#if>
        <#list optionalAttributes as attr><#assign index = index+1>
          ${includeArgs("umlp.dao.SetNeo4j", statement, "p"+index, genService.getDBType(attr.getSymbol().getType()), "o."+genService.getGetterName(attr.getName(), attr.getSymbol().getType())+"()", true, neo4j)}
          <#if attr_has_next>, </#if>
        </#list>
      )
  );
  <#list listAttributes as attr>
  update${attr.getName()?cap_first}(o.getGemId(), o.get${attr.getName()?cap_first}List());
  </#list>

  List<Long> iDs;
  <#list listAssocs as assocSide, assoc>
  <#if genService.isMap(assocSide)>
  iDs = new ArrayList<>(o._internal_get${assocSide.getName()?cap_first}GemMap().getValueIDs());
  <#else>
  iDs = o._internal_get${assocSide.getName()?cap_first}GemList().getIDs();
  </#if>
  HashMap<Long, List<Long>> ${assocSide.getName()?uncap_first}Map = new HashMap<>();
    <#if assocSide.isLeft()>
  iDs.forEach(id -> ${assocSide.getName()?uncap_first}Map.put(id, Arrays.asList(o.getGemId())));
    <#else>
  ${assocSide.getName()?uncap_first}Map.put(o.getGemId(), iDs);
    </#if>
  ${assocsToDefinitionDiagramMap[assoc.getName()]}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().addMany${assoc.getName()?cap_first}(${assocSide.getName()?uncap_first}Map);
  </#list>
  <#list gemAssocs as assocSide, assoc>
  if(o.get${assocSide.getName()?cap_first}Gem().getGemId() != -1){
  <#if assocSide.isLeft()>
    ${assocsToDefinitionDiagramMap[assoc.getName()]}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().add${assoc.getName()?cap_first}(o.get${assocSide.getName()?cap_first}Gem().getGemId(), o.getGemId(),0);
  <#else>
    ${assocsToDefinitionDiagramMap[assoc.getName()]}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().add${assoc.getName()?cap_first}(o.getGemId(), o.get${assocSide.getName()?cap_first}Gem().getGemId(),0);
  </#if>
  }
  </#list>
