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

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.sonar.javascript.se.SymbolicValue.Truthiness;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Symbol.Kind;

public class ProgramStateTest {

  private final Symbol symbol1 = new Symbol("symbol1", Kind.VARIABLE, null);
  private final Symbol symbol2 = new Symbol("symbol2", Kind.VARIABLE, null);
  private ProgramState state = ProgramState.emptyState();

  @Test
  public void constrain() throws Exception {
    state = state.copyAndAddValue(symbol1, SymbolicValue.UNKNOWN);
    assertThat(state.constrain(symbol1, Truthiness.FALSY).get(symbol1).truthiness()).isEqualTo(Truthiness.FALSY);
    assertThat(state.constrain(symbol2, Truthiness.FALSY).get(symbol2)).isNull();
  }

}
