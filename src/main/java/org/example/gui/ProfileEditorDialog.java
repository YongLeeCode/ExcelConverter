package org.example.gui;

import org.example.model.Calculation;
import org.example.model.ColumnMapping;
import org.example.model.OutputOptions;
import org.example.model.Profile;
import org.example.service.ProfileManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Profile create/edit dialog
 */
public class ProfileEditorDialog extends JDialog {

    private final ProfileManager profileManager;
    private final Profile originalProfile;
    private final boolean isEditMode;

    // Basic info
    private JTextField nameField;
    private JTextField descriptionField;
    private JTextField outputFileField;

    // Column mapping table
    private DefaultTableModel columnTableModel;
    private JTable columnTable;

    // Calculation column table
    private DefaultTableModel calcTableModel;
    private JTable calcTable;


    // Result
    private boolean saved = false;

    /**
     * New profile mode
     */
    public ProfileEditorDialog(Frame owner, ProfileManager profileManager) {
        this(owner, profileManager, null);
    }

    /**
     * Edit profile mode
     */
    public ProfileEditorDialog(Frame owner, ProfileManager profileManager, Profile profile) {
        super(owner, profile == null ? "Create New Profile" : "Edit Profile", true);
        this.profileManager = profileManager;
        this.originalProfile = profile;
        this.isEditMode = (profile != null);

        initUI();
        if (isEditMode) {
            loadProfileData();
        }

        setSize(700, 700);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Scrollable content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(createBasicInfoPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createColumnMappingPanel());
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createCalculationPanel());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createBasicInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Basic Info"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Profile name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Profile Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        nameField = new JTextField(30);
        panel.add(nameField, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        descriptionField = new JTextField(30);
        panel.add(descriptionField, gbc);

        // Output filename
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Output Filename:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        outputFileField = new JTextField(30);
        outputFileField.setToolTipText("e.g., output.csv, {filename}_converted.csv");
        panel.add(outputFileField, gbc);

        // Encoding info
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JLabel encodingLabel = new JLabel("CSV Encoding: UTF-8-BOM (Excel compatible)");
        encodingLabel.setForeground(Color.GRAY);
        encodingLabel.setFont(encodingLabel.getFont().deriveFont(Font.ITALIC, 11f));
        panel.add(encodingLabel, gbc);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

        return panel;
    }

    private JPanel createColumnMappingPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Column Mapping"));

