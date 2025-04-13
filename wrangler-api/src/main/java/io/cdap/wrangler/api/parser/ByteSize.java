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

import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for parsing byte size values with units (e.g., 10KB, 1.5MB).
 */
@PublicEvolving
public class ByteSize extends Token {
    private static final Pattern BYTE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([kKmMgGtTpP]?[bB])");
    private final long bytes;

    public ByteSize(String str) {
        super(TokenType.BYTE_SIZE, str);
        this.bytes = parseBytes(str);
    }

    private long parseBytes(String str) {
        Matcher matcher = BYTE_PATTERN.matcher(str);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Invalid byte size format '%s'. Expected format like 10KB, 1.5MB", str));
        }

        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2).toUpperCase();

        switch (unit) {
            case "B":
                return (long) value;
            case "KB":
                return (long) (value * 1024L);
            case "MB":
                return (long) (value * 1024L * 1024L);
            case "GB":
                return (long) (value * 1024L * 1024L * 1024L);
            case "TB":
                return (long) (value * 1024L * 1024L * 1024L * 1024L);
            case "PB":
                return (long) (value * 1024L * 1024L * 1024L * 1024L * 1024L);
            default:
                throw new IllegalArgumentException("Unknown byte size unit: " + unit);
        }
    }

    public long getBytes() {
        return bytes;
    }
}
    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", type().name());
        object.addProperty("value", originalValue);
        object.addProperty("bytes", bytes);
        return object;
    }

    /**
     * @return the size in bytes
     */
    public long getBytes() {
        return bytes;
    }

    private long parseBytes(String value) {
        Matcher matcher = BYTE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    String.format("Invalid byte size format '%s'. Examples: 10KB, 1.5MB", value));
        }

        double size = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2).toUpperCase();

        switch (unit) {
            case "B":
                return (long) size;
            case "KB":
                return (long) (size * 1000);
            case "MB":
                return (long) (size * 1000 * 1000);
            case "GB":
                return (long) (size * 1000 * 1000 * 1000);
            case "TB":
                return (long) (size * 1000L * 1000 * 1000 * 1000);
            case "PB":
                return (long) (size * 1000L * 1000 * 1000 * 1000 * 1000);
            case "KIB":
                return (long) (size * 1024);
            case "MIB":
                return (long) (size * 1024 * 1024);
            case "GIB":
                return (long) (size * 1024 * 1024 * 1024);
            case "TIB":
                return (long) (size * 1024L * 1024 * 1024 * 1024);
            case "PIB":
                return (long) (size * 1024L * 1024 * 1024 * 1024 * 1024);
            default:
                throw new IllegalArgumentException("Unknown byte unit: " + unit);
        }
    }
}
