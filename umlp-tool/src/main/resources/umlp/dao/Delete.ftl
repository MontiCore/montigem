<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyAssocs: the type qualified associations whose keys may be the deleted object (their pairs are deleted) -->
${tc.signature("cdName","tableName", "typeName", "associations", "assocsToDefinitionDiagramMap", "keyAssocs")}
${cd4c.method("public void delete${typeName}(long gemId)")}

delete${typeName} = "DELETE FROM ${tableName} WHERE id = ?";

try(Connection connection = DatabaseAccessManager.getConnection(); PreparedStatement st = connection.prepareStatement(delete${typeName}); ){
  <#list associations as assocSide, assoc>
      <#if assocSide.isLeft()>
          <#assign otherSide=assoc.getRight().getName()>
      <#else>
          <#assign otherSide=assoc.getLeft().getName()>
      </#if>
  ${assocsToDefinitionDiagramMap[assoc.getName()]}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().removeAllEntriesRelatedTo${otherSide?cap_first}(gemId);
  </#list>
  <#list keyAssocs as assoc>
  ${cdName}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().removeAllWithKey${assoc.getName()?cap_first}(gemId);
  </#list>
  synchronized(st){
    st.clearParameters();
    st.setLong(1, gemId);
    st.executeUpdate();
  }

  }catch (SQLException e){
  throw new RuntimeException(e);
}
