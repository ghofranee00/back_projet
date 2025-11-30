package com.iset.projet_integration.Repository;

import com.iset.projet_integration.Entities.Demande;
import com.iset.projet_integration.Entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DemandeRepository extends MongoRepository<Demande, String> {
    List<Demande> findByUser(User user);
    List<Demande> findByEtat(Demande.EtatDemande etat);
    List<Demande> findByUserAndEtat(User user, Demande.EtatDemande etat);
}