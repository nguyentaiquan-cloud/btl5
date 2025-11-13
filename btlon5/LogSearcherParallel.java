package com.mycompany.btlont5;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LogSearcherParallel {

    private static final String LOG_DIR = "logs";          // Thư mục chứa log
    private static final String KEYWORD = "login by 99";   // Từ khóa cần tìm
    private static final String RESULT_FILE = "ketqua.txt";// File kết quả

    public static void main(String[] args) {
        LogSearcherParallel searcher = new LogSearcherParallel();
        try {
            searcher.searchLogs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchLogs() throws Exception {
        Path logDir = Paths.get(LOG_DIR);

        if (!Files.isDirectory(logDir)) {
            System.err.println("Folder '" + LOG_DIR + "' does not exist. Please run LogGenerator first.");
            return;
        }

        // Lấy danh sách tất cả file log_*.txt trong thư mục logs
        List<Path> logFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "log_*.txt")) {
            for (Path p : stream) {
                logFiles.add(p);
            }
        }

        if (logFiles.isEmpty()) {
            System.out.println("No log_*.txt files found in folder '" + LOG_DIR + "'.");
            return;
        }

        System.out.println("Found " + logFiles.size() + " log files. Starting parallel search...");

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Future<List<SearchResult>>> futures = new ArrayList<>();

        // Gửi mỗi file vào thread pool để xử lý song song
        for (Path file : logFiles) {
            Callable<List<SearchResult>> task = () -> searchInFile(file);
            futures.add(executor.submit(task));
        }

        // Thu kết quả từ các thread
        List<SearchResult> allResults = new ArrayList<>();
        for (Future<List<SearchResult>> future : futures) {
            try {
                allResults.addAll(future.get());
            } catch (ExecutionException e) {
                // Nếu có lỗi khi xử lý một file
                e.getCause().printStackTrace();
            }
        }

        executor.shutdown();

        // Ghi tất cả kết quả ra file ketqua.txt
        writeResults(allResults);

        System.out.println("Search done.");
        System.out.println("Total matches: " + allResults.size());
        System.out.println("Results written to file: " + RESULT_FILE);
    }

    // Tìm từ khóa trong 1 file
    private List<SearchResult> searchInFile(Path file) {
        List<SearchResult> results = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.contains(KEYWORD)) {
                    results.add(new SearchResult(file.getFileName().toString(), lineNumber, line));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file + " - " + e.getMessage());
        }

        return results;
    }

    // Ghi kết quả ra file ketqua.txt
    private void writeResults(List<SearchResult> results) {
        Path resultPath = Paths.get(RESULT_FILE);

        try (BufferedWriter writer = Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8)) {
            for (SearchResult r : results) {
                // Format: FileName \t line X \t content
                writer.write(String.format("%s\tline %d\t%s", r.fileName, r.lineNumber, r.content));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing result file: " + e.getMessage());
        }
    }

    // Lưu thông tin 1 dòng match
    private static class SearchResult {
        final String fileName;
        final int lineNumber;
        final String content;

        SearchResult(String fileName, int lineNumber, String content) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.content = content;
        }
    }
}
