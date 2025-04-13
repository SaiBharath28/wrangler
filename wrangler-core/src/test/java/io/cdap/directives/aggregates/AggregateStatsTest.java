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
import io.cdap.wrangler.parser.GrammarBasedParser;
import io.cdap.wrangler.parser.RecipeCompiler;
import io.cdap.wrangler.parser.RecipeParser;
import io.cdap.wrangler.parser.TokenizedRecipe;
import io.cdap.wrangler.test.TestingRig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AggregateStatsTest {

    @Test
    public void testAggregateStats() throws Exception {
        String[] recipe = new String[] {
                "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
        };

        List<Row> rows = Arrays.asList(
                new Row("data_size", "10KB").add("response_time", "100ms"),
                new Row("data_size", "1.5MB").add("response_time", "2.5s"),
                new Row("data_size", "500B").add("response_time", "50ms")
        );

        List<Row> results = TestingRig.execute(recipe, rows);

        Assert.assertEquals(1, results.size());
        Row result = results.get(0);

        // 10KB + 1.5MB + 500B = 10240 + 1572864 + 500 = 1583604 bytes = ~1.5102 MB
        Assert.assertEquals(1.5102, (double) result.getValue("total_size_mb"), 0.001);

        // 100ms + 2.5s + 50ms = 0.1 + 2.5 + 0.05 = 2.65 seconds
        Assert.assertEquals(2.65, (double) result.getValue("total_time_sec"), 0.001);
    }
}
