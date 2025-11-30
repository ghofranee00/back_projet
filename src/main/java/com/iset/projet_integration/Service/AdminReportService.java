package com.iset.projet_integration.Service;

import com.iset.projet_integration.dto.SummaryReportDTO;
import com.iset.projet_integration.Repository.AdminReportRepository;
import com.iset.projet_integration.Repository.DemandeRepository;
import com.iset.projet_integration.Repository.PostRepository;
import com.iset.projet_integration.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminReportService {

    @Autowired
    private AdminReportRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private DemandeRepository demandeRepository;

    public SummaryReportDTO generateSummaryReport() {

        // 🔹 Donations
        long totalDonations = donationRepository.count();
        Map<String, Long> donationsByCategory = donationRepository.findAll().stream()
                .filter(d -> d.getCategorie() != null)
                .collect(Collectors.groupingBy(d -> d.getCategorie().name(), Collectors.counting()));

        // 🔹 Users
        long totalUsers = userRepository.count();
        Map<String, Long> usersByRole = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null)
                .collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));

        // 🔹 Posts
        long totalPosts = postRepository.count();

        // 🔹 Demandes
        long totalDemandes = demandeRepository.count();

        return new SummaryReportDTO(
                totalDonations,
                totalUsers,
                donationsByCategory,
                totalPosts,
                totalDemandes,
                usersByRole
        );
    }
}
