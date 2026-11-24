<#-- (c) https://github.com/MontiCore/monticore -->
<#-- keyAssocs: the type qualified associations whose keys may be the deleted object (their relationships are deleted;
     the key is a property, so DETACH DELETE does not reach them) -->
${tc.signature("cdName", "typeName", "associations", "keyAssocs")}
${cd4c.method("public void delete${typeName}(long gemId)")}
<#list keyAssocs as assoc>
${cdName}DAOFactory.getAssoc${assoc.getName()?cap_first}DAO().removeAllWithKey${assoc.getName()?cap_first}(gemId);
</#list>

if(delete${typeName} == null){
  delete${typeName} = "MATCH(n:${typeName} {gemId: $p1}) DETACH DELETE n";
}
DatabaseAccessManager4Neo4j.executeWrite(delete${typeName},Values.parameters("p1", gemId));

