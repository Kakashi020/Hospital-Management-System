package com.hospital;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

// ---------------- Interface for Insurance ----------------
interface Insurance {
    double applyInsurance(double amount);
}

// ---------------- Custom Exception ----------------
class InvalidInputException extends Exception {
    InvalidInputException(String msg) {
        super(msg);
    }
}

// ---------------- Patient Classes ----------------
class Patient {
    int patientId;
    String name, disease;
    int age;
    double treatmentCost;

    Patient(int patientId, String name, int age, String disease, double treatmentCost) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.disease = disease;
        this.treatmentCost = treatmentCost;
    }

    double getFinalBill() {
        return treatmentCost;
    }
}

class InsuredPatient extends Patient implements Insurance {
    double insuranceDiscount = 0.15;

    InsuredPatient(int patientId, String name, int age, String disease, double treatmentCost) {
        super(patientId, name, age, disease, treatmentCost);
    }

    @Override
    public double applyInsurance(double amount) {
        return amount * (1 - insuranceDiscount);
    }

    @Override
    double getFinalBill() {
        return applyInsurance(treatmentCost);
    }
}

// ---------------- MongoDB Connection ----------------
class MongoDBConnection {
    private static final String CONNECTION_STRING = "mongodb+srv://induc346_db_user:Tarun9988@cluster0.wfqzcxv.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
    private static final String DB_NAME = "HospitalDB";
    private static final String PATIENTS_COLLECTION = "patients";
    private static final String COUNTERS_COLLECTION = "counters";
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            try {
                MongoClient mongoClient = MongoClients.create(CONNECTION_STRING);
                database = mongoClient.getDatabase(DB_NAME);
                System.out.println("✅ Connected to MongoDB Atlas");
            } catch (Exception e) {
                System.err.println("❌ Failed to connect to MongoDB: " + e.getMessage());
            }
        }
        return database;
    }

    public static MongoCollection<Document> getPatientsCollection() {
        return getDatabase().getCollection(PATIENTS_COLLECTION);
    }

    public static int getNextPatientId() {
        MongoCollection<Document> countersCollection = getDatabase().getCollection(COUNTERS_COLLECTION);
        Document find = new Document("_id", "patientId");
        Document update = new Document("$inc", new Document("sequence_value", 1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
            .upsert(true)
            .returnDocument(ReturnDocument.AFTER);

        Document result = countersCollection.findOneAndUpdate(find, update, options);

        if (result == null) {
            countersCollection.insertOne(new Document("_id", "patientId").append("sequence_value", 101));
            return 101;
        } else {
            return result.getInteger("sequence_value");
        }
    }
}


// ---------------- Main GUI ----------------
public class HospitalManagementSystem extends JFrame {
    private JTextField tName, tAge, tDisease, tCost;
    private JRadioButton yes, no;
    private JButton addBtn, updateBtn, dischargeBtn, searchBtn, clearBtn, refreshBtn;
    private JTable table;
    private DefaultTableModel tableModel;
    private ObjectId selectedPatientObjectId = null;
    private static final DecimalFormat df = new DecimalFormat("0.00");


    HospitalManagementSystem() {
        setupUI();
        addListeners();
        refreshTable();
    }

    private void setupUI() {
        setTitle("🏥 Hospital Management System");
        setSize(1300, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        BackgroundImagePanel mainPanel = new BackgroundImagePanel("hospital_background.jpg");
        mainPanel.setLayout(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        JLabel title = new JLabel("Hospital Management System", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(new Color(255, 255, 255, 220));
        mainPanel.add(title, BorderLayout.NORTH);

        // --- Form Panel (Left) ---
        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setOpaque(false);
        RoundedPanel formPanel = new RoundedPanel(20);
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBackground(new Color(255, 255, 255, 200));
        formPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(100, 149, 237), 2, true),
                "Patient Details", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 18), new Color(33, 66, 99)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; tName = new JTextField(15); formPanel.add(tName, gbc);
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Age:"), gbc);
        gbc.gridx = 1; tAge = new JTextField(); formPanel.add(tAge, gbc);
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Disease:"), gbc);
        gbc.gridx = 1; tDisease = new JTextField(); formPanel.add(tDisease, gbc);
        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Treatment Cost:"), gbc);
        gbc.gridx = 1; tCost = new JTextField(); formPanel.add(tCost, gbc);
        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(new JLabel("Insurance:"), gbc);
        yes = new JRadioButton("Yes"); no = new JRadioButton("No", true);
        yes.setOpaque(false); no.setOpaque(false);
        ButtonGroup bg = new ButtonGroup(); bg.add(yes); bg.add(no);
        JPanel insPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); insPanel.setOpaque(false);
        insPanel.add(yes); insPanel.add(no);
        gbc.gridx = 1; formPanel.add(insPanel, gbc);
        formContainer.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(formContainer, BorderLayout.WEST);

        // --- Table Panel ---
        tableModel = new DefaultTableModel(new String[]{"_id", "Patient ID", "Name", "Age", "Disease", "Cost", "Insurance", "Final Bill", "Discharge"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 8) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        hideTableColumn(table, 0);

        TableColumn dischargeColumn = table.getColumnModel().getColumn(8);
        dischargeColumn.setMinWidth(80);
        dischargeColumn.setMaxWidth(80);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setOpaque(false);


        addBtn = createButton("➕ Add", new Color(46, 204, 113), "Add a new patient");
        updateBtn = createButton("🔄 Update", new Color(52, 152, 219), "Update selected patient's details");
        dischargeBtn = createButton("❌ Discharge Selected", new Color(231, 76, 60), "Discharge all patients checked in the table");
        searchBtn = createButton("🔍 Search by ID", new Color(241, 196, 15), "Search for a patient by their ID");
        clearBtn = createButton("✨ Clear Form", new Color(155, 89, 182), "Clear the form fields");
        refreshBtn = createButton("🔃 Refresh", new Color(52, 73, 94), "Refresh the patient list from the database");

        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(dischargeBtn);
        btnPanel.add(searchBtn);
        btnPanel.add(clearBtn);
        btnPanel.add(refreshBtn);

        mainPanel.add(btnPanel, BorderLayout.SOUTH);
        updateBtn.setEnabled(false);

        setVisible(true);
    }


    private void hideTableColumn(JTable tbl, int colIndex) {
        TableColumn column = tbl.getColumnModel().getColumn(colIndex);
        column.setMinWidth(0);
        column.setMaxWidth(0);
        column.setWidth(0);
        column.setPreferredWidth(0);
    }

    private JButton createButton(String text, Color color, String tooltip) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(color.darker(), 2, true));
        btn.setPreferredSize(new Dimension(180, 40));
        btn.setToolTipText(tooltip);
        return btn;
    }

    private void addListeners() {
        addBtn.addActionListener(e -> addPatient());
        updateBtn.addActionListener(e -> updatePatient());
        dischargeBtn.addActionListener(e -> dischargeSelectedPatients());
        searchBtn.addActionListener(e -> searchPatientById());
        clearBtn.addActionListener(e -> clearForm());
        refreshBtn.addActionListener(e -> refreshTable());


        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int selectedRow = table.getSelectedRow();
                selectedPatientObjectId = (ObjectId) tableModel.getValueAt(selectedRow, 0);


                tName.setText(tableModel.getValueAt(selectedRow, 2).toString());
                tAge.setText(tableModel.getValueAt(selectedRow, 3).toString());
                tDisease.setText(tableModel.getValueAt(selectedRow, 4).toString());
                tCost.setText(tableModel.getValueAt(selectedRow, 5).toString());
                if ("Yes".equals(tableModel.getValueAt(selectedRow, 6).toString())) {
                    yes.setSelected(true);
                } else {
                    no.setSelected(true);
                }
                updateBtn.setEnabled(true);
            }
        });
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        MongoCollection<Document> collection = MongoDBConnection.getPatientsCollection();
        for (Document doc : collection.find().sort(new Document("patientId", 1))) { // Sort by patient ID
            tableModel.addRow(new Object[]{
                    doc.getObjectId("_id"),
                    "P-" + doc.getInteger("patientId"),
                    doc.getString("name"),
                    doc.getInteger("age"),
                    doc.getString("disease"),
                    doc.getDouble("treatmentCost"),
                    doc.getBoolean("insured") ? "Yes" : "No",
                    df.format(doc.getDouble("finalBill")),
                    false
            });
        }
    }

    private void addPatient() {

        try {
            String name = tName.getText().trim();
            String disease = tDisease.getText().trim();
            if (name.isEmpty() || disease.isEmpty() || tAge.getText().trim().isEmpty() || tCost.getText().trim().isEmpty()) {
                throw new InvalidInputException("All fields are required.");
            }
            if (!name.matches("^[a-zA-Z ]+$")) {
                throw new InvalidInputException("Name must only contain letters and spaces.");
            }
            int age = Integer.parseInt(tAge.getText().trim());
            double cost = Double.parseDouble(tCost.getText().trim());
            if (age <= 0 || cost <= 0) {
                throw new InvalidInputException("Age and Cost must be positive values.");
            }

            int newPatientId = MongoDBConnection.getNextPatientId();
            boolean insured = yes.isSelected();
            Patient p = insured ? new InsuredPatient(newPatientId, name, age, disease, cost)
                                : new Patient(newPatientId, name, age, disease, cost);

            Document doc = new Document("patientId", p.patientId)
                    .append("name", p.name)
                    .append("age", p.age)
                    .append("disease", p.disease)
                    .append("treatmentCost", p.treatmentCost)
                    .append("insured", insured)
                    .append("finalBill", p.getFinalBill());

            MongoDBConnection.getPatientsCollection().insertOne(doc);
            JOptionPane.showMessageDialog(this, "✅ Patient P-" + newPatientId + " Added Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshTable();
            clearForm();

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "⚠ Error: Please enter valid numbers for Age and Cost.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (InvalidInputException iie) {
            JOptionPane.showMessageDialog(this, "⚠ Error: " + iie.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "⚠ Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePatient() {
        if (selectedPatientObjectId == null) {
            JOptionPane.showMessageDialog(this, "⚠ Please select a patient from the table to update.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            double newCost = Double.parseDouble(tCost.getText().trim());
            boolean isInsured = yes.isSelected();
            double finalBill = isInsured ? newCost * (1 - 0.15) : newCost;

            Document updatedDoc = new Document("name", tName.getText().trim())
                    .append("age", Integer.parseInt(tAge.getText().trim()))
                    .append("disease", tDisease.getText().trim())
                    .append("treatmentCost", newCost)
                    .append("insured", isInsured)
                    .append("finalBill", finalBill);

            UpdateResult result = MongoDBConnection.getPatientsCollection().updateOne(
                    Filters.eq("_id", selectedPatientObjectId),
                    new Document("$set", updatedDoc)
            );

            if (result.getModifiedCount() > 0) {
                JOptionPane.showMessageDialog(this, "✅ Patient Updated Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "⚠ No changes were made.", "Update Information", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "⚠ Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void dischargeSelectedPatients() {
        List<ObjectId> idsToDischarge = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isChecked = (Boolean) tableModel.getValueAt(i, 8);
            if (isChecked != null && isChecked) {
                idsToDischarge.add((ObjectId) tableModel.getValueAt(i, 0));
            }
        }

        if (idsToDischarge.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠ No patients selected for discharge. Please tick the checkboxes.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to discharge the selected " + idsToDischarge.size() + " patient(s)?",
                "Confirm Discharge", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            DeleteResult result = MongoDBConnection.getPatientsCollection().deleteMany(Filters.in("_id", idsToDischarge));
            if (result.getDeletedCount() > 0) {
                JOptionPane.showMessageDialog(this, "✅ " + result.getDeletedCount() + " Patient(s) Discharged Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshTable();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "⚠ Could not discharge patients.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void searchPatientById() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Patient ID to Search (e.g., P-1,2,....):");
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                int patientId = Integer.parseInt(idStr.trim().replace("P-", ""));
                Document doc = MongoDBConnection.getPatientsCollection().find(Filters.eq("patientId", patientId)).first();
                if (doc != null) {
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        String tableId = tableModel.getValueAt(i, 1).toString().replace("P-", "");
                        if (patientId == Integer.parseInt(tableId)) {
                            table.setRowSelectionInterval(i, i);
                            table.scrollRectToVisible(table.getCellRect(i, 0, true));
                            return;
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "⚠ Patient ID " + patientId + " not found!", "Search Result", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "⚠ Invalid ID. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        tName.setText("");
        tAge.setText("");
        tDisease.setText("");
        tCost.setText("");
        no.setSelected(true);
        table.clearSelection();
        selectedPatientObjectId = null;
        updateBtn.setEnabled(false);
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Nimbus LaF not available, using default.");
        }
        SwingUtilities.invokeLater(HospitalManagementSystem::new);
    }
}

// Custom Panels
class BackgroundImagePanel extends JPanel {
    private BufferedImage backgroundImage;
    public BackgroundImagePanel(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Background image not found at: " + resourcePath);
                setBackground(new Color(33, 66, 99));
            } else {
                backgroundImage = ImageIO.read(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setBackground(new Color(33, 66, 99));
        }
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
        }
    }
}
class RoundedPanel extends JPanel {
    private final int cornerRadius;
    public RoundedPanel(int radius) {
        this.cornerRadius = radius;
        setOpaque(false);
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension arcs = new Dimension(cornerRadius, cornerRadius);
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(getBackground());
        graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);
    }
}