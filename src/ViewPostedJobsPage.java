/*import java.awt.*;
import java.sql.*;
import javax.swing.*;
import java.util.*;
import java.util.Date;
import java.util.List;

public class ViewPostedJobsPage extends JFrame {
    private int userId;

    public ViewPostedJobsPage(int userId) {
        this.userId = userId;
        setTitle("View Posted Jobs");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set a clean white background for the UI
        JPanel jobsPanel = new JPanel();
        jobsPanel.setLayout(new BoxLayout(jobsPanel, BoxLayout.Y_AXIS));
        jobsPanel.setBackground(Color.WHITE); // White background for better appearance

        JScrollPane scrollPane = new JScrollPane(jobsPanel);
        add(scrollPane, BorderLayout.CENTER);

        loadJobs(jobsPanel);
        setVisible(true);
    }

    private void loadJobs(JPanel jobsPanel) {
        String jobsQuery = "SELECT j.job_id, j.job_title, j.job_description, j.payment_type, j.timeline, j.status, p.proposal_id, j.skills " +
                           "FROM jobs j " +
                           "LEFT JOIN proposals p ON j.job_id = p.job_id AND p.proposal_status = 'accepted' " +
                           "WHERE j.user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(jobsQuery)) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int jobId = resultSet.getInt("job_id");
                String jobTitle = resultSet.getString("job_title");
                String jobDescription = resultSet.getString("job_description");
                String paymentType = resultSet.getString("payment_type");
                Date timeline = resultSet.getDate("timeline");
                boolean status = resultSet.getBoolean("status");
                String jobSkills = resultSet.getString("skills"); // skills from the job
                Integer proposalId = (Integer) resultSet.getObject("proposal_id"); // Null if no proposal is accepted

                // Fetch proposals sorted by combined score
                List<Proposal> proposals = getSortedProposals(jobId, jobSkills);

                // Create job panel
                JPanel jobPanel = new JPanel();
                jobPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10)); // Side-by-side layout
                jobPanel.setBackground(Color.WHITE);
                jobPanel.setPreferredSize(new Dimension(450, 120));

                // Job Details (Left side)
                JPanel jobDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); // Arrange details side-by-side
                jobDetailsPanel.setBackground(Color.WHITE);

                // Display job details in a single row
                JLabel jobLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>" + jobTitle + "</span></html>");
                jobLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 14));

                JLabel timelineLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Timeline: " + timeline + "</span></html>");
                timelineLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                jobDetailsPanel.add(jobLabel);
                jobDetailsPanel.add(timelineLabel);

                jobPanel.add(jobDetailsPanel);

                // Check if job is accepted and show proposals
                if (status) {
                    String freelancerName = getFreelancerName(jobId);
                    JLabel freelancerLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#28A745;'>Freelancer: " + freelancerName + "</span></html>");
                    freelancerLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
                    jobPanel.add(freelancerLabel);

                    // Show proposals sorted by combined score
                    showProposalsForJob(jobPanel, proposals);
                } else {
                    // View Proposals Button
                    JButton viewProposalsButton = new JButton("View Proposals");
                    viewProposalsButton.setBackground(new Color(0, 123, 255));
                    viewProposalsButton.setForeground(Color.WHITE);
                    viewProposalsButton.setFocusPainted(false);
                    viewProposalsButton.setOpaque(true);
                    viewProposalsButton.setBorderPainted(false);
                    viewProposalsButton.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
                    viewProposalsButton.addActionListener(e -> showProposalsForJob(jobPanel, proposals));
                    jobPanel.add(Box.createHorizontalGlue());  // Push button to the right
                    jobPanel.add(viewProposalsButton);
                }

                jobsPanel.add(jobPanel);
                jobsPanel.add(Box.createVerticalStrut(10)); // Add vertical spacing between job panels
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading jobs: " + e.getMessage());
        }
    }

    private List<Proposal> getSortedProposals(int jobId, String jobSkills) {
        List<Proposal> proposals = new ArrayList<>();
        String proposalsQuery = "SELECT p.proposal_id, p.freelancer_id, u.name AS freelancer_name, p.bid_amount, p.estimated_time, p.rating, p.possessed_skills " +
                                "FROM proposals p " +
                                "JOIN users u ON p.freelancer_id = u.id " +
                                "WHERE p.job_id = ? AND (p.proposal_status = 'accepted')";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(proposalsQuery)) {

            statement.setInt(1, jobId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int proposalId = resultSet.getInt("proposal_id");
                int freelancerId = resultSet.getInt("freelancer_id");
                String freelancerName = resultSet.getString("freelancer_name");
                double bidAmount = resultSet.getDouble("bid_amount");
                int estimatedTime = resultSet.getInt("estimated_time");
                double rating = resultSet.getDouble("rating");
                String possessedSkills = resultSet.getString("possessed_skills");

                // Calculate skill similarity score
                double skillSimilarityScore = calculateSkillSimilarity(jobSkills, possessedSkills);

                // Calculate combined score: 70% skill similarity, 30% rating
                double combinedScore = (0.7 * skillSimilarityScore) + (0.3 * rating);

                proposals.add(new Proposal(proposalId, freelancerId, freelancerName, bidAmount, estimatedTime, rating, possessedSkills, combinedScore));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading proposals: " + e.getMessage());
        }

        // Sort proposals by combined score in descending order
        proposals.sort(Comparator.comparingDouble(Proposal::getCombinedScore).reversed());

        return proposals;
    }

    private double calculateSkillSimilarity(String jobSkills, String possessedSkills) {
        Set<String> jobSkillsSet = new HashSet<>(Arrays.asList(jobSkills.split(",")));
        Set<String> possessedSkillsSet = new HashSet<>(Arrays.asList(possessedSkills.split(",")));

        Set<String> commonSkills = new HashSet<>(jobSkillsSet);
        commonSkills.retainAll(possessedSkillsSet);

        return (double) commonSkills.size() / jobSkillsSet.size(); // Normalized common skills
    }

    private void showProposalsForJob(JPanel jobPanel, List<Proposal> proposals) {
        JPanel proposalsPanel = new JPanel();
        proposalsPanel.setLayout(new BoxLayout(proposalsPanel, BoxLayout.Y_AXIS));
        proposalsPanel.setBackground(Color.WHITE);

        // Add proposals to the panel based on sorted order
        for (Proposal proposal : proposals) {
            JPanel proposalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
            proposalPanel.setBackground(Color.WHITE);

            JLabel freelancerLabel = new JLabel("Freelancer: " + proposal.getFreelancerName());
            JLabel bidLabel = new JLabel("Bid: $" + proposal.getBidAmount());
            JLabel skillScoreLabel = new JLabel("Skill Similarity: " + proposal.getSkillSimilarityScore());
            JLabel ratingLabel = new JLabel("Rating: " + proposal.getRating());

            proposalPanel.add(freelancerLabel);
            proposalPanel.add(bidLabel);
            proposalPanel.add(skillScoreLabel);
            proposalPanel.add(ratingLabel);

            proposalsPanel.add(proposalPanel);
        }

        JScrollPane scrollPane = new JScrollPane(proposalsPanel);
        jobPanel.add(scrollPane);
        jobPanel.revalidate();
    }

    private String getFreelancerName(int jobId) {
        String query = "SELECT u.name FROM users u " +
                       "JOIN proposals p ON u.id = p.freelancer_id " +
                       "WHERE p.job_id = ? AND p.proposal_status = 'accepted'";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, jobId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "No Freelancer Found";
    }

    // Proposal class to store the necessary proposal data
    class Proposal {
        private int proposalId;
        private int freelancerId;
        private String freelancerName;
        private double bidAmount;
        private int estimatedTime;
        private double rating;
        private String possessedSkills;
        private double combinedScore;
        private String jobSkills;

        public Proposal(int proposalId, int freelancerId, String freelancerName, double bidAmount, int estimatedTime, double rating, String possessedSkills, double combinedScore) {
            this.proposalId = proposalId;
            this.freelancerId = freelancerId;
            this.freelancerName = freelancerName;
            this.bidAmount = bidAmount;
            this.estimatedTime = estimatedTime;
            this.rating = rating;
            this.possessedSkills = possessedSkills;
            this.combinedScore = combinedScore;
        }

        public String getFreelancerName() {
            return freelancerName;
        }

        public double getCombinedScore() {
            return combinedScore;
        }

        public double getSkillSimilarityScore() {
            return calculateSkillSimilarity(jobSkills, possessedSkills);  // Ensure the method takes correct arguments
        }


        public double getBidAmount() {
            return bidAmount;
        }

        public double getRating() {
            return rating;
        }

    }
} */

