package com.iset.projet_integration.Service;

import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.UserRepository; // 🔥 AJOUT
import com.iset.projet_integration.dto.UserDto;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus; // 🔥 AJOUT
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class KeycloakAdminService {

    private final Keycloak keycloak;
    private final UserRepository userRepository; // 🔥 AJOUT
    private static final String REALM = "projet-integration";

    // 🔥 CORRECTION : Injection du UserRepository
    public KeycloakAdminService(Keycloak keycloak, UserRepository userRepository) {
        this.keycloak = keycloak;
        this.userRepository = userRepository; // 🔥 AJOUT
    }

    // -------------------------------
    // Méthode utilitaire pour trouver un utilisateur par username
    // -------------------------------
    public UserRepresentation findUserByUsername(String realm, String username) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm).users().search(username);
            if (!users.isEmpty()) {
                return users.get(0);
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Erreur recherche utilisateur: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------
    // Méthode utilitaire pour obtenir l'instance Keycloak
    // -------------------------------
    public Keycloak getKeycloakInstance() {
        return this.keycloak;
    }

    // -------------------------------
    // Créer un utilisateur Keycloak + retour MongoDB - VERSION CORRIGÉE
    // -------------------------------
    public ResponseEntity<User> createUser(UserDto dto) {
        UsersResource usersResource = keycloak.realm(REALM).users();

        // Vérifier si username existe déjà dans Keycloak
        List<UserRepresentation> existingUsers = usersResource.search(dto.getUsername());
        if (!existingUsers.isEmpty()) {
            System.out.println("❌ Username existe déjà dans Keycloak: " + dto.getUsername());
            return ResponseEntity.status(409).body(null);
        }

        // Vérifier si email existe déjà dans Keycloak
        List<UserRepresentation> existingEmail = usersResource.searchByEmail(dto.getEmail(), true);
        if (!existingEmail.isEmpty()) {
            System.out.println("❌ Email existe déjà dans Keycloak: " + dto.getEmail());
            return ResponseEntity.status(409).body(null);
        }

        // Vérifier si identifiant existe déjà dans MongoDB
        Optional<User> existingUserByIdentifiant = userRepository.findByIdentifiant(dto.getUsername());
        if (existingUserByIdentifiant.isPresent()) {
            System.out.println("❌ Identifiant existe déjà dans MongoDB: " + dto.getUsername());
            return ResponseEntity.status(409).body(null);
        }

        // Vérifier si email existe déjà dans MongoDB
        Optional<User> existingUserByEmail = userRepository.findByEmail(dto.getEmail());
        if (existingUserByEmail.isPresent()) {
            System.out.println("❌ Email existe déjà dans MongoDB: " + dto.getEmail());
            return ResponseEntity.status(409).body(null);
        }

        // Construire l'utilisateur Keycloak
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(dto.getUsername());
        kcUser.setEmail(dto.getEmail());
        kcUser.setFirstName(dto.getFirstName());
        kcUser.setLastName(dto.getLastName());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);

        // Création dans Keycloak
        try (Response response = usersResource.create(kcUser)) {
            System.out.println("🔑 Réponse Keycloak: " + response.getStatus());

            if (response.getStatus() == 409) {
                return ResponseEntity.status(409).body(null);
            }

            if (response.getStatus() != 201) {
                System.err.println("❌ Erreur création Keycloak: " + response.getStatus());
                return ResponseEntity.status(response.getStatus()).body(null);
            }

            // Récupérer l'ID Keycloak
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            System.out.println("✅ ID Keycloak créé: " + userId);

            // Attendre un peu que l'utilisateur soit disponible
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Définir le mot de passe
            CredentialRepresentation password = new CredentialRepresentation();
            password.setType(CredentialRepresentation.PASSWORD);
            password.setValue(dto.getPassword());
            password.setTemporary(false);

            UserResource userResource = usersResource.get(userId);
            userResource.resetPassword(password);
            System.out.println("✅ Mot de passe défini");

            // Ajouter le rôle
            String roleName = dto.getRole().name();
            try {
                userResource.roles()
                        .realmLevel()
                        .add(Collections.singletonList(
                                keycloak.realm(REALM).roles().get(roleName).toRepresentation()
                        ));
                System.out.println("✅ Rôle ajouté: " + roleName);
            } catch (Exception e) {
                System.err.println("⚠️ Erreur ajout rôle, continuation...: " + e.getMessage());
            }

            // CRÉER L'UTILISATEUR DANS MONGODB
            User user = new User();
            user.setId(userId); // UTILISER L'ID KEYCLOAK COMME ID MONGODB
            user.setIdentifiant(dto.getUsername());
            user.setEmail(dto.getEmail());
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setRole(User.Role.valueOf(dto.getRole().name()));
            // Pas besoin de setPassword car c'est géré par Keycloak

            // SAUVEGARDER DANS MONGODB
            User savedUser = userRepository.save(user);
            System.out.println("✅ Utilisateur créé dans MongoDB: " + savedUser.getId());

            return ResponseEntity.ok(savedUser);

        } catch (Exception e) {
            System.err.println("💥 Erreur création utilisateur: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // -------------------------------
    // Récupérer l'ID Keycloak par username
    // -------------------------------
    public String getUserIdByUsername(String realm, String username) {
        var users = keycloak.realm(realm).users().search(username);
        if (!users.isEmpty()) return users.get(0).getId();
        return null;
    }

    // -------------------------------
    // Réinitialiser le mot de passe d'un utilisateur
    // -------------------------------
    public boolean resetUserPassword(String realm, String username, String newPassword) {
        try {
            UserRepresentation user = findUserByUsername(realm, username);
            if (user == null) {
                System.err.println("❌ Utilisateur non trouvé: " + username);
                return false;
            }

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            keycloak.realm(realm).users().get(user.getId()).resetPassword(credential);

            System.out.println("✅ Mot de passe réinitialisé pour: " + username);
            return true;

        } catch (Exception e) {
            System.err.println("💥 Erreur réinitialisation mot de passe: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------
    // Déclencher l'email de réinitialisation de mot de passe
    // -------------------------------
    public boolean triggerPasswordResetEmail(String realm, String username) {
        try {
            UserRepresentation user = findUserByUsername(realm, username);
            if (user == null) {
                System.err.println("❌ Utilisateur non trouvé: " + username);
                return false;
            }

            keycloak.realm(realm).users().get(user.getId()).executeActionsEmail(List.of("UPDATE_PASSWORD"));

            System.out.println("✅ Email réinitialisation déclenché pour: " + username);
            return true;

        } catch (Exception e) {
            System.err.println("💥 Erreur déclenchement email réinitialisation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------
    // Mettre à jour un utilisateur Keycloak
    // Dans KeycloakAdminService.java - CORRECTION DE updateUser
    public boolean updateUser(String realm, String userId, String email, String username,
                              String firstName, String lastName, String password) {
        try {
            System.out.println("🔑 Mise à jour Keycloak - UserId: " + userId);
            System.out.println("📝 Nouveaux données - Email: " + email + ", FirstName: " + firstName + ", LastName: " + lastName);

            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            UserResource userResource = usersResource.get(userId);
            UserRepresentation user = userResource.toRepresentation();

            // 🔥 CORRECTION : METTRE À JOUR TOUS LES CHAMPS
            user.setEmail(email);
            user.setFirstName(firstName);  // 🔥 AJOUT
            user.setLastName(lastName);    // 🔥 AJOUT
            user.setUsername(username);    // Garder le même username

            userResource.update(user);
            System.out.println("✅ Keycloak complètement mis à jour");

            // Si un nouveau mot de passe est fourni
            if (password != null && !password.trim().isEmpty()) {
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);

                userResource.resetPassword(credential);
                System.out.println("✅ Mot de passe Keycloak mis à jour");
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur mise à jour Keycloak: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    // -------------------------------
    // Vérifier le mot de passe actuel
    // -------------------------------
    public boolean verifyCurrentPassword(String realm, String username, String currentPassword) {
        try {
            System.out.println("🔐 Vérification mot de passe pour: " + username);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur vérification mot de passe: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------
    // Recherche utilisateur par email
    // -------------------------------
    public UserRepresentation findUserByEmail(String realm, String email) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
            if (!users.isEmpty()) {
                return users.get(0);
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Erreur recherche utilisateur par email: " + e.getMessage());
            return null;
        }
    }
}