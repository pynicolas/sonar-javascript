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

import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;

public class SymbolicValue {

  public enum Truthiness { TRUTHY, FALSY, UNKNOWN }

  public static final SymbolicValue NULL_OR_UNDEFINED = new SymbolicValue(true, Truthiness.FALSY);
  public static final SymbolicValue UNKNOWN = new SymbolicValue(false, Truthiness.UNKNOWN);

  private final boolean alwaysNullOrUndefined;
  private final Truthiness truthiness;

  private SymbolicValue(boolean alwaysNullOrUndefined, Truthiness truthiness) {
    this.alwaysNullOrUndefined = alwaysNullOrUndefined;
    this.truthiness = truthiness;
  }

  public static SymbolicValue get(ExpressionTree expression) {
    if (expression.is(Kind.IDENTIFIER_REFERENCE)) {
      IdentifierTree identifier = (IdentifierTree) expression;
      // TODO undefined may be used as an an identifier in a non-global scope
      if ("undefined".equals(identifier.name())) {
        return NULL_OR_UNDEFINED;
      }
    }
    return expression.is(Kind.NULL_LITERAL) ? NULL_OR_UNDEFINED : UNKNOWN;
  }

  public boolean isAlwaysNullOrUndefined() {
    return alwaysNullOrUndefined;
  }

  public Truthiness truthiness() {
    return truthiness;
  }

  public SymbolicValue constrain(Truthiness truthiness) {
    return new SymbolicValue(alwaysNullOrUndefined, truthiness);
  }

}
