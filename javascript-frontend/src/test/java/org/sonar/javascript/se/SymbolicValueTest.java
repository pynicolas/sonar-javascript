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
import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.javascript.se.SymbolicValue.Truthiness;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.ExpressionStatementTree;

import static org.fest.assertions.Assertions.assertThat;

public class SymbolicValueTest {

  private ActionParser<Tree> parser = JavaScriptParserBuilder.createParser(Charsets.UTF_8);

  @Test
  public void null_value() throws Exception {
    SymbolicValue value = symbolicValue("null");
    assertThat(value.isAlwaysNullOrUndefined()).isTrue();
    assertThat(value.truthiness()).isEqualTo(Truthiness.FALSY);
  }

  @Test
  public void undefined_value() throws Exception {
    SymbolicValue value = symbolicValue("undefined");
    assertThat(value.isAlwaysNullOrUndefined()).isTrue();
    assertThat(value.truthiness()).isEqualTo(Truthiness.FALSY);
  }

  @Test
  public void literals() throws Exception {
    assertThat(symbolicValue("42").isAlwaysNullOrUndefined()).isFalse();
    assertThat(symbolicValue("'str'").isAlwaysNullOrUndefined()).isFalse();
  }

  @Test
  public void identifier() throws Exception {
    SymbolicValue value = symbolicValue("x");
    assertThat(value.isAlwaysNullOrUndefined()).isFalse();
    assertThat(value.truthiness()).isEqualTo(Truthiness.UNKNOWN);
  }

  private SymbolicValue symbolicValue(String expressionSource) {
    ScriptTree script = (ScriptTree) parser.parse(expressionSource);
    ExpressionStatementTree expressionStatement = (ExpressionStatementTree) script.items().items().get(0);
    return SymbolicValue.get((ExpressionTree) expressionStatement.expression());
  }

}
