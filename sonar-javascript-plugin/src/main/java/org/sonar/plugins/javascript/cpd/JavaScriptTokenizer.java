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
package org.sonar.plugins.javascript.cpd;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.typed.ActionParser;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.cpd.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.javascript.ast.visitors.SubscriptionAstTreeVisitor;
import org.sonar.javascript.model.internal.lexical.InternalSyntaxToken;
import org.sonar.javascript.parser.EcmaScriptParser;
import org.sonar.plugins.javascript.api.AstTreeVisitorContext;
import org.sonar.plugins.javascript.api.JavaScriptCheck;
import org.sonar.plugins.javascript.api.symbols.SymbolModel;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

public class JavaScriptTokenizer implements Tokenizer {

  private static final Logger LOG = LoggerFactory.getLogger(JavaScriptTokenizer.class);

  private final ActionParser<Tree> parser;

  public JavaScriptTokenizer(Charset charset) {
    this.parser = EcmaScriptParser.createParser(charset);
  }

  @Override
  public final void tokenize(SourceCode source, Tokens cpdTokens) {
    String fileName = source.getFileName();
    try {
      ScriptTree tree = (ScriptTree) parser.parse(new File(fileName));
      AstTreeVisitorContext context = new TokenVisitorContext(tree);
      new TokenVisitor(fileName, cpdTokens).scanFile(context);
    } catch (RecognitionException e) {
      LOG.debug("CPD Error: failed to parse " + fileName, e);
    }
    cpdTokens.add(TokenEntry.getEOF());
  }

  private static String getTokenImage(InternalSyntaxToken syntaxToken) {
    if (GenericTokenType.LITERAL.equals(syntaxToken.getTokenType())) {
      return GenericTokenType.LITERAL.getValue();
    }
    return syntaxToken.text();
  }

  private static class TokenVisitor extends SubscriptionAstTreeVisitor {

    private static final ImmutableList<Kind> NODES_TO_VISIT = ImmutableList.of(Kind.TOKEN);

    private final String fileName;
    private final Tokens cpdTokens;

    public TokenVisitor(String fileName, Tokens cpdTokens) {
      this.cpdTokens = cpdTokens;
      this.fileName = fileName;
    }

    @Override
    public List<Kind> nodesToVisit() {
      return NODES_TO_VISIT;
    }

    @Override
    public void visitNode(Tree tree) {
      InternalSyntaxToken token = (InternalSyntaxToken) tree;
      cpdTokens.add(new TokenEntry(getTokenImage(token), fileName, token.line()));
    }

  }

  private static class TokenVisitorContext implements AstTreeVisitorContext {
  
    private final ScriptTree topTree;
  
    public TokenVisitorContext(ScriptTree topTree) {
      this.topTree = topTree;
    }
  
    @Override
    public ScriptTree getTopTree() {
      return topTree;
    }
  
    @Override
    public void addIssue(JavaScriptCheck check, Tree tree, String message) {
      // nothing
    }
  
    @Override
    public void addIssue(JavaScriptCheck check, int line, String message) {
      // nothing
    }
  
    @Override
    public void addIssue(JavaScriptCheck check, Tree tree, String message, double cost) {
      // nothing
    }
  
    @Override
    public void addIssue(JavaScriptCheck check, int line, String message, double cost) {
      // nothing
    }
  
    @Override
    public void addFileIssue(JavaScriptCheck check, String message) {
      // nothing
    }
  
    @Override
    public File getFile() {
      return null;
    }
  
    @Override
    public SymbolModel getSymbolModel() {
      return null;
    }
  
    @Override
    public String[] getPropertyValues(String name) {
      return new String[] {};
    }
  
    @Override
    public int getComplexity(Tree tree) {
      return 0;
    }
  
  }

}
