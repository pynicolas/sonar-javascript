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

import java.util.List;
import org.junit.Test;
import org.sonar.javascript.se.Constraint;
import org.sonar.javascript.se.ProgramState;
import org.sonar.javascript.se.Relation;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;

import static org.fest.assertions.Assertions.assertThat;

public class RelationalSymbolicValueTest {

  SymbolicValue sv1 = new SimpleSymbolicValue(1);
  SymbolicValue sv2 = new SimpleSymbolicValue(2);
  RelationalSymbolicValue relationalValue = new RelationalSymbolicValue(Kind.LESS_THAN, sv1, sv2);
  ProgramState emptyState = ProgramState.emptyState();
  
  @Test
  public void constraint() {
    assertThat(relationalValue.constraint(ProgramState.emptyState())).isEqualTo(Constraint.BOOLEAN);
  }
  
  @Test
  public void relation() {
    assertThat(relationalValue.relation(Constraint.BOOLEAN)).isNull();
    assertThat(relationalValue.relation(Constraint.TRUTHY)).isEqualTo(new Relation(Kind.LESS_THAN, sv1, sv2));
    assertThat(relationalValue.relation(Constraint.FALSY)).isEqualTo(new Relation(Kind.GREATER_THAN_OR_EQUAL_TO, sv1, sv2));
  }

  @Test
  public void constrain_to_stupid_constraint() {
    assertThat(relationalValue.constrain(emptyState, Constraint.STRING)).isEmpty();
  }

  @Test
  public void constrain_to_truthy() {
    List<ProgramState> constrainedStates = relationalValue.constrain(emptyState, Constraint.TRUTHY);
    assertThat(constrainedStates).hasSize(1);
    assertThat(constrainedStates.get(0).getConstraint(relationalValue)).isEqualTo(Constraint.TRUE);
  }

  @Test
  public void constrain_to_falsy() {
    List<ProgramState> constrainedStates = relationalValue.constrain(emptyState, Constraint.FALSY);
    assertThat(constrainedStates).hasSize(1);
    assertThat(constrainedStates.get(0).getConstraint(relationalValue)).isEqualTo(Constraint.FALSE);
  }

  @Test
  public void constrain_with_incompatible_relation() throws Exception {
    RelationalSymbolicValue lessThan = new RelationalSymbolicValue(Kind.LESS_THAN, sv1, sv2);
    RelationalSymbolicValue greaterThan = new RelationalSymbolicValue(Kind.GREATER_THAN, sv1, sv2);
    ProgramState constrainedState = lessThan.constrain(emptyState, Constraint.TRUTHY).get(0);

    assertThat(greaterThan.constrain(constrainedState, Constraint.TRUTHY)).isEmpty();
    assertThat(lessThan.constrain(constrainedState, Constraint.FALSY)).isEmpty();
  }

}
