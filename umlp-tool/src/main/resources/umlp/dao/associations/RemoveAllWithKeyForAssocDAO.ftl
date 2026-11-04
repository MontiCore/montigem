<#-- (c) https://github.com/MontiCore/monticore -->
<#-- removes the pairs of a type qualified association whose key is the object with keyGemId (deleted key object) -->
${tc.signature("association", "sqlTable", "keyColumn")}
${cd4c.method("public void removeAllWithKey${association.getName()?cap_first}(long keyGemId)")}
  String removeAllWithKey = "DELETE FROM ${sqlTable} WHERE ${keyColumn} = ?";
  try(Connection connection = DatabaseAccessManager.getConnection(); PreparedStatement st = connection.prepareStatement(removeAllWithKey)){
    st.clearParameters();
    st.setLong(1, keyGemId);
    st.executeUpdate();
  }catch (SQLException e){
    throw new RuntimeException(e);
  }
