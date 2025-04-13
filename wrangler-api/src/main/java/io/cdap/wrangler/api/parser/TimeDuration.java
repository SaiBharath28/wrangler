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
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token representing time duration values with units (e.g., 100ms, 1.5s, 2h)
 */
@PublicEvolving
public class TimeDuration implements Token {
    private static final Pattern DURATION_PATTERN =
            Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(ns|us|ms|s|m|h|d)$", Pattern.CASE_INSENSITIVE);

    private final long nanoseconds;
    private final String originalValue;

    public TimeDuration(String value) {
        this.originalValue = value.trim();
        this.nanoseconds = parseDuration(this.originalValue);
    }

    @Override
    public Object value() {
        return nanoseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", type().name());
        object.addProperty("value", originalValue);
        object.addProperty("nanoseconds", nanoseconds);
        return object;
    }

    /**
     * @return the duration in nanoseconds
     */
    public long getNanoseconds() {
        return nanoseconds;
    }

    /**
     * @return the duration in milliseconds
     */
    public long getMilliseconds() {
        return nanoseconds / 1_000_000;
    }

    /**
     * @return the duration in seconds
     */
    public double getSeconds() {
        return nanoseconds / 1_000_000_000.0;
    }

    private long parseDuration(String value) {
        Matcher matcher = DURATION_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    String.format("Invalid time duration format '%s'. Examples: 100ms, 1.5s, 2h", value));
        }

        double duration = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();

        switch (unit) {
            case "ns":
                return (long) duration;
            case "us":
                return (long) (duration * 1000);
            case "ms":
                return (long) (duration * 1000 * 1000);
            case "s":
                return (long) (duration * 1000 * 1000 * 1000);
            case "m":
                return (long) (duration * 60 * 1000 * 1000 * 1000L);
            case "h":
                return (long) (duration * 60 * 60 * 1000 * 1000 * 1000L);
            case "d":
                return (long) (duration * 24 * 60 * 60 * 1000 * 1000 * 1000L);
            default:
                throw new IllegalArgumentException("Unknown time unit: " + unit);
        }
    }
}