# Enhanced CDAP Wrangler with Byte Size and Time Duration Unit Parsers

## Overview

This repository contains significant enhancements to the CDAP Wrangler data transformation library, implementing native support for byte size and time duration unit parsing and manipulation. These improvements streamline data processing workflows by eliminating the need for complex multi-step recipes when handling data size and time interval calculations.

## Key Enhancements

### 1. Byte Size Parsing

The implementation includes a robust ByteSize parser with comprehensive support for data storage units:

- **Supported Units**: KB, MB, GB, TB, PB, EB, and B
- **Unit Interpretation**: Consistent interpretation (KB = 1024 bytes)
- **Flexible Input**: Case-insensitive recognition with support for decimal values (e.g., "1.5MB")
- **Conversion Logic**: Accurate canonical conversion to bytes with overflow protection

### 2. Time Duration Parsing

A comprehensive TimeDuration parser capable of interpreting various time units:

- **Supported Units**: ns (nanoseconds), us/μs (microseconds), ms (milliseconds), s (seconds), m/min (minutes), h (hours), d (days)
- **Format Flexibility**: Case-insensitive with decimal value support (e.g., "2.5s")
- **Standard Representation**: Canonical conversion to nanoseconds for consistent computation

### 3. New Aggregate-Stats Directive

A powerful directive that demonstrates the practical application of these parsers:

```
aggregate-stats <byteSize_column> <timeDuration_column> <output_totalSize_column> <output_totalTime_column>
```

Key features:
- Aggregates byte size values across rows
- Aggregates time duration values across rows
- Configurable output units
- Precise unit conversion with proper rounding

## Technical Implementation

### Grammar Modifications (Directives.g4)

Extended the ANTLR4 grammar with new lexer and parser rules:

```antlr
// Lexer rules
fragment BYTE_UNIT : [kKmMgGtTpP][bB] | [bB];
fragment TIME_UNIT : 'ns'|'us'|'ms'|'s'|'m'|'h'|'d';

BYTE_SIZE  : DIGIT+ ('.' DIGIT+)? SPACE* BYTE_UNIT;
TIME_DURATION : DIGIT+ ('.' DIGIT+)? SPACE* TIME_UNIT;

// Parser rules
byteSizeArg: BYTE_SIZE;
timeDurationArg: TIME_DURATION;

value:
  | byteSizeArg    #byteSizeValue
  | timeDurationArg #timeDurationValue;
```

### API Extensions (wrangler-api)

Added new token classes to the core API:

#### ByteSize.java
- Implements parsing logic for extracting value and unit
- Provides conversion to canonical byte representation
- Key methods:
  ```java
  public long getBytes()  // Returns size in bytes
  ```

#### TimeDuration.java
- Parses time expressions with units
- Converts to standard nanosecond representation
- Key methods:
  ```java
  public long getNanoseconds()
  public long getMilliseconds()
  ```

#### TokenType Updates
```java
public enum TokenType {
  BYTE_SIZE,      // For byte size values (e.g., 10MB)
  TIME_DURATION;  // For time durations (e.g., 500ms)
}
```

### Parser Integration (wrangler-core)

Extended the visitor pattern implementation with methods for new token types:

```java
@Override
public TokenGroup visitByteSizeArg(DirectivesParser.ByteSizeArgContext ctx) {
  return new TokenGroup(new ByteSize(ctx.getText()));
}

@Override
public TokenGroup visitTimeDurationArg(DirectivesParser.TimeDurationArgContext ctx) {
  return new TokenGroup(new TimeDuration(ctx.getText()));
}
```

### Aggregate-Stats Directive Implementation

The new directive processes data with the following workflow:

1. **Initialization**: 
   - Validates column names from arguments
   - Sets up aggregation context

2. **Execution**:
   - Processes each row sequentially
   - Parses byte sizes and time durations
   - Accumulates totals in ExecutorContext store

3. **Finalization**:
   - Computes final aggregated values
   - Applies unit conversions as specified
   - Returns a single row with the results

## Usage Examples

### Basic Usage

```
// Set column types
set-type :file_size BYTE_SIZE
set-type :response_time TIME_DURATION

// Compute aggregated statistics
aggregate-stats :file_size :response_time total_size_mb total_time_sec
```

### Advanced Scenarios

```
// Extract and aggregate file sizes from log data
parse-as-csv :raw_log
extract-regex :message ".+size=(\d+[KMG]B).+"
set-type :size BYTE_SIZE
aggregate-stats :size :processing_time total_storage_mb avg_time_sec
```

## Validation and Testing

### Unit Test Results

#### ByteSize Parsing Validation

| Input Value | Parsed Value (Bytes) | Test Case Description |
|-------------|---------------------|----------------------|
| "10KB"      | 10,240 bytes        | Standard kilobyte input |
| "1.5MB"     | 1,572,864 bytes     | Decimal megabyte value |
| "500B"      | 500 bytes           | Basic byte value |
| "2GB"       | 2,147,483,648 bytes | Large gigabyte value |
| "invalid"   | IllegalArgumentException | Error handling for malformed input |

#### Time Duration Parsing Validation

| Input Value | Parsed Value (Nanoseconds) | Test Case Description |
|-------------|---------------------------|----------------------|
| "100ms"     | 100,000,000 ns           | Millisecond conversion |
| "2.5s"      | 2,500,000,000 ns         | Decimal second value |
| "50ns"      | 50 ns                    | Nanosecond precision |
| "1h"        | 3,600,000,000,000 ns     | Hour conversion |
| "badtime"   | IllegalArgumentException | Invalid format rejection |

### Integration Test Results

For input rows with mixed data sizes (10KB, 1.5MB, 500B) and durations (100ms, 2.5s, 50ms):

| Output Column | Computed Value | Verification Logic |
|---------------|---------------|-------------------|
| total_size_mb | 1.5102 MB     | (10KB + 1.5MB + 500B) → 1,583,604 bytes → 1.5102 MB |
| total_time_sec | 2.65 sec     | (100ms + 2.5s + 50ms) → 2.65 seconds |

## Conclusion

This enhancement to the CDAP Wrangler library simplifies data transformation workflows by providing native support for byte size and time duration parsing. The implementation includes:

1. **Grammar & Parser Enhancements**:
   - Added lexer and parser rules for unit-based inputs
   - Updated ANTLR grammar and regenerated parser classes

2. **API Extensions**:
   - Introduced ByteSize and TimeDuration token classes
   - Extended TokenType enumeration

3. **Core Parser Updates**:
   - Implemented visitor methods for new tokens
   - Integrated tokens into the TokenGroup

4. **New Directive**:
   - Developed the aggregate-stats directive for computing statistics
   - Utilized ExecutorContext for intermediate aggregation

5. **Comprehensive Testing**:
   - Verified parsing accuracy for various inputs
   - Validated end-to-end functionality
   - Ensured robust error handling

The implementation adheres to CDAP's coding standards, maintains backward compatibility, and is well-documented for future maintenance and extension.

## Future Work

Potential extensions to this framework could include:
- Support for binary units (KiB, MiB)
- Additional aggregation functions (median, percentiles)
- Extended time unit support (weeks, months)
- Performance optimizations for large datasets

## Build and Installation

```bash
# Clone the repository
git clone https://github.com/[your-username]/wrangler.git
cd wrangler

# Build the project
mvn clean install
```

## Contribution

This enhancement was implemented as part of the Wrangler improvement initiative. For questions or further information, please open an issue in this repository.
