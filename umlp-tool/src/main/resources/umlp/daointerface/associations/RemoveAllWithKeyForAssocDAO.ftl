<#-- (c) https://github.com/MontiCore/monticore -->
<#-- removes the pairs of a type qualified association whose key is the object with keyGemId (deleted key object) -->
${tc.signature("association")}
${cd4c.method("public abstract void removeAllWithKey${association.getName()?cap_first}(long keyGemId)")}
