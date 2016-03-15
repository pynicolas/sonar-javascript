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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
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
import org.sonar.javascript.se.SymbolicValue.Truthiness;
import org.sonar.javascript.tree.symbols.Scope;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionTree;
import org.sonar.plugins.javascript.api.tree.declaration.InitializedBindingElementTree;
import org.sonar.plugins.javascript.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.tree.statement.ForObjectStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.VariableDeclarationTree;
import org.sonar.plugins.javascript.api.visitors.SubscriptionVisitorCheck;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2583",
  name = "Conditions should not unconditionally evaluate to \"true\" or to \"false\"",
  priority = Priority.BLOCKER,
  tags = {Tags.BUG, Tags.CERT, Tags.CWE, Tags.MISRA})
@SqaleSubCharacteristic(SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("15min")
@ActivatedByDefault
public class AlwaysTrueOrFalseConditionCheck extends SubscriptionVisitorCheck {

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
    private final Deque<BlockExecution> workList = new ArrayDeque<>();
    private final SetMultimap<Tree, Truthiness> conditionResults = HashMultimap.create();

    public SymbolicExecution(Scope functionScope, ControlFlowGraph cfg) {
      cfgStartNode = cfg.start();
      LocalVariables localVariables = new LocalVariables(functionScope, cfg);
      this.trackedVariables = localVariables.trackableVariables();
      this.functionParameters = localVariables.functionParameters();
    }

    private class BlockExecution {
      private final ControlFlowBlock block;
      private final ProgramState state;

      public BlockExecution(ControlFlowBlock block, ProgramState state) {
        this.block = block;
        this.state = state;
      }
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
        workList.addLast(new BlockExecution((ControlFlowBlock) cfgStartNode, initialState));
      }

      for (int i = 0; i < 10000 && !workList.isEmpty(); i++) {
        BlockExecution blockExecution = workList.removeFirst();
        Tree branchingTree = blockExecution.block.branchingTree();
        if (branchingTree != null && branchingTree.is(Kind.TRY_STATEMENT)) {
          return;
        }
        execute(blockExecution);
      }

      if (workList.isEmpty()) {
        reportIssues();
      }
    }

    private void reportIssues() {
      for (Entry<Tree, Collection<Truthiness>> entry : conditionResults.asMap().entrySet()) {
        Collection<Truthiness> results = entry.getValue();
        if (results.size() == 1 && !Truthiness.UNKNOWN.equals(results.iterator().next())) {
          String result = Truthiness.TRUTHY.equals(results.iterator().next()) ? "true" : "false";
          addIssue(entry.getKey(), String.format("Change this condition so that it does not always evaluate to \"%s\".", result));
        }
      }

    }

    private void execute(BlockExecution blockExecution) {
      ControlFlowBlock block = blockExecution.block;
      ProgramState currentState = blockExecution.state;
      if (currentState.countVisits(block) > 2) {
        return;
      }

      for (Tree element : block.elements()) {
        if (element.is(Kind.ASSIGNMENT)) {
          AssignmentExpressionTree assignment = (AssignmentExpressionTree) element;
          currentState = store(currentState, assignment.variable(), assignment.expression());

        } else if (element.is(Kind.INITIALIZED_BINDING_ELEMENT)) {
          InitializedBindingElementTree initialized = (InitializedBindingElementTree) element;
          currentState = store(currentState, initialized.left(), initialized.right());
        }
      }

      handleSuccessors(block, currentState);
    }

    private void pushAllSuccessors(ControlFlowBlock block, ProgramState currentState) {
      for (ControlFlowBlock successor : Iterables.filter(block.successors(), ControlFlowBlock.class)) {
        pushSuccessor(block, currentState, successor);
      }
    }

    private void pushSuccessor(ControlFlowBlock block, ProgramState currentState, ControlFlowNode successor) {
      if (successor instanceof ControlFlowBlock) {
        workList.addLast(new BlockExecution((ControlFlowBlock) successor, currentState.copyAndAddVisitedBlock(block)));
      }
    }

    private void handleSuccessors(ControlFlowBlock block, ProgramState incomingState) {
      Tree branchingTree = block.branchingTree();
      ProgramState currentState = incomingState;
      if (branchingTree != null) {

        if (branchingTree.is(
          Kind.IF_STATEMENT,
          Kind.WHILE_STATEMENT,
          Kind.FOR_STATEMENT,
          Kind.DO_WHILE_STATEMENT,
          Kind.CONDITIONAL_AND,
          Kind.CONDITIONAL_OR)) {

          handleConditionSuccessors(block, currentState);
          return;

        } else if (branchingTree.is(Kind.FOR_IN_STATEMENT, Kind.FOR_OF_STATEMENT)) {
          ForObjectStatementTree forTree = (ForObjectStatementTree) branchingTree;
          Tree variable = forTree.variableOrExpression();
          if (variable.is(Kind.VAR_DECLARATION)) {
            VariableDeclarationTree declaration = (VariableDeclarationTree) variable;
            variable = declaration.variables().get(0);
          }
          currentState = store(currentState, variable, forTree.expression());
        }
      }

      pushAllSuccessors(block, currentState);
    }

    private void handleConditionSuccessors(ControlFlowBlock block, ProgramState currentState) {
      Symbol trackedVariable;
      Truthiness trueSuccessorTruthiness;
      Truthiness falseSuccessorTruthiness;

      Tree lastElement = block.elements().get(block.elements().size() - 1);
      boolean isUnaryNot = lastElement.is(Kind.LOGICAL_COMPLEMENT);
      if (isUnaryNot) {
        UnaryExpressionTree unary = (UnaryExpressionTree) lastElement;
        trackedVariable = trackedVariable(unary.expression());
        trueSuccessorTruthiness = SymbolicValue.Truthiness.FALSY;
        falseSuccessorTruthiness = SymbolicValue.Truthiness.TRUTHY;
      } else {
        trackedVariable = trackedVariable(lastElement);
        trueSuccessorTruthiness = SymbolicValue.Truthiness.TRUTHY;
        falseSuccessorTruthiness = SymbolicValue.Truthiness.FALSY;
      }

      if (trackedVariable != null) {
        SymbolicValue curentValue = currentState.get(trackedVariable);
        Truthiness currentTruthiness = curentValue == null ? SymbolicValue.Truthiness.FALSY : curentValue.truthiness();
        Truthiness conditionTruthiness = isUnaryNot ? currentTruthiness.not() : currentTruthiness;
        conditionResults.put(lastElement, conditionTruthiness);
        if (currentTruthiness != falseSuccessorTruthiness) {
          pushSuccessor(block, currentState.constrain(trackedVariable, trueSuccessorTruthiness), block.trueSuccessor());
        }
        if (currentTruthiness != trueSuccessorTruthiness) {
          pushSuccessor(block, currentState.constrain(trackedVariable, falseSuccessorTruthiness), block.falseSuccessor());
        }
      } else {
        pushAllSuccessors(block, currentState);
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
