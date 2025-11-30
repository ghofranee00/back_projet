package com.iset.projet_integration.Controller;

import com.iset.projet_integration.Entities.Notification;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.UserRepository;
import com.iset.projet_integration.Service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // 🔹 Récupérer toutes les notifications d'un utilisateur
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()") // ✅ Tous les utilisateurs authentifiés

    public List<Notification> getNotificationsByUser(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        return notificationService.getNotificationsByUser(user);
    }

    // 🔹 Récupérer les notifications non lues d'un utilisateur
    @GetMapping("/user/{userId}/non-lues")
    @PreAuthorize("isAuthenticated()") // ✅ Tous les utilisateurs authentifiés

    public List<Notification> getNotificationsNonLues(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        return notificationService.getNotificationsNonLues(user);
    }

    // 🔹 Marquer une notification comme lue
    @PutMapping("/{id}/marquer-lue")
    @PreAuthorize("isAuthenticated()") // ✅ Tous les utilisateurs authentifiés

    public Notification marquerCommeLue(@PathVariable String id) {
        return notificationService.marquerCommeLue(id);
    }

    // 🔹 Marquer toutes les notifications comme lues pour un utilisateur
    @PutMapping("/user/{userId}/marquer-toutes-lues")
    @PreAuthorize("isAuthenticated()") // ✅ Tous les utilisateurs authentifiés

    public String marquerToutesCommeLues(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        notificationService.marquerToutesCommeLues(user);
        return "Toutes les notifications marquées comme lues pour l'utilisateur " + userId;
    }

    // 🔹 Compter les notifications non lues
    @GetMapping("/user/{userId}/count-non-lues")
    @PreAuthorize("isAuthenticated()") // ✅ Tous les utilisateurs authentifiés

    public long compterNotificationsNonLues(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        return notificationService.compterNotificationsNonLues(user);
    }

    // 🔹 Supprimer une notification
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // ✅ Tous les utilisateurs authentifiés

    public String supprimerNotification(@PathVariable String id) {
        notificationService.supprimerNotification(id);
        return "Notification " + id + " supprimée avec succès";
    }

    // 🔹 Supprimer toutes les notifications d'un utilisateur
    @DeleteMapping("/user/{userId}/toutes")
    @PreAuthorize("isAuthenticated()") // ✅ Tous les utilisateurs authentifiés

    public String supprimerToutesNotifications(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        notificationService.supprimerToutesNotifications(user);
        return "Toutes les notifications supprimées pour l'utilisateur " + userId;
    }

    // 🔹 Récupérer une notification par ID
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // ✅ Tous les utilisateurs authentifiés

    public Notification getNotificationById(@PathVariable String id) {
        return notificationService.getNotificationById(id);
    }
}