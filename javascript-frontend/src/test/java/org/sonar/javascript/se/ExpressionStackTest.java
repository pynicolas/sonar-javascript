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
package org.sonar.javascript.se;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.javascript.cfg.ControlFlowGraph;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.javascript.se.sv.EqualToSymbolicValue;
import org.sonar.javascript.se.sv.LiteralSymbolicValue;
import org.sonar.javascript.se.sv.LogicalNotSymbolicValue;
import org.sonar.javascript.se.sv.SimpleSymbolicValue;
import org.sonar.javascript.se.sv.SpecialSymbolicValue;
import org.sonar.javascript.se.sv.SymbolicValue;
import org.sonar.javascript.se.sv.SymbolicValueWithConstraint;
import org.sonar.javascript.se.sv.TypeOfSymbolicValue;
import org.sonar.javascript.se.sv.UnknownSymbolicValue;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.ExpressionStatementTree;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.javascript.se.ExpressionStack.emptyStack;
import static org.sonar.javascript.se.sv.UnknownSymbolicValue.UNKNOWN;

public class ExpressionStackTest {

  private ExpressionStack stack = ExpressionStack.emptyStack();
  private ActionParser<Tree> parser = JavaScriptParserBuilder.createParser(Charsets.UTF_8);
  private SymbolicValue simple1 = new SimpleSymbolicValue(1);
  private SymbolicValue simple2 = new SimpleSymbolicValue(2);

  private ProgramState programState = ProgramState.emptyState();

  @Test
  public void null_value() throws Exception {
    execute("null");
    assertSingleValueInStack(SpecialSymbolicValue.NULL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void identifier() throws Exception {
    executeTopExpression("a");
  }

  @Test
  public void numeric_literal() throws Exception {
    execute("42");
    assertSingleValueInStack(LiteralSymbolicValue.class);
  }

  @Test
  public void logical_not() throws Exception {
    execute("!a");
    assertSingleValueInStack(LogicalNotSymbolicValue.class);
  }

  @Test
  public void equal_unknown() throws Exception {
    execute("a == foo()");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.BOOLEAN));
  }

  @Test
  public void equal_null() throws Exception {
    execute("a == null");
    assertSingleValueInStack(new EqualToSymbolicValue(simple1, Constraint.NULL_OR_UNDEFINED));
    assertSingleValueInStackWithConstraint(Constraint.BOOLEAN);
  }

  @Test
  public void null_equal() throws Exception {
    execute("null == a");
    assertSingleValueInStack(new EqualToSymbolicValue(simple1, Constraint.NULL_OR_UNDEFINED));
    assertSingleValueInStackWithConstraint(Constraint.BOOLEAN);
  }

  @Test
  public void not_equal_null() throws Exception {
    execute("a != null");
    assertSingleValueInStack(new EqualToSymbolicValue(simple1, Constraint.NOT_NULLY));
    assertSingleValueInStackWithConstraint(Constraint.BOOLEAN);
  }

  @Test
  public void strict_equal_null() throws Exception {
    execute("a === null");
    assertSingleValueInStack(new EqualToSymbolicValue(simple1, Constraint.NULL));
    assertSingleValueInStackWithConstraint(Constraint.BOOLEAN);
  }

  @Test
  public void strict_not_equal_null() throws Exception {
    execute("a !== null");
    assertSingleValueInStack(new EqualToSymbolicValue(simple1, Constraint.NULL.not()));
    assertSingleValueInStackWithConstraint(Constraint.BOOLEAN);
  }

  @Test
  public void typeof() throws Exception {
    execute("typeof a");
    assertSingleValueInStack(TypeOfSymbolicValue.class);
    assertSingleValueInStackWithConstraint(Constraint.STRING);
  }

  @Test
  public void yield() throws Exception {
    execute("yield a");
    assertSingleValueInStack(UNKNOWN);

    execute("yield");
    assertSingleValueInStack(UNKNOWN);
  }

