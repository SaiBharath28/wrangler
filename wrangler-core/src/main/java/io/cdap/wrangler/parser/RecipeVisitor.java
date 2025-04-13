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

package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.LazyNumber;
import io.cdap.wrangler.api.RecipeSymbol;
import io.cdap.wrangler.api.SourceInfo;
import io.cdap.wrangler.api.Triplet;
import io.cdap.wrangler.api.parser.Bool;
import io.cdap.wrangler.api.parser.BoolList;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.ColumnNameList;
import io.cdap.wrangler.api.parser.DirectiveName;
import io.cdap.wrangler.api.parser.Expression;
import io.cdap.wrangler.api.parser.Identifier;
import io.cdap.wrangler.api.parser.Numeric;
import io.cdap.wrangler.api.parser.NumericList;
import io.cdap.wrangler.api.parser.Properties;
import io.cdap.wrangler.api.parser.Ranges;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TextList;
import io.cdap.wrangler.api.parser.Token;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.parser.grammar.DirectivesParser;
import io.cdap.wrangler.parser.grammar.DirectivesBaseVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.cdap.wrangler.parser.grammar.DirectivesParser;
import io.cdap.wrangler.parser.grammar.DirectivesParser.ByteSizeArgContext;
import io.cdap.wrangler.parser.grammar.DirectivesParser.TimeDurationArgContext;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class <code>RecipeVisitor</code> implements the visitor pattern
 * used during traversal of the AST tree. The <code>ParserTree#Walker</code>
 * invokes appropriate methods as call backs with information about the node.
 *
 * <p>In order to understand what's being invoked, please look at the grammar file
 * <tt>Directive.g4</tt></p>.
 *
 * <p>This class exposes a <code>getTokenGroups</code> method for retrieving the
 * <code>RecipeSymbol</code> after visiting. The <code>RecipeSymbol</code> represents
 * all the <code>TokenGroup</code> for all directives in a recipe. Each directive
 * will create a <code>TokenGroup</code></p>
 *
 * <p> As the <code>ParseTree</code> is walking through the call graph, it generates
 * one <code>TokenGroup</code> for each directive in the recipe. Each <code>TokenGroup</code>
 * contains parsed <code>Tokens</code> for that directive along with more information like
 * <code>SourceInfo</code>. A collection of <code>TokenGroup</code> consistutes a <code>RecipeSymbol</code>
 * that is returned by this function.</p>
 */
public final class RecipeVisitor extends DirectivesBaseVisitor<RecipeSymbol.Builder> {
  private static final Logger LOG = LoggerFactory.getLogger(RecipeVisitor.class);
  private RecipeSymbol.Builder builder = new RecipeSymbol.Builder();

  /**
   * Returns a <code>RecipeSymbol</code> for the recipe being parsed. This
   * object has all the tokens that were successfully parsed along with source
   * information for each directive in the recipe.
   *
   * @return An compiled object after parsing the recipe.
   */
  public RecipeSymbol getCompiledUnit() {
    return builder.build();
  }

  /**
   * A Recipe is made up of Directives and Directives is made up of each individual
   * Directive. This method is invoked on every visit to a new directive in the recipe.
   */
  @Override
  public RecipeSymbol.Builder visitDirective(DirectivesParser.DirectiveContext ctx) {
    builder.createTokenGroup(getOriginalSource(ctx));
    return super.visitDirective(ctx);
  }

  /**
   * A Directive can include identifiers, this method extracts that token that is being
   * identified as token of type <code>Identifier</code>.
   */
  @Override
  public RecipeSymbol.Builder visitIdentifier(DirectivesParser.IdentifierContext ctx) {
    builder.addToken(new Identifier(ctx.Identifier().getText()));
    return super.visitIdentifier(ctx);
  }

  // Add these methods to your RecipeVisitor class

  @Override
  public TokenGroup visitByteSizeArg(DirectivesParser.ByteSizeArgContext ctx) {
    return new TokenGroup(new ByteSize(ctx.getText()));
  }

