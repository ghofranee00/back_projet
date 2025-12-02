package com.iset.projet_integration.Repository;

import com.iset.projet_integration.Entities.Donation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends MongoRepository<Donation, String> {

    // Trouver par donor
    List<Donation> findByDonorId(String donorId);

    // Trouver par statut
    List<Donation> findByStatus(Donation.StatutDonation status);

    // Trouver par post
    List<Donation> findByPostId(String postId);

    // Filtrage avancé avec @Query
    @Query("SELECT d FROM Donation d WHERE " +
            "(:categorie IS NULL OR d.categorie = :categorie) AND " +
            "(:region IS NULL OR d.region = :region) AND " +
            "(:status IS NULL OR d.status = :status)")
    List<Donation> findByCategorieAndRegionAndStatus(
            @Param("categorie") Donation.Categorie categorie,
            @Param("region") String region,
            @Param("status") Donation.StatutDonation status);
}