/*import java.awt.*;
import java.sql.*;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class ViewPostedJobsPage extends JFrame {
    private int userId;

    public ViewPostedJobsPage(int userId) {
        this.userId = userId;
        setTitle("View Posted Jobs");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set a clean white background for the UI
        JPanel jobsPanel = new JPanel();
        jobsPanel.setLayout(new BoxLayout(jobsPanel, BoxLayout.Y_AXIS));
        jobsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(jobsPanel);
        add(scrollPane, BorderLayout.CENTER);

        loadJobs(jobsPanel);
        setVisible(true);
    }

    private void loadJobs(JPanel jobsPanel) {
        String jobsQuery = "SELECT j.job_id, j.job_title, j.job_description, j.payment_type, j.timeline, j.status, j.skills " +
                           "FROM jobs j " +
                           "WHERE j.user_id = ?";
    
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(jobsQuery)) {
    
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
    
            while (resultSet.next()) {
                int jobId = resultSet.getInt("job_id");
                String jobTitle = resultSet.getString("job_title");
                String jobDescription = resultSet.getString("job_description");
                String paymentType = resultSet.getString("payment_type");
                java.sql.Date timeline = resultSet.getDate("timeline");
                boolean status = resultSet.getBoolean("status");
                String jobSkills = resultSet.getString("skills");  // Job skills column
    
                JPanel jobPanel = new JPanel();
                jobPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));
                jobPanel.setBackground(Color.WHITE);
                jobPanel.setPreferredSize(new Dimension(450, 120));
    
                // Job Details (Left side)
                JPanel jobDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
                jobDetailsPanel.setBackground(Color.WHITE);
    
                JLabel jobLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>" + jobTitle + "</span></html>");
                jobLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 14));
    
                JLabel timelineLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Timeline: " + timeline + "</span></html>");
                timelineLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
    
                jobDetailsPanel.add(jobLabel);
                jobDetailsPanel.add(timelineLabel);
    
                jobPanel.add(jobDetailsPanel);
    
                // View Proposals Button
                JButton viewProposalsButton = new JButton("View Proposals");
                viewProposalsButton.setBackground(new Color(0, 123, 255)); // Blue background
                viewProposalsButton.setForeground(Color.WHITE);
                viewProposalsButton.setFocusPainted(false);
                viewProposalsButton.setOpaque(true);
                viewProposalsButton.setBorderPainted(false);
                viewProposalsButton.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
                viewProposalsButton.addActionListener(e -> showProposalsForJob(jobId, jobSkills));
    
                jobPanel.add(viewProposalsButton);
    
                jobsPanel.add(jobPanel);
                jobsPanel.add(Box.createVerticalStrut(10)); // Add vertical spacing between job panels
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading jobs: " + e.getMessage());
        }
    }

    // Show proposals for a job, sorted by combined score
    private void showProposalsForJob(int jobId, String jobSkills) {
        JFrame proposalsFrame = new JFrame("Proposals for Job ID: " + jobId);
        proposalsFrame.setSize(600, 400);
        proposalsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        proposalsFrame.setLocationRelativeTo(this);

        JPanel proposalsPanel = new JPanel();
        proposalsPanel.setLayout(new BoxLayout(proposalsPanel, BoxLayout.Y_AXIS));
        proposalsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(proposalsPanel);
        proposalsFrame.add(scrollPane, BorderLayout.CENTER);

        loadProposalsForJob(jobId, jobSkills, proposalsPanel);

        proposalsFrame.setVisible(true);
    }

    // Load proposals for a job, and sort them based on skill similarity and rating
    private void loadProposalsForJob(int jobId, String jobSkills, JPanel proposalsPanel) {
        proposalsPanel.removeAll();

        

                                String proposalsQuery = "SELECT p.proposal_id, p.freelancer_id, u.name AS freelancer_name, " +
                                "p.bid_amount, p.estimated_time, p.rating, p.possessed_skills, " +
                                "(cardinality(array(SELECT unnest(p.possessed_skills) " +
                                "                  INTERSECT " +
                                "                  SELECT unnest(string_to_array(r.skills, ','))))::float / " +
                                " NULLIF(cardinality(p.possessed_skills), 0)) * 0.7 + p.rating * 0.3 AS combined_score " +
                                "FROM proposals p " +
                                "JOIN users u ON p.freelancer_id = u.id " +
                                "JOIN resume r ON p.freelancer_id = r.user_id " +
                                "WHERE p.job_id = ? AND (p.proposal_status = 'pending' OR p.proposal_status IS NULL) " +
                                "AND p.freelancer_id != ? " +
                                "ORDER BY combined_score DESC";
        
 

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(proposalsQuery)) {

            statement.setInt(1, jobId);
            statement.setInt(2, userId);
            ResultSet resultSet = statement.executeQuery();

            List<Proposal> proposals = new ArrayList<>();

            while (resultSet.next()) {
                int proposalId = resultSet.getInt("proposal_id");
                int freelancerId = resultSet.getInt("freelancer_id");
                String freelancerName = resultSet.getString("freelancer_name");
                double bidAmount = resultSet.getDouble("bid_amount");
                int estimatedTime = resultSet.getInt("estimated_time");
                double rating = resultSet.getDouble("rating");
                String possessedSkills = resultSet.getString("possessed_skills");

                double skillSimilarityScore = calculateSkillSimilarity(possessedSkills, jobSkills);
                double combinedScore = 0.7 * skillSimilarityScore + 0.3 * rating;

                proposals.add(new Proposal(proposalId, freelancerId, freelancerName, bidAmount, estimatedTime, rating, possessedSkills, combinedScore));
            }

            // Sort proposals by combined score in descending order
            proposals.sort(Comparator.comparingDouble(Proposal::getCombinedScore).reversed());

            for (Proposal proposal : proposals) {
                JPanel proposalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10)); // Side-by-side layout
                proposalPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                proposalPanel.setPreferredSize(new Dimension(550, 120));
                proposalPanel.setBackground(Color.WHITE);

                JLabel freelancerLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>Freelancer: " + proposal.getFreelancerName() + "</span></html>");
                freelancerLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                JLabel bidLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Bid: $" + proposal.getBidAmount() + "</span></html>");
                bidLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                JLabel ratingLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#28A745;'>Rating: " + proposal.getRating() + "</span></html>");
                ratingLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                proposalPanel.add(freelancerLabel);
                proposalPanel.add(bidLabel);
                proposalPanel.add(ratingLabel);

                proposalsPanel.add(proposalPanel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading proposals: " + e.getMessage());
        }
    }

    // Calculate skill similarity score (simple method to calculate common skills as a percentage)
    private double calculateSkillSimilarity(String possessedSkills, String jobSkills) {
        Set<String> possessedSkillsSet = new HashSet<>(Arrays.asList(possessedSkills.split(",")));
        Set<String> jobSkillsSet = new HashSet<>(Arrays.asList(jobSkills.split(",")));

        Set<String> commonSkills = new HashSet<>(possessedSkillsSet);
        commonSkills.retainAll(jobSkillsSet);

        return (double) commonSkills.size() / jobSkillsSet.size();
    }

    // Proposal class to store proposal data
    class Proposal {
        private int proposalId;
        private int freelancerId;
        private String freelancerName;
        private double bidAmount;
        private int estimatedTime;
        private double rating;
        private String possessedSkills;
        private double combinedScore;

        public Proposal(int proposalId, int freelancerId, String freelancerName, double bidAmount, int estimatedTime, double rating, String possessedSkills, double combinedScore) {
            this.proposalId = proposalId;
            this.freelancerId = freelancerId;
            this.freelancerName = freelancerName;
            this.bidAmount = bidAmount;
            this.estimatedTime = estimatedTime;
            this.rating = rating;
            this.possessedSkills = possessedSkills;
            this.combinedScore = combinedScore;
        }

        public String getFreelancerName() {
            return freelancerName;
        }

        public double getBidAmount() {
            return bidAmount;
        }

        public double getRating() {
            return rating;
        }

        public double getCombinedScore() {
            return combinedScore;
        }
    }
}*/

