package com.mycompany.btlont5;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class LogGenerator {

    private static final int NUM_FILES = 3000;         // Số file log
    private static final int LINES_PER_FILE = 20000;   // Số dòng mỗi file
    private static final String LOG_DIR = "logs";      // Thư mục chứa log
    private static final String KEYWORD = "login by 99";

    public static void main(String[] args) {
        try {
            generateLogs();
            System.out.println("Done! Created " + NUM_FILES + " log files in folder '" + LOG_DIR + "'.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateLogs() throws IOException {
        Path dir = Paths.get(LOG_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // Ngày bắt đầu để format tên file
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("dd_MM_yy");

        Random random = new Random();

        for (int i = 0; i < NUM_FILES; i++) {
            LocalDate fileDate = startDate.plusDays(i);
            String fileName = "log_" + fileDate.format(fileFormatter) + ".txt";
            Path filePath = dir.resolve(fileName);

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                for (int lineNum = 1; lineNum <= LINES_PER_FILE; lineNum++) {
                    LocalDateTime now = LocalDateTime.now();

                    String logLine;
                    // Cứ mỗi 5000 dòng CHẮC CHẮN chèn 1 dòng có "login by 99"
                    if (lineNum % 5000 == 0) {
                        logLine = String.format(
                                "%s INFO User login by 99 from IP 192.168.%d.%d",
                                now, random.nextInt(256), random.nextInt(256)
                        );
                    } else {
                        // Các dòng khác là log bình thường ngẫu nhiên
                        int userId = random.nextInt(100);
                        String action = switch (random.nextInt(4)) {
                            case 0 -> "login success";
                            case 1 -> "login fail";
                            case 2 -> "read data";
                            default -> "update profile";
                        };
                        logLine = String.format(
                                "%s INFO user %d %s",
                                now, userId, action
                        );
                    }

                    writer.write(logLine);
                    writer.newLine();
                }
            }

            System.out.println("Created file: " + fileName);
        }
    }
}
