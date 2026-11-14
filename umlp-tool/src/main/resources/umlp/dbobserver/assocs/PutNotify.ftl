<#-- (c) https://github.com/MontiCore/monticore -->
<#-- a value was put into a GemMap: stores the pair of owner and value; the key is not stored, it is derived
     from the value when loading (attribute qualified associations) -->
${tc.signature("cdName","className","assocName","assocSide", "otherClassName", "keyType")}
${cd4c.method("public void notifyPut${assocSide.getName()?cap_first}(${otherClassName} ${otherClassName?uncap_first}, ${keyType} key, long valueGemId)")}
<#if assocSide.isLeft()>
  ${cdName}DAOFactory.getAssoc${assocName?cap_first}DAO().add${assocName?cap_first}(valueGemId, ${otherClassName?uncap_first}.getGemId(), -1);
<#else>
  ${cdName}DAOFactory.getAssoc${assocName?cap_first}DAO().add${assocName?cap_first}(${otherClassName?uncap_first}.getGemId(), valueGemId, -1);
</#if>