  @Test
  public void delete() throws Exception {
    execute("delete a.prop");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.BOOLEAN));
  }

  @Test
  public void this_object() throws Exception {
    execute("this");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));
  }

  @Test
  public void new_expression() throws Exception {
    SymbolicValueWithConstraint expected = new SymbolicValueWithConstraint(Constraint.ARRAY.or(Constraint.OTHER_OBJECT));

    execute("new a");
    assertSingleValueInStack(expected);

    execute("new a(1)");
    assertSingleValueInStack(expected);

    execute("new Array(1)");
    assertSingleValueInStack(expected);
  }

  @Test
  public void call_expression() throws Exception {
    execute("a(b, c)");
    assertSingleValueInStack(UnknownSymbolicValue.class);
  }

  @Test
  public void array_literal() throws Exception {
    execute("[a, b, c]");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.ARRAY));
  }

  @Test
  public void template_literal() throws Exception {
    execute("`${a} ${b} ${c}`");
    assertSingleValueInStack(UnknownSymbolicValue.class);
  }

  @Test
  public void comma_operator() throws Exception {
    execute("a, b");
    assertSingleValueInStack(SimpleSymbolicValue.class);
  }

  @Test
  public void arrow_function() throws Exception {
    execute("() => { foo(); }");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.FUNCTION));
  }

  @Test
  public void empty_object_literal() throws Exception {
    execute("x = {}");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));
  }

  @Test
  public void object_literal() throws Exception {
    execute("x = {...a}");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));

    execute("x = {a: b}");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));

    execute("x = {'str': b}");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));

    execute("x = {42: b}");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));

    execute("x = {[1 + 2]: b}");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));

    execute("x = { method(){} }");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));

    execute("x = { a }");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));

    execute("x = { a, b }");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));

    execute("x = { a, b: foo(), ... rest }");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));
  }

  @Test
  public void await_expression() throws Exception {
    execute("await foo");
    assertSingleValueInStack(UNKNOWN);
  }

  @Test
  public void class_expression() throws Exception {
    execute("class {}");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.OTHER_OBJECT));
  }

  @Test
  public void boolean_expressions() throws Exception {
    execute("a < b");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.BOOLEAN));

    execute("a <= b");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.BOOLEAN));

    execute("a > b");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.BOOLEAN));


    execute("a >= b");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.BOOLEAN));
  }

  @Test
  public void number_expressions() throws Exception {
    execute("a - b");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("a * b");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("a / b");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("a % b");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("a++");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("a--");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("++a");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("--a");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("-a");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("+a");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));

    execute("~a");
    assertSingleValueInStack(new SymbolicValueWithConstraint(Constraint.NUMBER));
  }

  @Test
  public void plus_binary_expression() throws Exception {
    execute("x = a + b");
    assertSingleValueInStackWithConstraint(Constraint.NUMBER.or(Constraint.STRING));

    execute("x = a + 'str'");
    assertSingleValueInStackWithConstraint(Constraint.STRING);

    execute("x = a + 1");
    assertSingleValueInStackWithConstraint(Constraint.NUMBER);

    execute("x = 'str' + 1");
    assertSingleValueInStackWithConstraint(Constraint.STRING);

    execute("x = 'str' + true");
    assertSingleValueInStackWithConstraint(Constraint.STRING);

    execute("x = a + true");
    assertSingleValueInStackWithConstraint(Constraint.NUMBER);
  }

  @Test
  public void isEmpty() throws Exception {
    assertThat(emptyStack().isEmpty()).isTrue();
    assertThat(emptyStack().push(simple1).isEmpty()).isFalse();
  }

  @Test
  public void equals() throws Exception {
    assertThat(emptyStack().push(simple1)).isEqualTo(emptyStack().push(simple1));
    assertThat(emptyStack().push(simple1)).isNotEqualTo(emptyStack().push(simple2));
    assertThat(emptyStack().push(simple1)).isNotEqualTo("");
  }

  @Test
  public void hash_code() throws Exception {
    assertThat(emptyStack().push(simple1).hashCode()).isEqualTo(emptyStack().push(simple1).hashCode());
  }

  @Test
  public void to_string() throws Exception {
    assertThat(emptyStack().push(simple1).toString()).isEqualTo(ImmutableList.of(simple1).toString());
  }

  private void assertSingleValueInStack(Class<? extends SymbolicValue> expectedClass) {
    assertThat(stack.size()).isEqualTo(1);
    assertThat(stack.peek()).isInstanceOf(expectedClass);
  }

  private void assertSingleValueInStack(SymbolicValue expected) {
    assertThat(stack.size()).isEqualTo(1);

    if (expected instanceof EqualToSymbolicValue) {
      assertThat(((EqualToSymbolicValue) expected).equalToSV(stack.peek())).isTrue();

    } else if (expected instanceof SymbolicValueWithConstraint) {
      assertThat(stack.peek()).isInstanceOf(SymbolicValueWithConstraint.class);
      assertThat(stack.peek().constraint(mock(ProgramState.class))).isEqualTo(expected.constraint(mock(ProgramState.class)));

    } else {
      assertThat(stack.peek()).isEqualTo(expected);
    }
  }

  private void assertSingleValueInStackWithConstraint(Constraint constraint) {
    assertThat(stack.size()).isEqualTo(1);
    SymbolicValue peek = stack.peek();
    assertThat(peek.constraint(programState)).isEqualTo(constraint);
  }

  private void pushValues(SymbolicValue... values) {
    for (SymbolicValue value : values) {
      stack = stack.push(value);
    }
  }

  private void execute(String expressionSource) {
    stack = ExpressionStack.emptyStack();
    ScriptTree script = (ScriptTree) parser.parse(expressionSource);

    ControlFlowGraph cfg = ControlFlowGraph.build(script);
    for (Tree element : cfg.start().elements()) {
      if (element.is(Kind.IDENTIFIER_REFERENCE)) {
        pushValues(simple1);
      } else if (element instanceof ExpressionTree) {
        stack = stack.execute((ExpressionTree) element);
      }
    }


  }

  private void executeTopExpression(String expressionSource) {
    ScriptTree script = (ScriptTree) parser.parse(expressionSource);
    Tree tree = script.items().items().get(0);
    if (tree.is(Kind.EXPRESSION_STATEMENT)) {
      ExpressionStatementTree expressionStatement = (ExpressionStatementTree) tree;
      ExpressionTree expression = (ExpressionTree) expressionStatement.expression();
      stack = stack.execute(expression);
    } else {
      stack = stack.execute((ExpressionTree) tree);
    }
  }
}
