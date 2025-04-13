/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler;

import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.wrangler.api.CompileException;
import io.cdap.wrangler.api.CompileStatus;
import io.cdap.wrangler.api.Compiler;
import io.cdap.wrangler.api.DirectiveLoadException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.GrammarMigrator;
import io.cdap.wrangler.api.Pair;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.RecipeParser;
import io.cdap.wrangler.api.RecipePipeline;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.SyntaxError;
import io.cdap.wrangler.executor.RecipePipelineExecutor;
import io.cdap.wrangler.parser.GrammarBasedParser;
import io.cdap.wrangler.parser.MigrateToV2;
import io.cdap.wrangler.parser.RecipeCompiler;
import io.cdap.wrangler.proto.Contexts;
import io.cdap.wrangler.registry.CompositeDirectiveRegistry;
import io.cdap.wrangler.registry.SystemDirectiveRegistry;
import io.cdap.wrangler.schema.TransientStoreKeys;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Utility class for testing Wrangler directives and recipes.
 * Provides methods to execute recipes and validate results.
 */
public final class TestingRig {
  private static final Logger LOG = LoggerFactory.getLogger(TestingRig.class);

  private TestingRig() {
    // Prevent instantiation
  }

  /**
   * Executes a recipe and returns the output schema.
   *
   * @param recipe Array of directive strings
   * @param rows Input rows to process
   * @param inputSchema Input schema
   * @return Output schema after applying transformations
   * @throws DirectiveParseException if there's an error parsing directives
   * @throws DirectiveLoadException if directives cannot be loaded
   * @throws RecipeException if there's an error executing the recipe
   */
  public static Schema executeAndGetSchema(String[] recipe, List<Row> rows, Schema inputSchema)
          throws DirectiveParseException, DirectiveLoadException, RecipeException {
    ExecutorContext context = new TestingPipelineContext().setSchemaManagementEnabled();
    context.getTransientStore().set(TransientVariableScope.GLOBAL,
            TransientStoreKeys.INPUT_SCHEMA, inputSchema);
    execute(recipe, rows, context);
    return context.getTransientStore().get(TransientStoreKeys.OUTPUT_SCHEMA);
  }

  /**
   * Executes a recipe on input rows.
   *
   * @param recipe Array of directive strings
   * @param rows Input rows to process
   * @return Transformed rows
   * @throws DirectiveParseException if there's an error parsing directives
   * @throws DirectiveLoadException if directives cannot be loaded
   * @throws RecipeException if there's an error executing the recipe
   */
  public static List<Row> execute(String[] recipe, List<Row> rows)
          throws RecipeException, DirectiveParseException, DirectiveLoadException {
    return execute(recipe, rows, new TestingPipelineContext());
  }

  /**
   * Executes a recipe on input rows with custom context.
   *
   * @param recipe Array of directive strings
   * @param rows Input rows to process
   * @param context Execution context
   * @return Transformed rows
   * @throws DirectiveParseException if there's an error parsing directives
   * @throws RecipeException if there's an error executing the recipe
   */
  public static List<Row> execute(String[] recipe, List<Row> rows, ExecutorContext context)
          throws RecipeException, DirectiveParseException {
    try {
      CompositeDirectiveRegistry registry = new CompositeDirectiveRegistry(
              SystemDirectiveRegistry.INSTANCE
      );

      String migrate = new MigrateToV2(recipe).migrate();
      RecipeParser parser = new GrammarBasedParser(Contexts.SYSTEM, migrate, registry);
      return new RecipePipelineExecutor(parser, context).execute(rows);
    } catch (DirectiveLoadException e) {
      throw new RecipeException("Failed to load directives", e);
    }
  }

  /**
   * Executes a recipe and returns both results and errors.
   *
   * @param recipe Array of directive strings
   * @param rows Input rows to process
   * @return Pair containing results and errors
   * @throws DirectiveParseException if there's an error parsing directives
   * @throws RecipeException if there's an error executing the recipe
   */
  public static Pair<List<Row>, List<Row>> executeWithErrors(String[] recipe, List<Row> rows)
          throws RecipeException, DirectiveParseException {
    return executeWithErrors(recipe, rows, new TestingPipelineContext());
  }

