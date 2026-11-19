<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("typeName", "cdName", "attributes", "optionalAttributes", "listAttributes", "listAssocs", "gemAssocs", "assocsToDefinitionDiagramMap")}
${cd4c.method("public void addManyAssocsToBatch${typeName}(Collection<${typeName}> objectsToInsert)")}
// TODO replace this with Commands

  for(${typeName} o : objectsToInsert) {
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
     ${assocsToDefinitionDiagramMap[assoc.getName()]}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().addManyToBatch${assoc.getName()?cap_first}(${assocSide.getName()?uncap_first}Map);

  </#list>
  <#list gemAssocs as assocSide, assoc>
    if(o.get${assocSide.getName()?cap_first}Gem().getGemId() != -1){
    HashMap<Long, List<Long>> ${assocSide.getName()?uncap_first}Map = new HashMap<>();
      <#if assocSide.isLeft()>
        ${assocSide.getName()?uncap_first}Map.put(o.get${assocSide.getName()?cap_first}Gem().getGemId(), Arrays.asList(o.getGemId()));
      <#else>
        ${assocSide.getName()?uncap_first}Map.put(o.getGemId(), Arrays.asList(o.get${assocSide.getName()?cap_first}Gem().getGemId()));
      </#if>
      ${assocsToDefinitionDiagramMap[assoc.getName()]}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().addManyToBatch${assoc.getName()?cap_first}(${assocSide.getName()?uncap_first}Map);
    }
  </#list>
    }
