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

package io.cdap.wrangler.api.parser;

/**
 * Enumeration of all supported token types in the Wrangler parsing system.
 * Each token type corresponds to a specific kind of data that can be parsed
 * from directives.
 *
 * <p>When adding new token types:</p>
 * <ol>
 *   <li>Add the new enum constant here</li>
 *   <li>Create corresponding Token implementation</li>
 *   <li>Update the parser grammar to recognize the new type</li>
 *   <li>Add appropriate visitor methods in the parser</li>
 * </ol>
 */
public enum TokenType {
  /**
   * Represents a text/string value enclosed in quotes.
   * Example: "hello world", 'example'
   */
  TEXT,

  /**
   * Represents a numeric value, either integer or decimal.
   * Example: 42, 3.14, -1.5
   */
  NUMERIC,

  /**
   * Represents a boolean value (true/false).
   * Example: true, false
   */
  BOOLEAN,

  /**
   * Represents an identifier (unquoted string).
   * Example: column1, my_variable
   */
  IDENTIFIER,

  /**
   * Represents a directive name.
   * Example: parse-as-csv, set-column
   */
  DIRECTIVE_NAME,

  /**
   * Represents a column name (prefixed with colon).
   * Example: :user_id, :timestamp
   */
  COLUMN_NAME,

  /**
   * Represents a list of column names.
   * Example: :col1,:col2,:col3
   */
  COLUMN_NAME_LIST,

  /**
   * Represents a list of numeric values.
   * Example: 1,2,3,4,5
   */
  NUMERIC_LIST,

  /**
   * Represents a list of boolean values.
   * Example: true,false,true
   */
  BOOLEAN_LIST,

  /**
   * Represents a list of text values.
   * Example: "a","b","c"
   */
  TEXT_LIST,

  /**
   * Represents numeric ranges with assignments.
   * Example: 1:5="low", 6:10="high"
   */
  RANGES,

  /**
   * Represents an expression block.
   * Example: exp:{ $1 > 10 }
   */
  EXPRESSION,

  /**
   * Represents properties (key-value pairs).
   * Example: prop:{format="csv", delimiter=","}
   */
  PROPERTIES,

  /**
   * Represents byte size values with units.
   * Example: 10MB, 1.5GB, 1024KiB
   * @see ByteSize
   */
  BYTE_SIZE,

  /**
   * Represents time duration values with units.
   * Example: 100ms, 5s, 2h30m
   * @see TimeDuration
   */
  TIME_DURATION,

  /**
   * Represents a string value (alias for TEXT).
   * Maintained for backward compatibility.
   */
  STRING;

  /**
   * Returns whether this token type represents a list type.
   * @return true if this is a list type (COLUMN_NAME_LIST, NUMERIC_LIST, etc.)
   */
  public boolean isListType() {
    return this == COLUMN_NAME_LIST ||
            this == NUMERIC_LIST ||
            this == BOOLEAN_LIST ||
            this == TEXT_LIST;
  }

  /**
   * Returns whether this token type represents a numeric type.
   * @return true if this is NUMERIC or NUMERIC_LIST
   */
  public boolean isNumericType() {
    return this == NUMERIC || this == NUMERIC_LIST;
  }
}