  @Override
  public TokenGroup visitTimeDurationArg(DirectivesParser.TimeDurationArgContext ctx) {
    return new TokenGroup(new TimeDuration(ctx.getText()));
  }

  // Update the visitValue method to handle the new types
  @Override
  public TokenGroup visitValue(DirectivesParser.ValueContext ctx) {
    if (ctx.numericValue() != null) {
      return visitNumber(ctx.numericValue().number());
    } else if (ctx.stringValue() != null) {
      return visitText(ctx.stringValue().text());
    } else if (ctx.identifierValue() != null) {
      return visitIdentifier(ctx.identifierValue().identifier());
    } else if (ctx.booleanValue() != null) {
      return visitBool(ctx.booleanValue().bool());
    } else if (ctx.byteSizeValue() != null) {
      return visitByteSizeArg(ctx.byteSizeValue().byteSizeArg());
    } else if (ctx.timeDurationValue() != null) {
      return visitTimeDurationArg(ctx.timeDurationValue().timeDurationArg());
    }
    throw new IllegalArgumentException("Unsupported value type");
  }

  /**
   * A Directive can include properties (which are a collection of key and value pairs),
   * this method extracts that token that is being identified as token of type <code>Properties</code>.
   */
  @Override
  public RecipeSymbol.Builder visitPropertyList(DirectivesParser.PropertyListContext ctx) {
    Map<String, Token> props = new HashMap<>();
    List<DirectivesParser.PropertyContext> properties = ctx.property();
    for (DirectivesParser.PropertyContext property : properties) {
      String identifier = property.Identifier().getText();
      Token token;
      if (property.number() != null) {
        token = new Numeric(new LazyNumber(property.number().getText()));
      } else if (property.bool() != null) {
        token = new Bool(Boolean.valueOf(property.bool().getText()));
      } else {
        String text = property.text().getText();
        token = new Text(text.substring(1, text.length() - 1));
      }
      props.put(identifier, token);
    }
    builder.addToken(new Properties(props));
    return builder;
  }

  /**
   * A Pragma is an instruction to the compiler to dynamically load the directives being specified
   * from the <code>DirectiveRegistry</code>. These do not affect the data flow.
   *
   * <p>E.g. <code>#pragma load-directives test1, test2, test3;</code> will collect the tokens
   * test1, test2 and test3 as dynamically loadable directives. <p>
   */
  @Override
  public RecipeSymbol.Builder visitPragmaLoadDirective(DirectivesParser.PragmaLoadDirectiveContext ctx) {
    List<TerminalNode> identifiers = ctx.identifierList().Identifier();
    for (TerminalNode identifier : identifiers) {
      builder.addLoadableDirective(identifier.getText());
    }
    return builder;
  }

  /**
   * A Pragma version is a informational directive to notify compiler about the grammar that is should
   * be using to parse the directives below.
   */
  @Override
  public RecipeSymbol.Builder visitPragmaVersion(DirectivesParser.PragmaVersionContext ctx) {
    builder.addVersion(ctx.Number().getText());
    return builder;
  }

  /**
   * A Directive can include number ranges like start:end=value[,start:end=value]*. This
   * visitor method allows you to collect all the number ranges and create a token type
   * <code>Ranges</code>.
   */
  @Override
  public RecipeSymbol.Builder visitNumberRanges(DirectivesParser.NumberRangesContext ctx) {
    List<Triplet<Numeric, Numeric, String>> output = new ArrayList<>();
    List<DirectivesParser.NumberRangeContext> ranges = ctx.numberRange();
    for (DirectivesParser.NumberRangeContext range : ranges) {
      List<TerminalNode> numbers = range.Number();
      String text = range.value().getText();
      if (text.startsWith("'") && text.endsWith("'")) {
        text = text.substring(1, text.length() - 1);
      }
      Triplet<Numeric, Numeric, String> val =
              new Triplet<>(new Numeric(new LazyNumber(numbers.get(0).getText())),
                      new Numeric(new LazyNumber(numbers.get(1).getText())),
                      text
              );
      output.add(val);
    }
    builder.addToken(new Ranges(output));
    return builder;
  }

