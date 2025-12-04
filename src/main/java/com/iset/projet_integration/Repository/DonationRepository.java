package com.iset.projet_integration.Repository;

import com.iset.projet_integration.Entities.Donation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends MongoRepository<Donation, String> {

    // ✅ Trouver les donations par ID de post (via l'objet Post)
    @Query("{ 'post.$id': ?0 }")
    List<Donation> findByPostId(String postId);

    // ✅ Trouver les donations pour plusieurs posts (pour les needy)
    @Query("{ 'post.$id': { $in: ?0 } }")
    List<Donation> findByPostIdIn(List<String> postIds);

    // ✅ Trouver les donations par ID de donateur (via l'objet User/donor)
    @Query("{ 'donor.$id': ?0 }")
    List<Donation> findByDonorId(String donorId);

    // ✅ Trouver les donations par statut
    List<Donation> findByStatus(Donation.StatutDonation status);

    // ✅ Trouver les donations par catégorie
    List<Donation> findByCategorie(Donation.Categorie categorie);

    // ✅ Trouver les donations par région
    List<Donation> findByRegion(String region);

    // ✅ Trouver les donations par statut et région
    List<Donation> findByStatusAndRegion(Donation.StatutDonation status, String region);

    // ✅ Trouver les donations par statut et catégorie
    List<Donation> findByStatusAndCategorie(Donation.StatutDonation status, Donation.Categorie categorie);

    // ✅ Vérifier si une donation existe pour un post et un donateur
    @Query("{ 'post.$id': ?0, 'donor.$id': ?1 }")
    boolean existsByPostIdAndDonorId(String postId, String donorId);

    // ✅ Compter les donations par post
    @Query(value = "{ 'post.$id': ?0 }", count = true)
    Long countByPostId(String postId);

    // ✅ Compter les donations par donateur
    @Query(value = "{ 'donor.$id': ?0 }", count = true)
    Long countByDonorId(String donorId);

    // ✅ Compter les donations par statut
    Long countByStatus(Donation.StatutDonation status);

    // ✅ Query personnalisée pour filtrer (pour ta méthode filtrerDonations)
    @Query("{ 'categorie': ?0, 'region': ?1, 'status': ?2 }")
    List<Donation> findByCategorieAndRegionAndStatus(
            Donation.Categorie categorie,
            String region,
            Donation.StatutDonation status
    );

    // ✅ Trouver les donations d'un donateur triées par date
    @Query("{ 'donor.$id': ?0 }")
    List<Donation> findByDonorIdOrderByDateDonationDesc(String donorId);

    // ✅ Trouver les donations par statut et donateur, triées par date
    @Query("{ 'status': ?0, 'donor.$id': ?1 }")
    List<Donation> findByStatusAndDonorIdOrderByDateDonationDesc(
            Donation.StatutDonation status,
            String donorId
    );
}