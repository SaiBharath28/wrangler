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

import org.junit.Assert;
import org.junit.Test;

/**
 * Comprehensive tests for {@link TimeDuration} parser.
 */
public class TimeDurationTest {
    private static final double DELTA = 0.0001;

    // =============================================
    // Valid Input Tests
    // =============================================

    @Test
    public void testBasicNanosecond() {
        Assert.assertEquals(1L, new TimeDuration("1ns").getNanoseconds());
    }

    @Test
    public void testAllTimeUnits() {
        Assert.assertEquals(1L, new TimeDuration("1ns").getNanoseconds());
        Assert.assertEquals(1000L, new TimeDuration("1us").getNanoseconds());
        Assert.assertEquals(1_000_000L, new TimeDuration("1ms").getNanoseconds());
        Assert.assertEquals(1_000_000_000L, new TimeDuration("1s").getNanoseconds());
        Assert.assertEquals(60L * 1_000_000_000, new TimeDuration("1m").getNanoseconds());
        Assert.assertEquals(60L * 60 * 1_000_000_000, new TimeDuration("1h").getNanoseconds());
        Assert.assertEquals(24L * 60 * 60 * 1_000_000_000, new TimeDuration("1d").getNanoseconds());
    }

    @Test
    public void testCaseInsensitivity() {
        Assert.assertEquals(1_000_000L, new TimeDuration("1MS").getNanoseconds());
        Assert.assertEquals(1_000_000L, new TimeDuration("1ms").getNanoseconds());
        Assert.assertEquals(1_000_000L, new TimeDuration("1Ms").getNanoseconds());
    }

    @Test
    public void testDecimalValues() {
        Assert.assertEquals(1_500_000_000L, new TimeDuration("1.5s").getNanoseconds());
        Assert.assertEquals(750_000_000L, new TimeDuration("0.75s").getNanoseconds());
        Assert.assertEquals(90_000_000_000L, new TimeDuration("1.5m").getNanoseconds());
        Assert.assertEquals(1_500_000L, new TimeDuration("1.5ms").getNanoseconds());
    }

    @Test
    public void testWhitespaceHandling() {
        Assert.assertEquals(1_000_000L, new TimeDuration(" 1 ms ").getNanoseconds());
        Assert.assertEquals(60_000_000_000L, new TimeDuration(" 1  m ").getNanoseconds());
    }

    // =============================================
    // Edge Case Tests
    // =============================================

    @Test
    public void testZeroValue() {
        Assert.assertEquals(0L, new TimeDuration("0ns").getNanoseconds());
        Assert.assertEquals(0L, new TimeDuration("0s").getNanoseconds());
    }

    @Test
    public void testVerySmallValues() {
        Assert.assertEquals(1L, new TimeDuration("0.000001ms").getNanoseconds()); // Rounds to 1 ns
    }

    @Test
    public void testLargeValues() {
        Assert.assertEquals(Long.MAX_VALUE, new TimeDuration(Long.MAX_VALUE + "ns").getNanoseconds());
    }

    // =============================================
    // Token Interface Tests
    // =============================================

    @Test
    public void testTokenInterfaceImplementation() {
        TimeDuration duration = new TimeDuration("2.5h");
        Assert.assertEquals(2.5 * 60 * 60 * 1_000_000_000L, duration.value());
        Assert.assertEquals(TokenType.TIME_DURATION, duration.type());
    }

    // =============================================
    // Invalid Input Tests
    // =============================================

    @Test(expected = IllegalArgumentException.class)
    public void testMissingNumber() {
        new TimeDuration("ms");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new TimeDuration("1xs");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeValue() {
        new TimeDuration("-1s");
    }

    @Test(expected = NullPointerException.class)
    public void testNullInput() {
        new TimeDuration(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyString() {
        new TimeDuration("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat() {
        new TimeDuration("1s2ms");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleDecimalPoints() {
        new TimeDuration("1.5.2s");
    }

    @Test(expected = NumberFormatException.class)
    public void testValueOverflow() {
        new TimeDuration("9223372036854775808ns"); // Long.MAX_VALUE + 1
    }

    // =============================================
    // Conversion Method Tests
    // =============================================

    @Test
    public void testGetMilliseconds() {
        TimeDuration duration = new TimeDuration("1500ms");
        Assert.assertEquals(1500L, duration.getMilliseconds());
    }

    @Test
    public void testGetSeconds() {
        TimeDuration duration = new TimeDuration("2.5s");
        Assert.assertEquals(2.5, duration.getSeconds(), DELTA);
    }
}