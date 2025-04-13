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

package io.cdap.directives.aggregates;

import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.TestingRig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive tests for {@link AggregateStats} directive.
 */
public class AggregateStatsTest {
    private static final double DELTA = 0.0001;

    @Test
    public void testTotalAggregation() throws Exception {
        List<Row> rows = Arrays.asList(
                new Row("data_size", "1MB").add("response_time", "100ms"),
                new Row("data_size", "2MB").add("response_time", "200ms"),
                new Row("data_size", "0.5MB").add("response_time", "50ms")
        );

        String[] recipe = {
                "aggregate-stats :data_size :response_time total_size_mb total_time_sec MB s false"
        };

        List<Row> results = TestingRig.execute(recipe, rows);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(3.5, results.get(0).getValue("total_size_mb"), DELTA);
        Assert.assertEquals(0.35, results.get(0).getValue("total_time_sec"), DELTA);
    }

    @Test
    public void testAverageAggregation() throws Exception {
        List<Row> rows = Arrays.asList(
                new Row("size", "1MB").add("time", "100ms"),
                new Row("size", "2MB").add("time", "200ms"),
                new Row("size", "0.5MB").add("time", "50ms")
        );

        String[] recipe = {
                "aggregate-stats :size :time avg_size_mb avg_time_sec MB s true"
        };

        List<Row> results = TestingRig.execute(recipe, rows);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(1.166666, results.get(0).getValue("avg_size_mb"), DELTA);
        Assert.assertEquals(0.116666, results.get(0).getValue("avg_time_sec"), DELTA);
    }

    @Test
    public void testDifferentUnits() throws Exception {
        List<Row> rows = Arrays.asList(
                new Row("size", "1024KB").add("time", "1s"),
                new Row("size", "1MB").add("time", "1000ms")
        );

        String[] recipe = {
                "aggregate-stats :size :time total_size_gb total_time_min GB m false"
        };

        List<Row> results = TestingRig.execute(recipe, rows);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0.002048, results.get(0).getValue("total_size_gb"), DELTA);
        Assert.assertEquals(0.033333, results.get(0).getValue("total_time_min"), DELTA);
    }

    @Test
    public void testBinaryUnits() throws Exception {
        List<Row> rows = Arrays.asList(
                new Row("size", "1MiB").add("time", "1s"),
                new Row("size", "1MiB").add("time", "1s")
        );

        String[] recipe = {
                "aggregate-stats :size :time total_size_mib total_time_h MiB h false"
        };

        List<Row> results = TestingRig.execute(recipe, rows);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(2.0, results.get(0).getValue("total_size_mib"), DELTA);
        Assert.assertEquals(0.000555, results.get(0).getValue("total_time_h"), 0.000001);
    }

    @Test(expected = RuntimeException.class)
    public void testMissingColumn() throws Exception {
        List<Row> rows = Arrays.asList(
                new Row("wrong_column", "1MB").add("time", "100ms")
        );

        String[] recipe = {
                "aggregate-stats :size :time total_size total_time MB s false"
        };

        TestingRig.execute(recipe, rows);
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidDataFormat() throws Exception {
        List<Row> rows = Arrays.asList(
                new Row("size", "invalid").add("time", "100ms")
        );

        String[] recipe = {
                "aggregate-stats :size :time total_size total_time MB s false"
        };

        TestingRig.execute(recipe, rows);
    }

    @Test
    public void testEmptyInput() throws Exception {
        List<Row> rows = Collections.emptyList();

        String[] recipe = {
                "aggregate-stats :size :time total_size total_time MB s true"
        };

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0.0, results.get(0).getValue("total_size"));
        Assert.assertEquals(0.0, results.get(0).getValue("total_time"));
    }

    @Test
    public void testNullValues() throws Exception {
        List<Row> rows = Arrays.asList(
                new Row("size", null).add("time", "100ms"),
                new Row("size", "1MB").add("time", null)
        );

        String[] recipe = {
                "aggregate-stats :size :time total_size total_time MB s false"
        };

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(1.0, results.get(0).getValue("total_size"), DELTA);
        Assert.assertEquals(0.1, results.get(0).getValue("total_time"), DELTA);
    }

    @Test
    public void testCustomPrecision() throws Exception {
        List<Row> rows = Arrays.asList(
                new Row("size", "1.234MB").add("time", "123.456ms")
        );

        String[] recipe = {
                "aggregate-stats :size :time total_size total_time MB s false"
        };

        List<Row> results = TestingRig.execute(recipe, rows);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(1.234, results.get(0).getValue("total_size"), DELTA);
        Assert.assertEquals(0.123456, results.get(0).getValue("total_time"), DELTA);
    }
}