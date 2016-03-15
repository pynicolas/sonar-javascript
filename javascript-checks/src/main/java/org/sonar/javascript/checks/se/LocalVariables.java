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
package org.sonar.javascript.checks.se;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashSet;
import java.util.Set;
import org.sonar.javascript.cfg.ControlFlowBlock;
import org.sonar.javascript.cfg.ControlFlowGraph;
import org.sonar.javascript.tree.symbols.Scope;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Usage;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;

public class LocalVariables {

  private final Scope functionScope;
  private final SetMultimap<Symbol, IdentifierTree> localVarIdentifiersInCfg = HashMultimap.create();
  private final Set<Symbol> trackableVariables = new HashSet<>();
  private final Set<Symbol> functionParameters = new HashSet<>();

  public LocalVariables(Scope functionScope, ControlFlowGraph cfg) {
    this.functionScope = functionScope;

    for (ControlFlowBlock block : cfg.blocks()) {
      for (Tree element : block.elements()) {
        if (element instanceof IdentifierTree) {
          add((IdentifierTree) element);
        }
      }
    }

    for (Symbol localVar : localVarIdentifiersInCfg.keySet()) {
      if (!isWrittenOutsideCfg(localVar) && !localVar.is(Symbol.Kind.FUNCTION)) {
        trackableVariables.add(localVar);
      }
      if (localVar.is(Symbol.Kind.PARAMETER)) {
        functionParameters.add(localVar);
      }
    }
  }

  public Set<Symbol> trackableVariables() {
    return trackableVariables;
  }

  public Set<Symbol> functionParameters() {
    return functionParameters;
  }

  private boolean isWrittenOutsideCfg(Symbol localVar) {
    for (Usage usage : localVar.usages()) {
      if (usage.isWrite() && !localVarIdentifiersInCfg.get(localVar).contains(usage.identifierTree())) {
        return true;
      }
    }
    return false;
  }

  private void add(IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (symbol != null && isLocalVariable(symbol)) {
      localVarIdentifiersInCfg.put(identifier.symbol(), identifier);
    }
  }

  private boolean isLocalVariable(Symbol symbol) {
    Scope scope = symbol.scope();
    while (!scope.isGlobal()) {
      if (scope.equals(functionScope)) {
        return true;
      }
      scope = scope.outer();
    }
    return false;
  }
}
