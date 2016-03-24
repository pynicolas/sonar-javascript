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

import java.util.Map.Entry;

import org.sonar.javascript.cfg.ControlFlowBlock;
import org.sonar.plugins.javascript.api.symbols.Symbol;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;

public class ProgramState {

  private final ImmutableMap<Symbol, SymbolicValue> valuesBySymbol;
  private final ImmutableMultiset<ControlFlowBlock> visitedBlocks;

  private static final ProgramState EMPTY = new ProgramState(ImmutableMap.<Symbol, SymbolicValue>of(), ImmutableMultiset.<ControlFlowBlock>of());

  public static ProgramState emptyState() {
    return EMPTY;
  }

  private ProgramState(ImmutableMap<Symbol, SymbolicValue> valuesBySymbol, ImmutableMultiset<ControlFlowBlock> visitedBlocks) {
    this.valuesBySymbol = valuesBySymbol;
    this.visitedBlocks = visitedBlocks;
  }

  public ProgramState copyAndAddValue(Symbol symbol, SymbolicValue value) {
    ImmutableMap.Builder<Symbol, SymbolicValue> builder = ImmutableMap.<Symbol, SymbolicValue>builder();
    for (Entry<Symbol, SymbolicValue> entry : valuesBySymbol.entrySet()) {
      if (!entry.getKey().equals(symbol)) {
        builder.put(entry.getKey(), entry.getValue());
      }
    }
    builder.put(symbol, value);
    return new ProgramState(builder.build(), visitedBlocks);
  }

  public SymbolicValue get(Symbol symbol) {
    return valuesBySymbol.get(symbol);
  }

  public ProgramState copyAndAddVisitedBlock(ControlFlowBlock block) {
    ImmutableMultiset<ControlFlowBlock> newVisitedBlocks = ImmutableMultiset.<ControlFlowBlock>builder()
      .addAll(visitedBlocks)
      .add(block)
      .build();
    return new ProgramState(valuesBySymbol, newVisitedBlocks);
  }

  public int countVisits(ControlFlowBlock block) {
    return visitedBlocks.count(block);
  }

  public ProgramState constrain(Symbol symbol, Truthiness truthiness) {
    SymbolicValue value = get(symbol);
    if (value == null) {
      return this;
    }
    return copyAndAddValue(symbol, value.constrain(truthiness));
  }

}
