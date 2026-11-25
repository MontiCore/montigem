/* (c) https://github.com/MontiCore/monticore */
package umlp.backendrte.common;

import de.se_rwth.commons.logging.Log;
import org.neo4j.driver.Config;
import org.neo4j.driver.NotificationClassification;
import org.neo4j.driver.NotificationSeverity;
import org.neo4j.driver.Session;

import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.async.AsyncTransaction;
import org.neo4j.driver.async.ResultCursor;
import org.neo4j.driver.exceptions.Neo4jException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class DatabaseAccessManager4Neo4j extends DatabaseAccessManager {

  GemIdManager4Neo4j idManager4Neo4j = new GemIdManager4Neo4j();

  @Override
  protected long _getFreeGemId() {
    return idManager4Neo4j.getFreeGemId();
  }

  @Override
  protected void _clearLocalGemIds(){
    idManager4Neo4j.clear();
  }

  public static org.neo4j.driver.Driver driver;

  @Override
  protected void _setup() {
    loadDBPropertiesFromStandardFileLocation();
    String NEO4J_URI = databaseProperties.get("NEO4J_URI") != null? databaseProperties.get("NEO4J_URI").toString() : "bolt://localhost:7687";
    String NEO4J_USER = databaseProperties.get("NEO4J_USER") != null? databaseProperties.get("NEO4J_USER").toString() : "neo4j";
    String NEO4J_PASSWORD = databaseProperties.get("NEO4J_PASSWORD") != null? databaseProperties.get("NEO4J_PASSWORD").toString() : "";
    driver = org.neo4j.driver.GraphDatabase.driver(NEO4J_URI,
        org.neo4j.driver.AuthTokens.basic(
            NEO4J_USER, NEO4J_PASSWORD),
        Config.builder()
            .withMinimumNotificationSeverity(NotificationSeverity.OFF)  // NotificationSeverity.OFF to disable entirely
            .withDisabledNotificationClassifications(Set.of(NotificationClassification.HINT, NotificationClassification.GENERIC))  // filters categories as well
            .withoutEncryption()
            .withTelemetryDisabled(true)
            .build()
        );
    registerShutdownHook();
    Log.warn("New DB Connection created");
    createMetaNodeSingleton();
  }

  // Get a Read Session
  public static Session getSession() {
    // Commit all ongoing writes before doing a read
    synchronized (writeFireAndForgetTransaction) {
      // Before reading we want to be sure to have commited all open write queries
      writeFireAndForgetTransaction.close();
      return driver.session(SessionConfig.forDatabase("neo4j"));
    }
  }

  static final class TransactionHolder {
    AsyncTransaction fireAndForgetTransaction = null;
    int batchSize;
    int queryCounter = 0;

    TransactionHolder(int batchSize) {
      this.batchSize = batchSize;
    }

    // the queries sent in the open transaction, only read if its commit fails, see logRollback
    final List<CompletionStage<ResultCursor>> sentResults = new ArrayList<>();
    final List<String> sentQueries = new ArrayList<>();
    final List<Value> sentParameters = new ArrayList<>();

    synchronized void executeFireAndForget(String query) {
      ensureExistingTransaction();
      queryCounter++;
      remember(fireAndForgetTransaction.runAsync(query), query, null);
      if(queryCounter == batchSize) {
        close();
      }
    }

    synchronized void executeFireAndForget(String query, Value vals) {
      ensureExistingTransaction();
      queryCounter++;
      remember(fireAndForgetTransaction.runAsync(query, vals), query, vals);
      if(queryCounter == batchSize) {
        close();
      }
    }

    void remember(CompletionStage<ResultCursor> result, String query, Value vals) {
      sentResults.add(result);
      sentQueries.add(query);
      sentParameters.add(vals);
    }

    synchronized void ensureExistingTransaction() {
      if(fireAndForgetTransaction == null) {
        fireAndForgetTransaction = driver.session(AsyncSession.class).beginTransactionAsync()
            .toCompletableFuture().join();
      }
    }

    synchronized void close() {
      if(fireAndForgetTransaction != null) {
        Throwable[] failure = new Throwable[1];
        fireAndForgetTransaction.commitAsync().exceptionally(ex -> {
          failure[0] = ex;
          return null;
        }).toCompletableFuture().join();
        try {
          if (failure[0] != null) {
            logRollback(failure[0]);
          }
        } finally {
          // also if Log.error throws (fail quick): the next write starts a new transaction
          sentResults.clear();
          sentQueries.clear();
          sentParameters.clear();
          fireAndForgetTransaction = null;
        }
      }
    }

    /**
     * all queries of the transaction are lost: once one of them fails, Neo4j ignores the following ones and the commit
     * fails ("Commit exchange contains ignored messages"); names the query that failed first and its error
     */
    void logRollback(Throwable commitFailure) {
      for (int i = 0; i < sentResults.size(); i++) {
        try {
          sentResults.get(i).thenCompose(ResultCursor::consumeAsync).toCompletableFuture().join();
        } catch (RuntimeException ex) {
          String error = describe(ex);
          if (!error.contains("ignored")) {
            Log.error("0xDB010 Neo4j rolled back the " + sentResults.size() + " queries of a transaction: query "
                + (i + 1) + " failed: " + sentQueries.get(i) + " (parameters "
                + (sentParameters.get(i) == null ? "none" : sentParameters.get(i).keys()) + "): " + error);
            return;
          }
        }
      }
      Log.error("0xDB010 Neo4j rolled back the " + sentResults.size() + " queries of a transaction: "
          + describe(commitFailure));
    }

    static String describe(Throwable ex) {
      Throwable t = ex;
      while (t instanceof CompletionException && t.getCause() != null) {
        t = t.getCause();
      }
      String code = t instanceof Neo4jException ? ((Neo4jException) t).code() + " " : "";
      return code + t.getMessage();
    }
  }

  static final TransactionHolder writeFireAndForgetTransaction = new TransactionHolder(2000);

  public static void executeWrite(String cypherQuery, Value values) {
    synchronized (writeFireAndForgetTransaction) {
      writeFireAndForgetTransaction.executeFireAndForget(cypherQuery, values);
    }
  }

  public static void executeWrite(String cypherQuery) {
    synchronized (writeFireAndForgetTransaction) {
      writeFireAndForgetTransaction.executeFireAndForget(cypherQuery);
    }
  }

  @Override
  protected void _executeStatement(String query) {
    executeWrite(query);
  }

  /**
   * This method is called once when the DB is started to create a MetaNode which only contains a GemID Counter for now.
   * The MetaNode is a singleton and is only created, if it does not exist already (DB was initialized beforehand).
   */
  protected void createMetaNodeSingleton() {
    boolean createMetaNode = false;
    try(Session session = DatabaseAccessManager4Neo4j.getSession()) {
      createMetaNode = !session.run("MATCH(n:METANODE) return n").hasNext();
    }
    if(createMetaNode) {
      executeWrite("CREATE (n:METANODE {gemIdCounter: 0})");
    }
  }

  public static void executeStatement(String cypher){
    try(Session session = getSession()){
      session.run(cypher);
    }
  }


  public static void init() {
    DatabaseAccessManager4Neo4j impl = new DatabaseAccessManager4Neo4j();
    DatabaseAccessManager.setInstance(impl);
    impl._setup();
  }

  public static void initBoltLocalhost(int port, String user, String password) {
    DatabaseAccessManager4Neo4j impl = new DatabaseAccessManager4Neo4j();
    DatabaseAccessManager.setInstance(impl);
    impl._setupBoltLocalhost(port, user, password);
  }

  protected void _setupBoltLocalhost(int port, String user, String password) {
    driver = org.neo4j.driver.GraphDatabase.driver("bolt://localhost:"+port,
          org.neo4j.driver.AuthTokens.basic(
              user, password));
    registerShutdownHook();
    Log.warn("New DB Connection created");
    createMetaNodeSingleton();
  }

  protected void registerShutdownHook(){
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutdown hook triggered - closing database connection");
      DatabaseAccessManager.closeConnection();
    }));
  }

  @Override
  public void _closeConnection(){
    writeFireAndForgetTransaction.close();
    driver.close();
  }

}
