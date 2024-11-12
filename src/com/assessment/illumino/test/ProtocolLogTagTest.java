package com.assessment.illumino.test;

import org.junit.jupiter.api.*;

import com.assessment.illumino.main.ProtocolLogTag;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolLogTagTest {

    private static final String FLOW_LOG_FILE = "test_flow_log.txt";
    private static final String LOOKUP_CSV_FILE = "test_lookup.csv";
    private static final String OUTPUT_REPORT_FILE = "test_output_report.txt";

    @BeforeEach
    public void setUp() throws IOException {
        // lookup table CSV file
        String lookupData = """
                25,tcp,sv_p1
                23,tcp,sv_p1
                443,tcp,sv_p2
                110,tcp,email
                993,tcp,email
                143,tcp,email
                """;
        Files.writeString(Paths.get(LOOKUP_CSV_FILE), lookupData);

        // flow log file
        String flowLogData = """
                2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK
                2 123456789012 eni-4d3c2b1a 192.168.1.100 203.0.113.101 23 49154 6 15 12000 1620140761 1620140821 REJECT OK
                2 123456789012 eni-5e6f7g8h 192.168.1.101 198.51.100.3 25 49155 6 10 8000 1620140761 1620140821 ACCEPT OK
                2 123456789012 eni-7i8j9k0l 172.16.0.101 192.0.2.203 993 49157 6 8 5000 1620140761 1620140821 ACCEPT OK
                2 123456789012 eni-6m7n8o9p 10.0.2.200 198.51.100.4 143 49158 6 18 14000 1620140761 1620140821 ACCEPT OK
                """;
        Files.writeString(Paths.get(FLOW_LOG_FILE), flowLogData);
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up test files
        Files.deleteIfExists(Paths.get(FLOW_LOG_FILE));
        Files.deleteIfExists(Paths.get(LOOKUP_CSV_FILE));
        Files.deleteIfExists(Paths.get(OUTPUT_REPORT_FILE));
    }

    @Test
    public void testLoadLookupTable() throws IOException {
        ProtocolLogTag.loadLookupTable(LOOKUP_CSV_FILE);

        // Check if the lookup table is loaded correctly
        assertEquals("sv_p1", ProtocolLogTag.lookupTable.get("25,tcp"));
        assertEquals("sv_p2", ProtocolLogTag.lookupTable.get("443,tcp"));
        assertEquals("email", ProtocolLogTag.lookupTable.get("110,tcp"));
        assertNull(ProtocolLogTag.lookupTable.get("9999,tcp"));
    }

    @Test
    public void testProcessLogLine() throws IOException {
        // Load the lookup table
        ProtocolLogTag.loadLookupTable(LOOKUP_CSV_FILE);

        // Clear existing data to ensure a fresh state
        ProtocolLogTag.tagCount.clear();
        ProtocolLogTag.portProtocolCount.clear();

        // Process sample log lines
        ProtocolLogTag.processLogLine("2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK");
        ProtocolLogTag.processLogLine("2 123456789012 eni-4d3c2b1a 192.168.1.100 203.0.113.101 23 49154 6 15 12000 1620140761 1620140821 REJECT OK");

        // Check if the key "sv_p2" exists in the tagCount map
        AtomicInteger svP2Count = ProtocolLogTag.tagCount.get("sv_p2");
        assertNotNull(svP2Count, "'sv_p2' key is missing in tagCount");
        
        // If not null, validate the count
        if (svP2Count != null) {
            assertEquals(1, svP2Count.get());
        }

        // Check if the key "sv_p1" exists in the tagCount map
        AtomicInteger svP1Count = ProtocolLogTag.tagCount.get("sv_p1");
        assertNotNull(svP1Count, "'sv_p1' key is missing in tagCount");
        
        // If not null, validate the count
        if (svP1Count != null) {
            assertEquals(1, svP1Count.get());
        }

        // Validate port-protocol counts
        AtomicInteger port443Count = ProtocolLogTag.portProtocolCount.get("443,tcp");
        assertNotNull(port443Count, "'443,tcp' key is missing in portProtocolCount");
        if (port443Count != null) {
            assertEquals(1, port443Count.get());
        }

        AtomicInteger port23Count = ProtocolLogTag.portProtocolCount.get("23,tcp");
        assertNotNull(port23Count, "'23,tcp' key is missing in portProtocolCount");
        if (port23Count != null) {
            assertEquals(1, port23Count.get());
        }
    }

    @Test
    public void testGenerateReport() throws IOException {
        // Load the lookup table and process the flow logs
        ProtocolLogTag.loadLookupTable(LOOKUP_CSV_FILE);
        ProtocolLogTag.processFlowLogs(FLOW_LOG_FILE);

        // Generate the report
        ProtocolLogTag.generateReport(OUTPUT_REPORT_FILE);

        // Validate that the report file was created
        assertTrue(Files.exists(Paths.get(OUTPUT_REPORT_FILE)));

        // Validate the contents of the report
        List<String> reportLines = Files.readAllLines(Paths.get(OUTPUT_REPORT_FILE));
        assertTrue(reportLines.contains("Tag,Count"));
        assertTrue(reportLines.contains("sv_p1,2"));
        assertTrue(reportLines.contains("email,2"));
        assertTrue(reportLines.contains("Port,Protocol,Count"));
        assertTrue(reportLines.contains("443,tcp,1"));
        assertTrue(reportLines.contains("993,tcp,1"));
    }

    @Test
    public void testEndToEndFlow() throws IOException {
        ProtocolLogTag.loadLookupTable(LOOKUP_CSV_FILE);
        ProtocolLogTag.processFlowLogs(FLOW_LOG_FILE);
        ProtocolLogTag.generateReport(OUTPUT_REPORT_FILE);

        List<String> reportLines = Files.readAllLines(Paths.get(OUTPUT_REPORT_FILE));

        assertTrue(reportLines.contains("Tag,Count"));
        assertTrue(reportLines.contains("sv_p2,1"));
        assertTrue(reportLines.contains("sv_p1,2"));
        assertTrue(reportLines.contains("email,2"));
        assertTrue(reportLines.contains("untagged,0"));
    }
}