<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value was put into a GemMap: stores the pair of owner and value. Attribute qualified associations
     (keyDerived) do not store the key, it is derived from the value when loading; type qualified ones store it with
     the pair (a value moved to another key of the owner updates the key of its pair) -->
${tc.signature("cdName","className","assocName","assocSide", "otherClassName", "keyType", "keyDerived")}
${cd4c.method("public void notifyPut${assocSide.getName()?cap_first}(${otherClassName} ${otherClassName?uncap_first}, ${keyType} key, long valueGemId)")}
<#assign method><#if keyDerived>add<#else>put</#if></#assign>
<#assign keyArg><#if keyDerived>-1<#else>key</#if></#assign>
<#if assocSide.isLeft()>
  ${cdName}DAOFactory.getAssoc${assocName?cap_first}DAO().${method}${assocName?cap_first}(valueGemId, ${otherClassName?uncap_first}.getGemId(), ${keyArg});
<#else>
  ${cdName}DAOFactory.getAssoc${assocName?cap_first}DAO().${method}${assocName?cap_first}(${otherClassName?uncap_first}.getGemId(), valueGemId, ${keyArg});
</#if>
