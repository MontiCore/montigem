package umlp.generator.gui.guivariability;

import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.guidsl.GUIDSLMill;
import de.monticore.guidsl._ast.ASTGUIAdapt;
import de.monticore.guidsl._ast.ASTGUIBlock;
import de.monticore.guidsl._ast.ASTGUIBlockContent;
import de.monticore.guidsl._ast.ASTGUICompilationUnit;
import de.monticore.guidsl._ast.ASTGUIComponent;
import de.monticore.guidsl._ast.ASTGUIRemove;
import de.monticore.guidsl._util.IGUIDSLTypeDispatcher;
import de.monticore.guidsl._visitor.GUIDSLHandler;
import de.monticore.guidsl._visitor.GUIDSLTraverser;
import de.monticore.ocl.setexpressions._ast.ASTSetCollectionItem;
import de.monticore.ocl.setexpressions._ast.ASTSetEnumeration;
import de.monticore.ocl.setexpressions._ast.ASTSetValueItem;
import de.monticore.ocl.setexpressions._visitor.SetExpressionsHandler;
import de.monticore.ocl.setexpressions._visitor.SetExpressionsTraverser;
import de.monticore.types.mcbasictypes._ast.ASTMCImportStatement;
import de.se_rwth.commons.logging.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GUIVariabilityHandler implements GUIDSLHandler, SetExpressionsHandler {

    private GUIDSLTraverser traverser;

    private final ASTGUICompilationUnit adaptedAST;
    private ASTSetValueItem argValueToAdd = null; //stores the new ArgValue to insert
    private String componentNameToChange = null; //the Name of the Component needs to change with the current GUIVariability statement

    private AdaptionOptions adaptionOptions = null; //stores which operation to perform

    List<ASTMCImportStatement> imports = null; //stores the imports of the Delta AST

    /**
     * Used to save the four different operations between methods. NONE is used as an error state and should not be used in other cases
     */
    private enum AdaptionOptions{
        REMOVE,
        REPLACE,
        AFTER,
        BEFORE,
        NONE
    }

    @Override
    public GUIDSLTraverser getTraverser() {
        return traverser;
    }

    @Override
    public void setTraverser(GUIDSLTraverser traverser) {
        this.traverser = traverser;
    }

    /**
     * The constructor fot GUIVariabilityHandler handler
     * @param adaptedAST the AST that should be adapted when traversing a GUIVariability AST, correct function is only guaranteed
     *                   when the GUIVariabilityHandler is traversing a DeltaAST that does adapt specifically the adaptedAST.
     */
    public GUIVariabilityHandler(ASTGUICompilationUnit adaptedAST) {
        traverser = GUIDSLMill.traverser();
        traverser.setGUIDSLHandler(this);
        traverser.setSetExpressionsHandler(this);
        this.adaptedAST = adaptedAST;
    }

    @Override
    public void setTraverser(SetExpressionsTraverser traverser) {
        if (traverser instanceof GUIDSLTraverser) {
            this.traverser = (GUIDSLTraverser) traverser;
        }
    }

    /**
     * The ASTGUICompilationUnit contains the List of imports,
     * for a working model after the adaption imports in the GUIVariability AST need to be added to the adapted (non-GUIVariability) AST if not already present.
     * This is accomplished by saving the List of imports to the respective instance variable for later use in the traversal of the adapted AST.
     * The factor to differentiate the two ASTs is the variable componentNameToChange as it is set before the adapted ast is traversed and reset to null right after.
     * @param node the current node in the AST
     */
    public void handle (ASTGUICompilationUnit node){
        if(componentNameToChange == null){ //GUIVariability AST
            imports = node.getMCImportStatementList();
        }
        else{ //AST to adapt, imports need to be added if not already existing
            for(ASTMCImportStatement importStatement : imports){
                if(node.getMCImportStatementList().stream().noneMatch(
                        i -> i.getMCQualifiedName().getBaseName().equals(importStatement.getMCQualifiedName().getBaseName()))) { //import statement has no equals defined so contains doesnt work
                    node.addMCImportStatement(importStatement);
                }
                else if (node.getMCImportStatementList().stream().noneMatch(
                        i -> i.getQName().equals(importStatement.getQName()))) {
                    Log.warn("0x45c0100 Conflicting import " + importStatement.getMCQualifiedName().getBaseName() + ". Import is already present in Base model with different package");
                }
            }
        }
        GUIDSLHandler.super.handle(node); //Maybe not the best
    }

    /**
     * handles the ASTGUIAdapt statement, this statement has three different operations that can be identified by the node.getPosition() method
     * this method saves them and the argValue that should be added as well as the component name to search for
     * to the respective instance variable for later use in the traversal
     * Then the AST that should be adapted is traversed to change that AST according to the GUIAdapt.
     * @param node the current node in the AST
     */
    public void handle (ASTGUIAdapt node) {

        componentNameToChange = node.getName();

//        if(adaptedAST.getEnclosingScope().resolveVariable(componentNameToChange).isEmpty()){ //ToDO when symboltable works
//            Log.error("GUIComponent " + componentNameToChange " was not found in the scope");
//        };

        switch (node.getPosition()){
            case 2:
                adaptionOptions = AdaptionOptions.BEFORE;
                break;
            case 1:
                adaptionOptions = AdaptionOptions.AFTER;
                break;
            case 6:
                adaptionOptions = AdaptionOptions.REPLACE;
                break;
            default:
                adaptionOptions = AdaptionOptions.NONE;
                break;
        }

        argValueToAdd = GUIDSLMill.setValueItemBuilder().setExpression(node.getExpression()).build();

        adaptedAST.accept(getTraverser());

        componentNameToChange = null; //reset in case there are more adaptions
    }

    /**
     * works the same as handle(ASTGUIAdapt) except there is only a single operator (remove) that does not add an argValue
     * @param node the current node in the AST
     */
    public  void handle (ASTGUIRemove node)
    {
        //Needed method to differentiate referenceParam

        componentNameToChange = node.getName();

//        if(adaptedAST.getEnclosingScope().resolveVariable(componentNameToChange).isEmpty()){ //ToDO when symboltable works
//            Log.error("GUIComponent " + componentNameToChange " was not found in the scope");
//        };

        adaptionOptions = AdaptionOptions.REMOVE;
        argValueToAdd = null;

        adaptedAST.accept(getTraverser());

        componentNameToChange = null; //reset in case there are more adaptions
    }

    /**
     * If the guiComponent, guiIterate or guiGuard with the correct name (componentNameToChange) is present in this GUIList
     * the method will perform the operation specified before in adaptionOptions.
     * Otherwise, the children are traversed normally this is also the case when traversing a GUIVariability AST
     * The factor to differentiate the two ASTs is the variable componentNameToChange as it is set before the adapted ast is traversed and reset to null right after
     * @param node the current node in the AST
     */
    public void handle(ASTSetEnumeration node){
        if(componentNameToChange != null) {  //otherwise traversing GUIVariability AST
          IGUIDSLTypeDispatcher typeDispatcher = GUIDSLMill.typeDispatcher();
            for (ASTSetCollectionItem item : node.getSetCollectionItemList()) {
                if (typeDispatcher.isSetExpressionsASTSetValueItem(item)) {
                  ASTSetValueItem argValue = typeDispatcher.asSetExpressionsASTSetValueItem(item);
                  ASTExpression expression = argValue.getExpression();

                  if ((typeDispatcher.isGUIDSLASTGUIComponent(expression)
                      &&typeDispatcher.asGUIDSLASTGUIComponent(expression).isPresentId()
                      && typeDispatcher.asGUIDSLASTGUIComponent(expression).getId().getName().equals(componentNameToChange)) ||
                      (typeDispatcher.isGUIDSLASTGUIGuard(expression)
                          && typeDispatcher.asGUIDSLASTGUIGuard(expression).isPresentId()
                          && typeDispatcher.asGUIDSLASTGUIGuard(expression).getId().getName().equals(componentNameToChange)) ||
                      (typeDispatcher.isGUIDSLASTGUIIterate(expression)
                          &&typeDispatcher.asGUIDSLASTGUIIterate(expression).isPresentId()
                          && typeDispatcher.asGUIDSLASTGUIIterate(expression).getId().getName().equals(componentNameToChange))
                  ) {
                    int index = node.indexOfSetCollectionItem(argValue);
                    switch (adaptionOptions) {
                      case BEFORE:
                        node.addAllSetCollectionItems(index, flatMapGUIArgValue(argValueToAdd));
                        return;
                      case AFTER:
                        node.addAllSetCollectionItems(index + 1, flatMapGUIArgValue(argValueToAdd));
                        return;
                      case REMOVE:
                        node.removeSetCollectionItem(index);
                        return;
                      case REPLACE:
                        node.removeSetCollectionItem(index);
                        node.addAllSetCollectionItems(index, flatMapGUIArgValue(argValueToAdd));
                        return;
                      case NONE:
                        Log.error("0x45c0000 Error when selecting delta Options, this is probably the result of a grammar alteration");
                    }
                  }
                }
            }
        }

        //variable not in List or GUIVariability AST
        node.forEachSetCollectionItems(v -> v.accept(getTraverser()));
    }

    /**
     * Works the same as handle(ASTGUIList) except that the argValues need to be wrapped in a GUIBlock context before they can be added
     * @param node the current node in the AST
     */
    public void handle(ASTGUIBlock node){

        if(componentNameToChange != null) {  //otherwise traversing GUIVariability AST

            List<ASTGUIBlockContent> blockContents = node.getGUIBlockContentList();

            //adapts and removes shouldn't be here as GUIVariability should not be adapted itself
            for (ASTGUIBlockContent blockContent : blockContents) { //Error checking first
                if (blockContent.isPresentGUIRemove() || blockContent.isPresentGUIAdapt())
                    Log.error("0x45c0001 Attempting to adapt GUIVariability AST, this is not allowed");
            }

            for (ASTGUIBlockContent blockContent : blockContents) {
                if ((blockContent.isPresentExpression()
                        && GUIDSLMill.typeDispatcher().isGUIDSLASTGUIComponent(blockContent.getExpression())
                        && ((ASTGUIComponent) blockContent.getExpression()).isPresentId()
                        && ((ASTGUIComponent) blockContent.getExpression()).getId().getName().equals(componentNameToChange)) ||
                    (blockContent.isPresentGUIGuard()
                        && blockContent.getGUIGuard().isPresentId()
                        && blockContent.getGUIGuard().getId().getName().equals(componentNameToChange)) ||
                    (blockContent.isPresentGUIIterate()
                        && blockContent.getGUIIterate().isPresentId()
                        && blockContent.getGUIIterate().getId().getName().equals(componentNameToChange))
                ) {
                    int index = blockContents.indexOf(blockContent);
                    switch (adaptionOptions) {
                        case BEFORE:
                            node.addAllGUIBlockContents(index, mapToBlockContent(flatMapGUIArgValue(argValueToAdd)));
                            return;
                        case AFTER:
                            node.addAllGUIBlockContents(index + 1, mapToBlockContent(flatMapGUIArgValue(argValueToAdd)));
                            return;
                        case REMOVE:
                            node.removeGUIBlockContent(index);
                            return;
                        case REPLACE:
                            node.removeGUIBlockContent(index);
                            node.addAllGUIBlockContents(index, mapToBlockContent(flatMapGUIArgValue(argValueToAdd)));
                            return;
                        case NONE:
                            Log.error("0x45c0000 Error when selecting adaption Options, this is probably the result of a grammar alteration");
                    }
                }

            }
        }

        //variable not in Block or GUIVariability AST
        node.forEachGUIBlockContents(c -> c.accept(getTraverser()));
    }

    /**
     * Wraps all argValues in the parameter to GUIBlockContext, as GUIBlockContext does not support GUILists
     * none of the argValues can be a GUIList
     * @param argValues the argValues to be wrapped, non should be a GUIList
     * @return a List of the Wrapped ArgValues
     */
    private List<ASTGUIBlockContent> mapToBlockContent(List<ASTSetCollectionItem> argValues){
        return argValues.stream().map(this::buildGUIBlockContext).collect(Collectors.toList());
    }

    /**
     * Wraps the argValue in a GUIBlockContext, this only works if the parameter is not a GUIList,
     * as GUIBlockContext does not support GUILists.
     * @param argValue the argValue to wrap in a GUIBlockContext, should not be a GUIList
     * @return the GUIBlockContext containing the parameter
     */
    private ASTGUIBlockContent buildGUIBlockContext(ASTSetCollectionItem argValue){
      if (GUIDSLMill.typeDispatcher().isSetExpressionsASTSetValueItem(argValue)) {
        ASTSetValueItem val = GUIDSLMill.typeDispatcher().asSetExpressionsASTSetValueItem(argValue);
        if (GUIDSLMill.typeDispatcher().isGUIDSLASTGUIComponent(val.getExpression())) {
          return GUIDSLMill.gUIBlockContentBuilder()
              .setExpression(GUIDSLMill.typeDispatcher().asGUIDSLASTGUIComponent(val.getExpression()))
              .build();
        }
        if (GUIDSLMill.typeDispatcher().isGUIDSLASTGUIIterate(val.getExpression())) {
          return GUIDSLMill.gUIBlockContentBuilder()
              .setGUIIterate(GUIDSLMill.typeDispatcher().asGUIDSLASTGUIIterate(val.getExpression()))
              .build();
        }
        if (GUIDSLMill.typeDispatcher().isGUIDSLASTGUIGuard(val.getExpression())) {
          return GUIDSLMill.gUIBlockContentBuilder()
              .setGUIGuard(GUIDSLMill.typeDispatcher().asGUIDSLASTGUIGuard(val.getExpression()))
              .build();
        }
        if (GUIDSLMill.typeDispatcher().isExpressionsBasisASTExpression(val.getExpression())) {
          return GUIDSLMill.gUIBlockContentBuilder()
              .setExpression(GUIDSLMill.typeDispatcher().asExpressionsBasisASTExpression(val.getExpression()))
              .build();
        }
      }
      Log.error("0x45c0003 encountered unknown type of guiArgValue, this is likely due to grammar changes");

      return null;
    }

    /**
     * maps all the argValues contained in the parameter argValue,
     * this means that argValues contained in a GUIList will be recursively flat mapped to a single list
     * @param argValue the Arg value to map
     * @return a List of all ArgValues contained in the parameter
     */
    private List<ASTSetCollectionItem> flatMapGUIArgValue(ASTSetValueItem argValue){
      LinkedList<ASTSetCollectionItem> result = new LinkedList<>();
      if(GUIDSLMill.typeDispatcher().isSetExpressionsASTSetEnumeration(argValue.getExpression())) { //Lists need to be mapped to a single list
        for (ASTSetCollectionItem argValueFromList : GUIDSLMill.typeDispatcher().asSetExpressionsASTSetEnumeration(argValue.getExpression()).getSetCollectionItemList()) {
          if (GUIDSLMill.typeDispatcher().isSetExpressionsASTSetValueItem(argValueFromList)) {
            result.addAll(flatMapGUIArgValue(GUIDSLMill.typeDispatcher().asSetExpressionsASTSetValueItem(argValueFromList)));
          }
        }
      }
      else {
        result.add(argValue);
      }
      return result;
    }
}
