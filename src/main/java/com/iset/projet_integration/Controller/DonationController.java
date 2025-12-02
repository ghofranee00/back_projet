package com.iset.projet_integration.Controller;

import com.iset.projet_integration.Entities.*;
import com.iset.projet_integration.Repository.PostRepository;
import com.iset.projet_integration.Repository.UserRepository;
import com.iset.projet_integration.Service.DonationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/donations")
@CrossOrigin(origins = "*")
public class DonationController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('DONNATEUR', 'ASSOCIATION')")
    public ResponseEntity<?> createDonation(
            @RequestBody DonationRequest request,
            Authentication authentication) {

        try {
            System.out.println("=== 🔴 DEBUG START ===");

            // 1. Afficher TOUTE la requête
            System.out.println("📦 FULL REQUEST BODY:");
            System.out.println("  postId: '" + request.getPostId() + "'");
            System.out.println("  categorie: '" + request.getCategorie() + "'");
            System.out.println("  region: '" + request.getRegion() + "'");
            System.out.println("  details: '" + request.getDetails() + "'");
            System.out.println("  images: " + request.getImages());

            // 2. Analyser l'ID caractère par caractère
            if (request.getPostId() != null) {
                System.out.println("🔍 POST ID ANALYSIS:");
                System.out.println("  Length: " + request.getPostId().length());
                System.out.println("  Is empty: " + request.getPostId().isEmpty());
                System.out.println("  Trimmed: '" + request.getPostId().trim() + "'");

                // Vérifier si c'est un ObjectId valide
                boolean isValidObjectId = request.getPostId().matches("[0-9a-fA-F]{24}");
                System.out.println("  Is valid ObjectId (24 hex chars): " + isValidObjectId);

                // Afficher chaque caractère
                System.out.println("  Characters:");
                for (int i = 0; i < request.getPostId().length(); i++) {
                    char c = request.getPostId().charAt(i);
                    System.out.println("    [" + i + "] = '" + c + "' (code: " + (int) c + ")");
                }
            }

            // 3. Vérifier TOUS les posts dans la base
            System.out.println("🗄️ ALL POSTS IN DATABASE:");
            List<Post> allPosts = postRepository.findAll();
            if (allPosts.isEmpty()) {
                System.out.println("  ❌ NO POSTS IN DATABASE!");
            } else {
                System.out.println("  Total posts: " + allPosts.size());
                for (int i = 0; i < Math.min(allPosts.size(), 10); i++) {
                    Post p = allPosts.get(i);
                    System.out.println("  [" + i + "] ID: '" + p.getId() + "'");
                    System.out.println("      Type: " + p.getTypeDemande());
                    System.out.println("      Content preview: " +
                            (p.getContenu() != null ?
                                    p.getContenu().substring(0, Math.min(50, p.getContenu().length())) : "null"));
                }
            }

            // 4. Essayer de trouver le post
            if (request.getPostId() != null) {
                System.out.println("🔎 SEARCHING FOR POST:");

                // Méthode 1: existsById
                boolean exists = postRepository.existsById(request.getPostId());
                System.out.println("  Method 1 - existsById('" + request.getPostId() + "'): " + exists);

                // Méthode 2: findById
                Optional<Post> postOpt = postRepository.findById(request.getPostId());
                System.out.println("  Method 2 - findById('" + request.getPostId() + "'): " +
                        (postOpt.isPresent() ? "FOUND" : "NOT FOUND"));

                // Méthode 3: Chercher manuellement
                System.out.println("  Method 3 - Manual search:");
                for (Post p : allPosts) {
                    if (p.getId().equals(request.getPostId())) {
                        System.out.println("    ✅ Exact match found!");
                        System.out.println("      ID: '" + p.getId() + "'");
                        System.out.println("      Content: " + p.getContenu());
                        break;
                    } else if (p.getId().equalsIgnoreCase(request.getPostId())) {
                        System.out.println("    ⚠️ Case-insensitive match!");
                    } else if (p.getId().contains(request.getPostId()) ||
                            request.getPostId().contains(p.getId())) {
                        System.out.println("    ⚠️ Partial match with ID: '" + p.getId() + "'");
                    }
                }

                if (!exists) {
                    System.out.println("❌ POST NOT FOUND!");
                    System.out.println("   Request ID: '" + request.getPostId() + "'");
                    System.out.println("   Available IDs:");
                    for (Post p : allPosts) {
                        System.out.println("   - '" + p.getId() + "'");
                    }
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Post not found. ID: '" + request.getPostId() + "'");
                }
            } else {
                System.out.println("❌ POST ID IS NULL IN REQUEST!");
            }

            System.out.println("=== 🟢 DEBUG END ===");

            // ... reste du code normal

            return ResponseEntity.ok("Test success");

        } catch (Exception e) {
            System.out.println("💥 EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Lister toutes les donations - Accessible à tous les utilisateurs authentifiés
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllDonations(Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);

            List<Donation> donations;
            if (currentUser.getRole() == User.Role.ADMIN) {
                donations = donationService.listerDonations();
            } else {
                donations = donationService.getDonationsByUser(currentUser);
            }

            return ResponseEntity.ok(donations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Récupérer une donation par ID
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDonationById(
            @PathVariable String id,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            Donation donation = donationService.getDonationById(id);

            // Vérifier les permissions
            if (currentUser.getRole() != User.Role.ADMIN &&
                    !donation.getDonor().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas accès à cette donation");
            }

            return ResponseEntity.ok(donation);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 🔹 Filtrer donations - Accessible à tous
    @GetMapping("/filtrer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> filtrerDonations(
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String statut,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);

            // Convertir les paramètres en Enum
            Donation.Categorie categorieEnum = null;
            if (categorie != null && !categorie.isEmpty()) {
                categorieEnum = Donation.Categorie.valueOf(categorie.toUpperCase());
            }

            Donation.StatutDonation statutEnum = null;
            if (statut != null && !statut.isEmpty()) {
                statutEnum = Donation.StatutDonation.valueOf(statut.toUpperCase());
            }

            List<Donation> donations = donationService.filtrerDonations(categorieEnum, region, statutEnum);

            // Filtrage supplémentaire pour les non-ADMIN
            if (currentUser.getRole() != User.Role.ADMIN) {
                donations = donations.stream()
                        .filter(d -> d.getDonor().getId().equals(currentUser.getId()))
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(donations);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Paramètre invalide: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Récupérer donations par utilisateur
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDonationsByUser(
            @PathVariable String userId,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);

            // Un utilisateur ne peut voir que ses propres donations sauf s'il est ADMIN
            if (!currentUser.getId().equals(userId) && currentUser.getRole() != User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous n'avez pas accès aux donations de cet utilisateur");
            }

            User user = new User();
            user.setId(userId);
            List<Donation> donations = donationService.getDonationsByUser(user);

            return ResponseEntity.ok(donations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Donation supprimée avec succès");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Mettre à jour une donation - DONOR et ADMIN seulement
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DONNATEUR', 'ADMIN')")
    public ResponseEntity<?> updateDonation(
            @PathVariable String id,
            @RequestBody Donation donationDetails,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            Donation existingDonation = donationService.getDonationById(id);

            // Vérifier que le DONOR ne peut modifier que ses propres donations
            if (currentUser.getRole() == User.Role.DONNATEUR &&
                    !existingDonation.getDonor().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous ne pouvez modifier que vos propres donations");
            }

            // Empêcher la modification du statut via cette méthode
            donationDetails.setStatus(existingDonation.getStatus());

            // Garder les références originales
            donationDetails.setDonor(existingDonation.getDonor());
            donationDetails.setPost(existingDonation.getPost());

            Donation updated = donationService.updateDonation(id, donationDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Donation mise à jour avec succès");
            response.put("donation", updated);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
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
}