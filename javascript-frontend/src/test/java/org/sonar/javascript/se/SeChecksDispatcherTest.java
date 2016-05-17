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
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.List;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.javascript.tree.symbols.Scope;
import org.sonar.javascript.visitors.JavaScriptVisitorContext;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.visitors.Issue;
import org.sonar.plugins.javascript.api.visitors.PreciseIssue;

import static org.fest.assertions.Assertions.assertThat;

public class SeChecksDispatcherTest {

  @Test
  public void test() throws Exception {
    SeChecksDispatcher seChecksDispatcher = new SeChecksDispatcher(ImmutableList.<SeCheck>of(new TestSeCheck()));
    List<Issue> issues = seChecksDispatcher.scanFile(createContext(new File("src/test/resources/se/se_dispatcher_test.js")));
    assertThat(issues).hasSize(4);
    assertThat(((PreciseIssue) issues.get(0)).primaryLocation().message()).isEqualTo("Start of execution");
    assertThat(((PreciseIssue) issues.get(1)).primaryLocation().message()).isEqualTo("before element");
    assertThat(((PreciseIssue) issues.get(2)).primaryLocation().message()).isEqualTo("after element");
    assertThat(((PreciseIssue) issues.get(3)).primaryLocation().message()).isEqualTo("End of execution");
  }

  private class TestSeCheck extends SeCheck {
    @Override
    public void startOfExecution(Scope functionScope) {
      super.startOfExecution(functionScope);
      addIssue(functionScope.tree(), "Start of execution");
    }

    @Override
    public void endOfExecution(Scope functionScope) {
      super.endOfExecution(functionScope);
      addIssue(functionScope.tree(), "End of execution");
    }

    @Override
    public void beforeBlockElement(ProgramState currentState, Tree element) {
      super.beforeBlockElement(currentState, element);
      addIssue(element, "before element");
    }

    @Override
    public void afterBlockElement(ProgramState currentState, Tree element) {
      super.afterBlockElement(currentState, element);
      addIssue(element, "after element");
    }
  }

  private static JavaScriptVisitorContext createContext(File file) {
    ScriptTree scriptTree = (ScriptTree) JavaScriptParserBuilder.createParser(Charsets.UTF_8).parse(file);
    return new JavaScriptVisitorContext(scriptTree, file, new Settings());
  }
}
