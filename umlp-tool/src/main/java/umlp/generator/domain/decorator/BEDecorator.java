/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator;

import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.monticore.generating.templateengine.TemplateHookPoint;
import de.monticore.io.paths.MCPath;
import de.monticore.ocl.ocl._ast.ASTOCLCompilationUnit;
import de.se_rwth.commons.logging.Log;
import umlp.generator.AbstractDecorator;
import umlp.generator.domain.UMLPGeneratorConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BEDecorator extends AbstractDecorator {

  boolean neo4j_as_default_database = false;
  boolean generate_neo4j_daos = false;

  boolean spring = false;
  boolean shell = false;

  protected MCPath hwPath;
  protected List<ASTOCLCompilationUnit> oclCompilationUnits;

  protected Set<String> supportedDesers = new LinkedHashSet<>();

  public BEDecorator(GlobalExtensionManagement glex, MCPath hwPath) {
    this(glex, hwPath, List.of());
  }

  public BEDecorator(GlobalExtensionManagement glex, MCPath hwPath,
                     List<ASTOCLCompilationUnit> oclCompilationUnits) {
    super(glex);
    this.hwPath = hwPath;
    this.oclCompilationUnits = new ArrayList<>(oclCompilationUnits);

    initExtraDeSers();

    if(glex.hasGlobalVar("UMLPGeneratorConfig")){
      spring = UMLPGeneratorConfig.getInstance().isUseSpring();
      shell = UMLPGeneratorConfig.getInstance().isUseShell();
      generate_neo4j_daos = UMLPGeneratorConfig.getInstance().isGenNeo4j();
      neo4j_as_default_database = UMLPGeneratorConfig.getInstance().isUseNeo4j();

      if(neo4j_as_default_database & !generate_neo4j_daos) {
        Log.error("If you want to use neo4j as default database, then neo4j generation has to be activated.");
      }
    }
  }

  private void initExtraDeSers() {
    // TODO: Why is there another set in CommandDecorator?
    supportedDesers.addAll(getStandardSupportedDeSers());
  }

  public static Set<String> getStandardSupportedDeSers() {
    Set<String> standardSupportedDeSers = new LinkedHashSet<>();

    standardSupportedDeSers.add("java.time.LocalDate");
    standardSupportedDeSers.add("java.time.LocalDateTime");
    standardSupportedDeSers.add("java.time.LocalTime");
    return standardSupportedDeSers;
  }

  @Override
  public void transform(ASTCDCompilationUnit cdAST, ASTCDCompilationUnit decoratedAST) {
    usedDecorators.clear();

    DomainClassDecorator domainClassDecorator;

    domainClassDecorator = new DomainClassDecorator(glex, hwPath, oclCompilationUnits);
    domainClassDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: CreateDecorator", BEDecorator.class.getName());

    DomainInterfaceDecorator domainInterfaceDecorator = new DomainInterfaceDecorator(glex);
    domainInterfaceDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: CreateDecorator", BEDecorator.class.getName());

    DAOFactoryDecorator daoFactoryDecorator = new DAOFactoryDecorator(glex, generate_neo4j_daos);
    daoFactoryDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DAOFactoryDecorator", BEDecorator.class.getName());

    DAOInterfaceDecorator daoInterfaceDecorator = new DAOInterfaceDecorator(glex);
    daoInterfaceDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DAOInterfaceDecorator", BEDecorator.class.getName());

    QueryBuilderDecorator queryBuilderDecorator = new QueryBuilderDecorator(glex);
    queryBuilderDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: QueryBuilderDecorator", BEDecorator.class.getName());

    // Generate DAOs
    DAODecorator daoDecorator = new DAODecorator(glex);
    daoDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DAODecorator", BEDecorator.class.getName());

    if(this.generate_neo4j_daos) {
      DAONeo4jDecorator daoNeo4jDecorator = new DAONeo4jDecorator(glex);
      daoNeo4jDecorator.transform(cdAST, decoratedAST);
      Log.debug("After decorator: DAONeo4jDecorator", BEDecorator.class.getName());
    }

    // Generate DeSer
    DeSerDecorator deSerDecorator = new DeSerDecorator(glex, hwPath, supportedDesers);
    deSerDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DeSerDecorator", BEDecorator.class.getName());

    // Generate builder classes
    BuilderDecorator builderDecorator = new BuilderDecorator(glex);
    builderDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: BuilderDecorator", BEDecorator.class.getName());

    // Generate delegated/concretized builder classes
    DelegatedBuilderDecorator delegatedBuilderDecorator = new DelegatedBuilderDecorator(glex);
    delegatedBuilderDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DelegatedBuilderDecorator", BEDecorator.class.getName());


    // Generate DeSer builder classes
    DeSerBuilderDecorator deSerBuilderDecorator = new DeSerBuilderDecorator(glex, hwPath);
    deSerBuilderDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DeSerBuilderDecorator", BEDecorator.class.getName());

    // Generate ErrorCollector classes
    ErrorCollectorDecorator errorCollectorDecorator= new ErrorCollectorDecorator(glex);
    errorCollectorDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ErrorCollectorDecorator", BEDecorator.class.getName());

    ObserverDecorator observerDecorator = remember(new ObserverDecorator(glex));
    observerDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ObserverDecorator", BEDecorator.class.getName());

    ManagerDbObserverDecorator managerDbObserverDecorator = new ManagerDbObserverDecorator(glex);
    managerDbObserverDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ManagerDbObserverDecorator", BEDecorator.class.getName());

    DbObserverDecorator dbObserverDecorator= new DbObserverDecorator(glex);
    dbObserverDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DbObserverDecorator", BEDecorator.class.getName());

    CommandSendingObserverDecorator cmdSendingDecorator = new CommandSendingObserverDecorator(glex);
    cmdSendingDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: CommandSendingObserverDecorator", BEDecorator.class.getName());

    CommandObserverDecorator cmdDecorator = new CommandObserverDecorator(glex);
    cmdDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: CommandObserverDecorator", BEDecorator.class.getName());

    AccessPolicyDecorator accessDecorator = new AccessPolicyDecorator(glex);
    accessDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: AccessPolicyDecorator", BEDecorator.class.getName());

    // Generate DiagramObserver classes
    DiagramObserverDecorator diagramObserverDecorator = remember(new DiagramObserverDecorator(glex));
    diagramObserverDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DiagramObserverDecorator", BEDecorator.class.getName());

    // Generate manager classes
    ManagerDecorator managerDecorator = new ManagerDecorator(glex);
    managerDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ManagerDecorator", BEDecorator.class.getName());

    DelegationManagerDecorator delegationManagerDecorator = new DelegationManagerDecorator(glex);
    delegationManagerDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DelegationManagerDecorator", BEDecorator.class.getName());

    ClientDelegationManagerDecorator clientDelegationManagerDecorator = new ClientDelegationManagerDecorator(glex);
    clientDelegationManagerDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ClientDelegationManagerDecorator", BEDecorator.class.getName());

    ManagerImplDecorator managerImplDecorator = new ManagerImplDecorator(glex);
    managerImplDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ManagerImplDecorator", BEDecorator.class.getName());

    ManagerClientImplDecorator managerClientImplDecorator = new ManagerClientImplDecorator(glex);
    managerClientImplDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ManagerClientImplDecorator", BEDecorator.class.getName());

    ManagerMockDecorator managerMockDecorator = new ManagerMockDecorator(glex);
    managerMockDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ManagerMockDecorator", BEDecorator.class.getName());

    DummyProviderDecorator dummyProviderDecorator = new DummyProviderDecorator(glex);
    dummyProviderDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DummyProviderDecorator", BEDecorator.class.getName());

    ErrorObserverDecorator errorObserverDecorator = new ErrorObserverDecorator(glex);
    errorObserverDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ErrorObserverDecorator", BEDecorator.class.getName());

    GemListAssocDecorator gemListAssocDecorator = new GemListAssocDecorator(glex);
    gemListAssocDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: GemListAssocDecorator", BEDecorator.class.getName());

    GemMapAssocDecorator gemMapAssocDecorator = new GemMapAssocDecorator(glex);
    gemMapAssocDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: GemMapAssocDecorator", BEDecorator.class.getName());

    GemListDomainDecorator gemListDomainDecorator = new GemListDomainDecorator(glex);
    gemListDomainDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: GemListDomainDecorator", BEDecorator.class.getName());

    CommandObjectMapperDecorator commandObjectMapperDecorator = new CommandObjectMapperDecorator(glex);
    commandObjectMapperDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: CommandObjectMapperDecorator", BEDecorator.class.getName());

    BaseClassObjectMapperDecorator baseClassObjectMapperDecorator = new BaseClassObjectMapperDecorator(glex);
    baseClassObjectMapperDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: BaseClassObjectMapperDecorator", BEDecorator.class.getName());

    CommandResultDeSerDecorator commandResultDeSerDecorator = new CommandResultDeSerDecorator(glex);
    commandResultDeSerDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: CommandResultDeSerDecorator", BEDecorator.class.getName());

    // ALLObserver
    AllObserverDecorator allObserverDecorator= new AllObserverDecorator(glex);
    allObserverDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: AllObserverDecorator", BEDecorator.class.getName());


    if (shell) {
      ShellAPIDecorator shellAPIDecorator = new ShellAPIDecorator(glex);
      shellAPIDecorator.transform(cdAST, decoratedAST);
      Log.debug("After decorator: ShellAPIDecorator", ShellAPIDecorator.class.getName());

      ShellAPIClassDecorator shellApiClassDecorator = new ShellAPIClassDecorator(glex, hwPath, supportedDesers);
      shellApiClassDecorator.transform(cdAST, decoratedAST);
      Log.debug("After decorator: ShellAPIClassDecorator", ShellAPIClassDecorator.class.getName());
    }

    if (spring) {
      ServerApplicationDecorator serverApplicationDecorator = new ServerApplicationDecorator(glex, neo4j_as_default_database);
      serverApplicationDecorator.transform(cdAST, decoratedAST);
      Log.debug("After decorator: ServerApplicationDecorator", BEDecorator.class.getName());

      ShellApplicationDecorator shellApplicationDecorator = new ShellApplicationDecorator(glex);
      shellApplicationDecorator.transform(cdAST, decoratedAST);
      Log.debug("After decorator: ShellApplicationDecorator", BEDecorator.class.getName());
    }

    glex.replaceTemplate("cd2java.Imports", new TemplateHookPoint("umlp.Imports"));
  }


  public void addSupportedDeSer(String... deSers) {
    this.supportedDesers.addAll(Arrays.asList(deSers));
  }

  public void setNeo4j(boolean neo4j_as_default_database) {
    this.neo4j_as_default_database = neo4j_as_default_database;
    if (neo4j_as_default_database) {
      this.setGenerateNeo4jDaos(true);
    }
  }

  public void setGenerateNeo4jDaos(boolean generate_neo4j_daos) {
    this.generate_neo4j_daos = generate_neo4j_daos;
  }

  public void setSpring(boolean spring) {
    this.spring = spring;
  }

  public void setShell(boolean shell) {
    this.shell = shell;
  }
}
