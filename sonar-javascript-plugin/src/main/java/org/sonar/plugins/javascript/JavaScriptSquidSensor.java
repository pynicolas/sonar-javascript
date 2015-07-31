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
package org.sonar.plugins.javascript;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.source.Highlightable;
import org.sonar.javascript.EcmaScriptConfiguration;
import org.sonar.javascript.IssueSaver;
import org.sonar.javascript.JavaScriptAstScanner;
import org.sonar.javascript.JavaScriptCheckMessage;
import org.sonar.javascript.api.EcmaScriptMetric;
import org.sonar.javascript.ast.visitors.VisitorsBridge;
import org.sonar.javascript.checks.CheckList;
import org.sonar.javascript.highlighter.JavaScriptHighlighter;
import org.sonar.javascript.metrics.FileLinesVisitor;
import org.sonar.javascript.sq52.Sq52IssueSaver;
import org.sonar.plugins.javascript.api.CustomJavaScriptRulesDefinition;
import org.sonar.plugins.javascript.api.JavaScriptFileScanner;
import org.sonar.plugins.javascript.core.JavaScript;
import org.sonar.squidbridge.AstScanner;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceFunction;
import org.sonar.squidbridge.indexer.QueryByParent;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.sslr.parser.LexerlessGrammar;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JavaScriptSquidSensor implements Sensor {


  @DependedUpon
  public Collection<Metric> generatesNCLOCMetric() {
    return ImmutableList.<Metric>of(CoreMetrics.NCLOC, CoreMetrics.NCLOC_DATA);
  }

  private static final Logger LOG = LoggerFactory.getLogger(JavaScriptSquidSensor.class);
  private static final Number[] FUNCTIONS_DISTRIB_BOTTOM_LIMITS = {1, 2, 4, 6, 8, 10, 12, 20, 30};
  private static final Number[] FILES_DISTRIB_BOTTOM_LIMITS = {0, 5, 10, 20, 30, 60, 90};

  private final JavaScriptChecks checks;
  private final FileLinesContextFactory fileLinesContextFactory;
  private final ResourcePerspectives resourcePerspectives;
  private final FileSystem fileSystem;
  private final NoSonarFilter noSonarFilter;
  private final FilePredicate mainFilePredicate;
  private final PathResolver pathResolver;
  private final Settings settings;

  private SensorContext context;
  private AstScanner<LexerlessGrammar> scanner;

  public JavaScriptSquidSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory,
                               ResourcePerspectives resourcePerspectives, FileSystem fileSystem, NoSonarFilter noSonarFilter, PathResolver pathResolver, Settings settings) {
    this(checkFactory, fileLinesContextFactory, resourcePerspectives, fileSystem, noSonarFilter, pathResolver, settings, null);
  }

  public JavaScriptSquidSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory,
                               ResourcePerspectives resourcePerspectives, FileSystem fileSystem, NoSonarFilter noSonarFilter,
                               PathResolver pathResolver, Settings settings, @Nullable CustomJavaScriptRulesDefinition[] customRulesDefinition) {

    this.checks = JavaScriptChecks.createJavaScriptCheck(checkFactory)
      .addChecks(CheckList.REPOSITORY_KEY, CheckList.getChecks())
      .addCustomChecks(customRulesDefinition);
    this.fileLinesContextFactory = fileLinesContextFactory;
    this.resourcePerspectives = resourcePerspectives;
    this.fileSystem = fileSystem;
    this.noSonarFilter = noSonarFilter;
    this.pathResolver = pathResolver;
    this.mainFilePredicate = fileSystem.predicates().and(
      fileSystem.predicates().hasType(InputFile.Type.MAIN),
      fileSystem.predicates().hasLanguage(JavaScript.KEY));
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fileSystem.hasFiles(mainFilePredicate);
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    this.context = context;

    List<CodeVisitor> astNodeVisitors = Lists.newArrayList();
    List<JavaScriptFileScanner> treeVisitors = Lists.newArrayList();

    for (CodeVisitor visitor : checks.all()) {
      if (visitor instanceof JavaScriptFileScanner) {
        treeVisitors.add((JavaScriptFileScanner) visitor);
      } else {
        astNodeVisitors.add(visitor);
      }
    }

    astNodeVisitors.add(new VisitorsBridge(treeVisitors, resourcePerspectives, fileSystem, settings));
    astNodeVisitors.add(new FileLinesVisitor(fileLinesContextFactory, fileSystem, pathResolver));

    scanner = JavaScriptAstScanner.create(createConfiguration(), astNodeVisitors.toArray(new SquidAstVisitor[astNodeVisitors.size()]));
    scanner.scanFiles(Lists.newArrayList(fileSystem.files(mainFilePredicate)));

    Collection<SourceCode> squidSourceFiles = scanner.getIndex().search(new QueryByType(SourceFile.class));
    save(squidSourceFiles);

    highlight();
  }

  private void highlight() {
    JavaScriptHighlighter highlighter = new JavaScriptHighlighter(createConfiguration());

    for (InputFile inputFile : fileSystem.inputFiles(mainFilePredicate)) {
      Highlightable perspective = resourcePerspectives.as(Highlightable.class, inputFile);

      if (perspective != null) {
        highlighter.highlight(perspective, inputFile.file());

      } else {
        LOG.warn("Could not get " + Highlightable.class.getCanonicalName() + " for " + inputFile.file());
      }
    }
  }

  private EcmaScriptConfiguration createConfiguration() {
    return new EcmaScriptConfiguration(fileSystem.encoding());
  }

  private void save(Collection<SourceCode> squidSourceFiles) {
    for (SourceCode squidSourceFile : squidSourceFiles) {
      SourceFile squidFile = (SourceFile) squidSourceFile;

      String relativePath = pathResolver.relativePath(fileSystem.baseDir(), new java.io.File(squidFile.getKey()));
      File sonarFile = context.getResource(File.create(relativePath));
      InputFile inputFile = fileSystem.inputFile(fileSystem.predicates().hasRelativePath(relativePath));

      if (sonarFile != null) {
        noSonarFilter.addResource(sonarFile, squidFile.getNoSonarTagLines());
        saveClassComplexity(sonarFile, squidFile);
        saveFilesComplexityDistribution(sonarFile, squidFile);
        saveFunctionsComplexityAndDistribution(sonarFile, squidFile);
        saveMeasures(sonarFile, squidFile);
        saveIssues(inputFile, sonarFile, squidFile);

      } else {
        LOG.warn("Cannot save analysis information for file {}. Unable to retrieve the associated sonar resource.", squidFile.getKey());
      }
    }
  }

  private void saveMeasures(File sonarFile, SourceFile squidFile) {
    context.saveMeasure(sonarFile, CoreMetrics.LINES, squidFile.getDouble(EcmaScriptMetric.LINES));
    context.saveMeasure(sonarFile, CoreMetrics.NCLOC, squidFile.getDouble(EcmaScriptMetric.LINES_OF_CODE));
    context.saveMeasure(sonarFile, CoreMetrics.CLASSES, squidFile.getDouble(EcmaScriptMetric.CLASSES));
    context.saveMeasure(sonarFile, CoreMetrics.FUNCTIONS, squidFile.getDouble(EcmaScriptMetric.FUNCTIONS));
    context.saveMeasure(sonarFile, CoreMetrics.ACCESSORS, squidFile.getDouble(EcmaScriptMetric.ACCESSORS));
    context.saveMeasure(sonarFile, CoreMetrics.STATEMENTS, squidFile.getDouble(EcmaScriptMetric.STATEMENTS));
    context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY, squidFile.getDouble(EcmaScriptMetric.COMPLEXITY));
    context.saveMeasure(sonarFile, CoreMetrics.COMMENT_LINES, squidFile.getDouble(EcmaScriptMetric.COMMENT_LINES));
  }

  private void saveClassComplexity(org.sonar.api.resources.File sonarFile, SourceFile squidFile) {
    double complexityInClasses = 0;
    Set<SourceCode> children = squidFile.getChildren();

    if (children != null) {
      for (SourceCode sourceCode : squidFile.getChildren()) {
        if (sourceCode.isType(SourceClass.class)) {
          complexityInClasses += sourceCode.getDouble(EcmaScriptMetric.COMPLEXITY);
        }
      }
    }
    context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY_IN_CLASSES, complexityInClasses);
  }

  private void saveFunctionsComplexityAndDistribution(File sonarFile, SourceFile squidFile) {
    Collection<SourceCode> squidFunctionsInFile = scanner.getIndex().search(new QueryByParent(squidFile), new QueryByType(SourceFunction.class));
    RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, FUNCTIONS_DISTRIB_BOTTOM_LIMITS);
    double complexityInFunction = 0;
    for (SourceCode squidFunction : squidFunctionsInFile) {
      double functionComplexity = squidFunction.getDouble(EcmaScriptMetric.COMPLEXITY);
      complexityDistribution.add(functionComplexity);
      complexityInFunction += functionComplexity;
    }
    context.saveMeasure(sonarFile, complexityDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
    context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY_IN_FUNCTIONS, complexityInFunction);
  }

  private void saveFilesComplexityDistribution(File sonarFile, SourceFile squidFile) {
    RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, FILES_DISTRIB_BOTTOM_LIMITS);
    complexityDistribution.add(squidFile.getDouble(EcmaScriptMetric.COMPLEXITY));
    context.saveMeasure(sonarFile, complexityDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
  }

  private static final IssueSaver ISSUE_SAVER = isSq52() ? new Sq52IssueSaver() : new Sq45IssueSaver();

  private static boolean isSq52() {
    try {
      IssueBuilder.class.getMethod("newLocation");
    } catch (NoSuchMethodException e) {
      return false;
    }
    return true;
  }

  private void saveIssues(InputFile inputFile, File sonarFile, SourceFile squidFile) {
    Collection<CheckMessage> messages = squidFile.getCheckMessages();
    if (messages != null) {

      for (CheckMessage message : messages) {
        RuleKey ruleKey = checks.ruleKeyFor((CodeVisitor) message.getCheck());
        Issuable issuable = resourcePerspectives.as(Issuable.class, sonarFile);

        if (issuable != null && ruleKey != null) {
          ISSUE_SAVER.save(inputFile, issuable, ruleKey, (JavaScriptCheckMessage) message);
        }
      }
    }
  }

  private static class Sq45IssueSaver implements IssueSaver {

    @Override
    public void save(InputFile file, Issuable issuable, RuleKey ruleKey, JavaScriptCheckMessage message) {
      Issue issue = issuable.newIssueBuilder()
        .ruleKey(ruleKey)
        .line(message.getLine())
        .message(message.getText(Locale.ENGLISH))
        .build();
      issuable.addIssue(issue);
    }

  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
