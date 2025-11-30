package com.iset.projet_integration.Service;

import com.iset.projet_integration.Entities.Donation;
import com.iset.projet_integration.Entities.Notification;
import com.iset.projet_integration.Entities.Post;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.DonationRepository;
import com.iset.projet_integration.Repository.NotificationRepository;
import com.iset.projet_integration.Repository.PostRepository;
import com.iset.projet_integration.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    // 🔹 Créer une donation
    public Donation creerDonation(Donation donation) {
        if (donation.getDonor() == null || donation.getDonor().getId() == null) {
            throw new RuntimeException("Donor non fourni pour la donation");
        }
        if (donation.getPost() == null || donation.getPost().getId() == null) {
            throw new RuntimeException("Demande non fournie pour la donation");
        }

        // Récupérer le donor complet
        User donor = userRepository.findById(donation.getDonor().getId())
                .orElseThrow(() -> new RuntimeException("Donor introuvable"));
        donation.setDonor(donor);

        // Récupérer la demande complète
        Post post = postRepository.findById(donation.getPost().getId())
                .orElseThrow(() -> new RuntimeException("Demande introuvable"));
        donation.setPost(post);

        // Statut initial
        donation.setStatus(Donation.StatutDonation.EN_ATTENTE);

        Donation saved = donationRepository.save(donation);

        // Notification vers l’ADMIN
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        if (!admins.isEmpty()) {
            User admin = admins.get(0);
            Notification notif = new Notification(
                    "Nouvelle donation reçue de " + donor.getId(),
                    Notification.TypeNotification.DONATION,
                    Notification.StatutNotification.EN_ATTENTE,
                    donor,   // expéditeur
                    admin ,  // destinataire
                    saved
            );
            notificationRepository.save(notif);
        }

        return saved;
    }

    // 🔹 Lister toutes les donations
    public List<Donation> listerDonations() {
        return donationRepository.findAll();
    }

    // 🔹 Traiter une donation (ADMIN : accepter/refuser)
    public List<Notification> traiterDonation(String donationId, String action) {

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation non trouvée avec l'ID: " + donationId));

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
                    "Votre donation a été acceptée ✅",
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
                    "Une donation concernant votre demande a été acceptée ✅",
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
                    "Votre donation a été refusée ❌",
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
                    "Une donation concernant votre demande a été refusée ❌",
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

    // 🔹 Méthodes utilitaires

    public List<Donation> getDonationsByUser(User user) {
        return donationRepository.findAll().stream()
                .filter(d -> d.getDonor() != null && d.getDonor().getId().equals(user.getId()))
                .collect(Collectors.toList());
    }


    public List<Donation> getDonationsByStatut(Donation.StatutDonation statut) {
        return donationRepository.findAll().stream()
                .filter(d -> d.getStatus() == statut)
                .collect(Collectors.toList());
    }

    public Donation getDonationById(String id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation non trouvée avec l'ID: " + id));
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


}
