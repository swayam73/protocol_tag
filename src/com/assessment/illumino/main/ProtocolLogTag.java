package com.assessment.illumino.main;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ProtocolLogTag {

    public static final Map<String, String> lookupTable = new HashMap<>();
    public static final Map<String, AtomicInteger> tagCount = new ConcurrentHashMap<>();
    public static final Map<String, AtomicInteger> portProtocolCount = new ConcurrentHashMap<>();
    public static final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java <flow_log_file> <lookup_csv_file>");
            System.exit(1);
        }

        String flowLogFile = args[0];
        String lookupCsvFile = args[1];

        try {
            // Load lookup table from CSV file
            loadLookupTable(lookupCsvFile);
            
            // Process flow logs with parallel processing
            processFlowLogs(flowLogFile);
            
            // Generate output report
            generateReport("output_report.txt");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            forkJoinPool.shutdown();
        }
    }

    // Load lookup table from CSV file
    public static void loadLookupTable(String filePath) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String dstPort = parts[0].trim().toLowerCase();
                    String protocol = parts[1].trim().toLowerCase();
                    String tag = parts[2].trim().toLowerCase();
                    lookupTable.put(dstPort + "," + protocol, tag);
                }
            }
        }
    }

    // Process flow logs using ForkJoinPool for parallel processing
    public static void processFlowLogs(String filePath) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            forkJoinPool.submit(() -> 
                br.lines().parallel().forEach(ProtocolLogTag::processLogLine)
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error during parallel processing: " + e.getMessage());
        }
    }

    // Process each line of the flow log
    public static void processLogLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 14) return;

        String dstPort = parts[5];
        String protocolNumber = parts[7];
        String protocol = getProtocolName(protocolNumber);

        if (protocol == null) return;

        String key = dstPort + "," + protocol;
        String tag = lookupTable.getOrDefault(key, "untagged");

        // Increment tag count
        tagCount.computeIfAbsent(tag, k -> new AtomicInteger()).incrementAndGet();

        // Increment port/protocol combination count
        portProtocolCount.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    // Map protocol number to protocol name
    private static String getProtocolName(String protocolNumber) {
        switch (protocolNumber) {
            case "6": return "tcp";
            case "17": return "udp";
            case "1": return "icmp";
            default: return null;
        }
    }

    // Generate report and save to a file
    public static void generateReport(String outputFileName) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFileName))) {
            
            // Write Tag Counts
            writer.write("Tag Counts:\n");
            writer.write("Tag,Count\n");
            for (Map.Entry<String, AtomicInteger> entry : tagCount.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue().get() + "\n");
            }

            // Write Port/Protocol Combination Counts
            writer.write("\nPort/Protocol Combination Counts:\n");
            writer.write("Port,Protocol,Count\n");
            for (Map.Entry<String, AtomicInteger> entry : portProtocolCount.entrySet()) {
                String[] parts = entry.getKey().split(",");
                writer.write(parts[0] + "," + parts[1] + "," + entry.getValue().get() + "\n");
            }

        } catch (IOException e) {
            System.err.println("Error writing report: " + e.getMessage());
        }
    }
}