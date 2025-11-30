package com.iset.projet_integration.Service;

import com.iset.projet_integration.Entities.Demande;
import com.iset.projet_integration.Entities.Notification;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // 🔹 Créer une notification
    public Notification creerNotification(String message,
                                          Notification.TypeNotification type,
                                          Notification.StatutNotification statut,
                                          User expediteur,
                                          User destinataire,
                                          Demande demande) {
        Notification notification = new Notification(message, type, statut, expediteur, destinataire, demande);
        return notificationRepository.save(notification);
    }

    // 🔹 Lister les notifications d'un utilisateur
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByDestinataire(user);
    }

    // 🔹 Récupérer une notification par ID
    public Notification getNotificationById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée avec l'ID: " + id));
    }

    // 🔹 Marquer une notification comme lue
    public Notification marquerCommeLue(String id) {
        Notification notification = getNotificationById(id);
        notification.setLu(true);
        return notificationRepository.save(notification);
    }

    // 🔹 Marquer toutes les notifications comme lues pour un utilisateur
    public void marquerToutesCommeLues(User user) {
        List<Notification> notifications = notificationRepository.findByDestinataire(user);
        notifications.forEach(notification -> notification.setLu(true));
        notificationRepository.saveAll(notifications);
    }

    // 🔹 Supprimer une notification
    public void supprimerNotification(String id) {
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
        } else {
            throw new RuntimeException("Notification non trouvée avec l'ID: " + id);
        }
    }

    // 🔹 Supprimer toutes les notifications d'un utilisateur
    public void supprimerToutesNotifications(User user) {
        List<Notification> notifications = notificationRepository.findByDestinataire(user);
        notificationRepository.deleteAll(notifications);
    }

    // 🔹 Récupérer les notifications non lues d'un utilisateur
    public List<Notification> getNotificationsNonLues(User user) {
        return notificationRepository.findByDestinataire(user).stream()
                .filter(notification -> !notification.isLu())
                .collect(java.util.stream.Collectors.toList());
    }

    // 🔹 Récupérer les notifications par statut
    public List<Notification> getNotificationsByStatut(Notification.StatutNotification statut) {
        return notificationRepository.findByStatut(statut);
    }

    // 🔹 Récupérer les notifications par type
    public List<Notification> getNotificationsByType(Notification.TypeNotification type) {
        return notificationRepository.findAll().stream()
                .filter(notification -> notification.getType() == type)
                .collect(java.util.stream.Collectors.toList());
    }

    // 🔹 Compter les notifications non lues d'un utilisateur
    public long compterNotificationsNonLues(User user) {
        return notificationRepository.findByDestinataire(user).stream()
                .filter(notification -> !notification.isLu())
                .count();
    }

    // 🔹 Mettre à jour une notification
    public Notification updateNotification(String id, String nouveauMessage, Notification.StatutNotification nouveauStatut) {
        Notification notification = getNotificationById(id);
        notification.setMessage(nouveauMessage);
        notification.setStatut(nouveauStatut);
        return notificationRepository.save(notification);
    }
}