  /**
   * Executes a recipe with custom context and returns both results and errors.
   *
   * @param recipe Array of directive strings
   * @param rows Input rows to process
   * @param context Execution context
   * @return Pair containing results and errors
   * @throws DirectiveParseException if there's an error parsing directives
   * @throws RecipeException if there's an error executing the recipe
   */
  public static Pair<List<Row>, List<Row>> executeWithErrors(String[] recipe, List<Row> rows, ExecutorContext context)
          throws RecipeException, DirectiveParseException {
    try {
      CompositeDirectiveRegistry registry = new CompositeDirectiveRegistry(
              SystemDirectiveRegistry.INSTANCE
      );

      String migrate = new MigrateToV2(recipe).migrate();
      RecipeParser parser = new GrammarBasedParser(Contexts.SYSTEM, migrate, registry);
      RecipePipeline pipeline = new RecipePipelineExecutor(parser, context);
      List<Row> results = pipeline.execute(rows);
      List<Row> errors = pipeline.errors();
      return new Pair<>(results, errors);
    } catch (DirectiveLoadException e) {
      throw new RecipeException("Failed to load directives", e);
    }
  }

  /**
   * Creates a RecipePipeline for a recipe.
   *
   * @param recipe Array of directive strings
   * @return Initialized RecipePipeline
   * @throws DirectiveParseException if there's an error parsing directives
   * @throws DirectiveLoadException if directives cannot be loaded
   * @throws RecipeException if there's an error creating the pipeline
   */
  public static RecipePipeline createPipeline(String[] recipe)
          throws RecipeException, DirectiveParseException, DirectiveLoadException {
    CompositeDirectiveRegistry registry = new CompositeDirectiveRegistry(
            SystemDirectiveRegistry.INSTANCE
    );

    String migrate = new MigrateToV2(recipe).migrate();
    RecipeParser parser = new GrammarBasedParser(Contexts.SYSTEM, migrate, registry);
    return new RecipePipelineExecutor(parser, new TestingPipelineContext());
  }

  /**
   * Parses a recipe without executing it.
   *
   * @param recipe Array of directive strings
   * @return RecipeParser instance
   * @throws DirectiveParseException if there's an error parsing directives
   * @throws DirectiveLoadException if directives cannot be loaded
   */
  public static RecipeParser parse(String[] recipe) throws DirectiveParseException, DirectiveLoadException {
    CompositeDirectiveRegistry registry = new CompositeDirectiveRegistry(
            SystemDirectiveRegistry.INSTANCE
    );

    String migrate = new MigrateToV2(recipe).migrate();
    return new GrammarBasedParser(Contexts.SYSTEM, migrate, registry);
  }

  /**
   * Compiles a recipe and returns compilation status.
   *
   * @param recipe Array of directive strings
   * @return CompileStatus containing compilation results
   * @throws CompileException if there's an error during compilation
   * @throws DirectiveParseException if there's an error parsing directives
   */
  public static CompileStatus compile(String[] recipe) throws CompileException, DirectiveParseException {
    GrammarMigrator migrator = new MigrateToV2(recipe);
    Compiler compiler = new RecipeCompiler();
    return compiler.compile(migrator.migrate());
  }

  /**
   * Asserts that a recipe compiles successfully.
   *
   * @param recipe Array of directive strings
   * @throws CompileException if there's an error during compilation
   * @throws DirectiveParseException if there's an error parsing directives
   */
  public static void assertCompileSuccess(String[] recipe) throws CompileException, DirectiveParseException {
    CompileStatus status = compile(recipe);
    Assert.assertTrue("Recipe should compile successfully", status.isSuccess());
  }

  /**
   * Asserts that a recipe fails to compile and prints errors.
   *
   * @param recipe Array of directive strings
   * @throws CompileException if there's an error during compilation
   * @throws DirectiveParseException if there's an error parsing directives
   */
  public static void assertCompileFailure(String[] recipe) throws CompileException, DirectiveParseException {
    CompileStatus status = compile(recipe);
    if (!status.isSuccess()) {
      Iterator<SyntaxError> iterator = status.getErrors();
      while (iterator.hasNext()) {
        SyntaxError error = iterator.next();
        LOG.error("Compilation error at line {}:{} - {}",
                error.getLine(), error.getCharPositionInLine(), error.getMessage());
      }
    }
    Assert.assertFalse("Recipe should fail compilation", status.isSuccess());
  }
}