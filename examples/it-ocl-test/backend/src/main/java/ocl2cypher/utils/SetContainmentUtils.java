package ocl2cypher.utils;

import de.monticore.expressions.expressionsbasis._ast.ASTExpression;
import de.monticore.expressions.commonexpressions._ast.ASTBracketExpression;
import de.monticore.expressions.commonexpressions._ast.ASTCallExpression;
import de.monticore.expressions.commonexpressions._ast.ASTFieldAccessExpression;
import de.monticore.expressions.expressionsbasis._ast.ASTNameExpression;
import de.monticore.ocl.ocl.OCLMill;
import de.monticore.ocl.setexpressions._ast.*;
import jsweet.lang.Erased;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Objects;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 * Utility class for handling set isin/notin logic in OCL to Cypher translation
 */
@Erased
public class SetContainmentUtils {

    // ==================== MAIN LOGIC METHODS ====================

    /**
     * Main entry point for handling set containment operations (isin/notin).
     * Orchestrates the translation of OCL set containment expressions to Cypher queries.
     *
     * @param element Single value to check for membership.
     *                - Direct property (e.g., g.name)
     *                - Constant value (ASTLiteralExpression / ASTNumericExpression / ASTBooleanExpression),
     *                  e.g. "Alice", 21, true
     *                - Bracket expression (ASTBracketExpression) wrapping one of the above
     *                  since bracket expressions are traversed and resolved beforehand, element will be null in this context.
     *
     * @param set Aggregated set to check against.
     *            - Constant set (ASTSetEnumeration), e.g. ["Alice","Bob","John"]
     *            - Property collection (ASTFieldAccessExpression), e.g. g.members.name
     *            - Set operations (ASTUnionExpression / ASTIntersectionExpression / ASTSetMinusExpression),
     *              combining the above forms, e.g. g.members.name union ["Admin"]
     *
     * @param param The parameter name for the current context
     * @param query StringBuilder to append the query to
     */
    public static void handleSetContainmentLogic(ASTExpression element, ASTExpression set,
                                                 String param, StringBuilder query) {
        // Handle bracket expressions by unwrapping them
        if (set instanceof ASTBracketExpression) {
            handleSetContainmentLogic(element, ((ASTBracketExpression) set).getExpression(), param, query);
            return;
        }

        // Handle asSet() and asList() calls
        if (set instanceof ASTCallExpression) {
            ASTCallExpression call = (ASTCallExpression) set;
            if (call.getExpression() instanceof ASTFieldAccessExpression) {
                ASTFieldAccessExpression fa = (ASTFieldAccessExpression) call.getExpression();
                if ("asSet".equals(fa.getName()) || "asList".equals(fa.getName())) {
                    handleSetContainmentLogic(element, fa.getExpression(), param, query);
                    return;
                }
            }
        }

        // Handle set operations recursively (union, intersection, minus)
        if (handleSetOperationLogic(element, set, param, query)) {
            return;
        }

        // Handle set bracket operations recursively (union[], intersection[])
        if (handleSetBracketOperationLogic(element, set, param, query)){
            return;
        }

        // Basic logic
        buildSetExpression(element, set, param, query);
    }


    /**
     * Handles the base cases for set containment operations.
     * Processes multi field access expressions (ASTFieldAccessExpression) and set enumerations (ASTSetEnumeration).
     *
     * Leveraging Neo4j's native pattern matching capability, we can directly check
     * the existence (or non-existence) of a specific relationship in the graph,
     * instead of manually traversing nodes and aggregating them into a set for lookup.
     *
     * Using a pattern predicate in WHERE
     * - Efficiently checks if a Group has no :members relationship to a Person named "Alice"
     * - Does not bind the "Alice" node to a variable; only evaluates existence
     *
     * MATCH (g:Group)
     * WHERE NOT (g)-[:members]->({name: "Alice"})
     *
     * Instead of: Using aggregation and set membership check
     * - First collects all member names of a Group into a list
     * - Then checks if "Alice" is not in that list
     * - This approach involves extra work: collecting all members even if we only need to know about Alice
     *
     * MATCH (g:Group)-[:members]->(p)
     * WITH g, collect(p.name) AS memberNames
     * WHERE NOT "Alice" IN memberNames
     **/
    private static void buildSetExpression(ASTExpression element, ASTExpression set,
                                           String param, StringBuilder query) {

        if(set instanceof ASTFieldAccessExpression){
            String value = OCLMill.prettyPrint(element, false);
            String fieldAccessExpr = truncate(OCLMill.prettyPrint(set, false), param);
            List<String> fields = parseFieldAccess(fieldAccessExpr);

            query.append("(").append(param).append(")-[:").append(fields.get(0)).append("]");
            for(int i=1; i<fields.size()-1; i++) {
                query.append("->()-[:").append(fields.get(i)).append("]");
            }
            query.append("->({").append(fields.get(fields.size()-1)).append(":").append(value).append("})");
        }else if(set instanceof ASTSetEnumeration || set instanceof ASTNameExpression){
            String target = element == null ? "" :OCLMill.prettyPrint(element, false).toString();
            String list = OCLMill.prettyPrint(set, false).toString();
            if (set instanceof ASTSetEnumeration) {
                list = list.replace("{", "[").replace("}", "]");
            }
            query.append(target).append(" IN ").append(list);
        }else{
            throw new UnsupportedOperationException(
                    String.format("Unsupported expression types. Expected: %s, but got: %s and %s",
                            "ASTFieldAccessExpression and ASTSetEnumeration", element.getClass().getSimpleName(), set.getClass().getSimpleName()));
        }
    }

