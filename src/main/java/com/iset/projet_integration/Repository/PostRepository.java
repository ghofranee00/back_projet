package com.iset.projet_integration.Repository;

import com.iset.projet_integration.Entities.Demande;
import com.iset.projet_integration.Entities.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    // ✅ EXISTANT
    List<Post> findAllByOrderByDateCreationDesc();

    // ✅ EXISTANT
    List<Post> findByTypeDemande(Demande.TypeDemande typeDemande);

    // ✅ EXISTANT
    List<Post> findByUserId(String userId);

    // ✅ EXISTANT
    @Query("{ 'typeDemande': ?0, 'region': ?1 }")
    List<Post> findByTypeDemandeStringAndRegion(String type, String region);

    // 🆕 AJOUTER pour optimiser les likes
    @Query("{ '_id': ?0, 'likedByUserIds': ?1 }")
    boolean existsByIdAndLikedByUserIdsContains(String postId, String userId);
}