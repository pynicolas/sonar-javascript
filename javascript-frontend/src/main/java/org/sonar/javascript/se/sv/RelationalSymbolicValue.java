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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.javascript.se.Constraint;
import org.sonar.javascript.se.ProgramState;
import org.sonar.javascript.se.Relation;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;

public class RelationalSymbolicValue implements SymbolicValue {

  private final SymbolicValue leftOperand;
  private final SymbolicValue rightOperand;
  private final Relation relationWhenTrue;

  public RelationalSymbolicValue(Kind kind, SymbolicValue leftOperand, SymbolicValue rightOperand) {
    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
    this.relationWhenTrue = new Relation(kind, leftOperand, rightOperand);
  }

  @Override
  public List<ProgramState> constrain(ProgramState state, Constraint constraint) {
    if (constraint.isStricterOrEqualTo(Constraint.TRUTHY)) {
      return checkRelationsAndConstrain(state, constraint, relationWhenTrue);

    } else if (constraint.isStricterOrEqualTo(Constraint.FALSY)) {
      return checkRelationsAndConstrain(state, constraint, relationWhenTrue.not());

    }
    return ImmutableList.of();
  }

  private List<ProgramState> checkRelationsAndConstrain(ProgramState state, Constraint constraint, Relation relation) {
    for (Relation existingRelation : state.relations()) {
      if (!relation.isCompatibleWith(existingRelation)) {
        return ImmutableList.of();
      }
    }
    return ImmutableList.of(state.constrainOwnSV(this, constraint));
  }

  public SymbolicValue leftOperand() {
    return leftOperand;
  }

  public SymbolicValue rightOperand() {
    return rightOperand;
  }

  @Override
  public Constraint constraint(ProgramState state) {
    return Constraint.BOOLEAN;
  }

  @Override
  public Relation relation(Constraint constraint) {
    if (constraint.isStricterOrEqualTo(Constraint.TRUTHY)) {
      return relationWhenTrue;
    }
    if (constraint.isStricterOrEqualTo(Constraint.FALSY)) {
      return relationWhenTrue.not();
    }
    return null;
  }

}
