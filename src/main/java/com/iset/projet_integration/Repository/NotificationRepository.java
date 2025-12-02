package com.iset.projet_integration.Repository;

import com.iset.projet_integration.Entities.Notification;
import com.iset.projet_integration.Entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByDestinataire(User destinataire);
    List<Notification> findByLu(boolean lu);
    List<Notification> findByStatut(Notification.StatutNotification statut);
    List<Notification> findByDonationId(String donationId);

}