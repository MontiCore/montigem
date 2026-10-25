<#-- (c) https://github.com/MontiCore/monticore -->
<#-- mapType: the RTE type of the map (also fits a GemMap that is extended by hand-written code via TOP) -->
${tc.signature("className", "mapType")}
${cd4c.constructor("public ${className}(${mapType} map)")}
this.map = map;
