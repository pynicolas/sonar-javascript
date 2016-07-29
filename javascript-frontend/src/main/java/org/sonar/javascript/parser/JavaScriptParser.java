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
package org.sonar.javascript.parser;

import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Iterator;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;

public class JavaScriptParser extends ActionParser<Tree> {

  public JavaScriptParser(Charset charset) {
    super(
      charset,
      JavaScriptLegacyGrammar.createGrammarBuilder(),
      JavaScriptGrammar.class,
      new TreeFactory(),
      new JavaScriptNodeBuilder(),
      JavaScriptLegacyGrammar.SCRIPT);
  }

  @Override
  public Tree parse(File file) {
    return setParents(super.parse(file));
  }

  @Override
  public Tree parse(String source) {
    return setParents(super.parse(source));
  }

  private static Tree setParents(Tree tree) {
    JavaScriptTree jsTree = (JavaScriptTree) tree;
    Iterator<Tree> childrenIterator = jsTree.childrenIterator();
    while (childrenIterator.hasNext()) {
      JavaScriptTree child = (JavaScriptTree) childrenIterator.next();
      if (child != null) {
        child.setParent(tree);
        if (!child.isLeaf()) {
          setParents(child);
        }
      }
    }
    return tree;
  }

}
