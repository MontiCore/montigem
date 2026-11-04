<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("oneSideName", "otherSideName", "oneSideSQLName","otherSideSQLName", "sqlTable", "keyColumn")}
<#assign methodName = otherSideName?cap_first+"IdsWithKeysFor"+oneSideName?cap_first>
${cd4c.method("public void ensureSelect${methodName}Created()")}
select${methodName} = "SELECT ${otherSideSQLName}_id, ${keyColumn} FROM ${sqlTable}<#t>
<#lt> WHERE ${oneSideSQLName}_id = ?";
