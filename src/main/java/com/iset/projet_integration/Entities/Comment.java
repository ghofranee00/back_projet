package com.iset.projet_integration.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "comments")
public class Comment {

    @Id
    private String id;
    private String contenu;
    private Date dateCreation = new Date();
    private Date dateModification;

    @DBRef
    private User user;

    @DBRef
    private Post post;

    public Comment() {}

    public Comment(String contenu, User user, Post post) {
        this.contenu = contenu;
        this.user = user;
        this.post = post;
    }

    public void updateContent(String newContent) {
        this.contenu = newContent;
        this.dateModification = new Date();
    }

    // getters & setters
    public String getId() { return id; }
    public String getContenu() { return contenu; }
    public Date getDateCreation() { return dateCreation; }
    public Date getDateModification() { return dateModification; }
    public User getUser() { return user; }
    public Post getPost() { return post; }
    public void setId(String id) { this.id = id; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public void setUser(User user) { this.user = user; }
    public void setPost(Post post) { this.post = post; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }
    public void setDateModification(Date dateModification) { this.dateModification = dateModification; }
}
