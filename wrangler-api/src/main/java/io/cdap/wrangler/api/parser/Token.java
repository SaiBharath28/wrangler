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

import com.google.gson.JsonElement;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * Interface representing a parsed token from directives. All token types must implement this interface
 * to ensure consistent behavior across different token implementations.
 *
 * <p>Implementing classes should:</p>
 * <ul>
 *   <li>Provide immutable implementations</li>
 *   <li>Maintain thread safety</li>
 *   <li>Include proper null checks in constructors</li>
 *   <li>Implement proper equals() and hashCode() methods</li>
 * </ul>
 *
 * @see ByteSize
 * @see TimeDuration
 * @see TokenType
 */
@PublicEvolving
public interface Token {
    /**
     * Returns the underlying value of the token in its canonical form.
     * The returned object should be immutable.
     *
     * @return The token's value as an Object (typically a primitive wrapper or immutable type)
     * @throws IllegalStateException if the token value cannot be computed
     */
    Object value();

    /**
     * Returns the specific type of the token from the {@link TokenType} enumeration.
     * This method should always return the same value for a given implementation.
     *
     * @return TokenType enum value representing the token's type
     */
    TokenType type();

    /**
     * Provides JSON serialization of the token's complete state including both its type and value.
     * The implementation should include all relevant information needed to reconstruct the token.
     *
     * @return JsonElement representing the complete token state
     * @throws IllegalStateException if the token cannot be serialized
     */
    JsonElement toJson();
}