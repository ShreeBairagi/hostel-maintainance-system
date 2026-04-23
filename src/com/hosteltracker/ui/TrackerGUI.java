package com.hosteltracker.ui;

import com.hosteltracker.model.enums.RequestStatus;
import com.hosteltracker.model.request.Request;
import com.hosteltracker.model.user.MaintenanceStaff;
import com.hosteltracker.service.MaintenanceService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerGUI extends JFrame {
    private final MaintenanceService service;
    private final CardLayout roleCardLayout = new CardLayout();
    private final JPanel roleCardPanel = new JPanel(roleCardLayout);
    private final JComboBox<String> roleSelector = new JComboBox<>(
            new String[]{"Student", "Warden", "Maintenance Worker"});

    private DefaultTableModel studentTableModel;
    private DefaultTableModel wardenTableModel;
    private DefaultTableModel workerTaskTableModel;
    private DefaultTableModel staffTableModel;

    private final JTextField roomField = new JTextField();
    private final JComboBox<String> categoryBox = new JComboBox<>(new String[]{"plumbing", "electrical", "carpentry"});
    private final JComboBox<String> repairTypeBox = new JComboBox<>();
    private final JTextField descriptionField = new JTextField();
    private final JTextField photoPathField = new JTextField();
    private final JTextField trackRoomField = new JTextField();
    private final JTextArea trackResultArea = new JTextArea();

    private final JTextField assignIdField = new JTextField();
    private final JComboBox<String> availableStaffBox = new JComboBox<>();

    private final JTextField employeeIdField = new JTextField();
    private final JTextField employeeNameField = new JTextField();
    private final JComboBox<String> employeeSpecialtyBox =
            new JComboBox<>(new String[]{"plumbing", "electrical", "carpentry", "general"});

    private final JTextField statusIdField = new JTextField();
    private final JComboBox<RequestStatus> statusBox = new JComboBox<>(RequestStatus.values());
    private final JTextField workerNameFilterField = new JTextField();

    private final JComboBox<String> completedRequestBox = new JComboBox<>();
    private final JTextField ratingField = new JTextField();
    private final JLabel analyticsLabel = new JLabel("Click refresh to view issue distribution");
    private final Map<String, List<String>> repairOptionsByCategory = new HashMap<>();

    public TrackerGUI() {
        this.service = new MaintenanceService();
        setTitle("Hostel Room Maintenance Request Tracker");
        setSize(980, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topBar = new JPanel(new GridLayout(1, 2, 12, 12));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        topBar.add(new JLabel("Active Module"));
        topBar.add(roleSelector);
        roleSelector.addActionListener(e -> switchRoleView());

        roleCardPanel.add(buildStudentModule(), "Student");
        roleCardPanel.add(buildWardenModule(), "Warden");
        roleCardPanel.add(buildWorkerModule(), "Maintenance Worker");

        setLayout(new BorderLayout());
        add(topBar, BorderLayout.NORTH);
        add(roleCardPanel, BorderLayout.CENTER);

        initializeRepairOptions();
        configureRequestInputBehavior();
        switchRoleView();
        refreshAllViews();
    }

    private JPanel buildStudentModule() {
        JPanel container = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student Request", buildCreateRequestPanel());
        tabs.addTab("Track Request", buildTrackRequestPanel());
        tabs.addTab("Rate Service", buildRatingPanel());
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildWardenModule() {
        JPanel container = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Warden Assignment", buildAssignPanel());
        tabs.addTab("Manage Employees", buildEmployeeManagementPanel());
        tabs.addTab("All Requests", buildWardenTablePanel());
        tabs.addTab("Analytics", buildAnalyticsPanel());
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildWorkerModule() {
        JPanel container = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Status Update", buildStatusPanel());
        tabs.addTab("Assigned/Open Tasks", buildWorkerTaskPanel());
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    private void switchRoleView() {
        String role = String.valueOf(roleSelector.getSelectedItem());
        roleCardLayout.show(roleCardPanel, role);
    }

    private JPanel buildCreateRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        form.add(new JLabel("Room (e.g., A-101)"));
        form.add(roomField);
        form.add(new JLabel("Category"));
        form.add(categoryBox);
        form.add(new JLabel("Repair type"));
        form.add(repairTypeBox);
        form.add(new JLabel("Issue description"));
        form.add(descriptionField);
        form.add(new JLabel("Photo path (simulated)"));
        form.add(photoPathField);

        JButton submit = new JButton("Create Request");
        submit.addActionListener(e -> createRequest());

        form.add(new JLabel(""));
        form.add(submit);
        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildTrackRequestPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel row = new JPanel(new GridLayout(1, 3, 8, 8));
        row.add(new JLabel("Room Number"));
        row.add(trackRoomField);
        JButton check = new JButton("Show Open Requests");
        check.addActionListener(e -> trackOpenRequestsForRoom());
        row.add(check);

        trackResultArea.setEditable(false);
        trackResultArea.setLineWrap(true);
        trackResultArea.setWrapStyleWord(true);
        trackResultArea.setText("Enter room number (e.g., A-101) to view open requests.");
        panel.add(row);
        panel.add(new JScrollPane(trackResultArea));
        return panel;
    }

    private JPanel buildAssignPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        panel.add(new JLabel("Request ID"));
        panel.add(assignIdField);
        panel.add(new JLabel("Available workers"));
        panel.add(availableStaffBox);

        JButton loadAvailableButton = new JButton("Load Available Workers");
        loadAvailableButton.addActionListener(e -> refreshAvailableStaffDropdown());
        panel.add(new JLabel(""));
        panel.add(loadAvailableButton);

        JButton assignButton = new JButton("Assign Request");
        assignButton.addActionListener(e -> assignRequest());
        panel.add(new JLabel(""));
        panel.add(assignButton);
        return panel;
    }

    private JPanel buildEmployeeManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        form.add(new JLabel("Employee ID"));
        form.add(employeeIdField);
        form.add(new JLabel("Employee Name"));
        form.add(employeeNameField);
        form.add(new JLabel("Specialty"));
        form.add(employeeSpecialtyBox);
        JButton addButton = new JButton("Add Employee");
        addButton.addActionListener(e -> addEmployee());
        form.add(new JLabel(""));
        form.add(addButton);
        panel.add(form, BorderLayout.NORTH);

        String[] columns = {"Employee ID", "Name", "Specialty"};
        staffTableModel = createReadOnlyModel(columns);
        JTable table = new JTable(staffTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        panel.add(new JLabel("Request ID"));
        panel.add(statusIdField);
        panel.add(new JLabel("New status"));
        panel.add(statusBox);

        JButton updateButton = new JButton("Update Status");
        updateButton.addActionListener(e -> updateStatus());
        panel.add(new JLabel(""));
        panel.add(updateButton);
        return panel;
    }

    private JPanel buildRatingPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        panel.add(new JLabel("Completed Request"));
        panel.add(completedRequestBox);
        panel.add(new JLabel("Rating (1-5)"));
        panel.add(ratingField);

        JButton rateButton = new JButton("Submit Rating");
        rateButton.addActionListener(e -> submitRating());
        panel.add(new JLabel(""));
        panel.add(rateButton);
        return panel;
    }

    private JPanel buildWardenTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"ID", "Room", "Category", "Status", "Priority", "Assigned To", "Rating"};
        wardenTableModel = createReadOnlyModel(columns);
        JTable table = new JTable(wardenTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshAllViews());
        panel.add(refreshButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildWorkerTaskPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel filterPanel = new JPanel(new GridLayout(1, 3, 8, 8));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        filterPanel.add(new JLabel("Worker name filter"));
        filterPanel.add(workerNameFilterField);
        JButton refresh = new JButton("Refresh Tasks");
        refresh.addActionListener(e -> refreshAllViews());
        filterPanel.add(refresh);
        panel.add(filterPanel, BorderLayout.NORTH);

        String[] columns = {"ID", "Room", "Category", "Status", "Priority", "Assigned To"};
        workerTaskTableModel = createReadOnlyModel(columns);
        JTable table = new JTable(workerTaskTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        analyticsLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(analyticsLabel, BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh Analytics");
        refresh.addActionListener(e -> refreshAnalytics());
        panel.add(refresh, BorderLayout.SOUTH);
        return panel;
    }

    private DefaultTableModel createReadOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void createRequest() {
        try {
            String selectedRepairType = String.valueOf(repairTypeBox.getSelectedItem());
            String issueDescription = selectedRepairType;
            if ("other".equalsIgnoreCase(selectedRepairType)) {
                issueDescription = descriptionField.getText().trim();
                if (issueDescription.isEmpty()) {
                    throw new IllegalArgumentException("Please describe the issue when repair type is Other.");
                }
            }
            Request request = service.createRequest(
                    roomField.getText().trim(),
                    String.valueOf(categoryBox.getSelectedItem()),
                    issueDescription,
                    photoPathField.getText().trim()
            );
            JOptionPane.showMessageDialog(this,
                    "Request created. ID: " + request.getRequestId() + ", Priority: " + request.getPriority());
            refreshAllViews();
            roomField.setText("");
            descriptionField.setText("");
            photoPathField.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void trackOpenRequestsForRoom() {
        try {
            String room = trackRoomField.getText().trim();
            if (room.isEmpty()) {
                throw new IllegalArgumentException("Please enter a room number.");
            }

            List<Request> all = service.getAllRequests();
            StringBuilder output = new StringBuilder();
            for (Request request : all) {
                if (room.equalsIgnoreCase(request.getRoomNumber())
                        && request.getStatus() != RequestStatus.COMPLETED) {
                    output.append("ID ")
                            .append(request.getRequestId())
                            .append(" | ")
                            .append(request.getCategory())
                            .append(" | ")
                            .append(request.getIssueDescription())
                            .append(" | Status: ")
                            .append(request.getStatus())
                            .append(" | Assigned: ")
                            .append(request.getAssignedTo() == null ? "-" : request.getAssignedTo())
                            .append("\n");
                }
            }

            if (output.length() == 0) {
                trackResultArea.setText("No open requests found for room " + room + ".");
                return;
            }
            trackResultArea.setText(output.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void assignRequest() {
        try {
            int requestId = Integer.parseInt(assignIdField.getText().trim());
            String selectedStaff = String.valueOf(availableStaffBox.getSelectedItem());
            String staffName = extractStaffNameFromSelection(selectedStaff);
            service.assignRequest(requestId, staffName);
            JOptionPane.showMessageDialog(this, "Request assigned.");
            refreshAllViews();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addEmployee() {
        try {
            service.addMaintenanceStaff(
                    employeeIdField.getText().trim(),
                    employeeNameField.getText().trim(),
                    String.valueOf(employeeSpecialtyBox.getSelectedItem()));
            JOptionPane.showMessageDialog(this, "Employee added.");
            employeeIdField.setText("");
            employeeNameField.setText("");
            refreshAllViews();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus() {
        try {
            int requestId = Integer.parseInt(statusIdField.getText().trim());
            RequestStatus status = (RequestStatus) statusBox.getSelectedItem();
            service.updateRequestStatus(requestId, status);
            JOptionPane.showMessageDialog(this, "Status updated.");
            refreshAllViews();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitRating() {
        try {
            String selected = String.valueOf(completedRequestBox.getSelectedItem());
            int requestId = parseRequestIdFromSelection(selected);
            int rating = Integer.parseInt(ratingField.getText().trim());
            service.rateCompletedRequest(requestId, rating);
            JOptionPane.showMessageDialog(this, "Rating saved.");
            refreshAllViews();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAllViews() {
        List<Request> all = service.getAllRequests();
        refreshWardenTable(all);
        refreshWorkerTasks(all);
        refreshCompletedRequestOptions(all);
        refreshStaffTable();
        refreshAvailableStaffDropdown();
        refreshAnalytics();
    }

    private void refreshWardenTable(List<Request> all) {
        if (wardenTableModel == null) {
            return;
        }
        wardenTableModel.setRowCount(0);
        for (Request request : all) {
            wardenTableModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getRoomNumber(),
                    request.getCategory(),
                    request.getStatus(),
                    request.getPriority(),
                    request.getAssignedTo() == null ? "-" : request.getAssignedTo(),
                    request.getRating() == null ? "-" : request.getRating()
            });
        }
    }

    private void refreshWorkerTasks(List<Request> all) {
        if (workerTaskTableModel == null) {
            return;
        }
        workerTaskTableModel.setRowCount(0);
        String filter = workerNameFilterField.getText().trim().toLowerCase();
        for (Request request : all) {
            if (request.getStatus() == RequestStatus.COMPLETED) {
                continue;
            }
            String assignedTo = request.getAssignedTo() == null ? "" : request.getAssignedTo();
            if (!filter.isEmpty() && !assignedTo.toLowerCase().contains(filter)) {
                continue;
            }
            workerTaskTableModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getRoomNumber(),
                    request.getCategory(),
                    request.getStatus(),
                    request.getPriority(),
                    assignedTo.isEmpty() ? "-" : assignedTo
            });
        }
    }

    private void refreshAnalytics() {
        Map<String, Integer> dist = service.getIssueDistribution();
        if (dist.isEmpty()) {
            analyticsLabel.setText("No issues logged yet.");
            return;
        }

        StringBuilder text = new StringBuilder("<html><h3>Issue Distribution</h3>");
        for (Map.Entry<String, Integer> entry : dist.entrySet()) {
            text.append(entry.getKey()).append(": ").append(entry.getValue()).append("<br/>");
        }
        text.append("</html>");
        analyticsLabel.setText(text.toString());
    }

    private void refreshStaffTable() {
        if (staffTableModel == null) {
            return;
        }
        staffTableModel.setRowCount(0);
        List<MaintenanceStaff> allStaff = service.getAllMaintenanceStaff();
        for (MaintenanceStaff staff : allStaff) {
            staffTableModel.addRow(new Object[]{
                    staff.getUserId(),
                    staff.getName(),
                    staff.getSpecialty()
            });
        }
    }

    private void refreshAvailableStaffDropdown() {
        availableStaffBox.removeAllItems();
        String requestIdText = assignIdField.getText().trim();
        if (requestIdText.isEmpty()) {
            availableStaffBox.addItem("Enter request ID first");
            return;
        }
        try {
            int requestId = Integer.parseInt(requestIdText);
            List<MaintenanceStaff> availableStaff = service.getAvailableStaffForRequest(requestId);
            if (availableStaff.isEmpty()) {
                availableStaffBox.addItem("No available workers for this request");
                return;
            }
            for (MaintenanceStaff staff : availableStaff) {
                availableStaffBox.addItem(staff.getName() + " (" + staff.getUserId() + ") - " + staff.getSpecialty());
            }
        } catch (Exception ex) {
            availableStaffBox.addItem("Invalid request ID");
        }
    }

    private void initializeRepairOptions() {
        repairOptionsByCategory.put("plumbing", Arrays.asList(
                "Leaking tap",
                "Blocked drain",
                "Low water pressure",
                "Toilet issue",
                "Other"));
        repairOptionsByCategory.put("electrical", Arrays.asList(
                "Fan not working",
                "Light not working",
                "Socket issue",
                "Switch issue",
                "Other"));
        repairOptionsByCategory.put("carpentry", Arrays.asList(
                "Door lock issue",
                "Broken chair/desk",
                "Window latch problem",
                "Cupboard hinge issue",
                "Other"));
    }

    private void configureRequestInputBehavior() {
        categoryBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateRepairTypesForSelectedCategory();
            }
        });
        repairTypeBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDescriptionMode();
            }
        });
        updateRepairTypesForSelectedCategory();
    }

    private void updateRepairTypesForSelectedCategory() {
        String category = String.valueOf(categoryBox.getSelectedItem()).toLowerCase();
        List<String> options = repairOptionsByCategory.getOrDefault(category, Arrays.asList("Other"));
        repairTypeBox.removeAllItems();
        for (String option : options) {
            repairTypeBox.addItem(option);
        }
        updateDescriptionMode();
    }

    private void updateDescriptionMode() {
        String selectedRepairType = String.valueOf(repairTypeBox.getSelectedItem());
        boolean isOther = "other".equalsIgnoreCase(selectedRepairType);
        descriptionField.setEnabled(isOther);
        if (isOther) {
            descriptionField.setText("");
            descriptionField.setToolTipText("Describe the issue");
        } else {
            descriptionField.setText(selectedRepairType == null ? "" : selectedRepairType);
            descriptionField.setToolTipText("Auto-filled by repair type");
        }
    }

    private void refreshCompletedRequestOptions(List<Request> all) {
        completedRequestBox.removeAllItems();
        List<String> options = new ArrayList<>();
        for (Request request : all) {
            if (request.getStatus() == RequestStatus.COMPLETED) {
                options.add("ID " + request.getRequestId() + " | Room " + request.getRoomNumber() + " | "
                        + request.getCategory() + " | " + request.getIssueDescription());
            }
        }
        if (options.isEmpty()) {
            completedRequestBox.addItem("No completed requests available");
            completedRequestBox.setEnabled(false);
            return;
        }
        completedRequestBox.setEnabled(true);
        for (String option : options) {
            completedRequestBox.addItem(option);
        }
    }

    private int parseRequestIdFromSelection(String selection) {
        if (selection == null || !selection.startsWith("ID ")) {
            throw new IllegalArgumentException("No completed request available for rating.");
        }
        String[] parts = selection.split("\\|");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid request selection.");
        }
        String idPart = parts[0].trim().replace("ID ", "");
        return Integer.parseInt(idPart);
    }

    private String extractStaffNameFromSelection(String selection) {
        if (selection == null
                || selection.trim().isEmpty()
                || selection.startsWith("Enter request ID")
                || selection.startsWith("No available workers")
                || selection.startsWith("Invalid request ID")) {
            throw new IllegalArgumentException("Select a valid available worker.");
        }
        int bracketStart = selection.indexOf(" (");
        if (bracketStart <= 0) {
            throw new IllegalArgumentException("Invalid staff selection.");
        }
        return selection.substring(0, bracketStart).trim();
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            TrackerGUI app = new TrackerGUI();
            app.setVisible(true);
        });
    }
}

