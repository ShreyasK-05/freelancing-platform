/* import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class UploadResume extends JFrame {
    private int userId;
    private JTextField filePathField;
    private JButton uploadButton;

    public UploadResume(int userId) {
        this.userId = userId;
        setTitle("Upload Resume");
        setSize(500, 250); // Increased size for more space
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main content panel with gradient background
        JPanel gradientPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();
                Color color1 = new Color(85, 173, 238);
                Color color2 = new Color(50, 115, 220);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
            }
        };
        gradientPanel.setLayout(new GridBagLayout());

        // Content panel with a white background
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE); // White background for the content panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Select PDF Resume:"), gbc);

        filePathField = new JTextField(20);
        filePathField.setEditable(false); // Make the field non-editable, only set by "Browse"
        gbc.gridx = 1;
        panel.add(filePathField, gbc);

        JButton browseButton = new JButton("Browse");
        browseButton.setBackground(new Color(58, 123, 213)); // Button color
        browseButton.setForeground(Color.WHITE); // Button text color
        browseButton.setFont(new Font("Arial", Font.BOLD, 12));
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
                uploadButton.setEnabled(true); // Enable "Upload" button when a file is selected
            }
        });
        gbc.gridx = 2;
        panel.add(browseButton, gbc);

        // Center-align and style the "Upload" button
        uploadButton = new JButton("Upload");
        uploadButton.setEnabled(false); // Disabled until a file is selected
        uploadButton.setBackground(new Color(58, 123, 213)); // Set the same blue color as browse button
        uploadButton.setForeground(Color.WHITE);
        uploadButton.setFont(new Font("Arial", Font.BOLD, 14));
        uploadButton.addActionListener(e -> uploadResume());

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3; // Center and make the button span all columns
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(uploadButton, gbc);

        // Add white content panel to the gradient background panel
        gradientPanel.add(panel);
        add(gradientPanel);

        setVisible(true);
    }

    
    private void uploadResume() {
        String filePath = filePathField.getText();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file.");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection();
             FileInputStream fis = new FileInputStream(new File(filePath))) {
             
            String sql = "UPDATE users SET resume = ? WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setBinaryStream(1, fis, (int) new File(filePath).length());
            pstmt.setInt(2, userId);
            
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "Resume uploaded successfully!");
                dispose(); // Close the window
            } else {
                JOptionPane.showMessageDialog(this, "Failed to upload resume.");
            }
        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
 */
import java.awt.*;
import java.io.*;
import java.sql.*;
import javax.swing.*;

public class UploadResume extends JFrame {
    private int userId;
    private JTextField filePathField;
    private JButton uploadButton;

    public UploadResume(int userId) {
        this.userId = userId;
        setTitle("Upload Resume");
        setSize(500, 250); // Increased size for more space
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main content panel with gradient background
        JPanel gradientPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();
                Color color1 = new Color(85, 173, 238);
                Color color2 = new Color(50, 115, 220);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
            }
        };
        gradientPanel.setLayout(new GridBagLayout());

        // Content panel with a white background
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE); // White background for the content panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Select PDF Resume:"), gbc);

        filePathField = new JTextField(20);
        filePathField.setEditable(false); 
        gbc.gridx = 1;
        panel.add(filePathField, gbc);

        JButton browseButton = new JButton("Browse");
        browseButton.setBackground(new Color(58, 123, 213)); // Button color
        browseButton.setForeground(Color.WHITE); // Button text color
        browseButton.setFont(new Font("Arial", Font.BOLD, 12));
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
                uploadButton.setEnabled(true);
            }
        });
        gbc.gridx = 2;
        panel.add(browseButton, gbc);

        // Center-align and style the "Upload" button
        uploadButton = new JButton("Upload");
        uploadButton.setEnabled(false); // Disabled until a file is selected
        uploadButton.setBackground(new Color(58, 123, 213)); // Set the same blue color as browse button
        uploadButton.setForeground(Color.WHITE);
        uploadButton.setFont(new Font("Arial", Font.BOLD, 14));
        uploadButton.addActionListener(e -> uploadResume());

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3; // Center and make the button span all columns
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(uploadButton, gbc);

        // Add white content panel to the gradient background panel
        gradientPanel.add(panel);
        add(gradientPanel);

        setVisible(true);
    }

    private void uploadResume() {
        String filePath = filePathField.getText();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file.");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection();
             FileInputStream fis = new FileInputStream(new File(filePath))) {
             
            
            String sql = "UPDATE users SET resume = ? WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setBinaryStream(1, fis, (int) new File(filePath).length());
            pstmt.setInt(2, userId);
            
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "Resume uploaded successfully!");
                parseAndUploadResume(filePath); // Call method to parse and upload resume
                dispose(); // Close the window
            } else {
                JOptionPane.showMessageDialog(this, "Failed to upload resume.");
            }
        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // New method to call the Python script for parsing and uploading the resume
    private void parseAndUploadResume(String filePath) {
        try {
            // Construct the command to run the Python script
            String command = String.format("python parse_and_upload_resume.py \"%s\" %d", filePath, userId);

            // Use ProcessBuilder to execute the command
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            
            // Optionally, redirect output and error streams to the console for better logging
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read and log the output of the script (if needed)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // Log or process the output
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                JOptionPane.showMessageDialog(this, "Resume data parsed and uploaded successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Error parsing and uploading resume.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
