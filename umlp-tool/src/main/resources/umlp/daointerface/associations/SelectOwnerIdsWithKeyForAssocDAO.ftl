<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the gemIds of the owners that hold an entry with the key object keyGemId (bidirectional type qualified
     association), see dao/associations/SelectOwnerIdsWithKeyForAssocDAO -->
${tc.signature("association")}
${cd4c.method("public abstract java.util.List<Long> selectOwnerIdsWithKey${association.getName()?cap_first}(long keyGemId)")}