  /**
   * This visitor method extracts the custom directive name specified. The custom
   * directives are specified with a bang (!) at the start.
   */
  @Override
  public RecipeSymbol.Builder visitEcommand(DirectivesParser.EcommandContext ctx) {
    builder.addToken(new DirectiveName(ctx.Identifier().getText()));
    return builder;
  }

  /**
   * A Directive can consist of column specifiers. These are columns that the directive
   * would operate on. When a token of type column is visited, it would generate a token
   * type of type <code>ColumnName</code>.
   */
  @Override
  public RecipeSymbol.Builder visitColumn(DirectivesParser.ColumnContext ctx) {
    builder.addToken(new ColumnName(ctx.Column().getText().substring(1)));
    return builder;
  }

  /**
   * A Directive can consist of text field. These type of fields are enclosed within
   * a single-quote or a double-quote. This visitor method extracts the string value
   * within the quotes and creates a token type <code>Text</code>.
   */
  @Override
  public RecipeSymbol.Builder visitText(DirectivesParser.TextContext ctx) {
    String value = ctx.String().getText();
    builder.addToken(new Text(value.substring(1, value.length() - 1)));
    return builder;
  }

  /**
   * A Directive can consist of numeric field. This visitor method extracts the
   * numeric value <code>Numeric</code>.
   */
  @Override
  public RecipeSymbol.Builder visitNumber(DirectivesParser.NumberContext ctx) {
    LazyNumber number = new LazyNumber(ctx.Number().getText());
    builder.addToken(new Numeric(number));
    return builder;
  }

  /**
   * A Directive can consist of Bool field. The Bool field is represented as
   * either true or false. This visitor method extract the bool value into a
   * token type <code>Bool</code>.
   */
  @Override
  public RecipeSymbol.Builder visitBool(DirectivesParser.BoolContext ctx) {
    builder.addToken(new Bool(Boolean.valueOf(ctx.Bool().getText())));
    return builder;
  }

