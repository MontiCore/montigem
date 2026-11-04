<#-- (c) https://github.com/MontiCore/monticore -->
<#-- stores the pair of a type qualified association with its key, replacing the key the pair had -->
${tc.signature("association", "keyType")}
${cd4c.method("public abstract void put${association.getName()?cap_first}(long ${association.getLeft().getName()}GemId, long ${association.getRight().getName()}GemId, ${keyType} key)")}
