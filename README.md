##Overview

This Java program processes AWS VPC flow logs and maps each entry to a specific tag based on a lookup table. The lookup table is defined in a CSV file with the following columns: dstport, protocol, and tag. The program outputs the count of tags and counts for each port/protocol combination.

##Features

Parses flow log data (only default format, version 2 is supported).
Maps flow logs to tags based on a CSV lookup table.
Generates summary reports:
Count of matches for each tag.
Count of matches for each port/protocol combination.
Supports case-insensitive matching of protocol names.

##Assumptions
The program supports only the default VPC flow log format and version 2.
The flow log file size can be up to 10 MB.
The lookup table can contain up to 10,000 mappings.
Input files are plain text (ASCII).
Tags can map to multiple port/protocol combinations.
The solution is designed to work on local machines without requiring external libraries like Hadoop, Spark, or Pandas.</n>

## Project Structure


```bash
protocol_tag/
├── src/
│   └── com/
│       └── assessment/
│           └── illumino/
│               ├── main/
│               │   └── ProtocolLogTag.java
│               ├── test/
│               │   └── ProtocolLogTagTest.java
│     
├── README.md
```

##Prerequisites

Java 17 (or higher)

##Compilation and Running Instructions

Using Command Line

Navigate to the source directory:

```bash

cd src
##Compile the Java files:

javac com/assessment/illumino/main/ProtocolLogTag.java

##Run the program:

java com.assessment.illumino.main.ProtocolLogTag <flow_logs.txt> <lookup_table.csv>
```
Replace <flow_logs.txt> and <lookup_table.csv> with the paths to your input files.
