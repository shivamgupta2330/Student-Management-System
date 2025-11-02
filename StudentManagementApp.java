import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class StudentManagementApp extends JFrame {

    // ===== Model =====
    static class Student {
        String roll;
        String name;
        String course;
        String age;
        String phone;
        String address;

        Student(String roll, String name, String course, String age, String phone, String address) {
            this.roll = roll.trim();
            this.name = name.trim();
            this.course = course.trim();
            this.age = age.trim();
            this.phone = phone.trim();
            this.address = address.trim();
        }

        String[] toRow() {
            return new String[]{roll, name, course, age, phone, address};
        }

        static String[] headers() {
            return new String[]{"Roll No", "Name", "Course", "Age", "Phone", "Address"};
        }
    }

    // ===== Data Store (in-memory + CSV persistence) =====
    private final java.util.List<Student> students = new ArrayList<>();
    private final Path csvPath = Paths.get("students.csv");

    private void loadFromCSV() {
        students.clear();
        if (Files.exists(csvPath)) {
            try (BufferedReader br = Files.newBufferedReader(csvPath)) {
                String line;
                // skip header if present
                br.mark(1024);
                line = br.readLine();
                if (line != null && !line.equals(String.join(",", Student.headers()))) {
                    // first line is data; reset and read from beginning
                    br.reset();
                }
                else if (line == null) {
                    return;
                }
                while ((line = br.readLine()) != null) {
                    String[] parts = parseCSVLine(line, 6);
                    if (parts.length >= 6) {
                        students.add(new Student(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to load CSV: " + e.getMessage());
            }
        }
    }

    private void saveToCSV() {
        try (BufferedWriter bw = Files.newBufferedWriter(csvPath)) {
            // header
            bw.write(String.join(",", Student.headers()));
            bw.newLine();
            for (Student s : students) {
                bw.write(toCSV(s.roll) + "," + toCSV(s.name) + "," + toCSV(s.course) + "," +
                         toCSV(s.age) + "," + toCSV(s.phone) + "," + toCSV(s.address));
                bw.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save CSV: " + e.getMessage());
        }
    }

    private static String toCSV(String value) {
        if (value == null) value = "";
        boolean needQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        value = value.replace("\"", "\"\"");
        return needQuotes ? "\"" + value + "\"" : value;
    }

    private static String[] parseCSVLine(String line, int expectedCols) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        while (out.size() < expectedCols) out.add("");
        return out.toArray(new String[0]);
    }

    // ===== UI Components =====
    private final JTextField tfRoll = new JTextField();
    private final JTextField tfName = new JTextField();
    private final JTextField tfCourse = new JTextField();
    private final JTextField tfAge = new JTextField();
    private final JTextField tfPhone = new JTextField();
    private final JTextArea taAddress = new JTextArea(3, 20);
    private final JTable table;
    private final DefaultTableModel model;

    private final JTextField tfSearch = new JTextField();
    private final JComboBox<String> cbSearchBy = new JComboBox<>(new String[]{"Roll No", "Name"});
    private final JComboBox<String> cbSortBy = new JComboBox<>(new String[]{"Roll No", "Name"});

    public StudentManagementApp() {
        super("Student Management System - Java Swing");

        // Table
        model = new DefaultTableModel(Student.headers(), 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);

        // Layout
        setLayout(new BorderLayout(10, 10));
        add(buildFormPanel(), BorderLayout.WEST);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        // Load & show
        loadFromCSV();
        refreshTable();

        // Frame settings
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Student Form"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;

        // Labels
        p.add(new JLabel("Roll No:"), gc); gc.gridy++;
        p.add(new JLabel("Name:"), gc); gc.gridy++;
        p.add(new JLabel("Course:"), gc); gc.gridy++;
        p.add(new JLabel("Age:"), gc); gc.gridy++;
        p.add(new JLabel("Phone:"), gc); gc.gridy++;
        p.add(new JLabel("Address:"), gc);

        // Fields
        gc.gridx = 1; gc.gridy = 0; gc.weightx = 1.0;
        p.add(tfRoll, gc); gc.gridy++;
        p.add(tfName, gc); gc.gridy++;
        p.add(tfCourse, gc); gc.gridy++;
        p.add(tfAge, gc); gc.gridy++;
        p.add(tfPhone, gc); gc.gridy++;
        taAddress.setLineWrap(true);
        taAddress.setWrapStyleWord(true);
        JScrollPane addrScroll = new JScrollPane(taAddress);
        p.add(addrScroll, gc);

        // Buttons
        JPanel btns = new JPanel(new GridLayout(2, 3, 8, 8));
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear Form");
        JButton btnLoad = new JButton("Reload");
        JButton btnSave = new JButton("Save CSV");

        btns.add(btnAdd); btns.add(btnUpdate); btns.add(btnDelete);
        btns.add(btnClear); btns.add(btnLoad); btns.add(btnSave);

        gc.gridx = 0; gc.gridy++; gc.gridwidth = 2; gc.fill = GridBagConstraints.HORIZONTAL;
        p.add(btns, gc);

        // Actions
        btnAdd.addActionListener(e -> onAdd());
        btnUpdate.addActionListener(e -> onUpdate());
        btnDelete.addActionListener(e -> onDelete());
        btnClear.addActionListener(e -> clearForm());
        btnLoad.addActionListener(e -> { loadFromCSV(); refreshTable(); JOptionPane.showMessageDialog(this, "Data reloaded."); });
        btnSave.addActionListener(e -> { saveToCSV(); JOptionPane.showMessageDialog(this, "Saved to students.csv"); });

        // Fill form when row selected
        table.getSelectionModel().addListSelectionListener(ev -> {
            int r = table.getSelectedRow();
            if (r >= 0) {
                tfRoll.setText((String) model.getValueAt(r, 0));
                tfName.setText((String) model.getValueAt(r, 1));
                tfCourse.setText((String) model.getValueAt(r, 2));
                tfAge.setText((String) model.getValueAt(r, 3));
                tfPhone.setText((String) model.getValueAt(r, 4));
                taAddress.setText((String) model.getValueAt(r, 5));
            }
        });

        return p;
    }

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createTitledBorder("Student Records"));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBottomBar() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5,5,5,5);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        // Search
        gc.gridx = 0; p.add(new JLabel("Search By:"), gc);
        gc.gridx = 1; p.add(cbSearchBy, gc);
        gc.gridx = 2; tfSearch.setColumns(18); p.add(tfSearch, gc);
        JButton btnSearch = new JButton("Search");
        gc.gridx = 3; p.add(btnSearch, gc);
        JButton btnReset = new JButton("Reset View");
        gc.gridx = 4; p.add(btnReset, gc);

        // Sort
        gc.gridx = 5; p.add(new JLabel("Sort By:"), gc);
        gc.gridx = 6; p.add(cbSortBy, gc);
        JButton btnSort = new JButton("Sort");
        gc.gridx = 7; p.add(btnSort, gc);

        btnSearch.addActionListener(e -> onSearch());
        btnReset.addActionListener(e -> refreshTable());
        btnSort.addActionListener(e -> onSort());

        return p;
    }

    // ===== Actions =====
    private void onAdd() {
        Student s = readForm();
        if (s == null) return;
        // Ensure unique roll
        boolean exists = students.stream().anyMatch(st -> st.roll.equalsIgnoreCase(s.roll));
        if (exists) {
            JOptionPane.showMessageDialog(this, "Roll No already exists!");
            return;
        }
        students.add(s);
        refreshTable();
        saveToCSV();
        clearForm();
    }

    private void onUpdate() {
        int idx = table.getSelectedRow();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to update.");
            return;
        }
        String rollKey = (String) model.getValueAt(idx, 0);
        Student updated = readForm();
        if (updated == null) return;
        // keep roll unique; allow changing but check duplicates
        if (!rollKey.equalsIgnoreCase(updated.roll) &&
                students.stream().anyMatch(st -> st.roll.equalsIgnoreCase(updated.roll))) {
            JOptionPane.showMessageDialog(this, "Another student with same Roll No exists!");
            return;
        }
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).roll.equalsIgnoreCase(rollKey)) {
                students.set(i, updated);
                break;
            }
        }
        refreshTable();
        saveToCSV();
    }

    private void onDelete() {
        int idx = table.getSelectedRow();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to delete.");
            return;
        }
        String rollKey = (String) model.getValueAt(idx, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete student with Roll No: " + rollKey + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            students.removeIf(st -> st.roll.equalsIgnoreCase(rollKey));
            refreshTable();
            saveToCSV();
            clearForm();
        }
    }

    private void onSearch() {
        String by = (String) cbSearchBy.getSelectedItem();
        String q = tfSearch.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter something to search.");
            return;
        }
        List<Student> matches = students.stream().filter(st -> {
            if ("Roll No".equals(by)) return st.roll.toLowerCase().contains(q);
            return st.name.toLowerCase().contains(q);
        }).collect(Collectors.toList());

        if (matches.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No matching student found.");
            return;
        }
        reloadTable(matches);
        // select first row
        if (model.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
    }

    private void onSort() {
        String by = (String) cbSortBy.getSelectedItem();
        Comparator<Student> cmp;
        if ("Roll No".equals(by)) {
            cmp = Comparator.comparing(st -> st.roll.toLowerCase());
        } else {
            cmp = Comparator.comparing(st -> st.name.toLowerCase());
        }
        students.sort(cmp);
        refreshTable();
        saveToCSV();
    }

    private Student readForm() {
        String roll = tfRoll.getText().trim();
        String name = tfName.getText().trim();
        String course = tfCourse.getText().trim();
        String age = tfAge.getText().trim();
        String phone = tfPhone.getText().trim();
        String address = taAddress.getText().trim();

        if (roll.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Roll No and Name are required.");
            return null;
        }
        if (!age.isEmpty() && !age.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Age must be a number.");
            return null;
        }
        if (!phone.isEmpty() && !phone.matches("\\d{7,15}")) {
            JOptionPane.showMessageDialog(this, "Phone must be 7-15 digits (numbers only).");
            return null;
        }
        return new Student(roll, name, course, age, phone, address);
    }

    private void clearForm() {
        tfRoll.setText("");
        tfName.setText("");
        tfCourse.setText("");
        tfAge.setText("");
        tfPhone.setText("");
        taAddress.setText("");
        tfRoll.requestFocus();
    }

    private void refreshTable() {
        reloadTable(students);
    }

    private void reloadTable(List<Student> list) {
        model.setRowCount(0);
        for (Student s : list) {
            model.addRow(s.toRow());
        }
    }

    public static void main(String[] args) {
        // Use system look and feel for better appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(StudentManagementApp::new);
    }
}
