/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.javascript.checks;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.server.rule.RulesDefinition.SubCharacteristics;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.cfg.ControlFlowBlock;
import org.sonar.javascript.cfg.ControlFlowGraph;
import org.sonar.javascript.cfg.ControlFlowNode;
import org.sonar.javascript.checks.se.LocalVariables;
import org.sonar.javascript.se.ProgramState;
import org.sonar.javascript.se.SymbolicValue;
import org.sonar.javascript.tree.symbols.Scope;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionTree;
import org.sonar.plugins.javascript.api.tree.declaration.InitializedBindingElementTree;
import org.sonar.plugins.javascript.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.MemberExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.tree.statement.CatchBlockTree;
import org.sonar.plugins.javascript.api.tree.statement.ForObjectStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableDeclarationTree;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2259",
  name = "Properties of variables with \"Null\" or \"Undefined\" values should not be accessed",
  priority = Priority.BLOCKER,
  tags = {Tags.BUG, Tags.CERT, Tags.CWE, Tags.OWASP_A3, Tags.OWASP_A2, Tags.OWASP_A6, Tags.SECURITY})
@SqaleSubCharacteristic(SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("10min")
@ActivatedByDefault
public class TypeErrorOnNullOrUndefinedCheck extends SubscriptionVisitorCheck {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(
      Kind.FUNCTION_DECLARATION,
      Kind.GENERATOR_DECLARATION,
      Kind.FUNCTION_EXPRESSION,
      Kind.GENERATOR_FUNCTION_EXPRESSION,
      Kind.METHOD,
      Kind.GENERATOR_METHOD,
      Kind.ARROW_FUNCTION);
  }

  @Override
  public void visitNode(Tree tree) {
    FunctionTree functionTree = (FunctionTree) tree;
    if (functionTree.body().is(Kind.BLOCK)) {
      checkCFG(ControlFlowGraph.build((BlockTree) functionTree.body()), functionTree);
    }
  }

  private void checkCFG(ControlFlowGraph cfg, FunctionTree functionTree) {
    Scope functionScope = getContext().getSymbolModel().getScope(functionTree);
    new SymbolicExecution(functionScope, cfg).visitCfg();
  }

  private class SymbolicExecution {

    private final ControlFlowNode cfgStartNode;
    private final Set<Symbol> trackedVariables;
    private final Set<Symbol> functionParameters;
    private final SetMultimap<ControlFlowBlock, ControlFlowBlock> visitedEdges = HashMultimap.create();

    public SymbolicExecution(Scope functionScope, ControlFlowGraph cfg) {
      cfgStartNode = cfg.start();
      LocalVariables localVariables = new LocalVariables(functionScope, cfg);
      this.trackedVariables = localVariables.trackableVariables();
      this.functionParameters = localVariables.functionParameters();
    }

    public void visitCfg() {
      if (cfgStartNode instanceof ControlFlowBlock) {
        ProgramState initialState = ProgramState.emptyState();
        for (Symbol localVar : trackedVariables) {
          if ("arguments".equals(localVar.name())) {
            initialState = initialState.copyAndAddValue(localVar, SymbolicValue.UNKNOWN);
          }
        }
        for (Symbol functionParameter : functionParameters) {
          initialState = initialState.copyAndAddValue(functionParameter, SymbolicValue.UNKNOWN);
        }
        visitBlock((ControlFlowBlock) cfgStartNode, initialState);
      }
    }

    private void visitBlock(ControlFlowBlock block, ProgramState incomingState) {
      ProgramState currentState = incomingState;

      for (Tree element : block.elements()) {
        if (element.is(Kind.ASSIGNMENT)) {
          AssignmentExpressionTree assignment = (AssignmentExpressionTree) element;
          currentState = store(currentState, assignment.variable(), assignment.expression());

        } else if (element.is(Kind.INITIALIZED_BINDING_ELEMENT)) {
          InitializedBindingElementTree initialized = (InitializedBindingElementTree) element;
          currentState = store(currentState, initialized.left(), initialized.right());

        } else if (element.is(Kind.DOT_MEMBER_EXPRESSION, Kind.BRACKET_MEMBER_EXPRESSION)) {
          visitMemberExpression(currentState, (MemberExpressionTree) element);
        }
      }

      Tree branchingTree = block.branchingTree();
      if (branchingTree != null) {
        if (branchingTree.is(Kind.FOR_IN_STATEMENT, Kind.FOR_OF_STATEMENT)) {
          ForObjectStatementTree forTree = (ForObjectStatementTree) branchingTree;
          Tree variable = forTree.variableOrExpression();
          if (variable.is(Kind.VAR_DECLARATION)) {
            VariableDeclarationTree declaration = (VariableDeclarationTree) variable;
            variable = declaration.variables().get(0);
          }
          currentState = store(currentState, variable, forTree.expression());
        } else if (branchingTree.is(Kind.CATCH_BLOCK)) {
          CatchBlockTree catchBlock = (CatchBlockTree) branchingTree;
          currentState = currentState.copyAndAddValue(trackedVariable(catchBlock.parameter()), SymbolicValue.UNKNOWN);
        }
      }

      for (ControlFlowBlock successor : Iterables.filter(block.successors(), ControlFlowBlock.class)) {
        if (!visitedEdges.containsEntry(block, successor)) {
          visitedEdges.put(block, successor);
          visitBlock(successor, currentState);
        }
      }
    }

    private ProgramState store(ProgramState currentState, Tree left, ExpressionTree right) {
      Symbol trackedVariable = trackedVariable(left);
      if (trackedVariable != null) {
        SymbolicValue symbolicValue = SymbolicValue.get(right);
        return currentState.copyAndAddValue(trackedVariable, symbolicValue);
      }
      return currentState;
    }

    private void visitMemberExpression(ProgramState currentState, MemberExpressionTree memberExpression) {
      Symbol trackedVariable = trackedVariable(memberExpression.object());
      if (trackedVariable != null) {
        SymbolicValue value = currentState.get(trackedVariable);
        if (value == null || value.isAlwaysNullOrUndefined()) {
          addIssue(memberExpression.object(), String.format("\"%s\" is null or undefined", trackedVariable.name()));
        }
      }
    }

    @CheckForNull
    private Symbol trackedVariable(Tree tree) {
      if (tree.is(Kind.IDENTIFIER_REFERENCE, Kind.BINDING_IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) tree;
        Symbol symbol = identifier.symbol();
        return trackedVariables.contains(symbol) ? symbol : null;
      }
      return null;
    }

  }

}
