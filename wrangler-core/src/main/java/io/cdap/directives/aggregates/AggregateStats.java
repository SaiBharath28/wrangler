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
import io.cdap.wrangler.api.annotations.PublicEvolving;
import io.cdap.wrangler.api.parser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@PublicEvolving
public class AggregateStats implements Directive {
    private static final Logger LOG = LoggerFactory.getLogger(AggregateStats.class);
    public static final String NAME = "aggregate-stats";

    // Configuration parameters
    private String sizeColumn;
    private String timeColumn;
    private String outputSizeColumn;
    private String outputTimeColumn;
    private String sizeUnit = "MB";
    private String timeUnit = "s";
    private boolean calculateAverage = false;

    // Aggregation state
    private long totalBytes = 0;
    private long totalNanos = 0;
    private int rowCount = 0;

    @Override
    public UsageDefinition define() {
        return UsageDefinition.builder(NAME)
                .define("size-column", TokenType.COLUMN_NAME, "Column containing size values")
                .define("time-column", TokenType.COLUMN_NAME, "Column containing time values")
                .define("output-size-column", TokenType.COLUMN_NAME, "Column for output size results")
                .define("output-time-column", TokenType.COLUMN_NAME, "Column for output time results")
                .define("size-unit", TokenType.TEXT, "Output unit for size (B,KB,MB,GB,TB,KIB,MIB,GIB,TIB)", "MB")
                .define("time-unit", TokenType.TEXT, "Output unit for time (ns,us,ms,s,m,h,d)", "s")
                .define("calculate-average", TokenType.BOOLEAN, "Whether to calculate average instead of total", "false")
                .build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        try {
            this.sizeColumn = ((ColumnName) args.value("size-column")).value();
            this.timeColumn = ((ColumnName) args.value("time-column")).value();
            this.outputSizeColumn = ((ColumnName) args.value("output-size-column")).value();
            this.outputTimeColumn = ((ColumnName) args.value("output-time-column")).value();

            if (args.contains("size-unit")) {
                this.sizeUnit = ((Text) args.value("size-unit")).value();
            }
            if (args.contains("time-unit")) {
                this.timeUnit = ((Text) args.value("time-unit")).value();
            }
            if (args.contains("calculate-average")) {
                this.calculateAverage = ((Bool) args.value("calculate-average")).value();
            }

            validateUnits();

        } catch (Exception e) {
            throw new DirectiveParseException(
                    String.format("Invalid arguments for '%s'. %s", NAME, e.getMessage()), e);
        }
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        for (Row row : rows) {
            try {
                processRow(row);
                rowCount++;
            } catch (Exception e) {
                LOG.warn("Skipping row due to error: {}", e.getMessage());
                if (context != null && context.isStrict()) {
                    throw new DirectiveExecutionException(
                            String.format("Failed to process row %d", rowCount + 1), e);
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<Row> finalizeExecute() throws DirectiveExecutionException {
        Row result = new Row();

        try {
            double outputSize = convertBytes(totalBytes, sizeUnit);
            if (calculateAverage && rowCount > 0) {
                outputSize = outputSize / rowCount;
            }
            result.add(outputSizeColumn, outputSize);

            double outputTime = convertNanos(totalNanos, timeUnit);
            if (calculateAverage && rowCount > 0) {
                outputTime = outputTime / rowCount;
            }
            result.add(outputTimeColumn, outputTime);

        } catch (Exception e) {
            throw new DirectiveExecutionException("Failed to generate final results", e);
        }

        List<Row> results = new ArrayList<>(1);
        results.add(result);
        return results;
    }

    @Override
    public void destroy() {
        // Reset state
        totalBytes = 0;
        totalNanos = 0;
        rowCount = 0;
    }

    private void processRow(Row row) throws DirectiveExecutionException {
        Object sizeValue = row.getValue(sizeColumn);
        Object timeValue = row.getValue(timeColumn);

        if (sizeValue != null) {
            try {
                totalBytes += new ByteSize(sizeValue.toString()).getBytes();
            } catch (Exception e) {
                throw new DirectiveExecutionException(
                        String.format("Invalid size value '%s' in column '%s'", sizeValue, sizeColumn), e);
            }
        }

        if (timeValue != null) {
            try {
                totalNanos += new TimeDuration(timeValue.toString()).getNanoseconds();
            } catch (Exception e) {
                throw new DirectiveExecutionException(
                        String.format("Invalid time value '%s' in column '%s'", timeValue, timeColumn), e);
            }
        }
    }

    private void validateUnits() throws DirectiveParseException {
        try {
            convertBytes(1, sizeUnit); // Test conversion
        } catch (Exception e) {
            throw new DirectiveParseException("Invalid size unit: " + sizeUnit, e);
        }

        try {
            convertNanos(1, timeUnit); // Test conversion
        } catch (Exception e) {
            throw new DirectiveParseException("Invalid time unit: " + timeUnit, e);
        }
    }

    private double convertBytes(long bytes, String unit) {
        switch (unit.toUpperCase()) {
            case "B": return bytes;
            case "KB": return bytes / 1000.0;
            case "MB": return bytes / (1000.0 * 1000);
            case "GB": return bytes / (1000.0 * 1000 * 1000);
            case "TB": return bytes / (1000.0 * 1000 * 1000 * 1000);
            case "KIB": return bytes / 1024.0;
            case "MIB": return bytes / (1024.0 * 1024);
            case "GIB": return bytes / (1024.0 * 1024 * 1024);
            case "TIB": return bytes / (1024.0 * 1024 * 1024 * 1024);
            default: throw new IllegalArgumentException("Unsupported size unit: " + unit);
        }
    }

    private double convertNanos(long nanos, String unit) {
        switch (unit.toLowerCase()) {
            case "ns": return nanos;
            case "us": return nanos / 1000.0;
            case "ms": return nanos / (1000.0 * 1000);
            case "s": return nanos / (1000.0 * 1000 * 1000);
            case "m": return nanos / (60.0 * 1000 * 1000 * 1000);
            case "h": return nanos / (60.0 * 60 * 1000 * 1000 * 1000);
            case "d": return nanos / (24.0 * 60 * 60 * 1000 * 1000 * 1000);
            default: throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
}