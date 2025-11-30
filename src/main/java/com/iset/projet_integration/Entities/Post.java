package com.iset.projet_integration.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "posts")
public class Post {

    @Id
    private String id;

    private String contenu;

    private Demande.TypeDemande typeDemande;

    private Date dateCreation = new Date();

    // DBRef pour référencer un utilisateur existant
    @DBRef(lazy = false)
    private User user;

    private List<String> imageUrls = new ArrayList<>();
    private List<String> videoUrls = new ArrayList<>();

    private int likesCount = 0;
    private int commentsCount = 0;
    private List<String> likedByUserIds = new ArrayList<>();

    // 🔹 Constructeur vide (obligatoire pour MongoDB)
    public Post() {}

    // 🔹 Constructeur basique
    public Post(String contenu, Demande.TypeDemande typeDemande, User user) {
        this.contenu = contenu;
        this.typeDemande = typeDemande;
        this.user = user;
        this.dateCreation = new Date();
    }

    // 🔹 Constructeur complet
    public Post(String contenu, Demande.TypeDemande typeDemande, User user,
                List<String> imageUrls, List<String> videoUrls) {
        this.contenu = contenu;
        this.typeDemande = typeDemande;
        this.user = user;
        this.dateCreation = new Date();
        if (imageUrls != null) this.imageUrls = imageUrls;
        if (videoUrls != null) this.videoUrls = videoUrls;
    }

    // 🔹 Méthodes like / unlike
    public boolean toggleLike(String userId) {
        if (likedByUserIds.contains(userId)) {
            likedByUserIds.remove(userId);
            likesCount = Math.max(0, likesCount - 1);
            return false;
        } else {
            likedByUserIds.add(userId);
            likesCount++;
            return true;
        }
    }

    public boolean hasUserLiked(String userId) {
        return likedByUserIds.contains(userId);
    }

    // 🔹 Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Demande.TypeDemande getTypeDemande() { return typeDemande; }
    public void setTypeDemande(Demande.TypeDemande typeDemande) { this.typeDemande = typeDemande; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<String> getVideoUrls() { return videoUrls; }
    public void setVideoUrls(List<String> videoUrls) { this.videoUrls = videoUrls; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public List<String> getLikedByUserIds() { return likedByUserIds; }
    public void setLikedByUserIds(List<String> likedByUserIds) { this.likedByUserIds = likedByUserIds; }
}
