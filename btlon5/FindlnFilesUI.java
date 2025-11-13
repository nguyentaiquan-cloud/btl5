package com.mycompany.btlont5;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class FindInFilesUI extends JFrame {

    private JComboBox<String> cbFind;
    private JComboBox<String> cbDir;
    private JTextField txtFind;
    private JButton btnBrowse, btnFindAll;
    private JCheckBox chkWholeWord, chkMatchCase;
    private JComboBox<String> cbFileType;

    private JTextArea txtResult;
    private ExecutorService executor = Executors.newFixedThreadPool(8);

    public FindInFilesUI() {
        setTitle("Find in Files");
        setSize(850, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // FIND WHAT
        gbc.gridx = 0; gbc.gridy = 0;
        top.add(new JLabel("Find what :"), gbc);

        txtFind = new JTextField(20);
        cbFind = new JComboBox<>();
        cbFind.setEditable(true);
        cbFind.setEditor(new BasicComboBoxEditor() {
            public Component getEditorComponent() {
                return txtFind;
            }
        });

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
        top.add(cbFind, gbc);

        btnFindAll = new JButton("Find All");
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        top.add(btnFindAll, gbc);

        // DIRECTORY
        gbc.gridx = 0; gbc.gridy = 1;
        top.add(new JLabel("Directory :"), gbc);

        cbDir = new JComboBox<>();
        cbDir.setEditable(true);
        gbc.gridx = 1; gbc.gridy = 1;
        top.add(cbDir, gbc);

        btnBrowse = new JButton("Browse");
        gbc.gridx = 2; gbc.gridy = 1;
        top.add(btnBrowse, gbc);

        // FILE TYPE
        gbc.gridx = 0; gbc.gridy = 2;
        top.add(new JLabel("File type:"), gbc);

        cbFileType = new JComboBox<>(new String[]{"All", ".txt", ".log"});
        gbc.gridx = 1; gbc.gridy = 2;
        top.add(cbFileType, gbc);

        // OPTIONS
        JPanel opt = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chkWholeWord = new JCheckBox("Match whole word only");
        chkMatchCase = new JCheckBox("Match case");
        opt.add(chkWholeWord);
        opt.add(chkMatchCase);

        gbc.gridx = 1; gbc.gridy = 3;
        top.add(opt, gbc);

        add(top, BorderLayout.NORTH);

        // RESULT AREA
        txtResult = new JTextArea();
        txtResult.setEditable(false);
        txtResult.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtResult.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "Kết quả",
                TitledBorder.CENTER,
                TitledBorder.TOP
        ));

        JScrollPane scroll = new JScrollPane(txtResult);
        scroll.setBorder(null);

        add(scroll, BorderLayout.CENTER);

        // EVENTS
        btnBrowse.addActionListener(e -> chooseDirectory());
        btnFindAll.addActionListener(e -> startSearch());

        // CLICK RESULT → OPEN FILE
        txtResult.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                openFileFromResult(e);
            }
        });
    }

    private void chooseDirectory() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            cbDir.addItem(fc.getSelectedFile().getAbsolutePath());
            cbDir.setSelectedItem(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void startSearch() {
        txtResult.setText("");

        String keyword = txtFind.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập từ khóa!");
            return;
        }

        String dir = getComboValue(cbDir);
        if (dir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chọn thư mục!");
            return;
        }

        txtResult.append("Đang tìm...\n");

        executor.submit(() -> searchFolder(new File(dir), keyword));
    }

    private String getComboValue(JComboBox<String> cb) {
        Object v = cb.getEditor().getItem();
        return v == null ? "" : v.toString();
    }

    private void searchFolder(File folder, String keyword) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                searchFolder(f, keyword);
                continue;
            }

            String type = cbFileType.getSelectedItem().toString();
            if (!type.equals("All") && !f.getName().endsWith(type)) continue;

            executor.submit(() -> searchInFile(f, keyword));
        }
    }

    private void searchInFile(File file, String keyword) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line; int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;

                boolean match = matchLine(line, keyword);
                if (match) {
                    String result = file.getAbsolutePath() + " | line " + lineNum + " | " + line;

                    SwingUtilities.invokeLater(() ->
                            txtResult.append(result + "\n")
                    );
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean matchLine(String line, String key) {
        if (!chkMatchCase.isSelected()) {
            line = line.toLowerCase();
            key = key.toLowerCase();
        }

        if (chkWholeWord.isSelected()) {
            return Pattern.compile("\\b" + Pattern.quote(key) + "\\b").matcher(line).find();
        }

        return line.contains(key);
    }

    private void openFileFromResult(MouseEvent e) {
        int pos = txtResult.viewToModel2D(e.getPoint());
        try {
            int rowStart = Utilities.getRowStart(txtResult, pos);
            int rowEnd = Utilities.getRowEnd(txtResult, pos);
            String line = txtResult.getText().substring(rowStart, rowEnd);

            String[] parts = line.split("\\|");
            if (parts.length < 3) return;

            String path = parts[0].trim();
            String keyword = txtFind.getText();

            showFileViewer(path, keyword);

        } catch (Exception ignored) {}
    }

    private void showFileViewer(String path, String keyword) {
        JFrame frame = new JFrame("Preview: " + new File(path).getName());
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        JTextPane viewer = new JTextPane();
        viewer.setFont(new Font("Consolas", Font.PLAIN, 14));

        try {
            String content = Files.readString(Paths.get(path));
            viewer.setText(content);

            highlight(viewer, keyword);

        } catch (Exception e) {
            viewer.setText("Không thể đọc file!");
        }

        frame.add(new JScrollPane(viewer));
        frame.setVisible(true);
    }

    private void highlight(JTextPane pane, String word) throws Exception {
        Highlighter hl = pane.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        String text = pane.getText().toLowerCase();
        word = word.toLowerCase();

        int pos = 0;
        while ((pos = text.indexOf(word, pos)) >= 0) {
            hl.addHighlight(pos, pos + word.length(), painter);
            pos += word.length();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FindInFilesUI().setVisible(true));
    }
}
