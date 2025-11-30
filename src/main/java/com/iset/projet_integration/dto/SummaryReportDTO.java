package com.iset.projet_integration.dto;

import java.util.Map;

public class SummaryReportDTO {

    private long totalDonations;
    private long totalUsers;
    private Map<String, Long> donationsByCategory;

    // Nouveaux champs
    private long totalPosts;
    private long totalDemandes;
    private Map<String, Long> usersByRole;

    public SummaryReportDTO() {}

    public SummaryReportDTO(long totalDonations, long totalUsers, Map<String, Long> donationsByCategory,
                            long totalPosts, long totalDemandes, Map<String, Long> usersByRole) {
        this.totalDonations = totalDonations;
        this.totalUsers = totalUsers;
        this.donationsByCategory = donationsByCategory;
        this.totalPosts = totalPosts;
        this.totalDemandes = totalDemandes;
        this.usersByRole = usersByRole;
    }

    // getters / setters pour tous les champs
    public long getTotalDonations() { return totalDonations; }
    public void setTotalDonations(long totalDonations) { this.totalDonations = totalDonations; }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public Map<String, Long> getDonationsByCategory() { return donationsByCategory; }
    public void setDonationsByCategory(Map<String, Long> donationsByCategory) { this.donationsByCategory = donationsByCategory; }

    public long getTotalPosts() { return totalPosts; }
    public void setTotalPosts(long totalPosts) { this.totalPosts = totalPosts; }

    public long getTotalDemandes() { return totalDemandes; }
    public void setTotalDemandes(long totalDemandes) { this.totalDemandes = totalDemandes; }

    public Map<String, Long> getUsersByRole() { return usersByRole; }
    public void setUsersByRole(Map<String, Long> usersByRole) { this.usersByRole = usersByRole; }
}

