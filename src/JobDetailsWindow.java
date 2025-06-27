/* import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JobDetailsWindow extends JFrame {

    private Job job;

    public JobDetailsWindow(Job job) {
        this.job = job;

        // Set frame properties
        setTitle("Job Details - " + job.getJobTitle());
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Main panel for job details
        JPanel detailsPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Job Title
        detailsPanel.add(new JLabel("Job Title:"));
        detailsPanel.add(new JLabel(job.getJobTitle()));

        // Job Description
        

        // Create a JTextArea for displaying job description, allowing word wrap and scrollability
        JTextArea descriptionArea = new JTextArea(job.getJobDescription());
        descriptionArea.setLineWrap(true);  // Enable line wrapping
        descriptionArea.setWrapStyleWord(true);  // Wrap at word boundaries
        descriptionArea.setFont(new Font("Arial", Font.PLAIN, 14));
        descriptionArea.setEditable(false);  // Make it read-only

        // Put the JTextArea in a JScrollPane to handle overflow content
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);
        descriptionScrollPane.setPreferredSize(new Dimension(500, 200));  // Set a preferred size for the scroll pane
        descriptionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Add the JScrollPane (containing the description) to your layout
        detailsPanel.add(new JLabel("Description:"));
        detailsPanel.add(descriptionScrollPane);  // Add the JScrollPane here


        // Payment
        detailsPanel.add(new JLabel("Budget (Payment):"));
        detailsPanel.add(new JLabel("$" + job.getPayment().toString()));

        // Timeline
        detailsPanel.add(new JLabel("Timeline:"));
        detailsPanel.add(new JLabel(job.getTimeline().toString()));

        // Skills
        detailsPanel.add(new JLabel("Skills Required:"));
        detailsPanel.add(new JLabel(job.getSkills()));

        // Add detailsPanel to frame
        add(detailsPanel, BorderLayout.CENTER);

        // Panel for user input (bidding and proposal)
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Bid Amount input
        inputPanel.add(new JLabel("Enter Bid Amount:"));
        JTextField bidAmountField = new JTextField();
        inputPanel.add(bidAmountField);

        // Estimated Time input
        inputPanel.add(new JLabel("Estimated Time (in days):"));
        JTextField timeField = new JTextField();
        inputPanel.add(timeField);

        // Proposal Description input
        inputPanel.add(new JLabel("Proposal Description (min 100 characters):"));
        JTextArea proposalField = new JTextArea();
        proposalField.setWrapStyleWord(true);
        proposalField.setLineWrap(true);
        JScrollPane proposalScrollPane = new JScrollPane(proposalField);
        inputPanel.add(proposalScrollPane);

        // Submit button
        JButton submitButton = new JButton("Submit Proposal");
        inputPanel.add(submitButton);

        // Add inputPanel to frame
        add(inputPanel, BorderLayout.SOUTH);

        // Submit button action
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String bidAmountStr = bidAmountField.getText();
                String timeStr = timeField.getText();
                String proposal = proposalField.getText();

                if (proposal.length() < 100) {
                    JOptionPane.showMessageDialog(null, "Proposal description must be at least 100 characters long.");
                    return;
                }

                try {
                    BigDecimal bidAmount = new BigDecimal(bidAmountStr);
                    int estimatedTime = Integer.parseInt(timeStr);

                    // Call method to insert proposal into database
                    submitProposal(job.getJobId(), bidAmount, estimatedTime, proposal);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Please enter valid numbers for bid amount and estimated time.");
                }
            }
        });

        // Make the frame visible
        setVisible(true);
    }

    // Method to submit proposal to the database
    private void submitProposal(int jobId, BigDecimal bidAmount, int estimatedTime, String proposal) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "INSERT INTO proposals (job_id, bid_amount, estimated_time, proposal_text) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, jobId);
            stmt.setBigDecimal(2, bidAmount);
            stmt.setInt(3, estimatedTime);
            stmt.setString(4, proposal);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                JOptionPane.showMessageDialog(this, "Proposal submitted successfully!");
                this.dispose(); // Close the window after submission
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Print the stack trace to the console
            JOptionPane.showMessageDialog(this, "Error submitting proposal: " + e.getMessage());
        }
    }
}
  */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JobDetailsWindow extends JFrame {
    private Job job;
    private int freelancerId;
    private JTextField skillsField;
    private JTextArea proposalField;

    public JobDetailsWindow(Job job, int freelancerId) {
        this.job = job;
        this.freelancerId = freelancerId;

        // Set frame properties
        setTitle("Job Details - " + job.getJobTitle());
        setSize(1500, 1500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Gradient background panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(58, 123, 213);
                Color color2 = new Color(58, 183, 233);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());
        setContentPane(backgroundPanel);

        // Main panel for job details and input
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setPreferredSize(new Dimension(600, 600));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Job details panel with minimal spacing
        JPanel detailsPanel = new JPanel(new GridLayout(5, 2, 4, 4));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Job Details"));

        // Job details components
        detailsPanel.add(new JLabel("Job Title:"));
        detailsPanel.add(new JLabel(job.getJobTitle()));

        detailsPanel.add(new JLabel("Description:"));
        JTextArea descriptionArea = new JTextArea(job.getJobDescription());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Arial", Font.PLAIN, 14));
        descriptionArea.setEditable(false);
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);
        descriptionScrollPane.setPreferredSize(new Dimension(350, 60));
        detailsPanel.add(descriptionScrollPane);

        detailsPanel.add(new JLabel("Budget (Payment):"));
        detailsPanel.add(new JLabel("$" + job.getPayment().toString()));

        detailsPanel.add(new JLabel("Timeline:"));
        detailsPanel.add(new JLabel(job.getTimeline().toString()));

        detailsPanel.add(new JLabel("Skills Required:"));
        detailsPanel.add(new JLabel(job.getSkills()));

        mainPanel.add(detailsPanel, BorderLayout.NORTH);

        // Input panel for proposal details with minimal spacing
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 4, 4));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Proposal Input"));

        // Bid Amount input field
        inputPanel.add(new JLabel("Enter Bid Amount:", SwingConstants.RIGHT));
        JTextField bidAmountField = new JTextField(5);  // Smaller field width
        inputPanel.add(bidAmountField);

        // Skills Possessed input field (instance variable now)
        inputPanel.add(new JLabel("Skills Possessed (comma-separated):", SwingConstants.RIGHT));
        skillsField = new JTextField(20);  // Adjust the field width as necessary
        inputPanel.add(skillsField);

        // Estimated Time input field
        inputPanel.add(new JLabel("Estimated Time (in days):", SwingConstants.RIGHT));
        JTextField timeField = new JTextField(5);  // Smaller field width
        inputPanel.add(timeField);

        // Proposal Description input field (instance variable now)
        inputPanel.add(new JLabel("Proposal Description (min 100 characters):", SwingConstants.RIGHT));
        proposalField = new JTextArea(3, 20);
        proposalField.setWrapStyleWord(true);
        proposalField.setLineWrap(true);
        proposalField.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane proposalScrollPane = new JScrollPane(proposalField);
        proposalScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputPanel.add(proposalScrollPane);

        mainPanel.add(inputPanel, BorderLayout.CENTER);

        JButton submitButton = new JButton("Submit Proposal");
        submitButton.setBackground(new Color(58, 123, 213));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Arial", Font.BOLD, 15));
        submitButton.setFocusPainted(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(submitButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        backgroundPanel.add(mainPanel, gbc);

        pack();
        setVisible(true);

        // Fetch and populate resume data in the fields
        fillResumeData();

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String bidAmountStr = bidAmountField.getText();
                String timeStr = timeField.getText();
                String proposal = proposalField.getText();
                String possessedSkills = skillsField.getText();

                if (proposal.length() < 100) {
                    JOptionPane.showMessageDialog(null, "Proposal description must be at least 100 characters long.");
                    return;
                }

                try {
                    BigDecimal bidAmount = new BigDecimal(bidAmountStr);
                    int estimatedTime = Integer.parseInt(timeStr);
                    String[] skillsArray = possessedSkills.split("\\s*,\\s*");
                    // Call method to insert proposal into database
                    submitProposal(job.getJobId(), bidAmount, estimatedTime, proposal, skillsArray);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Please enter valid numbers for bid amount and estimated time.");
                }
            }
        });
    }

    private void fillResumeData() {
        // SQL query to fetch skills and experience from resume
        String sql = "SELECT skills, experience FROM resume WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, freelancerId); // Set freelancer ID
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String skills = rs.getString("skills");
                String experience = rs.getString("experience");

                // Set the skills and experience in the respective text fields
                skillsField.setText(skills != null ? skills : "No skills found");
                proposalField.setText(experience != null ? experience : "No experience found");
            } else {
                skillsField.setText("No skills found");
                proposalField.setText("No experience found");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching resume data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    

    private void submitProposal(int jobId, BigDecimal bidAmount, int estimatedTime, String proposal, String[] skillsArray) {
    try (Connection conn = DatabaseConnection.getConnection()) {
        // Retrieve the resume of the logged-in user
        byte[] resume = getResume(conn, freelancerId); // Assuming freelancerId is the logged-in user's ID

        // SQL query to insert proposal with freelancer_id as the logged-in user's ID
        String query = "INSERT INTO proposals (job_id, freelancer_id, bid_amount, estimated_time, proposal_text, resume, proposal_status, possessed_skills) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"; // Include resume in the query
        PreparedStatement stmt = conn.prepareStatement(query);

        // Set parameters for the prepared statement
        stmt.setInt(1, jobId);               // job_id from Job object
        stmt.setInt(2, freelancerId);         // ID of the logged-in user for freelancer_id
        stmt.setBigDecimal(3, bidAmount);     // bid amount from input field
        stmt.setInt(4, estimatedTime);        // estimated time from input field
        stmt.setString(5, proposal);          // proposal description from input field
        stmt.setBytes(6, resume);             // Add the resume to the insert
        stmt.setString(7, "pending");         // Set proposal_status to "pending" by default
        stmt.setArray(8, conn.createArrayOf("VARCHAR", skillsArray));
    
        // Execute the update and confirm submission
        int rowsInserted = stmt.executeUpdate();
        if (rowsInserted > 0) {
            JOptionPane.showMessageDialog(this, "Proposal submitted successfully!");
            this.dispose(); // Close the window after submission
        }
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error submitting proposal: " + e.getMessage());
    }
}

// Method to get the resume from the users table
private byte[] getResume(Connection conn, int freelancerId) throws SQLException {
    String sql = "SELECT resume FROM users WHERE id = ?";
    PreparedStatement pstmt = conn.prepareStatement(sql);
    pstmt.setInt(1, freelancerId);
    ResultSet rs = pstmt.executeQuery();
    if (rs.next()) {
        return rs.getBytes("resume");
    }
    return null; // Return null if no resume found
}

}
