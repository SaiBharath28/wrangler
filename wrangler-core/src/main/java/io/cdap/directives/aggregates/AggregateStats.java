/*
 * Copyright © 2023 CDAP. All rights reserved.
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

import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.*;
import io.cdap.wrangler.api.DirectiveParseException;

import java.util.ArrayList;
import java.util.List;

@Categories(categories = {"aggregate"})
public class AggregateStats implements Directive {
    public static final String NAME = "aggregate-stats";
    private String sizeColumn;
    private String timeColumn;
    private String totalSizeColumn;
    private String totalTimeColumn;
    private long totalBytes = 0;
    private long totalNanos = 0;
    private int rowCount = 0;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("size-column", TokenType.COLUMN);
        builder.define("time-column", TokenType.COLUMN);
        builder.define("total-size-column", TokenType.IDENTIFIER);
        builder.define("total-time-column", TokenType.IDENTIFIER);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.sizeColumn = ((ColumnName) args.value("size-column").value()).value();
        this.timeColumn = ((ColumnName) args.value("time-column").value()).value();
        this.totalSizeColumn = ((Text) args.value("total-size-column").value()).value();
        this.totalTimeColumn = ((Text) args.value("total-time-column").value()).value();
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveParseException {
        for (Row row : rows) {
            Object sizeObj = row.getValue(sizeColumn);
            Object timeObj = row.getValue(timeColumn);

            if (sizeObj instanceof String) {
                totalBytes += new ByteSize((String) sizeObj).getBytes();
            }

            if (timeObj instanceof String) {
                totalNanos += new TimeDuration((String) timeObj).getNanoseconds();
            }

            rowCount++;
        }

        // Return original rows during execution (aggregation happens in destroy)
        return rows;
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }

    @Override
    public List<Row> finalize() throws DirectiveParseException {
        Row result = new Row();
        result.add(totalSizeColumn, totalBytes / (1024.0 * 1024.0)); // Convert to MB
        result.add(totalTimeColumn, totalNanos / 1_000_000_000.0); // Convert to seconds

        List<Row> results = new ArrayList<>();
        results.add(result);
        return results;
    }
}
