<#-- (c) https://github.com/MontiCore/monticore -->
<#-- type qualified association: inserts the pair with its key, or updates the key of the pair (a value moved to
     another key of the same owner) -->
${tc.signature("assocName", "leftSideSQLName", "rightSideSQLName", "sqlTable", "keyColumn")}
${cd4c.method("public void ensurePut${assocName?cap_first}Created()")}
  if(put${assocName?cap_first} == null) {
    put${assocName?cap_first} =
      "MERGE INTO ${sqlTable} " +
      "USING ( VALUES (?, ?, ?) ) AS src (lhs, rhs, qualifier) " +
      "ON ${sqlTable}.${leftSideSQLName}_id = src.lhs AND ${sqlTable}.${rightSideSQLName}_id = src.rhs " +
      "WHEN MATCHED THEN " +
      "    UPDATE SET ${keyColumn} = src.qualifier " +
      "WHEN NOT MATCHED THEN " +
      "    INSERT (${leftSideSQLName}_id, ${rightSideSQLName}_id, ${keyColumn}) " +
      "    VALUES (src.lhs, src.rhs, src.qualifier)";
  }
