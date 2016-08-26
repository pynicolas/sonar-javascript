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
package org.sonar.javascript.se.sv;

import org.junit.Test;
import org.sonar.javascript.se.Constraint;
import org.sonar.javascript.se.ProgramState;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;

import static org.fest.assertions.Assertions.assertThat;

public class RelationalSymbolicValueTest {

  @Test
  public void constraint() {
    SymbolicValue sv1 = new SimpleSymbolicValue(1);
    SymbolicValue sv2 = new SimpleSymbolicValue(2);
    RelationalSymbolicValue relationalValue = new RelationalSymbolicValue(Kind.LESS_THAN, sv1, sv2);
    assertThat(relationalValue.constraint(ProgramState.emptyState())).isEqualTo(Constraint.BOOLEAN);
  }

}