        // Table model
        String[] columnNames = {"Source Column", "Output Column", "Type", "Required", "Unique Key"};
        columnTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return (column == 3 || column == 4) ? Boolean.class : String.class;
            }
        };
        columnTable = new JTable(columnTableModel);
        columnTable.setRowHeight(25);

        // Type combobox
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"string", "number", "date"});
        columnTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(typeCombo));

        JScrollPane scrollPane = new JScrollPane(columnTable);
        scrollPane.setPreferredSize(new Dimension(0, 150));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton addBtn = new JButton("+ Add Column");
        addBtn.addActionListener(e -> addColumnRow());
        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> removeSelectedColumnRows());

        buttonPanel.add(addBtn);
        buttonPanel.add(removeBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        return panel;
    }

    private JPanel createCalculationPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Calculated Columns (Optional)"));

        // Table model
        String[] columnNames = {"New Column", "Formula", "Insert After", "Format"};
        calcTableModel = new DefaultTableModel(columnNames, 0);
        calcTable = new JTable(calcTableModel);
        calcTable.setRowHeight(25);

        calcTable.getColumnModel().getColumn(1).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(calcTable);
        scrollPane.setPreferredSize(new Dimension(0, 120));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton addBtn = new JButton("+ Add Calculation");
        addBtn.addActionListener(e -> addCalcRow());
        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> removeSelectedCalcRows());

        JButton helpBtn = new JButton("?");
        helpBtn.setMargin(new Insets(2, 6, 2, 6));
        helpBtn.setToolTipText("Show formula help");
        helpBtn.addActionListener(e -> showFormulaHelp());

        buttonPanel.add(addBtn);
        buttonPanel.add(removeBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(helpBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

        return panel;
    }


    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveProfile());

        panel.add(cancelBtn);
        panel.add(saveBtn);

        return panel;
    }

    private void addColumnRow() {
        columnTableModel.addRow(new Object[]{"", "", "string", false, false});
    }

    private void removeSelectedColumnRows() {
        int[] rows = columnTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            columnTableModel.removeRow(rows[i]);
        }
    }

    private void addCalcRow() {
        calcTableModel.addRow(new Object[]{"", "", "", ""});
    }

    private void showFormulaHelp() {
        String help = """
            === Formula Help ===

            ▶ Math Operations:
              ${ColumnA} + ${ColumnB}    Addition
              ${ColumnA} - ${ColumnB}    Subtraction
              ${ColumnA} * ${ColumnB}    Multiplication
              ${ColumnA} / ${ColumnB}    Division

            ▶ Text Functions:
              LEFT(${Column}, n)         First n characters
              RIGHT(${Column}, n)        Last n characters
              SUBSTR(${Column}, start, length)   Substring (0-indexed)
              MID(${Column}, start, length)      Same as SUBSTR
              TRIM(${Column})            Remove leading/trailing spaces

            ▶ Examples:
              Column "Period" has value "2026012"
              - LEFT(${Period}, 4)    → "2026" (year)
              - RIGHT(${Period}, 2)   → "12" (month)
              - SUBSTR(${Period}, 4, 3) → "012"
            """;

        JTextArea textArea = new JTextArea(help);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 350));

        JOptionPane.showMessageDialog(this, scrollPane, "Formula Help",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void removeSelectedCalcRows() {
        int[] rows = calcTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            calcTableModel.removeRow(rows[i]);
        }
    }

    private void loadProfileData() {
        // Basic info
        nameField.setText(originalProfile.getProfileName());
        descriptionField.setText(originalProfile.getDescription());
        outputFileField.setText(originalProfile.getOutputFileName());

        // Name not editable in edit mode
        nameField.setEditable(false);
        nameField.setBackground(Color.LIGHT_GRAY);

        // Column mapping
        for (ColumnMapping col : originalProfile.getColumns()) {
            columnTableModel.addRow(new Object[]{
                col.getSource(),
                col.getTarget() != null ? col.getTarget() : "",
                col.getType(),
                col.isRequired(),
                col.isUniqueKey()
            });
        }

        // Calculation columns
        for (Calculation calc : originalProfile.getCalculations()) {
            calcTableModel.addRow(new Object[]{
                calc.getNewColumn(),
                calc.getFormula(),
                calc.getInsertAfter() != null ? calc.getInsertAfter() : "",
                calc.getFormat() != null ? calc.getFormat() : ""
            });
        }
    }

    private void saveProfile() {
        // Validation
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a profile name.",
                "Input Error", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }

        // Commit editing cells
        if (columnTable.isEditing()) {
            columnTable.getCellEditor().stopCellEditing();
        }
        if (calcTable.isEditing()) {
            calcTable.getCellEditor().stopCellEditing();
        }

        // Warn if no column mappings
        if (columnTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please add at least one column mapping.",
                "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Create Profile object
            Profile profile = new Profile();
            profile.setProfileName(name);
            profile.setDescription(descriptionField.getText().trim());
            profile.setOutputFileName(outputFileField.getText().trim());

            // Column mapping
            List<ColumnMapping> columns = new ArrayList<>();
            for (int i = 0; i < columnTableModel.getRowCount(); i++) {
                String source = (String) columnTableModel.getValueAt(i, 0);
                String target = (String) columnTableModel.getValueAt(i, 1);
                String type = (String) columnTableModel.getValueAt(i, 2);
                Boolean required = (Boolean) columnTableModel.getValueAt(i, 3);
                Boolean uniqueKey = (Boolean) columnTableModel.getValueAt(i, 4);

                if (source != null && !source.trim().isEmpty()) {
                    ColumnMapping col = new ColumnMapping();
                    col.setSource(source.trim());
                    col.setTarget(target != null && !target.trim().isEmpty() ? target.trim() : null);
                    col.setType(type != null ? type : "string");
                    col.setRequired(required != null && required);
                    col.setUniqueKey(uniqueKey != null && uniqueKey);
                    columns.add(col);
                }
            }
            profile.setColumns(columns);

            // Calculation columns
            List<Calculation> calculations = new ArrayList<>();
            for (int i = 0; i < calcTableModel.getRowCount(); i++) {
                String newColumn = (String) calcTableModel.getValueAt(i, 0);
                String formula = (String) calcTableModel.getValueAt(i, 1);
                String insertAfter = (String) calcTableModel.getValueAt(i, 2);
                String format = (String) calcTableModel.getValueAt(i, 3);

                if (newColumn != null && !newColumn.trim().isEmpty() &&
                    formula != null && !formula.trim().isEmpty()) {
                    Calculation calc = new Calculation();
                    calc.setNewColumn(newColumn.trim());
                    calc.setFormula(formula.trim());
                    calc.setInsertAfter(insertAfter != null && !insertAfter.trim().isEmpty() ?
                        insertAfter.trim() : null);
                    calc.setFormat(format != null && !format.trim().isEmpty() ?
                        format.trim() : null);
                    calculations.add(calc);
                }
            }
            profile.setCalculations(calculations);

            // Output options (use defaults)
            OutputOptions opts = new OutputOptions();
            opts.setOutputEncoding("UTF-8-BOM");
            opts.setDelimiter(",");
            opts.setSkipEmptyRows(true);
            opts.setTrimWhitespace(true);
            opts.setQuoteAll(false);
            profile.setOptions(opts);

            // Save
            profileManager.saveProfile(profile);
            saved = true;

            JOptionPane.showMessageDialog(this,
                "Profile saved: " + name,
                "Save Complete", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error saving profile: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Returns whether profile was saved
     */
    public boolean isSaved() {
        return saved;
    }
}
