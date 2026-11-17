<#-- (c) https://github.com/MontiCore/monticore -->
<#-- deletes the row of key -->
${tc.signature("tableName", "name", "keyType", "valueType", "keyDbType")}
<#assign attrName=name?cap_first>
${cd4c.method("public void remove${attrName}(long gemId, ${keyType} key, Map<${keyType}, ${valueType}> map)")}
if (remove${attrName}Statement == null) {
  remove${attrName}Statement = "DELETE FROM ${tableName} WHERE id = ? AND _key = ?";
}
try (Connection connection = DatabaseAccessManager.getConnection();
     PreparedStatement st = connection.prepareStatement(remove${attrName}Statement)) {
  st.clearParameters();
  st.setLong(1, gemId);
  ${includeArgs("umlp.dao.Set", "st", 2, keyDbType, "key", false, "")};
  st.executeUpdate();
} catch (SQLException e) {
  throw new RuntimeException(e);
}
