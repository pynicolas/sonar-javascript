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
package org.sonar.javascript.sq52;

import com.sonar.sslr.api.Token;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.javascript.IssueSaver;
import org.sonar.javascript.JavaScriptCheckMessage;
import org.sonar.javascript.model.internal.JavaScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;

import java.util.List;
import java.util.Locale;

public class Sq52IssueSaver implements IssueSaver {

  @Override
  public void save(InputFile file, Issuable issuable, RuleKey ruleKey, JavaScriptCheckMessage message) {
    String text = message.getText(Locale.ENGLISH);
    IssueBuilder issueBuilder = issuable.newIssueBuilder().ruleKey(ruleKey);
    if (message.getPrimaryLocation() == null) {
      issueBuilder = issueBuilder
        .line(message.getLine())
        .message(text);
    } else {
      JavaScriptTree tree = (JavaScriptTree) message.getPrimaryLocation();
      NewIssueLocation location = issueBuilder.newLocation()
        .message(text)
        .onFile(file)
        .at(location(file, tree));
      issueBuilder = issueBuilder.addLocation(location);
    }
    if (message.getSecondaryLocations() != null) {
      List<Tree> secondaryLocations = message.getSecondaryLocations();
      for (Tree tree : secondaryLocations) {
        issueBuilder = issueBuilder.addLocation(issueBuilder.newLocation().onFile(file).at(location(file, (JavaScriptTree) tree)));
      }
    }
    issuable.addIssue(issueBuilder.build());
  }

  private TextRange location(InputFile file, JavaScriptTree tree) {
    Token start = tree.getToken();
    Token end = tree.getLastToken();
    TextRange range = file.newRange(start.getLine(), start.getColumn(), end.getLine(), end.getColumn() + end.getOriginalValue().length());
    return range;
  }

}
