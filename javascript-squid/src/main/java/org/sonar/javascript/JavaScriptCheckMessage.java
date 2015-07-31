/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript;

import com.google.common.collect.ImmutableList;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.plugins.javascript.api.JavaScriptCheck;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.squidbridge.api.CheckMessage;

import java.util.List;

public class JavaScriptCheckMessage extends CheckMessage {

  private Tree primaryLocation = null;
  private List<Tree> secondaryLocations = ImmutableList.of();

  public JavaScriptCheckMessage(JavaScriptCheck check, String message) {
    super(check, message);
  }

  public Tree getPrimaryLocation() {
    return primaryLocation;
  }

  public void setPrimaryLocation(Tree primaryLocation) {
    setLine(((JavaScriptTree) primaryLocation).getLine());
    this.primaryLocation = primaryLocation;
  }

  public List<Tree> getSecondaryLocations() {
    return secondaryLocations;
  }

  public void setSecondaryLocations(List<Tree> secondaryLocations) {
    this.secondaryLocations = secondaryLocations;
  }

}
