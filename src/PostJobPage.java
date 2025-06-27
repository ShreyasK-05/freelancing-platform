import java.awt.*;
import java.sql.*;
import java.util.Calendar;
import java.util.Date;
import javax.swing.*;

public class PostJobPage extends JFrame {

    private int userId;
    private JTextField projectNameField;
    private JTextArea projectDescriptionArea;
    private JTextField skillsField;
    private JRadioButton payByHourButton, payFixedButton;
    private JSpinner timelineSpinner;
    private JCheckBox confirmCheckBox;
    private JButton submitButton;

    public PostJobPage(int userId) { 
        this.userId = userId; 
        setTitle("Post a Job");
        setSize(700, 600); // Increased window size for better visibility
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    
        // Set a gradient background color
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(58, 123, 213); // Freelancer-like blue
                Color color2 = new Color(58, 183, 233); // Lighter blue
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());
        add(backgroundPanel);
    
        // Create main panel with GridBagLayout for form alignment
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE); // Set the main panel to have a white background
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding for aesthetics
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
    
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Project Name:"), gbc);
        projectNameField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(projectNameField, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Project Description:"), gbc);
        projectDescriptionArea = new JTextArea(5, 20);
        gbc.gridx = 1;
        panel.add(new JScrollPane(projectDescriptionArea), gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Required Skills (comma separated):"), gbc);
        skillsField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(skillsField, gbc);
    
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Payment Options:"), gbc);
        payByHourButton = new JRadioButton("Pay by Hour");
        payFixedButton = new JRadioButton("Pay Fixed Price");
        ButtonGroup paymentGroup = new ButtonGroup();
        paymentGroup.add(payByHourButton);
        paymentGroup.add(payFixedButton);
    
        JPanel paymentPanel = new JPanel();
        paymentPanel.setOpaque(false); // Transparent background for payment options
        paymentPanel.add(payByHourButton);
        paymentPanel.add(payFixedButton);
        gbc.gridx = 1;
        panel.add(paymentPanel, gbc);
    
        // Budget Input Field
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Budget:"), gbc);
        JTextField budgetField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(budgetField, gbc);
    
        // Timeline Picker
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("Timeline (Due Date):"), gbc);
        SpinnerDateModel dateModel = new SpinnerDateModel();
        dateModel.setCalendarField(Calendar.DAY_OF_MONTH);
        timelineSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(timelineSpinner, "yyyy-MM-dd");
        timelineSpinner.setEditor(dateEditor);
        gbc.gridx = 1;
        panel.add(timelineSpinner, gbc);
    
        // Confirmation Checkbox
        confirmCheckBox = new JCheckBox("I confirm the details are correct");
        confirmCheckBox.setOpaque(false); // Transparent background for checkbox
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        panel.add(confirmCheckBox, gbc);
    
        // Submit button centered
        submitButton = new JButton("Submit");
        submitButton.setEnabled(false);
        submitButton.setBackground(new Color(58, 123, 213));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Arial", Font.BOLD, 14));
        confirmCheckBox.addActionListener(e -> submitButton.setEnabled(confirmCheckBox.isSelected()));
    
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER; // Center-align submit button
        panel.add(submitButton, gbc);
    
        backgroundPanel.add(panel); // Add form panel to background panel
    
        submitButton.addActionListener(e -> {
            if (confirmCheckBox.isSelected()) {
                Date selectedDate = (Date) timelineSpinner.getValue();
                Date currentDate = new Date();
                if (selectedDate.after(currentDate)) {
                    insertJobDetails(budgetField.getText());
                } else {
                    JOptionPane.showMessageDialog(this, "Please select a future date for the timeline.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please confirm the details.");
            }
        });
    
        setVisible(true);
    }
    
    private void insertJobDetails(String budget) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO jobs (job_title, job_description, payment_type, timeline, skills, user_id, payment) VALUES (?, ?, ?, ?, ?, ?, ?)"; // Updated SQL
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, projectNameField.getText());
                pstmt.setString(2, projectDescriptionArea.getText());
                pstmt.setString(3, getPaymentType());
                pstmt.setDate(4, new java.sql.Date(((Date) timelineSpinner.getValue()).getTime()));
                pstmt.setArray(5, conn.createArrayOf("text", skillsField.getText().trim().split("\\s*,\\s*")));
                pstmt.setInt(6, userId); // Set user_id from the constructor parameter
                pstmt.setDouble(7, Double.parseDouble(budget)); 
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Job posted successfully!");
                clearFields();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error posting job: " + e.getMessage());
        }
    }

    private String getPaymentType() {
        return payByHourButton.isSelected() ? "hourly" : "fixed"; // Match the database values
    }

    private void clearFields() {
        projectNameField.setText("");
        projectDescriptionArea.setText("");
        skillsField.setText("");
        payByHourButton.setSelected(false);
        payFixedButton.setSelected(false);
        timelineSpinner.setValue(new Date());
        confirmCheckBox.setSelected(false);
        submitButton.setEnabled(false);
    }

    public static void main(String[] args) {
        // Example usage: pass the current user's ID
        SwingUtilities.invokeLater(() -> new PostJobPage(1)); // Replace 1 with the actual user ID
    }
}