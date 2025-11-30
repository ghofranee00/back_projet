package com.iset.projet_integration.Entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "demandes")
//@JsonIgnoreProperties({"user"})

public class Demande {

    @Id
    private String id;

    private String contenu;
    private TypeDemande typeDemande;
    private EtatDemande etat = EtatDemande.EN_ATTENTE;
    private List<String> imageUrls = new ArrayList<>();;
    private List<String> videoUrls = new ArrayList<>();
    private Date dateCreation = new Date();

    // Statistiques
    private int helpCount;
    private int likesCount;
    private int commentsCount;
    private int sharesCount;

    // Référence à l'utilisateur
    @DBRef(lazy = false)
    private User user;

    public enum TypeDemande {
        NOURRITURE,
        LOGEMENT,
        ARGENT,
        VETEMENT,
        EDUCATION,
        SANTE
    }

    public enum EtatDemande {
        EN_ATTENTE,
        ACCEPTEE,
        REFUSEE
    }

    public Demande() {}

    public Demande(String contenu, TypeDemande typeDemande, User user) {
        this.contenu = contenu;
        this.typeDemande = typeDemande;
        this.user = user;
    }

    // Getters & Setters
    public String getId() { return id; }
    public String getContenu() { return contenu; }
    public TypeDemande getTypeDemande() { return typeDemande; }
    public EtatDemande getEtat() { return etat; }
    public List<String> getImageUrls() { return imageUrls; }
    public List<String> getVideoUrls() { return videoUrls; }
    public Date getDateCreation() { return dateCreation; }
    public int getHelpCount() { return helpCount; }
    public int getLikesCount() { return likesCount; }
    public int getCommentsCount() { return commentsCount; }
    public int getSharesCount() { return sharesCount; }
    public User getUser() { return user; }

    public void setId(String id) { this.id = id; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public void setTypeDemande(TypeDemande typeDemande) { this.typeDemande = typeDemande; }
    public void setEtat(EtatDemande etat) { this.etat = etat; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setVideoUrls(List<String> videoUrls) { this.videoUrls = videoUrls; }
    public void setHelpCount(int helpCount) { this.helpCount = helpCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    public void setSharesCount(int sharesCount) { this.sharesCount = sharesCount; }
    public void setUser(User user) { this.user = user; }
}