/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cdap.wrangler.api;

/**
 * An interface for managing directive aliases.
 *
 * <p>This interface provides a way to check if a directive
 * has an alias and to retrieve the alias name for a directive.
 */
public interface DirectiveAlias {

  /**
   * Checks if the given directive has an alias.
   *
   * @param directive the directive name to check
   * @return {@code true} if the directive has an alias, {@code false} otherwise
   */
  boolean hasAlias(String directive);

  /**
   * Returns the alias for the given directive.
   *
   * @param directive the directive name
   * @return the alias of the directive, or the original directive if none exists
   */
  String getAlias(String directive);
}
