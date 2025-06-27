import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

abstract class BasePage extends JFrame {
    protected int userId;

    public BasePage(String title, int userId) {
        super(title);
        this.userId = userId;
    }
}

interface DatabaseConnectionInterface {
    Connection getDatabaseConnection() throws SQLException;
}

public class Jobs extends JFrame implements DatabaseConnectionInterface{
    private JPanel jobPanelContainer;
    private JTextField minPriceField;
    private JTextField maxPriceField;
    private JTextField skillsField;
    private int userId;

    public Jobs(int userId) {
        this.userId = userId;

        setTitle("Job Listings");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel gradientLayer = new JPanel() {
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
        gradientLayer.setLayout(new BorderLayout(10, 10));
        setContentPane(gradientLayer);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setPreferredSize(new Dimension(1100, 700));

        gradientLayer.add(mainPanel, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setOpaque(false);

        JButton postJobButton = new JButton("Post a Job");
        JButton uploadResumeButton = new JButton("Upload Resume");
        postJobButton.setBackground(new Color(58, 123, 213));
        postJobButton.setForeground(Color.WHITE);
        uploadResumeButton.setBackground(new Color(58, 123, 213));
        uploadResumeButton.setForeground(Color.WHITE);

        postJobButton.addActionListener(e -> new PostJobPage(userId));
        uploadResumeButton.addActionListener(e -> new UploadResume(userId));

        JButton viewPostedJobsButton = new JButton("View Posted Jobs");
        viewPostedJobsButton.setBackground(new Color(58, 123, 213));
        viewPostedJobsButton.setForeground(Color.WHITE);
        viewPostedJobsButton.addActionListener(e -> new ViewPostedJobsPage(userId));
        buttonPanel.add(viewPostedJobsButton);

        buttonPanel.add(postJobButton);
        buttonPanel.add(uploadResumeButton);
        buttonPanel.add(viewPostedJobsButton);

        JPanel filterPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter Options"));
        filterPanel.setBackground(Color.WHITE);

        filterPanel.add(new JLabel("Min Price:", SwingConstants.RIGHT));
        minPriceField = new JTextField();
        filterPanel.add(minPriceField);
        
        filterPanel.add(new JLabel("Max Price:", SwingConstants.RIGHT));
        maxPriceField = new JTextField();
        filterPanel.add(maxPriceField);
        
        filterPanel.add(new JLabel("Skills:", SwingConstants.RIGHT));
        skillsField = new JTextField();
        filterPanel.add(skillsField);

        JButton filterButton = new JButton("Filter");
        filterButton.setBackground(new Color(58, 123, 213));
        filterButton.setForeground(Color.WHITE);
        filterButton.addActionListener(e -> filterJobs());
        filterPanel.add(filterButton);

        topPanel.add(buttonPanel, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        jobPanelContainer = new JPanel();
        jobPanelContainer.setLayout(new BoxLayout(jobPanelContainer, BoxLayout.Y_AXIS));
        jobPanelContainer.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(jobPanelContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        fetchJobsFromDatabase();

        setVisible(true);
    }

    public Connection getDatabaseConnection() throws SQLException { // Implementing interface method
        return DriverManager.getConnection("jdbc:postgresql://localhost:5432/mydb", "postgres", "mydatabase");
    }

    private void fetchJobsFromDatabase() {
        jobPanelContainer.removeAll();
        List<Job> jobList = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM jobs ORDER BY job_id ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Job job = new Job(
                    rs.getInt("job_id"),
                    rs.getString("job_title"),
                    rs.getString("job_description"),
                    rs.getBigDecimal("payment"),
                    rs.getDate("timeline"),
                    rs.getString("skills")
                );
                jobList.add(job);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching jobs from database.");
        }

        for (Job job : jobList) {
            jobPanelContainer.add(createJobCard(job));
        }
        jobPanelContainer.revalidate();
        jobPanelContainer.repaint();
    }

    private JPanel createJobCard(Job job) {
        JPanel jobCard = new JPanel(new GridBagLayout());
        jobCard.setPreferredSize(new Dimension(950, 60)); // Compact size for job card
        jobCard.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        jobCard.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel titleLabel = new JLabel(job.getJobTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        jobCard.add(titleLabel, gbc);

        JLabel paymentLabel = new JLabel("$" + (job.getPayment() != null ? job.getPayment().toString() : "N/A"));
        paymentLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        jobCard.add(paymentLabel, gbc);

        JLabel timelineLabel = new JLabel("Timeline: " + job.getTimeline().toString());
        timelineLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridx = 1;
        jobCard.add(timelineLabel, gbc);

        String shortDescription = job.getJobDescription().length() > 30 ? job.getJobDescription().substring(0, 30) + "..." : job.getJobDescription();
        JLabel descriptionLabel = new JLabel("Description: " + shortDescription);
        descriptionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridx = 2;
        jobCard.add(descriptionLabel, gbc);

        jobCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new JobDetailsWindow(job, userId);
            }
        });

        return jobCard;
    }
    
    private void filterJobs() {
        final BigDecimal minPrice;
        final BigDecimal maxPrice;
        String skills = skillsField.getText().trim();
    
        try {
            minPrice = minPriceField.getText().isEmpty() ? null : new BigDecimal(minPriceField.getText());
            maxPrice = maxPriceField.getText().isEmpty() ? null : new BigDecimal(maxPriceField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for prices.");
            return;
        }
    
        List<FilterCriteria<Job>> filters = new ArrayList<>();
    
        if (minPrice != null) {
            filters.add(new FilterCriteria<>(job -> job.getPayment() != null && job.getPayment().compareTo(minPrice) >= 0));
        }
    
        if (maxPrice != null) {
            filters.add(new FilterCriteria<>(job -> job.getPayment() != null && job.getPayment().compareTo(maxPrice) <= 0));
        }
    
        if (!skills.isEmpty()) {
            filters.add(new FilterCriteria<>(job -> job.getSkills() != null && job.getSkills().toLowerCase().contains(skills.toLowerCase())));
        }
    
        jobPanelContainer.removeAll();
    
        List<Job> filteredJobs = applyFilters(fetchJobs(), filters);
        for (Job job : filteredJobs) {
            jobPanelContainer.add(createJobCard(job));
        }
    
        jobPanelContainer.revalidate();
        jobPanelContainer.repaint();
    }
    

    private List<Job> fetchJobs() {
        List<Job> jobs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM jobs ORDER BY job_id ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Job job = new Job(
                    rs.getInt("job_id"),
                    rs.getString("job_title"),
                    rs.getString("job_description"),
                    rs.getBigDecimal("payment"),
                    rs.getDate("timeline"),
                    rs.getString("skills")
                );
                jobs.add(job);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching jobs from database.");
        }
        return jobs;
    }

    private List<Job> applyFilters(List<Job> jobs, List<FilterCriteria<Job>> filters) {
        List<Job> filteredJobs = new ArrayList<>(jobs);

        for (FilterCriteria<Job> filter : filters) {
            filteredJobs.removeIf(job -> !filter.matches(job));
        }

        return filteredJobs;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Jobs(1));
    }
}

class FilterCriteria<T> {
    private final Predicate<T> predicate;

    public FilterCriteria(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    public boolean matches(T item) {
        return predicate.test(item);
    }
}

class Job {
    private int jobId;
    private String jobTitle;
    private String jobDescription;
    private BigDecimal payment;
    private java.sql.Date timeline;
    private String skills;

    public Job(int jobId, String jobTitle, String jobDescription, BigDecimal payment, java.sql.Date timeline, String skills) {
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.jobDescription = jobDescription;
        this.payment = payment;
        this.timeline = timeline;
        this.skills = skills;
    }

    public int getJobId() {
        return jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public BigDecimal getPayment() {
        return payment;
    }

    public java.sql.Date getTimeline() {
        return timeline;
    }

    public String getSkills() {
        return skills;
    }
}
