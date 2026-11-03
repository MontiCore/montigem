<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the values are observed by the managers (key observers, see GemMapKeyRole), not by the map -->
${tc.signature("type")}
${cd4c.constructor("public ${type}(long ownerGemID)")}
super(null);
this.ownerGemID = ownerGemID;