//-----------------------------------------------------------------------------------------------------------------------
//with accept reject but not sorted
/* import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class ViewPostedJobsPage extends JFrame {
    private int userId;

    public ViewPostedJobsPage(int userId) {
        this.userId = userId;
        setTitle("View Posted Jobs");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set a clean white background for the UI
        JPanel jobsPanel = new JPanel();
        jobsPanel.setLayout(new BoxLayout(jobsPanel, BoxLayout.Y_AXIS));
        jobsPanel.setBackground(Color.WHITE); // White background for better appearance

        JScrollPane scrollPane = new JScrollPane(jobsPanel);
        add(scrollPane, BorderLayout.CENTER);

        loadJobs(jobsPanel);
        setVisible(true);
    }

    private void loadJobs(JPanel jobsPanel) {
        String jobsQuery = "SELECT j.job_id, j.job_title, j.job_description, j.payment_type, j.timeline, j.status, p.proposal_id " +
                           "FROM jobs j " +
                           "LEFT JOIN proposals p ON j.job_id = p.job_id AND p.proposal_status = 'accepted' " +
                           "WHERE j.user_id = ?";
    
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(jobsQuery)) {
    
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
    
            while (resultSet.next()) {
                int jobId = resultSet.getInt("job_id");
                String jobTitle = resultSet.getString("job_title");
                String jobDescription = resultSet.getString("job_description");
                String paymentType = resultSet.getString("payment_type");
                Date timeline = resultSet.getDate("timeline");
                boolean status = resultSet.getBoolean("status");
                Integer proposalId = (Integer) resultSet.getObject("proposal_id"); // Null if no proposal is accepted
    
                JPanel jobPanel = new JPanel();
                jobPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10)); // Side-by-side layout
                jobPanel.setBackground(Color.WHITE);
                jobPanel.setPreferredSize(new Dimension(450, 120));
    
                // Job Details (Left side)
                JPanel jobDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); // Arrange details side-by-side
                jobDetailsPanel.setBackground(Color.WHITE);
    
                // Display job details in a single row
                JLabel jobLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>" + jobTitle + "</span></html>");
                jobLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 14));
    
                JLabel timelineLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Timeline: " + timeline + "</span></html>");
                timelineLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
    
                jobDetailsPanel.add(jobLabel);
                jobDetailsPanel.add(timelineLabel);
    
                jobPanel.add(jobDetailsPanel);
    
                // Check if the job has been accepted
                if (status) {
                    String freelancerName = getFreelancerName(jobId); // Get freelancer name when job is accepted
                    JLabel freelancerLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#28A745;'>Freelancer: " + freelancerName + "</span></html>");
                    freelancerLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
                    jobPanel.add(freelancerLabel);
    
                    // If a proposal is accepted, show Escrow Payment button
                    if (proposalId != null) {
                        JButton escrowButton = new JButton("Escrow Payment");
                        escrowButton.addActionListener(e -> SwingUtilities.invokeLater(() -> {
                            // Convert Integer to int, with a default value of -1 if it's null
                            int validProposalId = (proposalId != null) ? proposalId : -1;
                            new EscrowPaymentSystem(jobId, validProposalId).setVisible(true); // Open Escrow window
                        }));
                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        buttonPanel.add(escrowButton);
                        jobPanel.add(buttonPanel);
                    }
                } else {
                    // View Proposals Button (Right side)
                    JButton viewProposalsButton = new JButton("View Proposals");
                    viewProposalsButton.setBackground(new Color(0, 123, 255)); // Blue background
                    viewProposalsButton.setForeground(Color.WHITE);
                    viewProposalsButton.setFocusPainted(false);
                    viewProposalsButton.setOpaque(true);
                    viewProposalsButton.setBorderPainted(false);
                    viewProposalsButton.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
                    viewProposalsButton.addActionListener(e -> showProposalsForJob(jobId));
    
                    // Add button in right side of panel
                    jobPanel.add(Box.createHorizontalGlue());  // Push button to the right
                    jobPanel.add(viewProposalsButton);
                }
    
                jobsPanel.add(jobPanel);
                jobsPanel.add(Box.createVerticalStrut(10)); // Add vertical spacing between job panels
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading jobs: " + e.getMessage());
        }
    }
    

    private String getFreelancerName(int jobId) {
        String query = "SELECT u.name FROM proposals p JOIN users u ON p.freelancer_id = u.id " +
                       "WHERE p.job_id = ? AND p.proposal_status = 'accepted'";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, jobId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("name");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Not Assigned"; // Return "Not Assigned" if no freelancer is assigned yet
    }

    private void showProposalsForJob(int jobId) {
        JFrame proposalsFrame = new JFrame("Proposals for Job ID: " + jobId);
        proposalsFrame.setSize(600, 400);
        proposalsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        proposalsFrame.setLocationRelativeTo(this);

        JPanel proposalsPanel = new JPanel();
        proposalsPanel.setLayout(new BoxLayout(proposalsPanel, BoxLayout.Y_AXIS));
        proposalsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(proposalsPanel);
        proposalsFrame.add(scrollPane, BorderLayout.CENTER);

        loadProposalsForJob(jobId, proposalsPanel, proposalsFrame);

        proposalsFrame.setVisible(true);
    }

    private void loadProposalsForJob(int jobId, JPanel proposalsPanel, JFrame proposalsFrame) {
        proposalsPanel.removeAll();

        String proposalsQuery = "SELECT p.proposal_id, p.freelancer_id, u.name AS freelancer_name, " +
                                "p.bid_amount, p.estimated_time FROM proposals p " +
                                "JOIN users u ON p.freelancer_id = u.id " +
                                "WHERE p.job_id = ? AND (p.proposal_status = 'pending' OR p.proposal_status IS NULL) " +
                                "AND p.freelancer_id != ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(proposalsQuery)) {

            statement.setInt(1, jobId);
            statement.setInt(2, userId);
            ResultSet resultSet = statement.executeQuery();

            boolean hasProposals = false;

            while (resultSet.next()) {
                hasProposals = true;
                int proposalId = resultSet.getInt("proposal_id");
                int freelancerId = resultSet.getInt("freelancer_id");
                String freelancerName = resultSet.getString("freelancer_name");
                double bidAmount = resultSet.getDouble("bid_amount");
                int estimatedTime = resultSet.getInt("estimated_time");

                JPanel proposalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10)); // Side-by-side layout
                proposalPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                proposalPanel.setPreferredSize(new Dimension(550, 120));
                proposalPanel.setBackground(Color.WHITE);

                JLabel freelancerLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>Freelancer: " + freelancerName + "</span></html>");
                freelancerLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                JLabel bidLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Bid Amount: $" + bidAmount + "</span></html>");
                bidLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                JLabel timeLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#28A745;'>Deadline: " + estimatedTime + " days</span></html>");
                timeLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                proposalPanel.add(freelancerLabel);
                proposalPanel.add(bidLabel);
                proposalPanel.add(timeLabel);

                JButton acceptButton = new JButton("Accept");
                JButton rejectButton = new JButton("Reject");

                acceptButton.setBackground(new Color(28, 200, 138));
                acceptButton.setForeground(Color.WHITE);
                rejectButton.setBackground(new Color(220, 53, 69));
                rejectButton.setForeground(Color.WHITE);

                acceptButton.addActionListener(e -> acceptProposal(jobId, proposalId));
                rejectButton.addActionListener(e -> rejectProposal(proposalId, proposalsPanel));

                proposalPanel.add(acceptButton);
                proposalPanel.add(rejectButton);

                proposalsPanel.add(proposalPanel);
            }

            if (!hasProposals) {
                JLabel noProposalsLabel = new JLabel("No proposals yet.");
                noProposalsLabel.setFont(new Font("Arial", Font.ITALIC, 14));
                proposalsPanel.add(noProposalsLabel);
            }

            proposalsPanel.revalidate();
            proposalsPanel.repaint();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(proposalsFrame, "Error loading proposals: " + e.getMessage());
        }
    }

    private void acceptProposal(int jobId, int proposalId) {
        String acceptQuery = "UPDATE proposals SET proposal_status = 'accepted' WHERE proposal_id = ?";
        String updateJobQuery = "UPDATE jobs SET status = true WHERE job_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement acceptStatement = connection.prepareStatement(acceptQuery);
             PreparedStatement updateJobStatement = connection.prepareStatement(updateJobQuery)) {

            connection.setAutoCommit(false); // Start transaction

            acceptStatement.setInt(1, proposalId);
            acceptStatement.executeUpdate();

            updateJobStatement.setInt(1, jobId);
            updateJobStatement.executeUpdate();

            connection.commit(); // Commit transaction

            JOptionPane.showMessageDialog(this, "Proposal accepted!");

            // Update job listing UI or close the proposals window
            this.dispose();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error accepting proposal: " + e.getMessage());
        }
    }

    private void rejectProposal(int proposalId, JPanel proposalsPanel) {
        String rejectQuery = "DELETE FROM proposals WHERE proposal_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(rejectQuery)) {

            statement.setInt(1, proposalId);
            statement.executeUpdate();

            // Remove rejected proposal from UI
            Component[] components = proposalsPanel.getComponents();
            for (Component component : components) {
                if (component instanceof JPanel && ((JPanel) component).getComponentCount() == 0) {
                    proposalsPanel.remove(component);
                    break;
                }
            }

            proposalsPanel.revalidate();
            proposalsPanel.repaint();

            JOptionPane.showMessageDialog(this, "Proposal rejected!");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error rejecting proposal: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ViewPostedJobsPage(1)); // Example user ID 1
    }
}  */
//--------------------------------------------------------------------------------------------------------------
/* import java.awt.*;
import java.sql.*;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class ViewPostedJobsPage extends JFrame {
    private int userId;

    public ViewPostedJobsPage(int userId) {
        this.userId = userId;
        setTitle("View Posted Jobs");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set a clean white background for the UI
        JPanel jobsPanel = new JPanel();
        jobsPanel.setLayout(new BoxLayout(jobsPanel, BoxLayout.Y_AXIS));
        jobsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(jobsPanel);
        add(scrollPane, BorderLayout.CENTER);

        loadJobs(jobsPanel);
        setVisible(true);
    }

    private void loadJobs(JPanel jobsPanel) {
        String jobsQuery = "SELECT j.job_id, j.job_title, j.job_description, j.payment_type, j.timeline, j.status, j.skills " +
                           "FROM jobs j " +
                           "WHERE j.user_id = ?";
    
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(jobsQuery)) {
    
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
    
            while (resultSet.next()) {
                int jobId = resultSet.getInt("job_id");
                String jobTitle = resultSet.getString("job_title");
                String jobDescription = resultSet.getString("job_description");
                String paymentType = resultSet.getString("payment_type");
                java.sql.Date timeline = resultSet.getDate("timeline");
                boolean status = resultSet.getBoolean("status");
                String jobSkills = resultSet.getString("skills");  // Job skills column
    
                JPanel jobPanel = new JPanel();
                jobPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));
                jobPanel.setBackground(Color.WHITE);
                jobPanel.setPreferredSize(new Dimension(450, 120));
    
                // Job Details (Left side)
                JPanel jobDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
                jobDetailsPanel.setBackground(Color.WHITE);
    
                JLabel jobLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>" + jobTitle + "</span></html>");
                jobLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 14));
    
                JLabel timelineLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Timeline: " + timeline + "</span></html>");
                timelineLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
    
                jobDetailsPanel.add(jobLabel);
                jobDetailsPanel.add(timelineLabel);
    
                jobPanel.add(jobDetailsPanel);
    
                // View Proposals Button
                JButton viewProposalsButton = new JButton("View Proposals");
                viewProposalsButton.setBackground(new Color(0, 123, 255)); // Blue background
                viewProposalsButton.setForeground(Color.WHITE);
                viewProposalsButton.setFocusPainted(false);
                viewProposalsButton.setOpaque(true);
                viewProposalsButton.setBorderPainted(false);
                viewProposalsButton.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
                viewProposalsButton.addActionListener(e -> showProposalsForJob(jobId, jobSkills));
    
                jobPanel.add(viewProposalsButton);
    
                jobsPanel.add(jobPanel);
                jobsPanel.add(Box.createVerticalStrut(10)); // Add vertical spacing between job panels
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading jobs: " + e.getMessage());
        }
    }

    // Show proposals for a job, sorted by combined score
    private void showProposalsForJob(int jobId, String jobSkills) {
        JFrame proposalsFrame = new JFrame("Proposals for Job ID: " + jobId);
        proposalsFrame.setSize(600, 400);
        proposalsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        proposalsFrame.setLocationRelativeTo(this);

        JPanel proposalsPanel = new JPanel();
        proposalsPanel.setLayout(new BoxLayout(proposalsPanel, BoxLayout.Y_AXIS));
        proposalsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(proposalsPanel);
        proposalsFrame.add(scrollPane, BorderLayout.CENTER);

        loadProposalsForJob(jobId, jobSkills, proposalsPanel);

        proposalsFrame.setVisible(true);
    }

    // Load proposals for a job, and sort them based on skill similarity and rating
    private void loadProposalsForJob(int jobId, String jobSkills, JPanel proposalsPanel) {
        proposalsPanel.removeAll();

        String proposalsQuery = "SELECT p.proposal_id, p.freelancer_id, u.name AS freelancer_name, " +
                                "p.bid_amount, p.estimated_time, p.rating, p.possessed_skills FROM proposals p " +
                                "JOIN users u ON p.freelancer_id = u.id " +
                                "WHERE p.job_id = ? AND (p.proposal_status = 'pending' OR p.proposal_status IS NULL) " +
                                "AND p.freelancer_id != ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(proposalsQuery)) {

            statement.setInt(1, jobId);
            statement.setInt(2, userId);
            ResultSet resultSet = statement.executeQuery();

            List<Proposal> proposals = new ArrayList<>();

            while (resultSet.next()) {
                int proposalId = resultSet.getInt("proposal_id");
                int freelancerId = resultSet.getInt("freelancer_id");
                String freelancerName = resultSet.getString("freelancer_name");
                double bidAmount = resultSet.getDouble("bid_amount");
                int estimatedTime = resultSet.getInt("estimated_time");
                double rating = resultSet.getDouble("rating");
                String possessedSkills = resultSet.getString("possessed_skills");

                double skillSimilarityScore = calculateSkillSimilarity(possessedSkills, jobSkills);
                double combinedScore = 0.7 * skillSimilarityScore + 0.3 * rating;

                proposals.add(new Proposal(proposalId, freelancerId, freelancerName, bidAmount, estimatedTime, rating, possessedSkills, combinedScore));
            }

            // Sort proposals by combined score in descending order
            proposals.sort(Comparator.comparingDouble(Proposal::getCombinedScore).reversed());

            for (Proposal proposal : proposals) {
                JPanel proposalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10)); // Side-by-side layout
                proposalPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                proposalPanel.setPreferredSize(new Dimension(550, 120));
                proposalPanel.setBackground(Color.WHITE);

                JLabel freelancerLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>Freelancer: " + proposal.getFreelancerName() + "</span></html>");
                freelancerLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                JLabel bidLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Bid: $" + proposal.getBidAmount() + "</span></html>");
                bidLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                JLabel ratingLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#28A745;'>Rating: " + proposal.getRating() + "</span></html>");
                ratingLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                proposalPanel.add(freelancerLabel);
                proposalPanel.add(bidLabel);
                proposalPanel.add(ratingLabel);

                proposalsPanel.add(proposalPanel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading proposals: " + e.getMessage());
        }
    }

    // Calculate skill similarity score (simple method to calculate common skills as a percentage)
    private double calculateSkillSimilarity(String possessedSkills, String jobSkills) {
        Set<String> possessedSkillsSet = new HashSet<>(Arrays.asList(possessedSkills.split(",")));
        Set<String> jobSkillsSet = new HashSet<>(Arrays.asList(jobSkills.split(",")));

        Set<String> commonSkills = new HashSet<>(possessedSkillsSet);
        commonSkills.retainAll(jobSkillsSet);

        return (double) commonSkills.size() / jobSkillsSet.size();
    }

    // Proposal class to store proposal data
    class Proposal {
        private int proposalId;
        private int freelancerId;
        private String freelancerName;
        private double bidAmount;
        private int estimatedTime;
        private double rating;
        private String possessedSkills;
        private double combinedScore;

        public Proposal(int proposalId, int freelancerId, String freelancerName, double bidAmount, int estimatedTime, double rating, String possessedSkills, double combinedScore) {
            this.proposalId = proposalId;
            this.freelancerId = freelancerId;
            this.freelancerName = freelancerName;
            this.bidAmount = bidAmount;
            this.estimatedTime = estimatedTime;
            this.rating = rating;
            this.possessedSkills = possessedSkills;
            this.combinedScore = combinedScore;
        }

        public String getFreelancerName() {
            return freelancerName;
        }

        public double getBidAmount() {
            return bidAmount;
        }

        public double getRating() {
            return rating;
        }

        public double getCombinedScore() {
            return combinedScore;
        }
    }
} */
//---------------------------------------
//without accept reject but sorted
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class ViewPostedJobsPage extends JFrame {
    private int userId;

    public ViewPostedJobsPage(int userId) {
        this.userId = userId;
        setTitle("View Posted Jobs");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set a clean white background for the UI
        JPanel jobsPanel = new JPanel();
        jobsPanel.setLayout(new BoxLayout(jobsPanel, BoxLayout.Y_AXIS));
        jobsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(jobsPanel);
        add(scrollPane, BorderLayout.CENTER);

        loadJobs(jobsPanel);
        setVisible(true);
    }

    private String getFreelancerName(int jobId) {
        String query = "SELECT u.name FROM proposals p JOIN users u ON p.freelancer_id = u.id " +
                       "WHERE p.job_id = ? AND p.proposal_status = 'accepted'";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, jobId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("name");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Not Assigned"; // Return "Not Assigned" if no freelancer is assigned yet
    }

    private void loadJobs(JPanel jobsPanel) {
        String jobsQuery = "SELECT j.job_id, j.job_title, j.job_description, j.payment_type, j.timeline, j.status, j.skills " +
                           "FROM jobs j " +
                           "WHERE j.user_id = ?";
    
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(jobsQuery)) {
    
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
    
            while (resultSet.next()) {
                int jobId = resultSet.getInt("job_id");
                String jobTitle = resultSet.getString("job_title");
                String jobDescription = resultSet.getString("job_description");
                String paymentType = resultSet.getString("payment_type");
                java.sql.Date timeline = resultSet.getDate("timeline");
                boolean status = resultSet.getBoolean("status");
                String jobSkills = resultSet.getString("skills");  // Job skills column
    
                JPanel jobPanel = new JPanel();
                jobPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));
                jobPanel.setBackground(Color.WHITE);
                jobPanel.setPreferredSize(new Dimension(450, 120));
    
                // Job Details (Left side)
                JPanel jobDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
                jobDetailsPanel.setBackground(Color.WHITE);
    
                JLabel jobLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>" + jobTitle + "</span></html>");
                jobLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 14));
    
                JLabel timelineLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Timeline: " + timeline + "</span></html>");
                timelineLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
    
                jobDetailsPanel.add(jobLabel);
                jobDetailsPanel.add(timelineLabel);
    
                jobPanel.add(jobDetailsPanel);
    
                // View Proposals Button
                JButton viewProposalsButton = new JButton("View Proposals");
                viewProposalsButton.setBackground(new Color(0, 123, 255)); // Blue background
                viewProposalsButton.setForeground(Color.WHITE);
                viewProposalsButton.setFocusPainted(false);
                viewProposalsButton.setOpaque(true);
                viewProposalsButton.setBorderPainted(false);
                viewProposalsButton.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));
                viewProposalsButton.addActionListener(e -> showProposalsForJob(jobId, jobSkills));
    
                jobPanel.add(viewProposalsButton);
    
                jobsPanel.add(jobPanel);
                jobsPanel.add(Box.createVerticalStrut(10)); // Add vertical spacing between job panels
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading jobs: " + e.getMessage());
        }
    }

    // Show proposals for a job, sorted by combined score
    private void showProposalsForJob(int jobId, String jobSkills) {
        JFrame proposalsFrame = new JFrame("Proposals for Job ID: " + jobId);
        proposalsFrame.setSize(600, 400);
        proposalsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        proposalsFrame.setLocationRelativeTo(this);

        JPanel proposalsPanel = new JPanel();
        proposalsPanel.setLayout(new BoxLayout(proposalsPanel, BoxLayout.Y_AXIS));
        proposalsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(proposalsPanel);
        proposalsFrame.add(scrollPane, BorderLayout.CENTER);

        loadProposalsForJob(jobId, jobSkills, proposalsPanel);

        proposalsFrame.setVisible(true);
    }

    // Load proposals for a job, and sort them based on skill similarity and rating
    private void loadProposalsForJob(int jobId, String jobSkills, JPanel proposalsPanel) {
        proposalsPanel.removeAll();

        String proposalsQuery = "SELECT p.proposal_id, p.freelancer_id, u.name AS freelancer_name, " +
                                "p.bid_amount, p.estimated_time, p.rating, p.possessed_skills FROM proposals p " +
                                "JOIN users u ON p.freelancer_id = u.id " +
                                "WHERE p.job_id = ? AND (p.proposal_status = 'pending' OR p.proposal_status IS NULL) " +
                                "AND p.freelancer_id != ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(proposalsQuery)) {

            statement.setInt(1, jobId);
            statement.setInt(2, userId);
            ResultSet resultSet = statement.executeQuery();

            List<Proposal> proposals = new ArrayList<>();

            while (resultSet.next()) {
                int proposalId = resultSet.getInt("proposal_id");
                int freelancerId = resultSet.getInt("freelancer_id");
                String freelancerName = resultSet.getString("freelancer_name");
                double bidAmount = resultSet.getDouble("bid_amount");
                int estimatedTime = resultSet.getInt("estimated_time");
                double rating = resultSet.getDouble("rating");
                String possessedSkills = resultSet.getString("possessed_skills");

                double skillSimilarityScore = calculateSkillSimilarity(possessedSkills, jobSkills);
                double combinedScore = 0.7 * skillSimilarityScore + 0.3 * rating;

                proposals.add(new Proposal(proposalId, freelancerId, freelancerName, bidAmount, estimatedTime, rating, possessedSkills, combinedScore));
            }

            // Sort proposals by combined score in descending order
            proposals.sort(Comparator.comparingDouble(Proposal::getCombinedScore).reversed());

            for (Proposal proposal : proposals) {
                JPanel proposalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10)); // Side-by-side layout
                proposalPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                proposalPanel.setPreferredSize(new Dimension(550, 120));
                proposalPanel.setBackground(Color.WHITE);

                JLabel freelancerLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#007BFF;'>Freelancer: " + proposal.getFreelancerName() + "</span></html>");
                freelancerLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                JLabel bidLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#FFC107;'>Bid: $" + proposal.getBidAmount() + "</span></html>");
                bidLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                JLabel ratingLabel = new JLabel("<html><span style='font-weight:bold; font-style:italic; color:#28A745;'>Rating: " + proposal.getRating() + "</span></html>");
                ratingLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 12));

                proposalPanel.add(freelancerLabel);
                proposalPanel.add(bidLabel);
                proposalPanel.add(ratingLabel);

                proposalsPanel.add(proposalPanel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading proposals: " + e.getMessage());
        }
    }

    

    // Calculate skill similarity score (simple method to calculate common skills as a percentage)
    private double calculateSkillSimilarity(String possessedSkills, String jobSkills) {
        Set<String> possessedSkillsSet = new HashSet<>(Arrays.asList(possessedSkills.split(",")));
        Set<String> jobSkillsSet = new HashSet<>(Arrays.asList(jobSkills.split(",")));

        Set<String> commonSkills = new HashSet<>(possessedSkillsSet);
        commonSkills.retainAll(jobSkillsSet);

        return (double) commonSkills.size() / jobSkillsSet.size();
    }

    // Proposal class to store proposal data
    class Proposal {
        private int proposalId;
        private int freelancerId;
        private String freelancerName;
        private double bidAmount;
        private int estimatedTime;
        private double rating;
        private String possessedSkills;
        private double combinedScore;

        public Proposal(int proposalId, int freelancerId, String freelancerName, double bidAmount, int estimatedTime, double rating, String possessedSkills, double combinedScore) {
            this.proposalId = proposalId;
            this.freelancerId = freelancerId;
            this.freelancerName = freelancerName;
            this.bidAmount = bidAmount;
            this.estimatedTime = estimatedTime;
            this.rating = rating;
            this.possessedSkills = possessedSkills;
            this.combinedScore = combinedScore;
        }

        public String getFreelancerName() {
            return freelancerName;
        }

        public double getBidAmount() {
            return bidAmount;
        }

        public double getRating() {
            return rating;
        }

        public double getCombinedScore() {
            return combinedScore;
        }
    }
} 
//------------------------------------------
