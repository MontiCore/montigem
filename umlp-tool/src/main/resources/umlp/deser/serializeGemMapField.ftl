<#-- (c) https://github.com/MontiCore/monticore -->
<#-- the ids of the values of an attribute qualified association, their keys are derived from them -->
${tc.signature("className", "fieldName")}
${cd4c.method("protected void serialize" + fieldName?cap_first + "(" + className + " obj, JsonPrinter printer)")}
  printer.beginArray("${fieldName}");
  for(long gemId : obj._internal_get${fieldName?cap_first}GemMap().getValueIDs()){
      printer.value(gemId);
  }
  printer.endArray();
