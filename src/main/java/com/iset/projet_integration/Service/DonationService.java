package com.iset.projet_integration.Service;

import com.iset.projet_integration.Entities.*;
import com.iset.projet_integration.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DonationService {

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // 🔹 Créer une donation à partir de DonationRequest
    public Donation creerDonation(DonationRequest donationRequest, String authenticatedUserId) {
        System.out.println("=== CRÉATION DONATION FROM REQUEST ===");
        System.out.println("PostId: " + donationRequest.getPostId());
        System.out.println("Catégorie: " + donationRequest.getCategorie());
        System.out.println("Région: " + donationRequest.getRegion());

        // Vérification postId
        if (donationRequest.getPostId() == null || donationRequest.getPostId().isEmpty()) {
            throw new RuntimeException("PostId non fourni pour la donation");
        }

        // Récupérer le post complet
        Post post = postRepository.findById(donationRequest.getPostId())
                .orElseThrow(() -> {
                    System.out.println("❌ Post introuvable avec ID: " + donationRequest.getPostId());
                    return new RuntimeException("Post introuvable");
                });
        System.out.println("✅ Post trouvé: " + post.getId());

        // ============ CORRECTION IMPORTANTE ============
        // RÉCUPÉRER L'ID UTILISATEUR DU SECURITY CONTEXT
        String donorId = null;

        try {
            // Méthode 1: Depuis SecurityContextHolder (Spring Security)
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                if (principal instanceof org.springframework.security.oauth2.jwt.Jwt) {
                    // Cas JWT token
                    org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) principal;
                    donorId = jwt.getSubject();
                    System.out.println("✅ DonorId extrait du JWT: " + donorId);
                } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    // Cas UserDetails
                    donorId = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                    System.out.println("✅ DonorId extrait de UserDetails: " + donorId);
                } else if (principal instanceof String) {
                    // Cas simple String
                    donorId = (String) principal;
                    System.out.println("✅ DonorId extrait comme String: " + donorId);
                }
            }

            // Méthode 2: Si toujours null, essayer de récupérer depuis la requête
            if (donorId == null || donorId.isEmpty()) {
                donorId = donationRequest.getDonorId();
                if (donorId != null && !donorId.isEmpty()) {
                    System.out.println("⚠️ Utilisation du donorId de la requête: " + donorId);
                }
            }

            // Méthode 3: Si toujours null, ERREUR
            if (donorId == null || donorId.isEmpty()) {
                System.err.println("❌ ERREUR CRITIQUE: Impossible de récupérer l'ID utilisateur!");
                System.err.println("   Authentication: " + authentication);
                System.err.println("   Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
                throw new RuntimeException("Utilisateur non authentifié. Impossible de déterminer le donorId.");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
            throw new RuntimeException("Erreur d'authentification: " + e.getMessage());
        }
        // ============ FIN CORRECTION ============

        // Récupérer le donor complet depuis MongoDB - LE RENDRE FINAL
        final String finalDonorId = donorId; // Créer une variable finale
        User donor = userRepository.findById(finalDonorId)
                .orElseThrow(() -> {
                    System.out.println("❌ Donor introuvable avec ID: " + finalDonorId);

                    // DEBUG: Afficher tous les utilisateurs disponibles
                    System.out.println("=== DEBUG: UTILISATEURS DISPONIBLES DANS MONGODB ===");
                    List<User> allUsers = userRepository.findAll();
                    if (allUsers.isEmpty()) {
                        System.out.println("   Aucun utilisateur trouvé dans MongoDB!");
                    } else {
                        for (User u : allUsers) {
                            System.out.println("   - ID: " + u.getId() +
                                    ", Username: " + u.getIdentifiant() +
                                    ", Email: " + u.getEmail() +
                                    ", Role: " + u.getRole());
                        }
                    }
                    System.out.println("=== FIN LISTE ===");

                    return new RuntimeException("Donor introuvable dans MongoDB. ID recherché: " + finalDonorId);
                });
        System.out.println("✅ Donor trouvé dans MongoDB: " + donor.getIdentifiant() + " (ID: " + donor.getId() + ")");

        // Créer la donation
        Donation donation = new Donation();
        donation.setPost(post);
        donation.setDonor(donor);
        donation.setCategorie(donationRequest.getCategorie());
        donation.setRegion(donationRequest.getRegion());
        donation.setDetails(donationRequest.getDetails());
        donation.setImages(donationRequest.getImages());

        // ✅ STOCKER LES CHAMPS SPÉCIFIQUES
        if (donationRequest.getChampsSpecifiques() != null) {
            donation.setDetailsSpecifiques(donationRequest.getChampsSpecifiques());
            System.out.println("Champs spécifiques sauvegardés: " +
                    donationRequest.getChampsSpecifiques().size() + " champs");
        }

        donation.setStatus(Donation.StatutDonation.EN_ATTENTE);
        donation.setDateDonation(LocalDateTime.now());

        // Valider selon la catégorie
        validerDonationSelonCategorie(donation);

        Donation saved = donationRepository.save(donation);
        System.out.println(" Donation créée avec ID: " + saved.getId());

        // Notification vers l'ADMIN
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        if (!admins.isEmpty()) {
            User admin = admins.get(0);
            final User finalDonor = donor; // Créer une variable finale pour donor aussi
            Notification notif = new Notification(
                    "New donation received from" + finalDonor.getIdentifiant(),
                    Notification.TypeNotification.DONATION,
                    Notification.StatutNotification.EN_ATTENTE,
                    finalDonor,
                    admin,
                    saved
            );
            notificationRepository.save(notif);
            System.out.println("✅ Notification créée pour l'admin");
        }

        return saved;
    }
    // ✅ NOUVELLE MÉTHODE : Valider selon la catégorie
    private void validerDonationSelonCategorie(Donation donation) {
        System.out.println("=== VALIDATION PAR CATÉGORIE ===");
        System.out.println("Catégorie: " + donation.getCategorie());

        if (donation.getDetailsSpecifiques() != null) {
            System.out.println("Champs spécifiques présents: " +
                    donation.getDetailsSpecifiques().keySet());

            switch (donation.getCategorie()) {
                case LOGEMENT:
                    System.out.println("📋 Validation donation LOGEMENT");
                    // Vous pouvez ajouter des validations spécifiques ici
                    if (donation.getDetailsSpecifiques().containsKey("typeLogement")) {
                        System.out.println("  Type logement: " +
                                donation.getDetailsSpecifiques().get("typeLogement"));
                    }
                    break;

                case VETEMENT:
                    System.out.println("👕 Validation donation VETEMENT");
                    break;

                case NOURRITURE:
                    System.out.println("🍎 Validation donation NOURRITURE");
                    break;

                case SANTE:
                    System.out.println("🏥 Validation donation SANTE");
                    break;

                case EDUCATION:
                    System.out.println("📚 Validation donation EDUCATION");
                    break;

                case ARGENT:
                    System.out.println("💰 Validation donation ARGENT");
                    break;
            }
        }
    }

    public Donation getDonationById(String id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation non trouvée avec l'ID: " + id));
    }

    // 🔹 MÉTHODE UTILITAIRE : Récupérer un champ spécifique
    public Object getChampSpecifique(String donationId, String champ) {
        Donation donation = getDonationById(donationId);
        if (donation.getDetailsSpecifiques() != null) {
            return donation.getDetailsSpecifiques().get(champ);
        }
        return null;
    }

    // 🔹 MÉTHODE UTILITAIRE : Mettre à jour un champ spécifique
    public void mettreAJourChampSpecifique(String donationId, String champ, Object valeur) {
        Donation donation = getDonationById(donationId);
        if (donation.getDetailsSpecifiques() != null) {
            donation.getDetailsSpecifiques().put(champ, valeur);
            donationRepository.save(donation);
        }
    }

    // 🔹 TRAITER UNE DONATION (ADMIN : accepter/refuser)
    public List<Notification> traiterDonation(String donationId, String action) {
        Donation donation = getDonationById(donationId);
        User donor = donation.getDonor();
        User needy = donation.getPost().getUser();

        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        if (admins.isEmpty()) {
            throw new RuntimeException("Aucun admin disponible pour traiter la donation !");
        }
        User admin = admins.get(0);

        List<Notification> notificationsCrees = new ArrayList<>();

        if (action.equalsIgnoreCase("accepter")) {
            donation.setStatus(Donation.StatutDonation.ACCEPTEE);
            donationRepository.save(donation);

            // Notification au donor
            Notification notifDonor = new Notification(
                    "Your donation has been accepted",
                    Notification.TypeNotification.DONATION,
                    Notification.StatutNotification.ACCEPTEE,
                    admin,
                    donor,
                    donation
            );
            notificationRepository.save(notifDonor);
            notificationsCrees.add(notifDonor);

            // Notification au needy
            Notification notifNeedy = new Notification(
                    "A donation regarding your request has been accepted ",
                    Notification.TypeNotification.DONATION,
                    Notification.StatutNotification.ACCEPTEE,
                    admin,
                    needy,
                    donation
            );
            notificationRepository.save(notifNeedy);
            notificationsCrees.add(notifNeedy);

        } else if (action.equalsIgnoreCase("refuser")) {
            donation.setStatus(Donation.StatutDonation.REFUSEE);
            donationRepository.save(donation);

            // Notification au donor
            Notification notifDonor = new Notification(
                    "Your donation was declined ",
                    Notification.TypeNotification.DONATION,
                    Notification.StatutNotification.REFUSEE,
                    admin,
                    donor,
                    donation
            );
            notificationRepository.save(notifDonor);
            notificationsCrees.add(notifDonor);

            // Notification au needy
            Notification notifNeedy = new Notification(
                    "A donation regarding your request has been denied ",
                    Notification.TypeNotification.DONATION,
                    Notification.StatutNotification.REFUSEE,
                    admin,
                    needy,
                    donation
            );
            notificationRepository.save(notifNeedy);
            notificationsCrees.add(notifNeedy);
        }

        return notificationsCrees;
    }

    // 🔹 MÉTHODES UTILITAIRES EXISTANTES
    public List<Donation> listerDonations() {
        return donationRepository.findAll();
    }

    public List<Donation> getDonationsByUser(User user) {
        return donationRepository.findAll().stream()
                .filter(d -> d.getDonor() != null && d.getDonor().getId().equals(user.getId()))
                .collect(Collectors.toList());
    }

    public List<Donation> getDonationsByUserId(String userId) {
        return donationRepository.findAll().stream()
                .filter(d -> d.getDonor() != null && d.getDonor().getId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Donation> getDonationsByStatut(Donation.StatutDonation statut) {
        return donationRepository.findAll().stream()
                .filter(d -> d.getStatus() == statut)
                .collect(Collectors.toList());
    }

    public Donation updateDonation(String id, Donation donationDetails) {
        Donation donation = getDonationById(id);

        donation.setCategorie(donationDetails.getCategorie());
        donation.setRegion(donationDetails.getRegion());
        donation.setDetails(donationDetails.getDetails());
        donation.setImages(donationDetails.getImages());

        return donationRepository.save(donation);
    }

    public void deleteDonation(String id) {
        Donation donation = getDonationById(id);

        // Supprimer notifications liées
        List<Notification> notificationsLiees = notificationRepository.findAll().stream()
                .filter(n -> n.getDonation() != null && n.getDonation().getId().equals(id))
                .collect(Collectors.toList());
        if (!notificationsLiees.isEmpty()) {
            notificationRepository.deleteAll(notificationsLiees);
        }

        donationRepository.delete(donation);
    }

    public List<Donation> filtrerDonations(Donation.Categorie categorie, String region, Donation.StatutDonation statut) {
        return donationRepository.findAll().stream()
                .filter(d -> (categorie == null || d.getCategorie() == categorie))
                .filter(d -> (region == null || (d.getRegion() != null && d.getRegion().equalsIgnoreCase(region))))
                .filter(d -> (statut == null || d.getStatus() == statut))
                .collect(Collectors.toList());
    }

    public boolean donationExistsForPostAndDonor(String postId, String donorId) {
        return donationRepository.findAll().stream()
                .anyMatch(d ->
                        d.getPost() != null && d.getPost().getId().equals(postId) &&
                                d.getDonor() != null && d.getDonor().getId().equals(donorId)
                );
    }

    public long countByStatus(Donation.StatutDonation status) {
        return donationRepository.findAll().stream()
                .filter(d -> d.getStatus() == status)
                .count();
    }

    public List<Donation> getRecentDonations(int limit) {
        return donationRepository.findAll().stream()
                .sorted((d1, d2) -> d2.getDateDonation().compareTo(d1.getDateDonation()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}