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

import com.google.common.base.Preconditions;
import org.sonar.javascript.se.sv.SymbolicValue;

/**
 * This class represents a constraint which is met by a {@link SymbolicValue} in a given {@link ProgramState}.
 * Possible constraints are NULL, UNDEFINED, ZERO, EMPTY_STRING, NAN, FALSE, TRUE, FUNCTION, TRUTHY_NUMBER, TRUTHY_STRING, ARRAY, OTHER_OBJECT and any possible combination of them.
 */
public class Constraint {

  /*
   * Internally, we represent each constraint with 12 bits.
   * Each bit is related to a subset of all possible values.
   * We assign each bit from left to right to the 12 following subsets:
   *
   * FALSY
   * 1. null
   * 2. undefined
   * 3. 0 (zero numeric value)
   * 4. empty string ("")
   * 5. NaN
   * 6. false (boolean value)
   *
   * TRUTHY
   * 7. true (boolean value)
   * 8. function
   * 9. truthy number (any number except 0)
   * 10. truthy string (any string except empty string)
   * 11. array
   * 12. other object
   *
   * We therefore have 2^12 possible constraints.
   *
   * Example: NULL is represented by "100 000 000 000" and NOT_NULL is represented by "011 111 111 111".
   */
  private int bitSet;

  public static final Constraint ANY_VALUE = new Constraint(0b111_111_111_111);
  public static final Constraint NO_POSSIBLE_VALUE = new Constraint(0b000_000_000_000);

  public static final Constraint NULL = new Constraint(0b100_000_000_000);
  public static final Constraint UNDEFINED = new Constraint(0b010_000_000_000);
  public static final Constraint NULL_OR_UNDEFINED = new Constraint(0b110_000_000_000);
  public static final Constraint NOT_NULLY = new Constraint(0b001_111_111_111);
  public static final Constraint TRUTHY = new Constraint(0b000_000_111_111);
  public static final Constraint FALSY = new Constraint(0b111_111_000_000);

  public enum SubConstraint {
    NULL(0b100_000_000_000),
    UNDEFINED(0b010_000_000_000),
    ZERO(0b001_000_000_000),
    EMPTY_STRING(0b000_100_000_000),
    NAN(0b000_010_000_000),
    FALSE(0b000_001_000_000),

    TRUE(0b000_000_100_000),
    FUNCTION(0b000_000_010_000),
    TRUTHY_NUMBER(0b000_000_001_000),
    TRUTHY_STRING(0b000_000_000_100),
    ARRAY(0b000_000_000_010),
    OTHER_OBJECT(0b000_000_000_001)
    ;

    private int bitSet;

    SubConstraint(int bitSet) {
      this.bitSet = bitSet;
    }
  }

  private Constraint(int bitSet) {
    Preconditions.checkArgument((bitSet & ~0b111_111_111_111) == 0);
    this.bitSet = bitSet;
  }

  private static Constraint get(int bitSet) {
    return new Constraint(bitSet);
  }

  public static Constraint get(SubConstraint subConstraints) {
    return get(subConstraints.bitSet);
  }

  public static Constraint or(SubConstraint ... subConstraints) {
    Preconditions.checkArgument(subConstraints.length > 0);

    int resultBitSet = subConstraints[0].bitSet;
    for (int i = 1; i < subConstraints.length; i++) {
      resultBitSet |= subConstraints[i].bitSet;
    }

    return get(resultBitSet);
  }

  public Constraint and(Constraint other) {
    return get(this.bitSet & other.bitSet);
  }

  public Constraint or(Constraint other) {
    return get(this.bitSet | other.bitSet);
  }

  public Constraint not() {
    return get(~this.bitSet & ANY_VALUE.bitSet);
  }

  public Truthiness truthiness() {
    if (this.equals(TRUTHY)) {
      return Truthiness.TRUTHY;
    } else if (isIncompatibleWith(TRUTHY)) {
      return Truthiness.FALSY;
    }
    return Truthiness.UNKNOWN;
  }

  public Nullability nullability() {
    if (isStricterOrEqualTo(NULL_OR_UNDEFINED)) {
      return Nullability.NULL;
    } else if (isStricterOrEqualTo(NOT_NULLY)) {
      return Nullability.NOT_NULL;
    }
    return Nullability.UNKNOWN;
  }

  public boolean isStricterOrEqualTo(Constraint other) {
    return and(other).equals(this);
  }

  public boolean isIncompatibleWith(Constraint other) {
    return and(other).equals(NO_POSSIBLE_VALUE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Constraint that = (Constraint) o;
    return bitSet == that.bitSet;
  }

  @Override
  public int hashCode() {
    int result = bitSet;
    result = 31 * result + bitSet;
    return result;
  }

  @Override
  public String toString() {
    if (this.equals(ANY_VALUE)) {
      return "ANY_VALUE";

    } else if (this.equals(NO_POSSIBLE_VALUE)) {
      return "NO_POSSIBLE_VALUE";

    } else if (this.equals(TRUTHY)) {
      return "TRUTHY";

    } else if (this.equals(FALSY)) {
      return "FALSY";

    } else {

      StringBuilder result = new StringBuilder();
      for (SubConstraint subConstraint : SubConstraint.values()) {
        if ((this.bitSet & subConstraint.bitSet) == subConstraint.bitSet) {
          result.append("|").append(subConstraint.toString());
        }
      }

      return result.substring(1);
    }
  }
}
