package com.iset.projet_integration.Controller;

import com.iset.projet_integration.Entities.*;
import com.iset.projet_integration.Repository.DonationRepository;
import com.iset.projet_integration.Repository.PostRepository;
import com.iset.projet_integration.Service.DonationService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/donations")
@CrossOrigin(origins = "http://localhost:4200")
public class DonationController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private DonationRepository donationRepository;

    // 🔹 CORRECTION : Créer une donation à partir de DonationRequest
    @PostMapping
    public ResponseEntity<?> createDonation(
            @RequestBody DonationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String authenticatedUserId = jwt.getSubject();
            Donation donation = donationService.creerDonation(request, authenticatedUserId);
            return ResponseEntity.ok(donation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔹 Mettre à jour le statut (accepter / refuser) par l'admin
    @PutMapping("/{id}/status")
    public ResponseEntity<Donation> updateStatus(
            @PathVariable String id,
            @RequestParam String action // "accepter" ou "refuser"
    ) {
        try {
            List<Notification> notifications = donationService.traiterDonation(id, action);
            System.out.println(" Notifications créées: " + notifications.size());
            Donation donation = donationService.getDonationById(id);
            return ResponseEntity.ok(donation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 🔹 Lister toutes les donations
    @GetMapping
    public ResponseEntity<List<Donation>> getAllDonations() {
        return ResponseEntity.ok(donationService.listerDonations());
    }

    // 🔹 Filtrer donations
    @GetMapping("/filtrer")
    public ResponseEntity<List<Donation>> filtrerDonations(
            @RequestParam(required = false) Donation.Categorie categorie,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Donation.StatutDonation statut
    ) {
        try {
            List<Donation> donations = donationService.filtrerDonations(categorie, region, statut);
            return ResponseEntity.ok(donations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 🔹 Récupérer donations par utilisateur
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Donation>> getDonationsByUser(@PathVariable String userId) {
        try {
            List<Donation> donations = donationService.getDonationsByUserId(userId);
            return ResponseEntity.ok(donations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 🔹 Supprimer une donation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDonation(@PathVariable String id) {
        try {
            donationService.deleteDonation(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 🔹 Afficher l'historique des donations pour un utilisateur
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<Donation>> getDonationHistory(@PathVariable String userId) {
        try {
            List<Donation> donations = donationService.getDonationsByUserId(userId);
            return ResponseEntity.ok(donations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 🔹 Mettre à jour une donation
    @PutMapping("/{id}")
    public ResponseEntity<Donation> updateDonation(
            @PathVariable String id,
            @RequestBody Donation donationDetails
    ) {
        try {
            Donation updated = donationService.updateDonation(id, donationDetails);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 🔹 Récupérer les donations pour un post spécifique
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Donation>> getDonationsByPostId(@PathVariable String postId) {
        try {
            List<Donation> donations = donationRepository.findByPostId(postId);
            return ResponseEntity.ok(donations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/needy/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getDonationsForNeedy(@PathVariable String userId) {
        try {
            System.out.println("📱 Frontend requesting donations for userId: " + userId);

            // Récupérer tous les posts de ce needy
            List<Post> needyPosts = postRepository.findByUserId(userId);
            System.out.println("📄 Found " + needyPosts.size() + " posts for user");

            if (needyPosts.isEmpty()) {
                System.out.println("ℹ No posts found for user, returning empty list");
                return ResponseEntity.ok(Collections.emptyList());
            }

            // CORRECTION: Convertir en ObjectId
            List<ObjectId> postObjectIds = needyPosts.stream()
                    .map(post -> new ObjectId(post.getId()))
                    .collect(Collectors.toList());

            System.out.println("📋 Post ObjectIds: " + postObjectIds);

            // DEBUG: Vérifier manuellement
            System.out.println("\n🔍 [DEBUG] Manual check of donations:");
            List<Donation> allDonations = donationRepository.findAll();
            List<Donation> matchingDonations = new ArrayList<>();

            for (Donation donation : allDonations) {
                if (donation.getPost() != null) {
                    String donationPostId = donation.getPost().getId();
                    boolean isInList = needyPosts.stream()
                            .anyMatch(post -> post.getId().equals(donationPostId));

                    System.out.println("   Donation " + donation.getId() +
                            " -> Post ID: " + donationPostId +
                            ", In list? " + isInList);

                    if (isInList) {
                        matchingDonations.add(donation);
                    }
                }
            }

            System.out.println(" Manually found " + matchingDonations.size() + " donations");

            // Si la méthode findByPostIdIn ne marche pas, utilise les donations trouvées manuellement
            List<Donation> donations = matchingDonations;

            // Convertir en Map
            List<Map<String, Object>> responseList = new ArrayList<>();

            for (Donation donation : donations) {
                Map<String, Object> donationMap = new HashMap<>();
                donationMap.put("id", donation.getId());

                // Infos du post
                if (donation.getPost() != null) {
                    donationMap.put("postId", donation.getPost().getId());
                    donationMap.put("postContent", donation.getPost().getContenu());
                }

                // Infos du donateur
                if (donation.getDonor() != null) {
                    donationMap.put("donorId", donation.getDonor().getId());
                    donationMap.put("donorName",
                            donation.getDonor().getFirstName() + " " + donation.getDonor().getLastName());
                    donationMap.put("donorEmail", donation.getDonor().getEmail());
                    donationMap.put("donorPhone", donation.getDonor().getPhone());
                }

                // Autres champs
                donationMap.put("dateDonation", donation.getDateDonation());
                donationMap.put("categorie", donation.getCategorie() != null ? donation.getCategorie().name() : null);
                donationMap.put("region", donation.getRegion());
                donationMap.put("details", donation.getDetails());
                donationMap.put("status", donation.getStatus() != null ? donation.getStatus().name() : null);

                responseList.add(donationMap);
                System.out.println("🎯 Added donation: " + donation.getId() +
                        " for post: " + donationMap.get("postId"));
            }

            System.out.println("✅ Returning " + responseList.size() + " donations");
            return ResponseEntity.ok(responseList);

        } catch (Exception e) {
            System.out.println("❌ Error in getDonationsForNeedy: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // 🔹 Récupérer une donation par ID
    @GetMapping("/{id}")
    public ResponseEntity<Donation> getDonationById(@PathVariable String id) {
        try {
            Donation donation = donationService.getDonationById(id);
            return ResponseEntity.ok(donation);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}