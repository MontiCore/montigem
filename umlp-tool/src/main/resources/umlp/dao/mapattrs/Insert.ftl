<#-- (c) https://github.com/MontiCore/monticore -->
<#-- stores all entries of a new object -->
${tc.signature("tableName", "name", "keyType", "valueType", "keyDbType", "valueDbType")}
<#assign attrName=name?cap_first>
${cd4c.method("public void insert${attrName}(long gemId, Map<${keyType}, ${valueType}> map)")}
if (map.isEmpty()) {
  return;
}
if (insert${attrName}Statement == null) {
  insert${attrName}Statement = "INSERT INTO ${tableName} (id, _key, _value, index) VALUES (?, ?, ?, ?)";
}
try (Connection connection = DatabaseAccessManager.getConnection();
     PreparedStatement st = connection.prepareStatement(insert${attrName}Statement)) {
  long index = 0;
  for (Map.Entry<${keyType}, ${valueType}> entry : map.entrySet()) {
    st.clearParameters();
    st.setLong(1, gemId);
    ${includeArgs("umlp.dao.Set", "st", 2, keyDbType, "entry.getKey()", false, "")};
    ${includeArgs("umlp.dao.Set", "st", 3, valueDbType, "entry.getValue()", false, "")};
    st.setLong(4, index++);
    st.addBatch();
  }
  st.executeBatch();
} catch (SQLException e) {
  throw new RuntimeException(e);
}
