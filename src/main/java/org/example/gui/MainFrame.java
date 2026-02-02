package org.example.gui;

import org.example.model.ConversionResult;
import org.example.model.Profile;
import org.example.service.ExcelConverterService;
import org.example.service.ProfileManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Main window frame
 */
public class MainFrame extends JFrame {

    private ProfileManager profileManager;
    private ExcelConverterService converterService;

    // UI Components
    private JComboBox<String> profileComboBox;
    private JLabel profileDescLabel;
    private DefaultListModel<FileItem> fileListModel;
    private JList<FileItem> fileList;
    private JTextField outputDirField;
    private JComboBox<String> outputFormatCombo;
    private JCheckBox mergeFilesCheck;
    private JLabel statusLabel;
    private JButton convertButton;
    private JButton cancelButton;

    // Work status
    private ConversionWorker currentWorker;
    private final ConversionWorker.ConversionCallback conversionCallback;

    public MainFrame() {
        this.conversionCallback = createConversionCallback();
        profileManager = new ProfileManager();
        converterService = new ExcelConverterService();

        initUI();
        loadProfiles();
    }

    private void initUI() {
        setTitle("Data File Converter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(650, 550);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(500, 400));

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top: Profile selection
        mainPanel.add(createProfilePanel(), BorderLayout.NORTH);

        // Center: File list
        mainPanel.add(createFileListPanel(), BorderLayout.CENTER);

        // Bottom: Output path + progress + buttons
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Drag and drop setup
        setupDragAndDrop();
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(new TitledBorder("1. Select Profile"));

        // Profile dropdown
        profileComboBox = new JComboBox<>();
        profileComboBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        profileComboBox.addActionListener(e -> onProfileSelected());

        // Profile description
        profileDescLabel = new JLabel(" ");
        profileDescLabel.setForeground(Color.GRAY);

        // New profile button
        JButton addBtn = new JButton("+");
        addBtn.setToolTipText("Create new profile");
        addBtn.setMargin(new Insets(2, 6, 2, 6));
        addBtn.addActionListener(e -> openProfileEditor(null));

        // Edit profile button
        JButton editBtn = new JButton("✎");
        editBtn.setToolTipText("Edit selected profile");
        editBtn.setMargin(new Insets(2, 6, 2, 6));
        editBtn.addActionListener(e -> {
            String selectedName = (String) profileComboBox.getSelectedItem();
            if (selectedName != null && !selectedName.startsWith("(")) {
                Profile profile = profileManager.getProfile(selectedName);
                if (profile != null) {
                    openProfileEditor(profile);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a profile to edit.",
                    "Notice", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Open profiles folder button
        JButton folderBtn = new JButton("📁");
        folderBtn.setToolTipText("Open profiles folder: " + profileManager.getProfilesPath());
        folderBtn.setMargin(new Insets(2, 6, 2, 6));
        folderBtn.addActionListener(e -> openProfilesFolder());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(folderBtn);

        JPanel topRow = new JPanel(new BorderLayout(5, 0));
        topRow.add(profileComboBox, BorderLayout.CENTER);
        topRow.add(buttonPanel, BorderLayout.EAST);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(profileDescLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFileListPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new TitledBorder("2. Select Files (drag & drop xlsx, csv)"));

        // File list
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new FileItemRenderer());

        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(0, 200));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JButton addButton = new JButton("Add Files");
        addButton.addActionListener(e -> addFiles());

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelectedFiles());

        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> fileListModel.clear());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Output path
        JPanel outputPanel = new JPanel(new BorderLayout(5, 0));
        outputPanel.setBorder(new TitledBorder("3. Output Location"));

        outputDirField = new JTextField();
        outputDirField.setText(System.getProperty("user.home") + File.separator + "Desktop");
        outputDirField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseOutputDir());

        outputPanel.add(outputDirField, BorderLayout.CENTER);
        outputPanel.add(browseButton, BorderLayout.EAST);

        // Status
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        statusPanel.add(statusLabel, BorderLayout.CENTER);

        // Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        // Output format selection
        actionPanel.add(new JLabel("Output:"));
        outputFormatCombo = new JComboBox<>(new String[]{"CSV", "Excel (xlsx)"});
        outputFormatCombo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        actionPanel.add(outputFormatCombo);
        actionPanel.add(Box.createHorizontalStrut(10));

        // Merge files checkbox
        mergeFilesCheck = new JCheckBox("Merge all into one file");
        mergeFilesCheck.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        actionPanel.add(mergeFilesCheck);
        actionPanel.add(Box.createHorizontalStrut(20));

        convertButton = new JButton("Convert");
        convertButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        convertButton.setPreferredSize(new Dimension(150, 40));
        convertButton.addActionListener(e -> startConversion());

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> cancelConversion());

