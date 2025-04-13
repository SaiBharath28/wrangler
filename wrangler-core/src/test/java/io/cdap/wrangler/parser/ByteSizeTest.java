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
 * Tests for {@link ByteSize} parser.
 */
public class ByteSizeTest {
    private static final double DELTA = 0.0001;

    // =============================================
    // Valid Input Tests
    // =============================================

    @Test
    public void testBasicByte() {
        Assert.assertEquals(1L, new ByteSize("1B").getBytes());
    }

    @Test
    public void testDecimalUnits() {
        Assert.assertEquals(1000L, new ByteSize("1KB").getBytes());
        Assert.assertEquals(1_000_000L, new ByteSize("1MB").getBytes());
        Assert.assertEquals(1_000_000_000L, new ByteSize("1GB").getBytes());
        Assert.assertEquals(1_000_000_000_000L, new ByteSize("1TB").getBytes());
    }

    @Test
    public void testBinaryUnits() {
        Assert.assertEquals(1024L, new ByteSize("1KiB").getBytes());
        Assert.assertEquals(1_048_576L, new ByteSize("1MiB").getBytes());
        Assert.assertEquals(1_073_741_824L, new ByteSize("1GiB").getBytes());
        Assert.assertEquals(1_099_511_627_776L, new ByteSize("1TiB").getBytes());
    }

    @Test
    public void testCaseInsensitivity() {
        Assert.assertEquals(1000L, new ByteSize("1kb").getBytes());
        Assert.assertEquals(1_000_000L, new ByteSize("1Mb").getBytes());
        Assert.assertEquals(1024L, new ByteSize("1kIb").getBytes());
        Assert.assertEquals(1_048_576L, new ByteSize("1mIb").getBytes());
    }

    @Test
    public void testDecimalValues() {
        Assert.assertEquals(1500L, new ByteSize("1.5KB").getBytes());
        Assert.assertEquals(768L, new ByteSize("0.75KiB").getBytes()); // 0.75 * 1024
        Assert.assertEquals(1_610_612_736L, new ByteSize("1.5GiB").getBytes()); // 1.5 * 1024^3
    }

    @Test
    public void testLargeValues() {
        Assert.assertEquals(Long.MAX_VALUE, new ByteSize(Long.MAX_VALUE + "B").getBytes());
        Assert.assertEquals(9_223_372_036_854_775_807L, new ByteSize("9223372036854775807B").getBytes());
    }

    @Test
    public void testWhitespaceHandling() {
        Assert.assertEquals(1000L, new ByteSize(" 1 KB ").getBytes());
        Assert.assertEquals(1024L, new ByteSize(" 1  KiB ").getBytes());
    }

    // =============================================
    // Token Interface Tests
    // =============================================

    @Test
    public void testTokenInterfaceImplementation() {
        ByteSize byteSize = new ByteSize("1.5MB");
        Assert.assertEquals(1_500_000L, byteSize.value());
        Assert.assertEquals(TokenType.BYTE_SIZE, byteSize.type());
    }

    // =============================================
    // Invalid Input Tests
    // =============================================

    @Test(expected = IllegalArgumentException.class)
    public void testMissingNumber() {
        new ByteSize("KB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUnit() {
        new ByteSize("1XB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeValue() {
        new ByteSize("-1MB");
    }

    @Test(expected = NullPointerException.class)
    public void testNullInput() {
        new ByteSize(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyString() {
        new ByteSize("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat() {
        new ByteSize("1MB2KB");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleDecimalPoints() {
        new ByteSize("1.5.2MB");
    }

    // =============================================
    // Edge Case Tests
    // =============================================

    @Test
    public void testZeroValue() {
        Assert.assertEquals(0L, new ByteSize("0B").getBytes());
        Assert.assertEquals(0L, new ByteSize("0KB").getBytes());
    }

    @Test
    public void testVerySmallValues() {
        Assert.assertEquals(1L, new ByteSize("0.000001MB").getBytes()); // Rounds to 1 byte
    }

    @Test(expected = NumberFormatException.class)
    public void testValueOverflow() {
        new ByteSize("9223372036854775808B"); // Long.MAX_VALUE + 1
    }
}

