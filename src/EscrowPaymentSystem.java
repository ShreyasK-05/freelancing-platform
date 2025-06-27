import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EscrowPaymentSystem extends JFrame {
    private static final String UPDATE_ESCROW_QUERY = 
        "UPDATE escrow_transactions SET progress = ?, rating = ?, review = ? WHERE job_id = ? AND proposal_id = ?";
    private static final String INSERT_PAYMENT_QUERY = 
        "INSERT INTO payments (job_id, proposal_id, amount) VALUES (?, ?, ?)";

    private JProgressBar progressBar;
    private JTextArea reviewArea;
    private JComboBox<Integer> ratingComboBox;
    private JButton releasePaymentButton;

    private int jobId;
    private int proposalId;

    private static final String FETCH_PROGRESS_QUERY = 
    "SELECT progress FROM escrow_transactions WHERE job_id = ? AND proposal_id = ?";

    public EscrowPaymentSystem(int jobId, int proposalId) {
        this.jobId = jobId;
        this.proposalId = proposalId;
        setTitle("Escrow Payment System");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Only this window will close
        
        initComponents();
        fetchAndSetProgress(); // Fetch and set initial progress here
        layoutComponents();
    }
    

private void fetchAndSetProgress() {
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(FETCH_PROGRESS_QUERY)) {

        stmt.setInt(1, jobId);
        stmt.setInt(2, proposalId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            int progress = rs.getInt("progress");
            progressBar.setValue(progress);  // Set the progress bar to the fetched progress
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
    }
}


    private void initComponents() {
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        reviewArea = new JTextArea(4, 30);
        reviewArea.setBorder(BorderFactory.createTitledBorder("Review"));

        ratingComboBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        ratingComboBox.setBorder(BorderFactory.createTitledBorder("Rating"));

        releasePaymentButton = new JButton("Submit Review & Release Payment");
        releasePaymentButton.addActionListener(new ReleasePaymentListener());
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(progressBar, BorderLayout.NORTH);

        JPanel reviewPanel = new JPanel(new GridLayout(2, 1));
        reviewPanel.add(new JScrollPane(reviewArea));
        reviewPanel.add(ratingComboBox);

        mainPanel.add(reviewPanel, BorderLayout.CENTER);
        mainPanel.add(releasePaymentButton, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private boolean validateJobAndProposal() {
        if (!isValidJobId(jobId)) {
            JOptionPane.showMessageDialog(this, "Invalid Job ID.");
            return false;
        }
        
        // Validate proposalId
        if (!isValidProposalId(proposalId)) {
            JOptionPane.showMessageDialog(this, "Invalid Proposal ID.");
            return false;
        }
        
        return true;
    }

    private boolean isValidJobId(int jobId) {
        String query = "SELECT COUNT(*) FROM jobs WHERE job_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, jobId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;  
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false; 
    }

    private boolean isValidProposalId(int proposalId) {
        String query = "SELECT COUNT(*) FROM proposals WHERE proposal_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, proposalId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;  // Returns true if proposal exists
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;  // Return false if the proposal doesn't exist
    }

    private class ReleasePaymentListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!validateJobAndProposal()) {
                return;
            }
    
            String review = reviewArea.getText();
            int rating = (int) ratingComboBox.getSelectedItem();
    
            if (progressBar.getValue() < 100) {
                int newProgress = Math.min(progressBar.getValue() + 25, 100);
                progressBar.setValue(newProgress);
    
                double amountReleased = releaseFundsBasedOnRating(rating, review);
                double maxPotentialAmount = calculateMaxPotentialAmount();
                double percentOfMaxPotential = (amountReleased / maxPotentialAmount) * 100;
    
                JOptionPane.showMessageDialog(null, String.format("Payment of $%.2f released (%.2f%% of potential max amount).", 
                                                                  amountReleased, percentOfMaxPotential));
    
                if (newProgress >= 100) {
                    JOptionPane.showMessageDialog(null, String.format("Total payment of $%.2f has been released.", maxPotentialAmount));
                }
            } else {
                JOptionPane.showMessageDialog(null, "Progress already at 100%. No further payment needed.");
            }
        }
    }
    
    private double releaseFundsBasedOnRating(int rating, String review) {
        double percentageToRelease = getPercentageForRating(rating);
        double amountToRelease = calculateAmountToRelease(percentageToRelease);
    
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
    
            // Check if escrow record exists
            PreparedStatement checkRecord = conn.prepareStatement(
                "SELECT escrow_id FROM escrow_transactions WHERE job_id = ? AND proposal_id = ?");
            checkRecord.setInt(1, jobId);
            checkRecord.setInt(2, proposalId);
            ResultSet rs = checkRecord.executeQuery();
    
            if (!rs.next()) {
                PreparedStatement insertEscrow = conn.prepareStatement(
                    "INSERT INTO escrow_transactions (job_id, proposal_id, amount, progress, rating, review) " +
                    "VALUES (?, ?, ?, ?, ?, ?)");
                insertEscrow.setInt(1, jobId);
                insertEscrow.setInt(2, proposalId);
                insertEscrow.setDouble(3, amountToRelease);
                insertEscrow.setInt(4, progressBar.getValue());
                insertEscrow.setInt(5, rating);
                insertEscrow.setString(6, review);
    
                insertEscrow.executeUpdate();
            }
    
            PreparedStatement updateEscrow = conn.prepareStatement(UPDATE_ESCROW_QUERY);
            updateEscrow.setInt(1, progressBar.getValue());
            updateEscrow.setInt(2, rating);
            updateEscrow.setString(3, review);
            updateEscrow.setInt(4, jobId);
            updateEscrow.setInt(5, proposalId);
    
            int rowsAffected = updateEscrow.executeUpdate();
    
            if (rowsAffected > 0) {
                PreparedStatement releasePayment = conn.prepareStatement(INSERT_PAYMENT_QUERY);
                releasePayment.setInt(1, jobId);
                releasePayment.setInt(2, proposalId);
                releasePayment.setDouble(3, amountToRelease);
    
                int paymentRowsAffected = releasePayment.executeUpdate();
                if (paymentRowsAffected > 0) {
                    conn.commit();
                    return amountToRelease;
                } else {
                    conn.rollback();
                }
            } else {
                conn.rollback();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, "Error updating escrow progress or releasing funds.");
        return 0;
    }
    
    private double getPercentageForRating(int rating) {
        return switch (rating) {
            case 5 -> 100;
            case 4 -> 90;
            case 3 -> 80;
            case 2 -> 70;
            case 1 -> 60;
            default -> 0;
        };
    }
    
    private double calculateAmountToRelease(double percentage) {
        double totalAmount = getTotalEscrowAmount();
        return totalAmount * (percentage / 100);
    }
    
    private double getTotalEscrowAmount() {
        double amount = 0.0;
    
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement getBidAmount = conn.prepareStatement("SELECT bid_amount FROM proposals WHERE proposal_id = ?");
            getBidAmount.setInt(1, proposalId);
            ResultSet bidResult = getBidAmount.executeQuery();
    
            if (bidResult.next()) {
                amount = bidResult.getDouble("bid_amount");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    
        return amount / 4;
    }
    
    
    private double calculateMaxPotentialAmount() {
        double totalAmount = getTotalEscrowAmount();
        return totalAmount;
    }   
}