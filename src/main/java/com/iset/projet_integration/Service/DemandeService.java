package com.iset.projet_integration.Service;

import com.iset.projet_integration.Entities.Demande;
import com.iset.projet_integration.Entities.Notification;
import com.iset.projet_integration.Entities.Post;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.DemandeRepository;
import com.iset.projet_integration.Repository.NotificationRepository;
import com.iset.projet_integration.Repository.PostRepository;
import com.iset.projet_integration.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public DemandeService(DemandeRepository demandeRepository,
                          NotificationRepository notificationRepository,
                          UserRepository userRepository,
                          PostRepository postRepository) {
        this.demandeRepository = demandeRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    // ============================
    // MÉTHODES POUR LE NEEDY
    // ============================

    // 🔥 Service pour récupérer les demandes du NEEDY connecté
    public List<Demande> getMesDemandesEnAttente(String username) {
        // Récupérer l'utilisateur par son username
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

        System.out.println("Récupération des demandes en attente pour: " + user.getIdentifiant());

        // Récupérer les demandes de cet utilisateur avec état EN_ATTENTE
        return demandeRepository.findByUserAndEtat(user, Demande.EtatDemande.EN_ATTENTE);
    }

    // 🔥 Service pour récupérer toutes les demandes du NEEDY connecté
    public List<Demande> getMesDemandes(String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

        System.out.println("Récupération de toutes les demandes pour: " + user.getIdentifiant());

        return demandeRepository.findByUser(user);
    }

    // 🔥 Service pour récupérer une demande spécifique (vérification de propriété)
    public Demande getMaDemande(String id, String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée: " + id));

        // Vérifier que la demande appartient bien à l'utilisateur
        if (!demande.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Accès refusé: Cette demande ne vous appartient pas");
        }

        return demande;
    }

    // 🔥 NOUVELLE MÉTHODE: Update pour needy avec username
    public Demande updateDemandeNeedy(String id, String username, Demande demandeDetails) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

        return updateDemandeNeedy(id, user, demandeDetails);
    }

    // 🔥 NOUVELLE MÉTHODE: Delete pour needy avec username
    public void deleteDemandeNeedy(String id, String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

        deleteDemandeNeedy(id, user);
    }

    // ============================
    // MÉTHODES DE CRÉATION
    // ============================

    // 🔹 Ajouter une demande simple
    public Demande creerDemande(Demande demande) {
        demande.setEtat(Demande.EtatDemande.EN_ATTENTE);
        Demande saved = demandeRepository.save(demande);

        // Notification vers l'admin
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        if (!admins.isEmpty()) {
            Notification notif = new Notification();
            notif.setMessage("Nouvelle demande reçue de " + demande.getUser().getIdentifiant());
            notif.setType(Notification.TypeNotification.valueOf(demande.getTypeDemande().name()));
            notif.setStatut(Notification.StatutNotification.EN_ATTENTE);
            notif.setExpediteur(demande.getUser());
            notif.setDestinataire(admins.get(0));
            notif.setDemande(saved);
            notificationRepository.save(notif);
        }
        return saved;
    }

    // 🔹 CORRIGÉ : Ajouter une demande avec images/vidéos
    public Demande creerDemandeAvecFichiers(Demande demande,
                                            List<MultipartFile> images,
                                            List<MultipartFile> videos,
                                            String userIdKeycloak) {

        // CORRECTION : Récupérer l'utilisateur par ID au lieu de identifiant
        User user = userRepository.findById(userIdKeycloak)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userIdKeycloak));

        demande.setUser(user); // 🔥 Assigner l'utilisateur

        // Convertir les fichiers en chemins (ou URL)
        if (images != null) {
            List<String> imagePaths = images.stream()
                    .map(this::saveFile)
                    .collect(Collectors.toList());
            demande.setImageUrls(imagePaths);
        }

        if (videos != null) {
            List<String> videoPaths = videos.stream()
                    .map(this::saveFile)
                    .collect(Collectors.toList());
            demande.setVideoUrls(videoPaths);
        }

        return creerDemande(demande);
    }

    // Méthode fictive pour stocker un fichier et retourner son chemin
    private String saveFile(MultipartFile file) {
        return file.getOriginalFilename();
    }

    // ============================
    // MÉTHODES GÉNÉRALES
    // ============================

    public List<Demande> listerDemandes() {
        return demandeRepository.findAll();
    }

    public Demande getDemandeById(String id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'ID: " + id));
    }

    public List<Demande> getDemandesByEtat(Demande.EtatDemande etat) {
        return demandeRepository.findByEtat(etat);
    }

    public Demande updateDemande(String id, Demande demandeDetails) {
        Demande demande = getDemandeById(id);
        demande.setContenu(demandeDetails.getContenu());
        demande.setTypeDemande(demandeDetails.getTypeDemande());

        // Mettre à jour images/videos seulement si fournis
        if (demandeDetails.getImageUrls() != null) {
            demande.setImageUrls(demandeDetails.getImageUrls());
        }
        if (demandeDetails.getVideoUrls() != null) {
            demande.setVideoUrls(demandeDetails.getVideoUrls());
        }

        return demandeRepository.save(demande);
    }

    public void deleteDemande(String id) {
        List<Notification> notifications = notificationRepository.findAll().stream()
                .filter(notif -> notif.getDemande() != null && notif.getDemande().getId().equals(id))
                .collect(Collectors.toList());
        notificationRepository.deleteAll(notifications);
        demandeRepository.deleteById(id);
    }

    // ============================
    // MÉTHODES POUR LE NEEDY (avec objet User)
    // ============================

    public Demande updateDemandeNeedy(String id, User user, Demande demandeDetails) {
        Demande demande = getDemandeById(id);

        // Vérifications de sécurité
        if (!demande.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à modifier cette demande.");
        }
        if (demande.getEtat() != Demande.EtatDemande.EN_ATTENTE) {
            throw new RuntimeException("Seules les demandes EN_ATTENTE peuvent être modifiées.");
        }

        // Mettre à jour SEULEMENT les champs autorisés
        if (demandeDetails.getContenu() != null) {
            demande.setContenu(demandeDetails.getContenu());
        }
        if (demandeDetails.getTypeDemande() != null) {
            demande.setTypeDemande(demandeDetails.getTypeDemande());
        }

        // NE PAS mettre à jour images/videos si null (garder les existants)
        if (demandeDetails.getImageUrls() != null) {
            demande.setImageUrls(demandeDetails.getImageUrls());
        }
        if (demandeDetails.getVideoUrls() != null) {
            demande.setVideoUrls(demandeDetails.getVideoUrls());
        }

        return demandeRepository.save(demande);
    }

    public void deleteDemandeNeedy(String id, User user) {
        Demande demande = getDemandeById(id);

        // Vérifications de sécurité
        if (!demande.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer cette demande.");
        }
        if (demande.getEtat() != Demande.EtatDemande.EN_ATTENTE) {
            throw new RuntimeException("Seules les demandes EN_ATTENTE peuvent être supprimées.");
        }

        // 🔥 CORRECTION: Supprimer d'abord les notifications manuellement
        try {
            // Récupérer les IDs des notifications problématiques
            List<Notification> problematicNotifications = notificationRepository.findAll().stream()
                    .filter(notif -> {
                        try {
                            return notif.getDemande() != null && notif.getDemande().getId().equals(id);
                        } catch (Exception e) {
                            System.out.println("⚠️ Notification problématique ignorée: " + e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            // Supprimer chaque notification individuellement
            for (Notification notification : problematicNotifications) {
                try {
                    notificationRepository.delete(notification);
                } catch (Exception e) {
                    System.out.println("⚠️ Impossible de supprimer une notification: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Erreur lors du nettoyage des notifications: " + e.getMessage());
        }

        // Supprimer la demande
        demandeRepository.delete(demande);
    }
    // ============================
    // TRAITEMENT DES DEMANDES (ADMIN)
    // ============================

    public List<Notification> traiterDemande(String demandeId, String action) {
        Demande demande = getDemandeById(demandeId);
        User needy = demande.getUser();

        // Récupérer un admin (avec vérification)
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        if (admins.isEmpty()) {
            throw new RuntimeException("Aucun administrateur trouvé");
        }
        User admin = admins.get(0);

        List<Notification> notificationsCrees = new ArrayList<>();

        if (action.equalsIgnoreCase("accepter")) {
            demande.setEtat(Demande.EtatDemande.ACCEPTEE);
            demandeRepository.save(demande);

            // Créer Post avec images et vidéos
            Post post = new Post();
            post.setContenu(demande.getContenu());
            post.setUser(needy);
            post.setImageUrls(demande.getImageUrls());
            post.setVideoUrls(demande.getVideoUrls());
            postRepository.save(post);

            // Notification pour le needy
            Notification notifNeedy = new Notification();
            notifNeedy.setMessage("Your request has been accepted ");
            notifNeedy.setType(Notification.TypeNotification.valueOf(demande.getTypeDemande().name()));
            notifNeedy.setStatut(Notification.StatutNotification.ACCEPTEE);
            notifNeedy.setExpediteur(admin);
            notifNeedy.setDestinataire(needy);
            notifNeedy.setDemande(demande);
            notificationRepository.save(notifNeedy);
            notificationsCrees.add(notifNeedy);

            // Notifications pour Donor et Association
            List<User> recepteurs = new ArrayList<>();
            recepteurs.addAll(userRepository.findByRole(User.Role.DONNATEUR));
            recepteurs.addAll(userRepository.findByRole(User.Role.ASSOCIATION));

            for (User user : recepteurs) {
                Notification notifDon = new Notification();
                notifDon.setMessage("New request published : " + demande.getContenu());
                notifDon.setType(Notification.TypeNotification.valueOf(demande.getTypeDemande().name()));
                notifDon.setStatut(Notification.StatutNotification.ACCEPTEE);
                notifDon.setExpediteur(needy);
                notifDon.setDestinataire(user);
                notificationRepository.save(notifDon);
                notificationsCrees.add(notifDon);
            }

            // Supprimer la demande initiale
            demandeRepository.delete(demande);

        } else if (action.equalsIgnoreCase("refuser")) {
            demande.setEtat(Demande.EtatDemande.REFUSEE);
            demandeRepository.save(demande);

            // Notification pour le needy
            Notification notifRefus = new Notification();
            notifRefus.setMessage("Your request '" + demande.getContenu() + "' was denied ");
            notifRefus.setType(Notification.TypeNotification.valueOf(demande.getTypeDemande().name()));
            notifRefus.setStatut(Notification.StatutNotification.REFUSEE);
            notifRefus.setExpediteur(admin);
            notifRefus.setDestinataire(needy);
            notifRefus.setDemande(demande);
            notificationRepository.save(notifRefus);
            notificationsCrees.add(notifRefus);

            // Supprimer la demande
            demandeRepository.delete(demande);
        }

        return notificationsCrees;
    }
}