        actionPanel.add(convertButton);
        actionPanel.add(cancelButton);

        panel.add(outputPanel);
        panel.add(statusPanel);
        panel.add(actionPanel);

        return panel;
    }

    private void setupDragAndDrop() {
        new FileDrop(fileList, files -> {
            for (File file : files) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".xlsx") || name.endsWith(".csv")) {
                    addFileIfNotExists(file);
                }
            }
        });
    }

    private void loadProfiles() {
        try {
            List<Profile> profiles = profileManager.loadAllProfiles();
            profileComboBox.removeAllItems();

            if (profiles.isEmpty()) {
                profileComboBox.addItem("(No profiles)");
                profileDescLabel.setText("Add JSON profiles to the profiles folder.");
            } else {
                for (Profile p : profiles) {
                    profileComboBox.addItem(p.getProfileName());
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Profile load error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onProfileSelected() {
        String selectedName = (String) profileComboBox.getSelectedItem();
        if (selectedName != null && !selectedName.startsWith("(")) {
            Profile profile = profileManager.getProfile(selectedName);
            if (profile != null) {
                profileDescLabel.setText(profile.getDescription() != null ?
                    profile.getDescription() : "No description");
            }
        }
    }

    /**
     * Open profiles folder
     */
    private void openProfilesFolder() {
        try {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            File folder = profileManager.getProfilesPath().toFile();

            // Create folder if it doesn't exist
            if (!folder.exists()) {
                folder.mkdirs();
            }

            desktop.open(folder);
        } catch (Exception e) {
            // Show path if folder opening fails
            JOptionPane.showMessageDialog(this,
                "Profiles folder path:\n" + profileManager.getProfilesPath().toAbsolutePath(),
                "Profiles Folder Location", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Open profile editor dialog
     * @param profile Profile to edit (null for new profile)
     */
    private void openProfileEditor(Profile profile) {
        ProfileEditorDialog dialog = new ProfileEditorDialog(this, profileManager, profile);
        dialog.setVisible(true);

        // Refresh profile list if saved
        if (dialog.isSaved()) {
            String previousSelection = (String) profileComboBox.getSelectedItem();
            loadProfiles();

            // Select newly created profile
            if (profile == null) {
                int itemCount = profileComboBox.getItemCount();
                if (itemCount > 0) {
                    profileComboBox.setSelectedIndex(itemCount - 1);
                }
            } else {
                // Keep previous selection for edits
                profileComboBox.setSelectedItem(previousSelection);
            }
        }
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".xlsx") || name.endsWith(".csv");
            }
            @Override
            public String getDescription() {
                return "Excel/CSV Files (*.xlsx, *.csv)";
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                addFileIfNotExists(file);
            }
        }
    }

    private void addFileIfNotExists(File file) {
        // Duplicate check
        for (int i = 0; i < fileListModel.size(); i++) {
            if (fileListModel.get(i).getFile().equals(file)) {
                return;
            }
        }
        fileListModel.addElement(new FileItem(file));
    }

    private void removeSelectedFiles() {
        int[] selected = fileList.getSelectedIndices();
        for (int i = selected.length - 1; i >= 0; i--) {
            fileListModel.remove(selected[i]);
        }
    }

    private void browseOutputDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(outputDirField.getText()));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startConversion() {
        // Validation
        String selectedProfile = (String) profileComboBox.getSelectedItem();
        if (selectedProfile == null || selectedProfile.startsWith("(")) {
            JOptionPane.showMessageDialog(this, "Please select a profile.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add files to convert.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File outputDir = new File(outputDirField.getText());
        if (!outputDir.exists()) {
            int result = JOptionPane.showConfirmDialog(this,
                "Output folder does not exist. Create it?",
                "Confirm", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                outputDir.mkdirs();
            } else {
                return;
            }
        }

        // Get file list
        java.util.List<File> files = new java.util.ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            files.add(fileListModel.get(i).getFile());
        }

        Profile profile = profileManager.getProfile(selectedProfile);

        // Apply selected output format
        String selectedFormat = (String) outputFormatCombo.getSelectedItem();
        String outputFormat = selectedFormat.contains("xlsx") ? "xlsx" : "csv";
        profile.getOptions().setOutputFormat(outputFormat);

        // Update UI state
        setUIEnabled(false);

        // Start background work
        boolean mergeFiles = mergeFilesCheck.isSelected();
        currentWorker = new ConversionWorker(profile, files, outputDir, converterService, conversionCallback, this, mergeFiles);
        currentWorker.execute();
    }

    /**
     * Create conversion callback
     */
    private ConversionWorker.ConversionCallback createConversionCallback() {
        return new ConversionWorker.ConversionCallback() {
            @Override
            public void onProgress(ProgressInfo info) {
                statusLabel.setText(String.format("Processing: %s (%,d rows)",
                    info.getFileName(), info.getCurrentRow()));
            }

            @Override
            public void onComplete(java.util.List<ConversionResult> results) {
                setUIEnabled(true);

                long successCount = results.stream().filter(ConversionResult::isSuccess).count();
                long totalInputRows = results.stream().mapToLong(ConversionResult::getInputRows).sum();
                long totalOutputRows = results.stream().mapToLong(ConversionResult::getOutputRows).sum();
                long totalDuplicates = results.stream().mapToLong(ConversionResult::getDuplicateRows).sum();

                StringBuilder sb = new StringBuilder();
                sb.append("Conversion Complete!\n\n");
                sb.append(String.format("Files: %d / %d succeeded\n", successCount, results.size()));
                sb.append(String.format("Input rows: %,d\n", totalInputRows));
                sb.append(String.format("Output rows: %,d\n", totalOutputRows));

                if (totalDuplicates > 0) {
                    sb.append(String.format("Duplicates skipped: %,d\n", totalDuplicates));
                }

                long skipped = totalInputRows - totalOutputRows - totalDuplicates;
                if (skipped > 0) {
                    sb.append(String.format("Empty rows skipped: %,d\n", skipped));
                }

                sb.append(String.format("\nOutput location: %s", outputDirField.getText()));

                // 파일별 상세 정보
                if (results.size() > 1 || successCount < results.size()) {
                    sb.append("\n\n--- Details ---");
                    for (ConversionResult r : results) {
                        sb.append("\n").append(r.getInputFile().getName()).append(": ");
                        if (r.isSuccess()) {
                            sb.append(String.format("%,d → %,d rows", r.getInputRows(), r.getOutputRows()));
                            if (r.getDuplicateRows() > 0) {
                                sb.append(String.format(" (%,d duplicates)", r.getDuplicateRows()));
                            }
                        } else {
                            sb.append("FAILED - ").append(r.getErrorMessage());
                        }
                    }
                }

                if (successCount == results.size()) {
                    JOptionPane.showMessageDialog(MainFrame.this, sb.toString(),
                        "Complete", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this, sb.toString(),
                        "Complete (with errors)", JOptionPane.WARNING_MESSAGE);
                }

                statusLabel.setText("Complete");
            }

            @Override
            public void onError(Exception e) {
                setUIEnabled(true);
                JOptionPane.showMessageDialog(MainFrame.this,
                    "Conversion error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }

            @Override
            public void onCancelled() {
                setUIEnabled(true);
                statusLabel.setText("Operation cancelled.");
            }
        };
    }

    private void cancelConversion() {
        if (currentWorker != null) {
            converterService.cancel();
            currentWorker.cancel(true);
        }
    }

    private void setUIEnabled(boolean enabled) {
        profileComboBox.setEnabled(enabled);
        fileList.setEnabled(enabled);
        outputDirField.setEnabled(enabled);
        convertButton.setEnabled(enabled);
        cancelButton.setEnabled(!enabled);

        if (enabled) {
            statusLabel.setText("Ready");
        }
    }
}
