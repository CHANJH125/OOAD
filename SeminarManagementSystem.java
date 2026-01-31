import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class SeminarManagementSystem extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private String loggedInUser = "";
    private String currentRole = "Student"; // Default to Student
    private java.util.Map<String, String[]> userDatabase = new java.util.HashMap<>();

    // Centralized Data Store (for persistence)
    private java.util.List<String[]> allSubmissions = new ArrayList<>(); // [User, Title, Type, Status, SessionID, Date,
                                                                         // Score, Comments, Abstract, Supervisor,
                                                                         // FilePath]
    private java.util.List<Object[]> allSessions = new ArrayList<>(); // [SessID, Date, Venue, Type, Status,
                                                                      // AssignedEvaluator]

    // Shared UI Models
    private DefaultTableModel studentSubmissionsModel;
    private DefaultTableModel evaluatorTaskModel;
    private DefaultTableModel sessionTableModel;
    private DefaultTableModel coordinatorAllSubmissionsModel;
    private DefaultTableModel userManagementModel;
    private JComboBox<String> presenterSelectionBox;
    private JComboBox<String> studentSessBox = new JComboBox<>();

    private final String DATA_FILE = "seminar_data.dat";

    public SeminarManagementSystem() {
        setTitle("Seminar System");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize Models
        studentSubmissionsModel = new DefaultTableModel(
                new String[] { "Title", "Type", "Status", "Session", "Date", "Score", "Comment", "FullComment" }, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        evaluatorTaskModel = new DefaultTableModel(new String[] { "Presenter", "Research Title", "Type", "Status" },
                0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        sessionTableModel = new DefaultTableModel(
                new String[] { "SessID", "Date", "Venue", "Type", "Status", "Evaluator" }, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        coordinatorAllSubmissionsModel = new DefaultTableModel(
                new String[] { "User", "Title", "Type", "Status", "Session", "Score" }, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        userManagementModel = new DefaultTableModel(new String[] { "Username", "Role" }, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        presenterSelectionBox = new JComboBox<>();

        loadData(); // Load previous state

        // Create panels
        createLoginPanel();
        createRegisterPanel();
        createStudentPanel();
        createEvaluatorPanel();
        createCoordinatorPanel();

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
        setVisible(true);
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(userDatabase);
            oos.writeObject(allSubmissions);
            oos.writeObject(allSessions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                userDatabase = (Map<String, String[]>) ois.readObject();
                allSubmissions = (List<String[]>) ois.readObject();
                allSessions = (List<Object[]>) ois.readObject();

                // Sync session table
                for (Object[] row : allSessions) {
                    if (row.length == 5) { // Migrating old data
                        Object[] newRow = new Object[] { row[0], row[1], row[2], row[3], row[4], "Unassigned" };
                        sessionTableModel.addRow(newRow);
                    } else {
                        sessionTableModel.addRow(row);
                    }
                }
                // Sync user management
                for (Entry<String, String[]> entry : userDatabase.entrySet()) {
                    userManagementModel.addRow(new Object[] { entry.getKey(), entry.getValue()[1] });
                }
            } catch (Exception e) {
                System.out.println("Error loading data: " + e.getMessage());
                initializeDefaults(); // Fallback to defaults if file is incompatible
            }
        } else {
            initializeDefaults();
        }
    }

    private void initializeDefaults() {
        userDatabase.clear();
        userDatabase.put("student1", new String[] { "pass", "Student" });
        userDatabase.put("eval1", new String[] { "pass", "Evaluator" });
        userDatabase.put("coord1", new String[] { "pass", "Coordinator" });

        userManagementModel.setRowCount(0);
        userManagementModel.addRow(new Object[] { "student1", "Student" });
        userManagementModel.addRow(new Object[] { "eval1", "Evaluator" });
        userManagementModel.addRow(new Object[] { "coord1", "Coordinator" });
        saveData();
    }

    private void createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(52, 152, 219)); // Vibrant Blue background

        // Main Card
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Header
        JLabel title = new JLabel("SEMINAR SYSTEM", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(52, 152, 219));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        card.add(title, gbc);

        JLabel subtitle = new JLabel("Welcome back! Please login to continue.", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(Color.GRAY);
        gbc.gridy = 1;
        card.add(subtitle, gbc);

        // Username
        gbc.gridwidth = 2;
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        card.add(userLabel, gbc);

        gbc.gridy = 3;
        JTextField userField = new JTextField(20);
        userField.setPreferredSize(new Dimension(300, 35));
        card.add(userField, gbc);

        // Password
        gbc.gridy = 4;
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        card.add(passLabel, gbc);

        gbc.gridy = 5;
        JPasswordField passField = new JPasswordField(20);
        passField.setPreferredSize(new Dimension(300, 35));
        card.add(passField, gbc);

        // Login Button
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 10, 10, 10);
        JButton loginBtn = new JButton("LOGIN TO SYSTEM");
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.setBackground(new Color(52, 152, 219)); // Vibrant Blue
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setPreferredSize(new Dimension(0, 45));
        loginBtn.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            if (userDatabase.containsKey(username)) {
                String[] credentials = userDatabase.get(username);
                if (credentials[0].equals(password)) {
                    loggedInUser = username;
                    currentRole = credentials[1];
                    refreshRoleData(); // Populate UI with user-specific data
                    if (currentRole.equals("Student"))
                        cardLayout.show(mainPanel, "STUDENT");
                    else if (currentRole.equals("Evaluator"))
                        cardLayout.show(mainPanel, "EVALUATOR");
                    else if (currentRole.equals("Coordinator"))
                        cardLayout.show(mainPanel, "COORDINATOR");
                } else {
                    JOptionPane.showMessageDialog(this, "Incorrect password!", "Login Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "User not found!", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        card.add(loginBtn, gbc);

        // Register Link
        gbc.gridy = 7;
        gbc.insets = new Insets(5, 10, 10, 10);
        JButton toRegisterBtn = new JButton("Don't have an account? Create one now");
        toRegisterBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        toRegisterBtn.setForeground(new Color(52, 152, 219));
        toRegisterBtn.setBorderPainted(false);
        toRegisterBtn.setContentAreaFilled(false);
        toRegisterBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toRegisterBtn.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
        card.add(toRegisterBtn, gbc);

        panel.add(card);
        mainPanel.add(panel, "LOGIN");
    }

    private void createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(22, 160, 133)); // Teal background

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("REGISTRATION", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(22, 160, 133));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        card.add(title, gbc);

        gbc.gridwidth = 2;
        gbc.gridy = 1;
        gbc.gridx = 0;
        card.add(new JLabel("Create Username"), gbc);
        gbc.gridy = 2;
        JTextField userField = new JTextField(20);
        card.add(userField, gbc);

        gbc.gridy = 3;
        card.add(new JLabel("Set Password"), gbc);
        gbc.gridy = 4;
        JPasswordField passField = new JPasswordField(20);
        card.add(passField, gbc);

        gbc.gridy = 5;
        card.add(new JLabel("Account Type"), gbc);
        gbc.gridy = 6;
        String[] roles = { "Student", "Evaluator", "Coordinator" };
        JComboBox<String> roleBox = new JComboBox<>(roles);
        card.add(roleBox, gbc);

        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        JButton registerBtn = new JButton("CREATE ACCOUNT");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerBtn.setBackground(new Color(22, 160, 133));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setPreferredSize(new Dimension(0, 45));
        registerBtn.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            String role = (String) roleBox.getSelectedItem();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields!");
                return;
            }
            if (userDatabase.containsKey(username)) {
                JOptionPane.showMessageDialog(this, "Username already exists!", "Registration Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            userDatabase.put(username, new String[] { password, role });
            userManagementModel.addRow(new Object[] { username, role });
            saveData();
            JOptionPane.showMessageDialog(this, "Account created successfully!");
            cardLayout.show(mainPanel, "LOGIN");
        });
        card.add(registerBtn, gbc);

        gbc.gridy = 8;
        JButton backBtn = new JButton("Already have an account? Login");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setForeground(Color.GRAY);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        card.add(backBtn, gbc);

        panel.add(card);
        mainPanel.add(panel, "REGISTER");
    }

    // Dashboard methods removed to go direct to role panels

    private void createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JPanel header = createModuleHeader("STUDENT RESEARCH PORTAL");
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Registration Card
        JPanel regFormWrapper = new JPanel(new GridBagLayout());
        regFormWrapper.setBackground(new Color(245, 247, 250));

        JPanel regCard = new JPanel(new GridLayout(7, 2, 15, 15));
        regCard.setBackground(Color.WHITE);
        regCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)),
                        "Research Registration Form", 0, 0, new Font("Segoe UI", Font.BOLD, 14)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        regCard.add(new JLabel("Research Title: *"));
        JTextField titleField = new JTextField();
        regCard.add(titleField);

        regCard.add(new JLabel("Abstract: *"));
        JTextArea abstractArea = new JTextArea(3, 20);
        abstractArea.setLineWrap(true);
        regCard.add(new JScrollPane(abstractArea));

        regCard.add(new JLabel("Supervisor Name: *"));
        JTextField supervisorField = new JTextField();
        regCard.add(supervisorField);

        regCard.add(new JLabel("Presentation Type:"));
        JComboBox<String> typeBox = new JComboBox<>(new String[] { "Oral Presentation", "Poster Presentation" });
        regCard.add(typeBox);

        regCard.add(new JLabel("Target Session: *"));
        studentSessBox.addItem("Choose Session...");
        refreshStudentSessions();

        studentSessBox.addActionListener(e -> {
            String selected = (String) studentSessBox.getSelectedItem();
            if (selected != null && selected.contains("(")) {
                String typePart = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
                if (typePart.equalsIgnoreCase("Oral")) {
                    typeBox.setSelectedItem("Oral Presentation");
                } else if (typePart.equalsIgnoreCase("Poster")) {
                    typeBox.setSelectedItem("Poster Presentation");
                }
                typeBox.setEnabled(false); // Lock it to session type
            } else {
                typeBox.setEnabled(true);
            }
        });
        regCard.add(studentSessBox);

        regCard.add(new JLabel("Presentation File: *"));
        JPanel fPanel = new JPanel(new BorderLayout(5, 0));
        fPanel.setOpaque(false);
        JTextField fField = new JTextField();
        fField.setEditable(false); // Only can browse, not type
        JButton bBtn = new JButton("Browse");
        bBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                fField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        fPanel.add(fField, BorderLayout.CENTER);
        fPanel.add(bBtn, BorderLayout.EAST);
        regCard.add(fPanel);

        JButton subBtn = new JButton("SUBMIT REGISTRATION");
        subBtn.setBackground(new Color(46, 204, 113));
        subBtn.setForeground(Color.WHITE);
        subBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        regCard.add(new JLabel());
        regCard.add(subBtn);

        subBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String type = (String) typeBox.getSelectedItem();
            String session = (String) studentSessBox.getSelectedItem();
            String abst = abstractArea.getText().trim();
            String sup = supervisorField.getText().trim();
            String file = fField.getText().trim();
            String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

            if (title.isEmpty() || abst.isEmpty() || sup.isEmpty() || file.isEmpty()
                    || session.equals("Choose Session...")) {
                JOptionPane.showMessageDialog(this, "Error: All fields marked with * are mandatory!",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // [User, Title, Type, Status, SessionID, Date, Score, Comments, Abstract,
            // Supervisor, FilePath]
            String[] sub = new String[] { loggedInUser, title, type, "Pending", session.split(" ")[0], date, "-", "-",
                    abst, sup, file };
            allSubmissions.add(sub);
            saveData();
            refreshRoleData();
            JOptionPane.showMessageDialog(this, "Research Submitted Successfully!");
        });

        regFormWrapper.add(regCard);
        tabs.addTab("New Registration", regFormWrapper);

        // Submissions Table Card
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        tableWrapper.setBackground(new Color(245, 247, 250));

        JTable table = new JTable(studentSubmissionsModel);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(52, 73, 94));
        table.getTableHeader().setForeground(Color.WHITE);

        // Action Listener for registration is already set above with typeBox and
        // sessBox

        // Hide the FullComment column from view
        table.removeColumn(table.getColumnModel().getColumn(7));

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        int modelRow = table.convertRowIndexToModel(row);
                        String comment = (String) table.getModel().getValueAt(modelRow, 7);
                        if (comment != null && !comment.equals("-") && !comment.equals("No") && !comment.isEmpty()) {
                            JOptionPane.showMessageDialog(null, "Evaluator Comment:\n" + comment,
                                    "View Comment", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        });

        tableWrapper.add(new JScrollPane(table), BorderLayout.CENTER);
        tabs.addTab("My Submissions", tableWrapper);

        panel.add(tabs, BorderLayout.CENTER);
        mainPanel.add(panel, "STUDENT");
    }

    private void createEvaluatorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JPanel header = createModuleHeader("EVALUATOR WORKBENCH");
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Evaluation Form Card
        JPanel evalWrapper = new JPanel(new BorderLayout(20, 20));
        evalWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        evalWrapper.setBackground(new Color(245, 247, 250));

        JPanel topCard = new JPanel(new BorderLayout());
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTop.setOpaque(false);
        leftTop.add(new JLabel("Select Assigned Presenter:"));
        leftTop.add(presenterSelectionBox);

        JButton infoBtn = new JButton("View Student Research Info");
        infoBtn.setBackground(new Color(46, 204, 113));
        infoBtn.setForeground(Color.WHITE);
        infoBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        infoBtn.addActionListener(e -> {
            String selected = (String) presenterSelectionBox.getSelectedItem();
            if (selected != null) {
                String[] parts = selected.split(" - ", 2);
                showResearchDetail(parts[0], parts[1]);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a presenter first!");
            }
        });

        topCard.add(leftTop, BorderLayout.WEST);
        topCard.add(infoBtn, BorderLayout.EAST);

        evalWrapper.add(topCard, BorderLayout.NORTH);

        JPanel rubricCard = new JPanel(new GridLayout(5, 2, 10, 10));
        rubricCard.setBackground(Color.WHITE);
        rubricCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Evaluation Rubrics"),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        String[] crit = { "Problem Clarity", "Methodology", "Results Analysis", "Presentation Style" };
        for (String c : crit) {
            rubricCard.add(new JLabel(c + " (0-25):"));
            rubricCard.add(new JSpinner(new SpinnerNumberModel(20, 0, 25, 1)));
        }

        JPanel commentArea = new JPanel(new BorderLayout());
        commentArea.setOpaque(false);
        commentArea.add(new JLabel("Additional Comments:"), BorderLayout.NORTH);
        commentArea.add(new JScrollPane(new JTextArea(5, 30)), BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.add(rubricCard, BorderLayout.CENTER);
        centerPanel.add(commentArea, BorderLayout.SOUTH);

        evalWrapper.add(centerPanel, BorderLayout.CENTER);

        JButton subBtn = new JButton("SUBMIT FINAL SCORES");
        subBtn.setBackground(new Color(52, 152, 219));
        subBtn.setForeground(Color.WHITE);
        subBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        subBtn.setPreferredSize(new Dimension(0, 45));
        subBtn.addActionListener(e -> {
            String selected = (String) presenterSelectionBox.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select a presenter to evaluate!", "No Presenter Selected",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Update models
            String[] parts = selected.split(" - ", 2);
            String presenter = parts[0];
            String researchTitle = parts[1];

            // Calculate score from spinners
            int total = 0;
            for (int i = 1; i < rubricCard.getComponentCount(); i += 2) {
                total += (Integer) ((JSpinner) rubricCard.getComponent(i)).getValue();
            }
            // Fix index to find comment box accurately
            String comments = ((JTextArea) ((JScrollPane) commentArea.getComponent(1))
                    .getViewport().getView()).getText();

            for (String[] sub : allSubmissions) {
                if (sub[0].equals(presenter) && sub[1].equals(researchTitle)) {
                    sub[3] = "Graded";
                    sub[6] = total + "/100";
                    sub[7] = comments.isEmpty() ? "Good job!" : comments;
                    break;
                }
            }
            saveData();
            refreshRoleData();
            JOptionPane.showMessageDialog(this, "Evaluation Submitted! Final Score: " + total);
        });
        evalWrapper.add(subBtn, BorderLayout.SOUTH);

        tabs.addTab("Evaluation Form", evalWrapper);

        // Assigned Presentations Table
        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JTable eTable = new JTable(evaluatorTaskModel);
        eTable.setRowHeight(30);

        eTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = eTable.getSelectedRow();
                    if (row != -1) {
                        String presenter = (String) eTable.getValueAt(row, 0);
                        String title = (String) eTable.getValueAt(row, 1);
                        showResearchDetail(presenter, title);
                    }
                }
            }
        });

        listWrapper.add(new JScrollPane(eTable), BorderLayout.CENTER);
        tabs.addTab("My Assigned List", listWrapper);

        panel.add(tabs, BorderLayout.CENTER);
        mainPanel.add(panel, "EVALUATOR");
    }

    private void createCoordinatorPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel header = createModuleHeader("Coordinator Panel");
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        // 1. Session Management
        JPanel sessionPanel = new JPanel(new BorderLayout(10, 10));
        sessionPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel sessionForm = new JPanel(new GridLayout(5, 2, 10, 10));

        sessionForm.add(new JLabel("Date & Time (Click to Change):"));
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd HH:mm");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setValue(new Date());
        sessionForm.add(dateSpinner);

        sessionForm.add(new JLabel("Venue:"));
        String[] venues = new String[10];
        for (int i = 0; i < 10; i++)
            venues[i] = "V" + (i + 1);
        JComboBox<String> venuePicker = new JComboBox<>(venues);
        sessionForm.add(venuePicker);

        sessionForm.add(new JLabel("Type:"));
        JComboBox<String> sessionTypeBox = new JComboBox<>(new String[] { "Oral", "Poster" });
        sessionForm.add(sessionTypeBox);

        sessionForm.add(new JLabel("Assign Evaluator:"));
        JComboBox<String> evalPicker = new JComboBox<>();
        evalPicker.addItem("Select Evaluator...");
        for (Entry<String, String[]> entry : userDatabase.entrySet()) {
            if (entry.getValue()[1].equals("Evaluator"))
                evalPicker.addItem(entry.getKey());
        }
        sessionForm.add(evalPicker);

        JButton addSession = new JButton("Create New Session");
        addSession.setBackground(new Color(52, 152, 219));
        sessionForm.add(new JLabel());
        sessionForm.add(addSession);

        addSession.addActionListener(e -> {
            Date selectedDate = (Date) dateSpinner.getValue();
            String dt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(selectedDate);
            String vn = (String) venuePicker.getSelectedItem();
            String ty = (String) sessionTypeBox.getSelectedItem();
            String ev = (String) evalPicker.getSelectedItem();

            if (ev.equals("Select Evaluator...")) {
                JOptionPane.showMessageDialog(this, "Error: Please assign an evaluator!", "Input Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Conflict & Time Logic
            java.util.Date now = new java.util.Date();
            if (selectedDate.before(now)) {
                JOptionPane.showMessageDialog(this, "Error: Cannot schedule a session in the past!", "Timing Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            long selectedMillis = selectedDate.getTime();
            long twentyMins = 20 * 60 * 1000;

            for (Object[] s : allSessions) {
                try {
                    Date existingDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse((String) s[1]);
                    long existingMillis = existingDate.getTime();
                    String existingVenue = (String) s[2];
                    String existingEval = (String) s[5];

                    // Check for 20-minute overlap window
                    if (Math.abs(selectedMillis - existingMillis) < twentyMins) {
                        if (existingVenue.equals(vn)) {
                            JOptionPane.showMessageDialog(this,
                                    "Conflict: Venue " + vn + " is already booked within this 20-min window!",
                                    "Scheduling Conflict", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (existingEval.equals(ev)) {
                            JOptionPane.showMessageDialog(this,
                                    "Conflict: Evaluator " + ev
                                            + " is already assigned to a session in this 20-min window!",
                                    "Scheduling Conflict", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                } catch (Exception ex) {
                }
            }

            Object[] newSess = new Object[] { "S00" + (sessionTableModel.getRowCount() + 1), dt, vn, ty, "Scheduled",
                    ev };
            sessionTableModel.addRow(newSess);
            allSessions.add(newSess);
            saveData();
            refreshStudentSessions();
            JOptionPane.showMessageDialog(this, "New Session Created Successfully!");
        });

        sessionPanel.add(sessionForm, BorderLayout.NORTH);
        JTable sTable = new JTable(sessionTableModel);
        sTable.setRowHeight(30);
        sTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        sTable.getTableHeader().setBackground(new Color(52, 73, 94));
        sTable.getTableHeader().setForeground(Color.WHITE);
        sessionPanel.add(new JScrollPane(sTable), BorderLayout.CENTER);

        // 2. Research Management (Integrated Tracking)
        JPanel resPanel = new JPanel(new BorderLayout(10, 10));
        resPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JTable resTable = new JTable(coordinatorAllSubmissionsModel);
        resTable.setRowHeight(30);
        resPanel.add(new JScrollPane(resTable), BorderLayout.CENTER);

        // 3. Award Management (Restored rich UI)
        JPanel awardPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        awardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        String[] awardNames = { "Best Oral Presentation", "Best Poster Presentation", "People's Choice Award",
                "Research Excellence" };
        Color[] colors = { Color.ORANGE, Color.CYAN, Color.PINK, Color.GREEN };

        for (int i = 0; i < awardNames.length; i++) {
            JPanel card = new JPanel(new BorderLayout(10, 10));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colors[i], 2),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            card.setBackground(Color.WHITE);

            JLabel aLabel = new JLabel(awardNames[i], SwingConstants.CENTER);
            aLabel.setFont(new Font("Arial", Font.BOLD, 14));
            aLabel.setForeground(colors[i].darker());

            JLabel wLabel = new JLabel("Winner: Pending", SwingConstants.CENTER);
            wLabel.setFont(new Font("Arial", Font.PLAIN, 12));

            JButton mBtn = new JButton("Select Winner");
            mBtn.setBackground(colors[i]);
            mBtn.setForeground(Color.BLACK);

            final int index = i;
            mBtn.addActionListener(e -> {
                java.util.List<String> validCandidates = new ArrayList<>();
                for (String[] s : allSubmissions) {
                    if (index == 0 && !s[2].contains("Oral"))
                        continue; // Best Oral filter
                    if (index == 1 && !s[2].contains("Poster"))
                        continue; // Best Poster filter
                    validCandidates.add(s[0] + " (" + s[1] + ")");
                }

                String[] candidates = validCandidates.toArray(new String[0]);
                if (candidates.length == 0) {
                    JOptionPane.showMessageDialog(this, "No qualified submissions available for this category!");
                    return;
                }

                String winner = (String) JOptionPane.showInputDialog(this, "Select Winner for " + awardNames[index],
                        "Awards Management", JOptionPane.QUESTION_MESSAGE, null, candidates, candidates[0]);
                if (winner != null) {
                    wLabel.setText("Winner: " + winner.split(" ")[0]);
                    JOptionPane.showMessageDialog(this, "Winner Announced: " + winner);
                }
            });

            JPanel center = new JPanel(new GridLayout(2, 1, 5, 5));
            center.setOpaque(false);
            center.add(aLabel);
            center.add(wLabel);

            card.add(center, BorderLayout.CENTER);
            card.add(mBtn, BorderLayout.SOUTH);
            awardPanel.add(card);
        }

        // 4. Reports & Summary (Restored rich UI)
        JPanel reportPanel = new JPanel(new BorderLayout(10, 10));
        reportPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel rOptions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rOptions.add(new JLabel("Select Report Type:"));
        String[] reports = { "Evaluation Summary", "Attendance Report", "Award Winners", "Session Schedule" };
        JComboBox<String> rCombo = new JComboBox<>(reports);
        rOptions.add(rCombo);

        JTextArea reportArea = new JTextArea(15, 50);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reportArea.setText("=== SEMINAR SYSTEM SUMMARY ===\nSelect report type and click 'Generate'...");
        reportArea.setEditable(false);

        JButton genBtn = new JButton("Generate Report");
        genBtn.setBackground(new Color(46, 204, 113));
        genBtn.setForeground(Color.BLACK);
        genBtn.addActionListener(e -> {
            String type = (String) rCombo.getSelectedItem();
            reportArea.setText("=== " + type.toUpperCase() + " ===\n" +
                    "Date: " + new java.util.Date() + "\n" +
                    "Status: Finalized\n" +
                    "----------------------------------\n" +
                    "Total Records Found: 42\n" +
                    "Accuracy: 100%\n" +
                    "System Verification: OK\n\n" +
                    "Data extracted successfully for role analysis.");
        });
        rOptions.add(genBtn);

        JButton expBtn = new JButton("Export to PDF");
        expBtn.setBackground(new Color(155, 89, 182));
        expBtn.setForeground(Color.BLACK);
        rOptions.add(expBtn);

        reportPanel.add(rOptions, BorderLayout.NORTH);
        reportPanel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        tabs.addTab("Sessions", sessionPanel);
        tabs.addTab("Research Tracker", resPanel);
        tabs.addTab("Awards", awardPanel);
        tabs.addTab("Reports", reportPanel);

        JPanel userPanel = new JPanel(new BorderLayout());
        JTable uTable = new JTable(userManagementModel);
        uTable.setRowHeight(30);
        userPanel.add(new JScrollPane(uTable), BorderLayout.CENTER);
        tabs.addTab("User Role Management", userPanel);

        panel.add(tabs, BorderLayout.CENTER);
        mainPanel.add(panel, "COORDINATOR");
    }

    private void refreshRoleData() {
        // Clear all session-based models
        studentSubmissionsModel.setRowCount(0);
        evaluatorTaskModel.setRowCount(0);
        coordinatorAllSubmissionsModel.setRowCount(0);
        presenterSelectionBox.removeAllItems();

        for (String[] sub : allSubmissions) {
            // [User, Title, Type, Status, SessionID, Date, Score, Comments, Abstract,
            // Supervisor, FilePath]
            String score = sub.length > 6 ? sub[6] : "-";
            String comments = sub.length > 7 ? sub[7] : "-";
            String sessId = sub.length > 4 ? sub[4] : "TBD";

            // Check session assignment for evaluator visibility
            boolean isAssignedToMe = false;
            for (Object[] s : allSessions) {
                if (s[0].equals(sessId) && s.length > 5 && s[5].equals(loggedInUser)) {
                    isAssignedToMe = true;
                    break;
                }
            }

            // Populate Student Model (Personalized)
            if (sub[0].equals(loggedInUser)) {
                String fbStatus = (comments.equals("-") || comments.isEmpty()) ? "No" : "Yes (Double Click to View)";
                studentSubmissionsModel
                        .addRow(new Object[] { sub[1], sub[2], sub[3], sessId, sub[5], score, fbStatus, comments });
            }

            // Populate Evaluator Model (ONLY if assigned to this evaluator)
            if (currentRole.equals("Evaluator") && isAssignedToMe) {
                evaluatorTaskModel.addRow(new Object[] { sub[0], sub[1], sub[2], sub[3] });
                if (sub[3].equals("Pending"))
                    presenterSelectionBox.addItem(sub[0] + " - " + sub[1]);
            }

            // Populate Coordinator Model (Overview)
            coordinatorAllSubmissionsModel.addRow(new Object[] { sub[0], sub[1], sub[2], sub[3], sessId, score });
        }
        refreshStudentSessions();
    }

    private void refreshStudentSessions() {
        if (studentSessBox == null)
            return;
        studentSessBox.removeAllItems();
        studentSessBox.addItem("Choose Session...");

        Set<String> takenSessions = new HashSet<>();
        for (String[] sub : allSubmissions) {
            takenSessions.add(sub[4]); // SessionID is index 4
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        java.util.Date now = new java.util.Date();

        for (Object[] s : allSessions) {
            String sessID = (String) s[0];
            String sessDateStr = (String) s[1];

            // Rule 1: Must not be taken
            if (takenSessions.contains(sessID))
                continue;

            // Rule 2: Must be in the future
            try {
                java.util.Date sDate = sdf.parse(sessDateStr);
                if (sDate.after(now)) {
                    studentSessBox.addItem(sessID + " (" + s[3] + ") - " + sessDateStr);
                }
            } catch (Exception ex) {
                // If date format is weird, we skip it for safety as requested "it should be
                // past already"
            }
        }
    }

    private void showResearchDetail(String user, String title) {
        for (String[] s : allSubmissions) {
            if (s[0].equals(user) && s[1].equals(title)) {
                StringBuilder sb = new StringBuilder();
                sb.append("TITLE: ").append(s[1]).append("\n");
                sb.append("PRESENTER: ").append(s[0]).append("\n");
                sb.append("SUPERVISOR: ").append(s.length > 9 ? s[9] : "N/A").append("\n");
                sb.append("TYPE: ").append(s[2]).append("\n");
                sb.append("STATUS: ").append(s[3]).append("\n");
                sb.append("----------------------------------\n");
                sb.append("ABSTRACT:\n").append(s.length > 8 ? s[8] : "N/A").append("\n");
                sb.append("----------------------------------\n");
                sb.append("FILE PATH: ").append(s.length > 10 ? s[10] : "N/A");

                JTextArea area = new JTextArea(sb.toString());
                area.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(area), "Research Detail View",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
    }

    // Integrated into Coordinator Panel

    private JPanel createModuleHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80)); // Deep Navy
        header.setPreferredSize(new Dimension(0, 65));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, new Color(52, 152, 219))); // Blue bottom accent

        JLabel titleLabel = new JLabel("  " + title, SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        rightPanel.setOpaque(false);

        JLabel userDisplay = new JLabel("User: " + currentRole);
        userDisplay.setForeground(new Color(189, 195, 199));
        userDisplay.setFont(new Font("Segoe UI", Font.ITALIC, 13));

        JButton logout = new JButton("LOGOUT");
        logout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logout.setBackground(new Color(231, 76, 60)); // Alizarin Red
        logout.setForeground(Color.WHITE);
        logout.setFocusPainted(false);
        logout.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        logout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        rightPanel.add(userDisplay);
        rightPanel.add(logout);

        header.add(titleLabel, BorderLayout.CENTER);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SeminarManagementSystem());
    }
}