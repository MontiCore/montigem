<#-- (c) https://github.com/MontiCore/monticore -->
<#-- updates the row of key or appends a new row -->
${tc.signature("tableName", "name", "keyType", "valueType", "keyDbType", "valueDbType")}
<#assign attrName=name?cap_first>
${cd4c.method("public void put${attrName}(long gemId, ${keyType} key, ${valueType} value, Map<${keyType}, ${valueType}> map)")}
if (put${attrName}Statement == null) {
  put${attrName}Statement = "UPDATE ${tableName} SET _value = ? WHERE id = ? AND _key = ?";
}
if (putNew${attrName}Statement == null) {
  putNew${attrName}Statement = "INSERT INTO ${tableName} (id, _key, _value, index) "
      + "SELECT ?, ?, ?, COALESCE(MAX(index), -1) + 1 FROM ${tableName} WHERE id = ?";
}
try (Connection connection = DatabaseAccessManager.getConnection();
     PreparedStatement st1 = connection.prepareStatement(put${attrName}Statement);
     PreparedStatement st2 = connection.prepareStatement(putNew${attrName}Statement)) {
  st1.clearParameters();
  ${includeArgs("umlp.dao.Set", "st1", 1, valueDbType, "value", false, "")};
  st1.setLong(2, gemId);
  ${includeArgs("umlp.dao.Set", "st1", 3, keyDbType, "key", false, "")};
  if (st1.executeUpdate() == 0) {
    st2.clearParameters();
    st2.setLong(1, gemId);
    ${includeArgs("umlp.dao.Set", "st2", 2, keyDbType, "key", false, "")};
    ${includeArgs("umlp.dao.Set", "st2", 3, valueDbType, "value", false, "")};
    st2.setLong(4, gemId);
    st2.executeUpdate();
  }
} catch (SQLException e) {
  throw new RuntimeException(e);
}
