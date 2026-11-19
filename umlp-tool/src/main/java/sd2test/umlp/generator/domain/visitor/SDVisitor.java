package sd2test.umlp.generator.domain.visitor;

import de.monticore.ast.ASTNode;
import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.lang.sd4development.SD4DevelopmentMill;
import de.monticore.lang.sd4development._ast.ASTSDCall;
import de.monticore.lang.sd4development._ast.ASTSDClass;
import de.monticore.lang.sd4development._ast.ASTSDCondition;
import de.monticore.lang.sd4development._ast.ASTSDEndCall;
import de.monticore.lang.sd4development._ast.ASTSDIncompleteExpression;
import de.monticore.lang.sd4development._ast.ASTSDNew;
import de.monticore.lang.sd4development._ast.ASTSDReturn;
import de.monticore.lang.sd4development._ast.ASTSDThrow;
import de.monticore.lang.sd4development._ast.ASTSDVariableDeclaration;
import de.monticore.lang.sd4development._visitor.SD4DevelopmentVisitor2;
import de.monticore.lang.sdbasis._ast.ASTSDActivityBar;
import de.monticore.lang.sdbasis._ast.ASTSDBody;
import de.monticore.lang.sdbasis._ast.ASTSDCompleteModifier;
import de.monticore.lang.sdbasis._ast.ASTSDFreeModifier;
import de.monticore.lang.sdbasis._ast.ASTSDInitialModifier;
import de.monticore.lang.sdbasis._ast.ASTSDObject;
import de.monticore.lang.sdbasis._ast.ASTSDObjectSource;
import de.monticore.lang.sdbasis._ast.ASTSDObjectTarget;
import de.monticore.lang.sdbasis._ast.ASTSDSendMessage;
import de.monticore.lang.sdbasis._ast.ASTSDVisibleModifier;
import de.monticore.lang.sdbasis._ast.ASTSequenceDiagram;
import de.monticore.lang.sdbasis._visitor.SDBasisVisitor2;
import de.monticore.ocl.oclexpressions.OCLExpressionsMill;
import de.monticore.ocl.oclexpressions._visitor.OCLExpressionsVisitor2;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import de.monticore.types.mcbasictypes._visitor.MCBasicTypesVisitor2;
import de.se_rwth.commons.SourcePosition;
import de.se_rwth.commons.logging.Log;
import sd2test.umlp.generator.domain.visitor.model.InteractionExtractable;
import sd2test.umlp.generator.domain.visitor.model.MatchingStrategy;
import sd2test.umlp.generator.domain.visitor.model.SDObject;
import sd2test.umlp.generator.domain.visitor.model.SDTestExtractable;
import sd2test.umlp.generator.domain.visitor.model.StateExtractedInteraction;
import sd2test.umlp.generator.domain.visitor.model.Trigger;
import sd2test.umlp.generator.domain.visitor.model.interaction.Actor;
import sd2test.umlp.generator.domain.visitor.model.interaction.ConstructorCallInteraction;
import sd2test.umlp.generator.domain.visitor.model.interaction.Interaction;
import sd2test.umlp.generator.domain.visitor.model.interaction.InteractionVariantMatcher;
import sd2test.umlp.generator.domain.visitor.model.interaction.MethodCallInteraction;
import sd2test.umlp.generator.domain.visitor.model.interaction.ReturnInteraction;
import sd2test.umlp.generator.domain.visitor.model.interaction.ReturnValue;
import sd2test.umlp.generator.domain.visitor.model.interaction.ThrowInteraction;
import sd2test.umlp.generator.domain.visitor.model.ocl.AssertStatement;
import sd2test.umlp.generator.domain.visitor.model.ocl.LetStatement;
import sd2test.umlp.generator.domain.visitor.model.ocl.OCLStatement;
import sd2test.umlp.generator.domain.visitor.state.AssertStatementState;
import sd2test.umlp.generator.domain.visitor.state.AwaitActionState;
import sd2test.umlp.generator.domain.visitor.state.AwaitConstructorSourceState;
import sd2test.umlp.generator.domain.visitor.state.BodySectionState;
import sd2test.umlp.generator.domain.visitor.state.ClosedActivityBarState;
import sd2test.umlp.generator.domain.visitor.state.ConstructorCallState;
import sd2test.umlp.generator.domain.visitor.state.DiagramState;
import sd2test.umlp.generator.domain.visitor.state.EndMessageAwaitSourceState;
import sd2test.umlp.generator.domain.visitor.state.EndMessageAwaitTargetState;
import sd2test.umlp.generator.domain.visitor.state.FinishedState;
import sd2test.umlp.generator.domain.visitor.state.ImportState;
import sd2test.umlp.generator.domain.visitor.state.InitialState;
import sd2test.umlp.generator.domain.visitor.state.InteractionState;
import sd2test.umlp.generator.domain.visitor.state.MethodCallState;
import sd2test.umlp.generator.domain.visitor.state.OCLAttachableState;
import sd2test.umlp.generator.domain.visitor.state.OpenActivityBarState;
import sd2test.umlp.generator.domain.visitor.state.PreInteractionBodyState;
import sd2test.umlp.generator.domain.visitor.state.SDObjectDeclarationState;
import sd2test.umlp.generator.domain.visitor.state.SDObjectSectionState;
import sd2test.umlp.generator.domain.visitor.state.SendMessageAwaitSourceState;
import sd2test.umlp.generator.domain.visitor.state.SendMessageAwaitTargetState;
import sd2test.umlp.generator.domain.visitor.state.VariableDeclarationState;
import sd2test.umlp.generator.domain.visitor.state.VisitorState;
import sd2test.umlp.generator.domain.visitor.state.VisitorStateVariantMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class SDVisitor implements SDBasisVisitor2,
                                        SD4DevelopmentVisitor2,
                                        OCLExpressionsVisitor2,
                                        MCBasicTypesVisitor2 {

  private VisitorState state = new InitialState();

  @Override
  public void visit(ASTMCImportStatement node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenInitial(InitialState initial) {
        initial.imports().add(node);
        return new ImportState(initial);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void endVisit(ASTMCImportStatement node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenImport(ImportState importState) {
        return importState.initialState();
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedEndVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSequenceDiagram node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenInitial(InitialState initial) {
        return new DiagramState(initial);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDObject node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenDiagram(DiagramState diagram) {
        var objectSection = new SDObjectSectionState(diagram);
        return whenSDObjectSection(objectSection);
      }

      @Override
      public VisitorState whenSDObjectSection(SDObjectSectionState objectSection) {
        var name = node.getName();
        String qualifiedTypeName = node.isPresentMCObjectType() ?
            node.getMCObjectType().printType() :
            raiseError("missing variable type", getSourcePosition(node));
        return new SDObjectDeclarationState(objectSection, name, qualifiedTypeName);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void endVisit(ASTSDObject node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenSDObjectDeclaration(SDObjectDeclarationState sdObjectDeclaration) {
        var matchingStrategy = sdObjectDeclaration.matchingStrategy();
        var name = sdObjectDeclaration.name();
        var qualifiedTypeName = sdObjectDeclaration.qualifiedTypeName();
        var sdObject = new SDObject(matchingStrategy, name, qualifiedTypeName);
        var sdObjectSection = sdObjectDeclaration.section();
        sdObjectSection.sdObjects().add(sdObject);
        return sdObjectSection;
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedEndVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDCompleteModifier node) {
    updateMatchingStrategy(MatchingStrategy.COMPLETE, node);
  }

  @Override
  public void visit(ASTSDFreeModifier node) {
    updateMatchingStrategy(MatchingStrategy.FREE, node);
  }

  @Override
  public void visit(ASTSDInitialModifier node) {
    updateMatchingStrategy(MatchingStrategy.INITIAL, node);
  }

  @Override
  public void visit(ASTSDVisibleModifier node) {
    updateMatchingStrategy(MatchingStrategy.VISIBLE, node);
  }

  private void updateMatchingStrategy(MatchingStrategy matchingStrategy, ASTNode node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenDiagram(DiagramState diagram) {
        return diagram.matchingStrategy(matchingStrategy);
      }

      @Override
      public VisitorState whenSDObjectDeclaration(SDObjectDeclarationState sdObjectDeclaration) {
        return sdObjectDeclaration.matchingStrategy(matchingStrategy);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDBody node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenSDObjectSection(SDObjectSectionState objectSectionState) {
        return new PreInteractionBodyState(objectSectionState);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDCondition node) {
    var expression = node.getExpression();
    var expressionString = OCLExpressionsMill.prettyPrint(expression, false);
    var assertStatement = new AssertStatement(expressionString);

    visitOCLStatement(assertStatement, node, AssertStatementState::new);
  }

  @Override
  public void endVisit(ASTSDCondition node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenAssertStatement(AssertStatementState assertStatement) {
        return assertStatement.previousState();
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedEndVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDVariableDeclaration node) {
    var qualifiedTypeName = node.getMCType().printType();
    var name = node.getName();
    var assignmentExpression = OCLExpressionsMill.prettyPrint(node.getAssignment(), false);
    var letStatement = new LetStatement(qualifiedTypeName, name, assignmentExpression);

    visitOCLStatement(letStatement, node, VariableDeclarationState::new);
  }

  @Override
  public void endVisit(ASTSDVariableDeclaration node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenVariableDeclaration(VariableDeclarationState variableDeclaration) {
        return variableDeclaration.previousState();
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedEndVisitError(node, state);
      }
    });
  }

  private <T extends OCLStatement> void visitOCLStatement(T statement,
                                                          ASTNode node,
                                                          Function<OCLAttachableState,
                                                              VisitorState> stateWrapper) {
    updateState(new VisitorStateVariantMatcher<VisitorState>() {
      @Override
      public VisitorState whenPreInteractionBody(PreInteractionBodyState preInteractionBody) {
        return addOCLStatementAndWrap(preInteractionBody);
      }

      @Override
      public VisitorState whenInteraction(InteractionState interaction) {
        return addOCLStatementAndWrap(interaction);
      }

      @Override
      public VisitorState whenClosedActivityBar(ClosedActivityBarState closedActivityBar) {
        return addOCLStatementAndWrap(closedActivityBar);
      }

      @Override
      public VisitorState whenOpenActivityBar(OpenActivityBarState openActivityBar) {
        return addOCLStatementAndWrap(openActivityBar);
      }

      @Override
      public VisitorState whenMethodCall(MethodCallState methodCall) {
        var previousState = methodCall.previousState();
        var interaction = methodCall.interaction();
        var interactionState = new InteractionState(previousState, interaction);
        return addOCLStatementAndWrap(interactionState);
      }

      @Override
      public VisitorState whenConstructorCall(ConstructorCallState constructorCall) {
        var previousState = constructorCall.previousState();
        var interaction = constructorCall.interaction();
        var interactionState = new InteractionState(previousState, interaction);
        return addOCLStatementAndWrap(interactionState);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }

      VisitorState addOCLStatementAndWrap(OCLAttachableState state) {
        state.oclStatements().add(statement);
        return stateWrapper.apply(state);
      }
    });
  }

  @Override
  public void visit(ASTSDSendMessage node) {
    visitInteractionStart(node, SendMessageAwaitSourceState::new);
  }

  @Override
  public void visit(ASTSDEndCall node) {
    visitInteractionStart(node, EndMessageAwaitTargetState::new);
  }

  private void visitInteractionStart(ASTNode node,
                                     Function<VisitorState, VisitorState> stateWrapper) {
    var sourcePosition = getSourcePosition(node);
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenPreInteractionBody(PreInteractionBodyState preInteractionBody) {
        return stateWrapper.apply(preInteractionBody);
      }

      @Override
      public VisitorState whenMethodCall(MethodCallState methodCall) {
        return finish(methodCall.extractInteraction());
      }

      @Override
      public VisitorState whenConstructorCall(ConstructorCallState constructorCall) {
        return finish(constructorCall.extractInteraction());
      }

      @Override
      public VisitorState whenInteraction(InteractionState interaction) {
        return finish(interaction.extractInteraction());
      }

      @Override
      public VisitorState whenOpenActivityBar(OpenActivityBarState openActivityBar) {
        return stateWrapper.apply(openActivityBar);
      }

      @Override
      public VisitorState whenClosedActivityBar(ClosedActivityBarState closedActivityBar) {
        return finish(closedActivityBar.extractInteraction());
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }

      private VisitorState finish(StateExtractedInteraction extractedInteraction) {
        var interaction = extractedInteraction.interaction();
        var previousState = extractedInteraction.previousState();
        var newPreviousState = finishInteraction(interaction, previousState, sourcePosition);
        return stateWrapper.apply(newPreviousState);
      }
    });
  }

  private VisitorState finishInteraction(Interaction interaction,
                                         VisitorState previousState,
                                         Optional<SourcePosition> sourcePosition) {
    var triggerCall =
        interaction.matchVariant(new InteractionVariantMatcher<Optional<MethodCallInteraction>,
            RuntimeException>() {

      @Override
      public Optional<MethodCallInteraction> whenMethodCall(MethodCallInteraction methodCall) {
        return Optional.of(methodCall).filter(MethodCallInteraction::isTrigger);
      }

      @Override
      public Optional<MethodCallInteraction> otherwise(Interaction interaction) {
        return Optional.empty();
      }
    });
    return previousState.matchVariant(triggerCall.isPresent() ? new VisitorStateVariantMatcher<>() {
      final Trigger trigger = new Trigger(triggerCall.get());

      @Override
      public VisitorState whenPreInteractionBody(PreInteractionBodyState preInteractionBody) {
        return new BodySectionState(preInteractionBody, trigger);
      }

      @Override
      public VisitorState whenBodySection(BodySectionState bodySection) {
        return bodySection.replaceLastTrigger(trigger);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseStateError("trigger is unreachable", state, sourcePosition);
      }
    } : new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenBodySection(BodySectionState bodySection) {
        bodySection.lastTrigger().resultingInteractions().add(interaction);
        return bodySection;
      }

      @Override
      public VisitorState whenOpenActivityBar(OpenActivityBarState openActivityBar) {
        openActivityBar.interactions().add(interaction);
        return openActivityBar;
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseStateError("interaction has no cause, did you forget a trigger?",
                               state,
                               sourcePosition);
      }
    });
  }

  @Override
  public void visit(ASTSDObjectSource node) {
    var name = node.getName();
    var source = new Actor(name, false);
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenSendMessageAwaitSource(SendMessageAwaitSourceState sendMessageAwaitSource) {
        return new SendMessageAwaitTargetState(sendMessageAwaitSource, source);
      }

      @Override
      public VisitorState whenEndMessageAwaitSource(EndMessageAwaitSourceState endMessageAwaitSource) {
        var previousState = endMessageAwaitSource.previousState().previousState();
        var target = endMessageAwaitSource.target();
        return new AwaitActionState(previousState, source, target);
      }

      @Override
      public VisitorState whenAwaitConstructorSource(AwaitConstructorSourceState awaitConstructorSource) {
        var previousState = awaitConstructorSource.previousState();
        var interaction = awaitConstructorSource.finish(source);
        return new ConstructorCallState(previousState, interaction);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDObjectTarget node) {
    var name = node.getName();
    var target = new Actor(name, false);
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenSendMessageAwaitTarget(SendMessageAwaitTargetState sendMessageAwaitTarget) {
        var previousState = sendMessageAwaitTarget.previousState().previousState();
        var source = sendMessageAwaitTarget.source();
        return new AwaitActionState(previousState, source, target);
      }

      @Override
      public VisitorState whenEndMessageAwaitTarget(EndMessageAwaitTargetState endMessageAwaitTarget) {
        return new EndMessageAwaitSourceState(endMessageAwaitTarget, target);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDClass node) {
    var name = node.getMCObjectType().printType();
    var actor = new Actor(name, true);
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenSendMessageAwaitSource(SendMessageAwaitSourceState sendMessageAwaitSource) {
        return new SendMessageAwaitTargetState(sendMessageAwaitSource, actor);
      }

      @Override
      public VisitorState whenSendMessageAwaitTarget(SendMessageAwaitTargetState sendMessageAwaitTarget) {
        var previousState = sendMessageAwaitTarget.previousState().previousState();
        var source = sendMessageAwaitTarget.source();
        return new AwaitActionState(previousState, source, actor);
      }

      @Override
      public VisitorState whenEndMessageAwaitTarget(EndMessageAwaitTargetState endMessageAwaitTarget) {
        return new EndMessageAwaitSourceState(endMessageAwaitTarget, actor);
      }

      @Override
      public VisitorState whenEndMessageAwaitSource(EndMessageAwaitSourceState endMessageAwaitSource) {
        var previousState = endMessageAwaitSource.previousState().previousState();
        var target = endMessageAwaitSource.target();
        return new AwaitActionState(previousState, actor, target);
      }

      @Override
      public VisitorState whenAwaitConstructorSource(AwaitConstructorSourceState awaitConstructorSource) {
        var previousState = awaitConstructorSource.previousState();
        var interaction = awaitConstructorSource.finish(actor);
        return new ConstructorCallState(previousState, interaction);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDCall node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenAwaitAction(AwaitActionState awaitAction) {
        var methodName = node.getName();
        var isStatic = node.isStatic();
        var isTrigger = node.isTrigger();
        var argumentExpressions = node.isPresentArguments() ?
            stringifyExpressions(node.getArguments().getExpressionList()) :
            Optional.<List<String>>empty();
        var interaction = new MethodCallInteraction(awaitAction.source(),
                                                    awaitAction.target(),
                                                    methodName,
                                                    isStatic,
                                                    isTrigger,
                                                    argumentExpressions);

        if (isTrigger && !interaction.isComplete()) {
          return raiseError("trigger interaction can't have unspecified arguments",
                            getSourcePosition(node));
        }

        var previousState = awaitAction.previousState();
        return new MethodCallState(previousState, interaction);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }


  @Override
  public void visit(ASTSDReturn node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenAwaitAction(AwaitActionState awaitAction) {
        var previousState = awaitAction.previousState();
        var source = awaitAction.source();
        var target = awaitAction.target();
        var hasReturnValue = node.isPresentExpression();
        var returnValue = hasReturnValue ?
            Optional.of(new ReturnValue(stringifyExpression(node.getExpression()))) :
            Optional.<ReturnValue>empty();
        var interaction = new ReturnInteraction(source, target, returnValue);
        return new InteractionState(previousState, interaction);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDThrow node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenAwaitAction(AwaitActionState awaitAction) {
        var previousState = awaitAction.previousState();
        var source = awaitAction.source();
        var target = awaitAction.target();
        var errorClassName = node.getMCObjectType().printType();
        var constructorParameterExpressions = node.isPresentArguments() ?
            stringifyExpressions(node.getArguments().getExpressionList()) :
            Optional.<List<String>>empty();
        var interaction = new ThrowInteraction(source,
                                               target,
                                               errorClassName,
                                               constructorParameterExpressions);
        return new InteractionState(previousState, interaction);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void visit(ASTSDNew node) {
    var declarationTypeName = node.getDeclarationType().printType();
    var initializationTypeName = node.getInitializationType().printType();
    var objectName = node.getName();
    var parameterExpressions = stringifyExpressions(node.getArguments().getExpressionList());
    visitInteractionStart(node,
                          state -> new AwaitConstructorSourceState(state,
                                                                   source -> new ConstructorCallInteraction(
                                                                       source,
                                                                       declarationTypeName,
                                                                       initializationTypeName,
                                                                       objectName,
                                                                       parameterExpressions)));
  }

  @Override
  public void visit(ASTSDActivityBar node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenMethodCall(MethodCallState methodCall) {
        return new OpenActivityBarState(methodCall);
      }

      @Override
      public VisitorState whenConstructorCall(ConstructorCallState constructorCall) {
        return new OpenActivityBarState(constructorCall);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }
    });
  }

  @Override
  public void endVisit(ASTSDActivityBar node) {
    updateState(new VisitorStateVariantMatcher<>() {

      @Override
      public VisitorState whenMethodCall(MethodCallState methodCall) {
        return finishInteraction(methodCall.extractInteraction());
      }

      @Override
      public VisitorState whenConstructorCall(ConstructorCallState constructorCall) {
        return finishInteraction(constructorCall.extractInteraction());
      }

      @Override
      public VisitorState whenInteraction(InteractionState interaction) {
        return finishInteraction(interaction.extractInteraction());
      }

      @Override
      public VisitorState whenOpenActivityBar(OpenActivityBarState openActivityBar) {
        return new ClosedActivityBarState(openActivityBar);
      }

      @Override
      public VisitorState whenClosedActivityBar(ClosedActivityBarState closedActivityBar) {
        return finishInteraction(closedActivityBar.extractInteraction());
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedVisitError(node, state);
      }

      private ClosedActivityBarState finishInteraction(StateExtractedInteraction extractedInteraction) {
        var interaction = extractedInteraction.interaction();
        var previousState = extractedInteraction.previousState();
        var openActivityBarState =
            previousState.matchVariant(new VisitorStateVariantMatcher<OpenActivityBarState>() {
          @Override
          public OpenActivityBarState whenOpenActivityBar(OpenActivityBarState openActivityBar) {
            return openActivityBar;
          }

          @Override
          public OpenActivityBarState otherwise(VisitorState state) {
            return raiseStateError("expected OpenActivityBarState", state, getSourcePosition(node));
          }
        });
        openActivityBarState.interactions().add(interaction);
        return new ClosedActivityBarState(openActivityBarState);
      }
    });
  }

  @Override
  public void endVisit(ASTSDBody node) {
    updateState(new VisitorStateVariantMatcher<>() {
      @Override
      public VisitorState whenPreInteractionBody(PreInteractionBodyState preInteractionBody) {
        return new FinishedState(preInteractionBody);
      }

      @Override
      public VisitorState whenBodySection(BodySectionState bodySection) {
        return new FinishedState(bodySection);
      }

      @Override
      public VisitorState whenMethodCall(MethodCallState methodCall) {
        return finish(methodCall);
      }

      @Override
      public VisitorState whenConstructorCall(ConstructorCallState constructorCall) {
        return finish(constructorCall);
      }

      @Override
      public VisitorState whenInteraction(InteractionState interaction) {
        return finish(interaction);
      }

      @Override
      public VisitorState whenClosedActivityBar(ClosedActivityBarState closedActivityBar) {
        return finish(closedActivityBar);
      }

      @Override
      public VisitorState otherwise(VisitorState state) {
        return raiseUnexpectedEndVisitError(node, state);
      }

      private FinishedState finish(InteractionExtractable extractable) {
        var extractedInteraction = extractable.extractInteraction();
        var sourcePosition = getSourcePosition(node);
        var interaction = extractedInteraction.interaction();
        var previousState = extractedInteraction.previousState();
        var newPreviousState = finishInteraction(interaction, previousState, sourcePosition);
        var testExtractable =
            newPreviousState.matchVariant(new VisitorStateVariantMatcher<SDTestExtractable>() {
          @Override
          public SDTestExtractable whenBodySection(BodySectionState bodySection) {
            return bodySection;
          }

          @Override
          public SDTestExtractable otherwise(VisitorState state) {
            return raiseStateError("did not expect end of body here", state, sourcePosition);
          }
        });

        return new FinishedState(testExtractable);
      }
    });
  }

  private void updateState(VisitorStateVariantMatcher<VisitorState> matcher) {
    this.state = state.matchVariant(matcher);
  }

  private <T> T raiseUnexpectedVisitError(ASTNode node, VisitorState state) {
    return raiseStateError("unexpected visit of node " + node, state, getSourcePosition(node));
  }

  private <T> T raiseUnexpectedEndVisitError(ASTNode node, VisitorState state) {
    return raiseStateError("unexpected visit-end of node " + node, state, getSourcePosition(node));
  }

  private <T> T raiseStateError(String message,
                                VisitorState state,
                                Optional<SourcePosition> sourcePosition) {
    return raiseError(message + " in state " + state, sourcePosition);
  }

  private <T> T raiseError(String message, Optional<SourcePosition> sourcePosition) {
    if (sourcePosition.isPresent()) {
      Log.error(message, sourcePosition.get());
    } else {
      Log.error(message);
    }

    throw new IllegalStateException(message);
  }

  public VisitorState currentState() {
    return state;
  }

  private static Optional<SourcePosition> getSourcePosition(ASTNode node) {
    return node.isPresent_SourcePositionStart() ?
        Optional.of(node.get_SourcePositionStart()) :
        Optional.empty();
  }

  private Optional<List<String>> stringifyExpressions(List<ASTExpression> expressions) {
    var expressionStrings = new ArrayList<String>();
    for (var expression : expressions) {
      var optExprStr = stringifyExpression(expression);
      if (optExprStr.isEmpty()) {
        return Optional.empty();
      }
      expressionStrings.add(optExprStr.get());
    }
    return Optional.of(expressionStrings);
  }

  private Optional<String> stringifyExpression(ASTExpression expression) {
    return expression instanceof ASTSDIncompleteExpression ?
        Optional.empty() :
        Optional.of(SD4DevelopmentMill.prettyPrint(expression, false));
  }
}
