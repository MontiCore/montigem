<#-- (c) https://github.com/MontiCore/monticore -->
${tc.signature("daoFactoryName", "domainClasses", "assocNames", "allClasses","neo4j_activated")}
${cd4c.method("public static void initNeo4J()")}
<#if !neo4j_activated>
throw new RuntimeException("If you want to use Neo4j activate its generation.");
<#else>
${daoFactoryName} impl = new ${daoFactoryName}();

<#list allClasses as domainClass>
  umlp.backendrte.common.DatabaseAccessManager4Neo4j.executeStatement("CREATE INDEX range_gem_id_${domainClass.getName()} IF NOT EXISTS FOR (n:${domainClass.getName()}) ON (n.gemId)");
</#list>
<#-- the indexes are built in the background: until they are online, Neo4j may refuse to compile queries ("insanely
     frequent schema changes") -->
umlp.backendrte.common.DatabaseAccessManager4Neo4j.executeStatement("CALL db.awaitIndexes(300)");

<#list domainClasses as domainClass>
  impl.${domainClass.getName()?uncap_first}DAO = new ${domainClass.getName()}DAONeo4J();
</#list>
<#list assocNames as assocname>
  impl.assoc${assocname?cap_first}DAO = new Assoc${assocname?cap_first}DAONeo4J();
</#list>
${daoFactoryName}.instance = impl;
</#if>
