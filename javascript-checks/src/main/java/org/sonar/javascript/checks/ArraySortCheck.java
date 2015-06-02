/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * dev@sonar.codehaus.org
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
package org.sonar.javascript.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.javascript.ast.resolve.type.PrimitiveType;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.CallExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.DotMemberExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.visitors.BaseTreeVisitor;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2871",
  name = "A compare function should be provided when using Array.prototype.sort()",
  priority = Priority.MAJOR,
  tags = {Tags.BUG})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("10min")
public class ArraySortCheck extends BaseTreeVisitor {

  @Override
  public void visitCallExpression(CallExpressionTree tree) {
    tree.callee();
    if (tree.arguments().parameters().isEmpty() && isArraySort(tree.callee())){
      getContext().addIssue(this, tree, "Provide a compare function.");
    }

    super.visitCallExpression(tree);
  }

  private boolean isArraySort(ExpressionTree callee) {
    if (callee.is(Tree.Kind.DOT_MEMBER_EXPRESSION)){
      DotMemberExpressionTree dotMemberExpressionTree = (DotMemberExpressionTree)callee;
      ExpressionTree property = dotMemberExpressionTree.property();
      boolean isSortProperty = (property instanceof IdentifierTree) && (((IdentifierTree) property).name().equals("sort"));
      boolean isArrayObject = dotMemberExpressionTree.object().types().contains(PrimitiveType.ARRAY);
      return isSortProperty && isArrayObject;
    }
    return false;
  }

}