  /**
   * A Directive can include a expression or a condition to be evaluated. When
   * such a token type is found, the visitor extracts the expression and generates
   * a token type <code>Expression</code> to be added to the <code>TokenGroup</code>
   */
  @Override
  public RecipeSymbol.Builder visitCondition(DirectivesParser.ConditionContext ctx) {
    int childCount = ctx.getChildCount();
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < childCount - 1; ++i) {
      ParseTree child = ctx.getChild(i);
      sb.append(child.getText()).append(" ");
    }
    builder.addToken(new Expression(sb.toString()));
    return builder;
  }

  /**
   * A Directive has name and in the parsing context it's called a command.
   * This visitor methods extracts the command and creates a toke type <code>DirectiveName</code>
   */
  @Override
  public RecipeSymbol.Builder visitCommand(DirectivesParser.CommandContext ctx) {
    builder.addToken(new DirectiveName(ctx.Identifier().getText()));
    return builder;
  }

  /**
   * This visitor methods extracts the list of columns specified. It creates a token
   * type <code>ColumnNameList</code> to be added to <code>TokenGroup</code>.
   */
  @Override
  public RecipeSymbol.Builder visitColList(DirectivesParser.ColListContext ctx) {
    List<TerminalNode> columns = ctx.Column();
    List<String> names = new ArrayList<>();
    for (TerminalNode column : columns) {
      names.add(column.getText().substring(1));
    }
    builder.addToken(new ColumnNameList(names));
    return builder;
  }

  /**
   * This visitor methods extracts the list of numeric specified. It creates a token
   * type <code>NumericList</code> to be added to <code>TokenGroup</code>.
   */
  @Override
  public RecipeSymbol.Builder visitNumberList(DirectivesParser.NumberListContext ctx) {
    List<TerminalNode> numbers = ctx.Number();
    List<LazyNumber> numerics = new ArrayList<>();
    for (TerminalNode number : numbers) {
      numerics.add(new LazyNumber(number.getText()));
    }
    builder.addToken(new NumericList(numerics));
    return builder;
  }

  /**
   * This visitor methods extracts the list of booleans specified. It creates a token
   * type <code>BoolList</code> to be added to <code>TokenGroup</code>.
   */
  @Override
  public RecipeSymbol.Builder visitBoolList(DirectivesParser.BoolListContext ctx) {
    List<TerminalNode> bools = ctx.Bool();
    List<Boolean> booleans = new ArrayList<>();
    for (TerminalNode bool : bools) {
      booleans.add(Boolean.parseBoolean(bool.getText()));
    }
    builder.addToken(new BoolList(booleans));
    return builder;
  }

  /**
   * This visitor methods extracts the list of strings specified. It creates a token
   * type <code>StringList</code> to be added to <code>TokenGroup</code>.
   */
  @Override
  public RecipeSymbol.Builder visitStringList(DirectivesParser.StringListContext ctx) {
    List<TerminalNode> strings = ctx.String();
    List<String> strs = new ArrayList<>();
    for (TerminalNode string : strings) {
      String text = string.getText();
      strs.add(text.substring(1, text.length() - 1));
    }
    builder.addToken(new TextList(strs));
    return builder;
  }

  /**
   * Visits a byte size argument context and creates a ByteSize token.
   *
   * @param ctx The parse tree node for byte size argument
   * @return RecipeSymbol.Builder containing the parsed ByteSize token
   * @throws IllegalArgumentException if the byte size format is invalid
   */
  @Override
  public RecipeSymbol.Builder visitByteSizeArg(DirectivesParser.ByteSizeArgContext ctx) {
    if (ctx == null || ctx.getText() == null) {
      throw new IllegalArgumentException("Byte size argument context cannot be null");
    }

    String input = ctx.getText().trim();
    LOG.debug("Parsing byte size argument: '{}'", input);

    try {
      ByteSize byteSize = new ByteSize(input);
      LOG.trace("Successfully parsed byte size: {}", byteSize);
      builder.addToken(byteSize);
      return builder;
    } catch (IllegalArgumentException e) {
      String errorMsg = String.format(
              "Invalid byte size format '%s'. Expected format like 10KB, 1.5MB, 2GiB. %s",
              input,
              e.getMessage()
      );
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  /**
   * Visits a time duration argument context and creates a TimeDuration token.
   *
   * @param ctx The parse tree node for time duration argument
   * @return RecipeSymbol.Builder containing the parsed TimeDuration token
   * @throws IllegalArgumentException if the time duration format is invalid
   */
  @Override
  public RecipeSymbol.Builder visitTimeDurationArg(DirectivesParser.TimeDurationArgContext ctx) {
    if (ctx == null || ctx.getText() == null) {
      throw new IllegalArgumentException("Time duration argument context cannot be null");
    }

    String input = ctx.getText().trim();
    LOG.debug("Parsing time duration argument: '{}'", input);

    try {
      TimeDuration timeDuration = new TimeDuration(input);
      LOG.trace("Successfully parsed time duration: {}", timeDuration);
      builder.addToken(timeDuration);
      return builder;
    } catch (IllegalArgumentException e) {
      String errorMsg = String.format(
              "Invalid time duration format '%s'. Expected format like 100ms, 1.5s, 2h. %s",
              input,
              e.getMessage()
      );
      LOG.error(errorMsg);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  private SourceInfo getOriginalSource(ParserRuleContext ctx) {
    int a = ctx.getStart().getStartIndex();
    int b = ctx.getStop().getStopIndex();
    Interval interval = new Interval(a, b);
    String text = ctx.start.getInputStream().getText(interval);
    int lineno = ctx.getStart().getLine();
    int column = ctx.getStart().getCharPositionInLine();
    return new SourceInfo(lineno, column, text);
  }
}