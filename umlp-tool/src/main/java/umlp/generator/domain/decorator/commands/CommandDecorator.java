/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.domain.decorator.commands;

import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.cdbasis._ast.ASTCDCompilationUnit;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.logging.Log;
import umlp.generator.AbstractDecorator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CommandDecorator extends AbstractDecorator {

  public static final String DE_SER = "DeSer";

  protected Set<String> supportedDeSers = new LinkedHashSet<>();

  public CommandDecorator(GlobalExtensionManagement glex) {
    super(glex);

    // TODO: Why is there another set in BEDecorator?
    supportedDeSers.add("java.time.LocalDate");
    supportedDeSers.add("java.time.LocalDateTime");
    supportedDeSers.add("java.time.LocalTime");
  }

  @Override
  public void transform(ASTCDCompilationUnit cdAST, ASTCDCompilationUnit decoratedAST) {
    List<ASTCDClass> addedDeSers = new ArrayList<>();
    CreateDecorator createDecorator = new CreateDecorator(glex);
    createDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(createDecorator.getAddedDeSers());
    Log.debug("After decorator: CreateDecorator", CommandDecorator.class.getName());

    DeleteDecorator deleteDecorator = new DeleteDecorator(glex);
    deleteDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(deleteDecorator.getAddedDeSers());
    Log.debug("After decorator: DeleteDecorator", CommandDecorator.class.getName());

    GetDecorator getDecorator = new GetDecorator(glex);
    getDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(getDecorator.getAddedDeSers());
    Log.debug("After decorator: GetDecorator", CommandDecorator.class.getName());

    LoadAllWithIDsDecorator loadAllWithIDsDecorator = new LoadAllWithIDsDecorator(glex);
    loadAllWithIDsDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(loadAllWithIDsDecorator.getAddedDeSers());
    Log.debug("After decorator: LoadAllWithIDsDecorator", CommandDecorator.class.getName());

    SetDecorator setDecorator = new SetDecorator(glex, supportedDeSers);
    setDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(setDecorator.getAddedDeSers());
    Log.debug("After decorator: SetDecorator", CommandDecorator.class.getName());

    CalculateDecorator calculateDecorator = new CalculateDecorator(glex, supportedDeSers);
    calculateDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(calculateDecorator.getAddedDeSers());
    Log.debug("After decorator: CalculateDecorator", CommandDecorator.class.getName());

    AddDecorator addDecorator = new AddDecorator(glex, supportedDeSers);
    addDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(addDecorator.getAddedDeSers());
    Log.debug("After decorator: AddDecorator", CommandDecorator.class.getName());

    PutDecorator putDecorator = new PutDecorator(glex, supportedDeSers);
    putDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(putDecorator.getAddedDeSers());
    Log.debug("After decorator: PutDecorator", CommandDecorator.class.getName());

    RekeyDecorator rekeyDecorator = new RekeyDecorator(glex, supportedDeSers);
    rekeyDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(rekeyDecorator.getAddedDeSers());
    Log.debug("After decorator: RekeyDecorator", CommandDecorator.class.getName());

    RemoveDecorator removeDecorator = new RemoveDecorator(glex, supportedDeSers);
    removeDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(removeDecorator.getAddedDeSers());
    Log.debug("After decorator: RemoveDecorator", CommandDecorator.class.getName());

    GetFilteredDecorator getFilteredDecorator = new GetFilteredDecorator(glex);
    getFilteredDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(getFilteredDecorator.getAddedDeSers());
    Log.debug("After decorator: GetFilteredDecorator", CommandDecorator.class.getName());

    AssocCommandDecorator assocCommandDecorator = new AssocCommandDecorator(glex);
    assocCommandDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(assocCommandDecorator.getAddedDeSers());
    Log.debug("After decorator: AssocCommandDecorator", CommandDecorator.class.getName());

    ManagerSubscriptionDecorator managerSubscriptionDecorator = new ManagerSubscriptionDecorator(glex);
    managerSubscriptionDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(managerSubscriptionDecorator.getAddedDeSers());
    Log.debug("After decorator: ManagerSubscriptionCommandDecorator", CommandDecorator.class.getName());

    OwnCommandDecorator cmdDecorator = new OwnCommandDecorator(glex, supportedDeSers);
    cmdDecorator.transform(cdAST, decoratedAST);
    addedDeSers.addAll(cmdDecorator.getAddedDeSers());
    Log.debug("After decorator: CmdDecorator", CommandDecorator.class.getName());

    AggregatedCommandDeSerDecorator aggregatedCommandDeSerDecorator = new AggregatedCommandDeSerDecorator(glex, addedDeSers);
    aggregatedCommandDeSerDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: XCommandDeserDecorator", CommandDecorator.class.getName());

    DomainTypeFilterDecorator domainTypeFilterDecorator = new DomainTypeFilterDecorator(glex);
    domainTypeFilterDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: DomainTypeFilterDecorator", CommandDecorator.class.getName());

    EventSendingObserverDecorator managerEventSendingObserverDecorator = new EventSendingObserverDecorator(glex);
    managerEventSendingObserverDecorator.transform(cdAST, decoratedAST);
    Log.debug("After decorator: ManagerEventSendingObserverDecorator", CommandDecorator.class.getName());

  }

  public void addSupportedDeSer(String... supportedDeSers) {
    this.supportedDeSers.addAll(Arrays.asList(supportedDeSers));
  }
}
