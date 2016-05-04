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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.sonar.api.server.rule.RulesDefinition.SubCharacteristics;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.se.ProgramState;
import org.sonar.javascript.se.SeCheck;
import org.sonar.javascript.se.SymbolicValue;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.MemberExpressionTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2259",
  name = "Properties of variables with \"null\" or \"undefined\" values should not be accessed",
  priority = Priority.CRITICAL,
  tags = {Tags.BUG, Tags.CERT, Tags.CWE, Tags.MISRA})
@SqaleSubCharacteristic(SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("15min")
@ActivatedByDefault
public class NullDereferenceCheck extends SeCheck {

  private static final String MESSAGE = "\"%s\" is null or undefined.";

  // map of tree elements (element of block of cfg) ans boolean value: true if this element always raises NPE
  private Map<Tree, Boolean> nullDereference = new HashMap<>();

  @Override
  public void beforeBlockElement(ProgramState currentState, Tree element) {
    ExpressionTree object = getObject(element);
    Symbol symbol = getSymbol(object);

    if (symbol != null) {
      SymbolicValue symbolicValue = currentState.get(symbol);

      if (symbolicValue != null) {
        if (symbolicValue.isDefinitelyNullOrUndefined() && nullDereference.get(element) != null && !nullDereference.get(element)) {
          nullDereference.put(element, false);
        } else {
          nullDereference.put(element, symbolicValue.isDefinitelyNullOrUndefined());
        }
      }
    }

  }

  private Symbol getSymbol(@Nullable  ExpressionTree object) {
    if (object != null && object.is(Kind.IDENTIFIER_REFERENCE)) {
      return ((IdentifierTree) object).symbol();
    }
    return null;
  }

  @Nullable
  private ExpressionTree getObject(Tree element) {
    if (element.is(Kind.BRACKET_MEMBER_EXPRESSION, Kind.DOT_MEMBER_EXPRESSION)) {
      return ((MemberExpressionTree) element).object();
    }
    return null;
  }

  @Override
  public void startOfExecution() {
    nullDereference = new HashMap<>();
  }

  @Override
  public void endOfExecution() {
    for (Entry<Tree, Boolean> entry : nullDereference.entrySet()) {
      if (entry.getValue()) {
        ExpressionTree object = getObject(entry.getKey());
        addIssue(object, String.format(MESSAGE, getSymbol(object).name()));
      }
    }

  }


}
