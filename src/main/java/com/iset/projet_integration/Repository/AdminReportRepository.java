package com.iset.projet_integration.Repository;


import com.iset.projet_integration.Entities.Donation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminReportRepository extends MongoRepository<Donation, String> {

    // Compter toutes les donations
    long count();

    // Récupérer toutes les donations par catégorie
    List<Donation> findByCategorie(Donation.Categorie categorie);

    // Récupérer toutes les donations par région
    List<Donation> findByRegion(String region);

    // Tu peux ajouter d'autres méthodes de filtrage ici si besoin
}
