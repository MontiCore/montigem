<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("cdName","className","assocName","assocSide", "otherClassName", "keyType")}
${cd4c.method("public void notifyPut${assocSide.getName()?cap_first}(${otherClassName} ${otherClassName?uncap_first}, ${keyType} key, long valueGemId)")}
  this.consumer.accept(null);
