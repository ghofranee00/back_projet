package com.iset.projet_integration.Controller;

import com.iset.projet_integration.Entities.*;
import com.iset.projet_integration.Repository.UserRepository;
import com.iset.projet_integration.Service.DonationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/donations")
public class DonationController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private UserRepository userRepository;

    // 🔹 Créer une donation - Seulement DONOR et ASSOCIATION
    @PostMapping
    @PreAuthorize("hasAnyRole('DONNATEUR', 'ASSOCIATION')")
    public ResponseEntity<?> createDonation(
            @RequestBody DonationRequest request,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);

            // Vérifier que l'utilisateur crée une donation pour lui-même
            if (!currentUser.getId().equals(request.getDonorId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous ne pouvez créer des donations que pour votre propre compte");
            }

            Donation donation = new Donation();
            donation.setCategorie(request.getCategorie());
            donation.setRegion(request.getRegion());
            donation.setDetails(request.getDetails());
            donation.setImages(request.getImages());

            // Utiliser l'utilisateur complet
            donation.setDonor(currentUser);

            if (request.getPostId() != null) {
                Post post = new Post();
                post.setId(request.getPostId());
                donation.setPost(post);
            }

            return ResponseEntity.ok(donationService.creerDonation(donation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Mettre à jour le statut - Seulement ADMIN
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestParam String action,
            Authentication authentication) {

        try {
            List<Notification> notifications = donationService.traiterDonation(id, action);
            Donation donation = donationService.getDonationById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("donation", donation);
            response.put("notifications", notifications);
            response.put("message", "Donation " + action + " avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Lister toutes les donations - Accessible à tous les utilisateurs authentifiés
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Donation> getAllDonations(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        // Les ADMIN voient tout, les autres ne voient que leurs donations
        if (currentUser.getRole() == User.Role.ADMIN) {
            return donationService.listerDonations();
        } else {
            return donationService.getDonationsByUser(currentUser);
        }
    }

    // 🔹 Filtrer donations - Accessible à tous
    @GetMapping("/filtrer")
    @PreAuthorize("isAuthenticated()")
    public List<Donation> filtrerDonations(
            @RequestParam(required = false) Donation.Categorie categorie,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Donation.StatutDonation statut,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        List<Donation> donations = donationService.filtrerDonations(categorie, region, statut);

        // Filtrage supplémentaire pour les non-ADMIN
        if (currentUser.getRole() != User.Role.ADMIN) {
            donations = donations.stream()
                    .filter(d -> d.getDonor().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        return donations;
    }

    // 🔹 Récupérer donations par utilisateur
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDonationsByUser(
            @PathVariable String userId,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        // Un utilisateur ne peut voir que ses propres donations sauf s'il est ADMIN
        if (!currentUser.getId().equals(userId) && currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Vous n'avez pas accès aux donations de cet utilisateur");
        }

        User user = new User();
        user.setId(userId);
        return ResponseEntity.ok(donationService.getDonationsByUser(user));
    }

    // 🔹 Supprimer une donation - DONOR et ADMIN seulement
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DONNATEUR', 'ADMIN')")
    public ResponseEntity<?> deleteDonation(
            @PathVariable String id,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            Donation donation = donationService.getDonationById(id);

            // Vérifier que le DONOR ne peut supprimer que ses propres donations
            if (currentUser.getRole() == User.Role.DONNATEUR &&
                    !donation.getDonor().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous ne pouvez supprimer que vos propres donations");
            }

            donationService.deleteDonation(id);
            return ResponseEntity.ok(Map.of("message", "Donation supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Mettre à jour une donation - DONOR et ADMIN seulement
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DONNATEUR', 'ADMIN')") // ⚠️ Corrigé: 'DONNATEUR' au lieu de 'DONOR'
    public ResponseEntity<?> updateDonation(
            @PathVariable String id,
            @RequestBody Donation donationDetails,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            Donation existingDonation = donationService.getDonationById(id);

            // Vérifier que le DONOR ne peut modifier que ses propres donations
            if (currentUser.getRole() == User.Role.DONNATEUR && // ⚠️ Corrigé: suppression espace
                    !existingDonation.getDonor().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous ne pouvez modifier que vos propres donations");
            }

            Donation updated = donationService.updateDonation(id, donationDetails);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Méthode utilitaire pour récupérer l'utilisateur courant
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
} // ✅ UNE SEULE ACCOLADE FERMANTE ICI