    /**
     * Handles traditional set operations (union, intersection, minus) with recursive logic.
     * Translates binary set operations e.g. "setA union/intersect/minus setB" to Cypher OR/AND/AND NOT expressions.
     */
    private static boolean handleSetOperationLogic(ASTExpression element, ASTExpression set,
                                                   String param, StringBuilder query) {
        SetOperationInfo operationInfo = extractSetOperationInfo(set);
        if (operationInfo == null) {
            return false;
        }

        StringBuilder queryLeft = buildSubQuery(element, operationInfo.left, param);
        StringBuilder queryRight = buildSubQuery(element, operationInfo.right, param);

        query.append("(")
                .append(queryLeft.toString())
                .append(operationInfo.operator)
                .append(queryRight.toString())
                .append(")");

        return true;
    }

    /**
     * Handles bracket-style set operations (union[...], intersection[...]) with flattening logic.
     * Processes multi field access set operations and flattens enumerations.
     *
     * e.g. 'validName' isin union [u.groups.members.name, ["A","B", "C"], ["D"]] will be converted to
     * WHERE (g) -[:members]-> ({name:'validName'}) OR 'validName' IN ["A","B", "C", "D"]
     */
    private static boolean handleSetBracketOperationLogic(ASTExpression element, ASTExpression set,
                                                          String param, StringBuilder query){
        if (set instanceof ASTSetUnionExpression || set instanceof ASTSetIntersectionExpression){
            ASTSetEnumeration enumeration = set instanceof ASTSetUnionExpression? (ASTSetEnumeration)((ASTSetUnionExpression) set).getSet()
                    : (ASTSetEnumeration)((ASTSetIntersectionExpression)set).getSet();
            List<ASTSetCollectionItem> setValueItems = enumeration.getSetCollectionItemList();

            query.append("(");
            String operator = set instanceof ASTSetUnionExpression ? " OR " : " AND ";
            List<ASTSetEnumeration> enumerations = new ArrayList<>();

            for (ASTSetCollectionItem item : setValueItems) {
                ASTSetValueItem setValueItem = (ASTSetValueItem) item;
                ASTExpression expression = setValueItem.getExpression();
                if (expression instanceof ASTSetEnumeration) {
                    enumerations.add((ASTSetEnumeration) expression);
                    continue;
                }
                StringBuilder subQuery = buildSubQuery(element,expression,param);

                query.append(subQuery.toString())
                        .append(operator);
            }

            // handle SetEnumerations separately
            if (!enumerations.isEmpty() && operator.equals(" OR ")) {
                StringBuilder enumerationResults = flattenSetUnionEnumerations(enumerations);
                query.append(OCLMill.prettyPrint(element,false)).append(" IN ").append(enumerationResults).append(")");
            }
            else if(!enumerations.isEmpty() && operator.equals(" AND ")) {
                StringBuilder enumerationResults = getSetIntersectionEnumerations(enumerations);
                query.append(OCLMill.prettyPrint(element,false)).append(" IN ").append(enumerationResults).append(")");
            }
            else{
                query.delete(query.length() - 4, query.length()).append(")");
            }
            return true;
        }
        return false;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Builds a sub-query for recursive set operations.
     * Wraps the recursive call in parentheses to maintain proper query structure.
     *
     * @param element The element to check for containment
     * @param expression The expression to process
     * @param param The parameter name for the current context
     * @return StringBuilder containing the sub-query with parentheses
     */
    private static StringBuilder buildSubQuery(ASTExpression element, ASTExpression expression, String param) {
        StringBuilder subQuery = new StringBuilder();
        subQuery.append("(");
        handleSetContainmentLogic(element, expression, param, subQuery);
        subQuery.append(")");
        return subQuery;
    }

    /**
     * Extracts operation information from different types of set expressions.
     * Maps AST set operation types to their corresponding Cypher operators.
     *
     * @param set The set expression to analyze
     * @return SetOperationInfo containing operator and operands, or null if not a recognized operation
     */
    private static SetOperationInfo extractSetOperationInfo(ASTExpression set) {
        if (set instanceof ASTUnionExpression) {
            ASTUnionExpression unionExpr = (ASTUnionExpression) set;
            return new SetOperationInfo(" OR ", unionExpr.getLeft(), unionExpr.getRight());
        } else if (set instanceof ASTIntersectionExpression) {
            ASTIntersectionExpression intersectionExpr = (ASTIntersectionExpression) set;
            return new SetOperationInfo(" AND ", intersectionExpr.getLeft(), intersectionExpr.getRight());
        } else if (set instanceof ASTSetMinusExpression) {
            ASTSetMinusExpression minusExpr = (ASTSetMinusExpression) set;
            return new SetOperationInfo(" AND NOT ", minusExpr.getLeft(), minusExpr.getRight());
        }

        return null;
    }

    /**
     * Flattens list of set enumerations into a single enumeration list.
     * Combines nested set enumerations from bracket operations into one flat list.
     *
     * @param enumerations List of ASTSetEnumeration objects to flatten
     * @return StringBuilder containing the flattened enumeration as a list string
     */
    private static StringBuilder flattenSetUnionEnumerations(List<ASTSetEnumeration> enumerations) {
        StringBuilder result = new StringBuilder();
        StringBuilder enumerationList = new StringBuilder();

        for (ASTSetEnumeration enumeration : enumerations) {
            enumerationList.append(OCLMill.prettyPrint(enumeration, false).toString()).append(",");
        }
        enumerationList.deleteCharAt(enumerationList.length() - 1);
        result.append(flattenSetUnion(enumerationList.toString()));
        return result;
    }


    /**
     * Flattens list of set enumerations into a single intersected enumeration list.
     * Calculate the intersected set.
     *
     * @param enumerations List of ASTSetEnumeration objects to flatten
     * @return StringBuilder containing the intersected enumeration as a list string
     */
    private static StringBuilder getSetIntersectionEnumerations(List<ASTSetEnumeration> enumerations) {
        StringBuilder result = new StringBuilder();

        if (enumerations.isEmpty()) {
            return result.append("[]");
        }

        List<Set<String>> sets = new ArrayList<>();
        for (ASTSetEnumeration enumeration : enumerations) {
            String setStr = OCLMill.prettyPrint(enumeration, false).toString().trim();

            // remove square brackets or curly braces
            if ((setStr.startsWith("[") && setStr.endsWith("]")) || (setStr.startsWith("{") && setStr.endsWith("}"))) {
                setStr = setStr.substring(1, setStr.length() - 1).trim();
            }
            Set<String> currentSet = new HashSet<>();

            if (!setStr.isEmpty()) {
                // split elements
                String[] elements = setStr.split(",");
                for (String element : elements) {
                    currentSet.add(element.trim());
                }
            }

            sets.add(currentSet);
        }

        // calculate the intersections
        Set<String> intersection = new HashSet<>(sets.get(0));
        for (int i = 1; i < sets.size(); i++) {
            intersection.retainAll(sets.get(i));
            if (intersection.isEmpty()) {
                break; // terminate when the set is empty
            }
        }

        // turn the result into string
        result.append("[");
        boolean first = true;
        for (String element : intersection) {
            if (!first) {
                result.append(", ");
            }
            result.append(element);
            first = false;
        }
        result.append("]");

        return result;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Parses a field access expression string into individual field components.
     * Splits dot-separated field access like "g.members.name" into ["members", "name"].
     *
     * @param fieldAccessExpr The field access expression string to parse
     * @return List of field names in access order
     */
    private static List<String> parseFieldAccess(String fieldAccessExpr){
        List<String> fields = Arrays.asList(fieldAccessExpr.split("\\."));
        return fields;
    }

    /**
     * Flattens union bracket syntax by removing union keywords and brackets.
     * Converts "union [["A"], ["B"]]" format to ["A", "B"].
     *
     * @param input The union expression string to flatten
     * @return Flattened string representation as a list
     */
    private static String flattenSetUnion(String input) {
        input = input.trim();
        if (input.startsWith("union")) {
            input = input.substring(5).trim();
        }

        input = input.replace("[", "").replace("]", "").replace("{", "").replace("}", "");

        String[] tokens = input.split(",");

        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            token = token.trim();
            if (!token.isEmpty()) {
                result.add(token);
            }
        }

        return result.toString();
    }

    /**
     * Truncates the parameter prefix from an expression string.
     * Removes context parameter like "g." from "g.members" to get "members".
     *
     * @param expr The expression string to truncate
     * @param param The parameter prefix to remove
     * @return The truncated expression string
     */
    private static String truncate(String expr, String param) {
        String prefix = param + ".";
        if (expr.startsWith(prefix)) {
            return expr.substring(prefix.length());
        }
        return expr;
    }

    // ==================== DATA CLASSES ====================

    /**
     * Helper class to hold set operation information.
     * Encapsulates the operator string and left/right operands for binary set operations.
     */
    private static class SetOperationInfo {
        final String operator;
        final ASTExpression left;
        final ASTExpression right;

        SetOperationInfo(String operator, ASTExpression left, ASTExpression right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }
    }
}
