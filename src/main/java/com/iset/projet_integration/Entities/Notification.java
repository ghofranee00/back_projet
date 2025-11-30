package com.iset.projet_integration.Entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "notifications")
//@JsonIgnoreProperties({"demande", "post"})

public class Notification {

    @Id
    private String id;

    private String message;
    private boolean lu = false;
    private Date dateEnvoi = new Date();
    private TypeNotification type;
    private StatutNotification statut = StatutNotification.EN_ATTENTE;

    // Références
    @DBRef(lazy = false)
    private Demande demande;

    @DBRef(lazy = false)
    private Post post; // ajouter si tu veux lier à un post
    @DBRef(lazy = false)
    private Donation donation;
    @DBRef(lazy = false)
    private User destinataire;

    @DBRef(lazy = false)
    private User expediteur;

    public Notification() {}

    public Notification(String message, TypeNotification type, StatutNotification statut,
                        User expediteur, User destinataire, Demande demande ) {
        this.message = message;
        this.type = type;
        this.statut = statut;
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.demande = demande;


    }
    // constructeur pour notification liée à une donation
    public Notification(String message, TypeNotification type, StatutNotification statut,
                        User expediteur, User destinataire, Donation donation) {
        this.message = message;
        this.type = type;
        this.statut = statut;
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.donation = donation;
    }
    // constructeur pour notification liée à un post
    public Notification(String message, TypeNotification type, StatutNotification statut,
                        User expediteur, User destinataire, Post post) {
        this.message = message;
        this.type = type;
        this.statut = statut;
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.post = post;
    }

    public Notification(String message, TypeNotification type, StatutNotification statut,
                        User expediteur, User destinataire) {
        this.message = message;
        this.type = type;
        this.statut = statut;
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.demande = null;
        this.post = null;
    }

    public Notification(String id, String message, boolean lu, Date dateEnvoi, TypeNotification type, StatutNotification statut, Demande demande, Post post, User destinataire, Donation donation, User expediteur) {
        this.id = id;
        this.message = message;
        this.lu = lu;
        this.dateEnvoi = dateEnvoi;
        this.type = type;
        this.statut = statut;
        this.demande = demande;
        this.post = post;
        this.destinataire = destinataire;
        this.donation = donation;
        this.expediteur = expediteur;
    }

    public enum TypeNotification {
        NOURRITURE,
        LOGEMENT,
        VETEMENT,
        EDUCATION,
        SANTE,
        ARGENT, // AJOUTER
        HABILLEMENT,
        MEDICAMENT ,
        DONATION

    }

    public enum StatutNotification {
        EN_ATTENTE,
        ACCEPTEE,
        REFUSEE
    }
    public Donation getDonation() {
        return donation;
    }

    public void setDonation(Donation donation) {
        this.donation = donation;
    }
    // Getters & Setters
    public String getId() { return id; }
    public String getMessage() { return message; }
    public boolean isLu() { return lu; }
    public Date getDateEnvoi() { return dateEnvoi; }
    public TypeNotification getType() { return type; }
    public StatutNotification getStatut() { return statut; }
    public Demande getDemande() { return demande; }
    public User getDestinataire() { return destinataire; }
    public User getExpediteur() { return expediteur; }

    public void setId(String id) { this.id = id; }
    public void setMessage(String message) { this.message = message; }
    public void setLu(boolean lu) { this.lu = lu; }
    public void setType(TypeNotification type) { this.type = type; }
    public void setStatut(StatutNotification statut) { this.statut = statut; }
    public void setDemande(Demande demande) { this.demande = demande; }
    public void setDestinataire(User destinataire) { this.destinataire = destinataire; }
    public void setExpediteur(User expediteur) { this.expediteur = expediteur; }
}