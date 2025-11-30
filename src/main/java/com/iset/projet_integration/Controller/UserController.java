package com.iset.projet_integration.Controller;

import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.UserRepository;
import com.iset.projet_integration.Service.KeycloakAdminService;
import com.iset.projet_integration.dto.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    public UserController(UserRepository userRepository, KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
    }


    // -------------------------------
    // Ajouter un utilisateur (Mongo + Keycloak)
    // -------------------------------
    @PostMapping("/ajouter")
    public ResponseEntity<User> ajouter(@RequestBody UserDto dto) {

        // Vérifier d'abord dans MongoDB avec Optional
        Optional<User> existingUserByEmail = userRepository.findByEmail(dto.getEmail());
        if (existingUserByEmail.isPresent()) {
            return ResponseEntity.status(409).body(null);
        }

        // Vérifier si l'identifiant existe déjà
        Optional<User> existingUserByIdentifiant = userRepository.findByIdentifiant(dto.getUsername());
        if (existingUserByIdentifiant.isPresent()) {
            return ResponseEntity.status(409).body(null);
        }

        ResponseEntity<User> keycloakResponse = keycloakAdminService.createUser(dto);

        if (keycloakResponse.getStatusCode() == HttpStatus.CONFLICT) {
            return ResponseEntity.status(409).body(null);
        }

        if (!keycloakResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(keycloakResponse.getStatusCode()).build();
        }

        // Enregistrer dans MongoDB
        User savedUser = userRepository.save(keycloakResponse.getBody());
        return ResponseEntity.ok(savedUser);
    }

    // -------------------------------
    // Lister tous les utilisateurs
    // -------------------------------
    @GetMapping("/liste")
    public ResponseEntity<List<User>> liste() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // -------------------------------
    // Mettre à jour le profil utilisateur (Mongo + Keycloak) - AMÉLIORÉ
    // -------------------------------
    // UserController.java - VERSION CORRIGÉE
    // UserController.java - VERSION CORRIGÉE
    // Dans UserController.java - CORRECTION DE updateUserProfile
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUserProfile(@PathVariable String id, @RequestBody User userDetails) {
        System.out.println("🔄 Début mise à jour profil pour ID: " + id);
        System.out.println("📝 Données reçues - FirstName: " + userDetails.getFirstName() +
                ", LastName: " + userDetails.getLastName() +
                ", Email: " + userDetails.getEmail());

        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            System.out.println("❌ Utilisateur non trouvé en base: " + id);
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        // Sauvegarder les anciennes valeurs pour les logs
        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();
        String oldEmail = user.getEmail();

        // Mettre à jour les informations
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setEmail(userDetails.getEmail());

        User updated = userRepository.save(user);
        System.out.println("✅ Profil MongoDB mis à jour: " + updated.getIdentifiant());
        System.out.println("📊 Avant -> Prénom: " + oldFirstName + ", Nom: " + oldLastName + ", Email: " + oldEmail);
        System.out.println("📊 Après -> Prénom: " + updated.getFirstName() + ", Nom: " + updated.getLastName() + ", Email: " + updated.getEmail());

        // 🔥 CORRECTION : SYNCHRONISATION KEYCLOAK COMPLÈTE
        try {
            System.out.println("🔄 Synchronisation Keycloak...");

            // Utiliser l'ID directement (puisque c'est le même que Keycloak)
            boolean keycloakUpdated = keycloakAdminService.updateUser(
                    "projet-integration",
                    id, // ID Keycloak (identique à MongoDB)
                    user.getEmail(),          // Nouvel email
                    user.getIdentifiant(),    // Username (inchangé)
                    user.getFirstName(),      // 🔥 NOUVEAU: Prénom
                    user.getLastName(),       // 🔥 NOUVEAU: Nom
                    null                      // Password null = pas de modification
            );

            if (keycloakUpdated) {
                System.out.println("✅ Keycloak complètement synchronisé (email, prénom, nom)");
            } else {
                System.err.println("❌ Échec synchronisation Keycloak");
            }

        } catch (Exception e) {
            System.err.println("💥 Erreur synchronisation Keycloak: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok(updated);
    }
    // 🔥 CORRIGÉ : Mettre à jour la photo de profil
    // -------------------------------
    // UserController.java - MODIFIEZ la méthode updateProfilePhoto
    // UserController.java - Version corrigée
    @PutMapping("/{id}/photo")
    public ResponseEntity<User> updateProfilePhoto(
            @PathVariable String id,
            @RequestParam("photo") MultipartFile photoFile) {

        System.out.println("📸 Début upload photo Base64 pour user: " + id);

        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            System.out.println("❌ Utilisateur non trouvé: " + id);
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        try {
            if (photoFile.isEmpty()) {
                System.out.println("❌ Fichier vide");
                return ResponseEntity.badRequest().body(null);
            }

            // 🔥 SOLUTION BASE64 - Stocker l'image directement dans la base de données
            byte[] imageBytes = photoFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String photoUrl = "data:" + photoFile.getContentType() + ";base64," + base64Image;

            user.setPhotoUrl(photoUrl);
            User updated = userRepository.save(user);

            System.out.println("✅ Photo sauvegardée en Base64, taille: " + base64Image.length() + " caractères");

            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            System.err.println("💥 ERREUR upload photo Base64:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Utilisateur non trouvé"));
            }

            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression: " + e.getMessage()));
        }}
    // -------------------------------
    // 🔥 NOUVEAU : Récupérer les statistiques de l'utilisateur
    // -------------------------------
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String id) {
        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Map<String, Object> stats = new HashMap<>();

        // Statistiques de base
        stats.put("userId", user.getId());
        stats.put("dateCreation", user.getDateCreation()); // Utiliser la date de création de l'utilisateur
        stats.put("role", user.getRole());

        // Statistiques selon le rôle
        switch (user.getRole()) {
            case NEEDY:
                stats.put("demandesCount", 0); // À implémenter avec votre logique
                stats.put("demandesAcceptees", 0); // À implémenter
                stats.put("demandesEnAttente", 0); // À implémenter
                break;
            case DONNATEUR:
            case ASSOCIATION:
                stats.put("aidesCount", 0); // À implémenter
                stats.put("aidesEnCours", 0); // À implémenter
                stats.put("aidesTerminees", 0); // À implémenter
                break;
            case ADMIN:
                stats.put("utilisateursGeres", userRepository.count()); // Exemple
                stats.put("demandesTraitees", 0); // À implémenter
                break;
        }

        // Statistiques générales
        stats.put("activiteMensuelle", 0); // À implémenter
        stats.put("scoreEngagement", 85); // Exemple

        return ResponseEntity.ok(stats);
    }

    // -------------------------------
    // 🔥 NOUVEAU : Récupérer le profil complet avec statistiques
    // -------------------------------
    @GetMapping("/{id}/profile-complet")
    public ResponseEntity<Map<String, Object>> getCompleteProfile(@PathVariable String id) {
        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Map<String, Object> profileComplet = new HashMap<>();

        // Informations de base
        profileComplet.put("user", user);

        // Statistiques
        Map<String, Object> stats = new HashMap<>();
        stats.put("demandesCount", 0);
        stats.put("demandesAcceptees", 0);
        stats.put("aidesCount", 0);
        stats.put("scoreEngagement", 85);
        stats.put("dateCreation", user.getDateCreation()); // Utiliser la date réelle
        profileComplet.put("stats", stats);

        // Dernières activités (à implémenter)
        profileComplet.put("recentActivities", List.of());

        return ResponseEntity.ok(profileComplet);
    }

    // -------------------------------
    // CORRIGÉ : Synchroniser l'utilisateur après login
    // -------------------------------
    @PostMapping("/sync")
    public ResponseEntity<User> syncUser(@RequestBody Map<String, String> userInfo) {
        String username = userInfo.get("username");
        String email = userInfo.get("email");
        String firstName = userInfo.get("firstName");
        String lastName = userInfo.get("lastName");
        String keycloakId = userInfo.get("sub"); // 🔥 ID Keycloak (doit être envoyé depuis Angular)

        System.out.println("Synchronisation utilisateur - ID Keycloak: " + keycloakId + ", Username: " + username);

        if (keycloakId == null || keycloakId.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // CORRECTION : Chercher d'abord par ID Keycloak (qui est l'ID MongoDB)
        Optional<User> existingUserById = userRepository.findById(keycloakId);

        if (existingUserById.isPresent()) {
            // Mettre à jour les informations si l'utilisateur existe déjà
            User existingUser = existingUserById.get();
            existingUser.setIdentifiant(username);
            existingUser.setEmail(email);
            existingUser.setFirstName(firstName);
            existingUser.setLastName(lastName);
            User updatedUser = userRepository.save(existingUser);
            System.out.println("Utilisateur mis à jour: " + updatedUser.getIdentifiant());
            return ResponseEntity.ok(updatedUser);
        }

        // Si pas trouvé par ID, chercher par identifiant (pour la rétrocompatibilité)
        Optional<User> existingUserByIdentifiant = userRepository.findByIdentifiant(username);
        if (existingUserByIdentifiant.isPresent()) {
            User existingUser = existingUserByIdentifiant.get();
            // Mettre à jour l'ID avec l'ID Keycloak
            existingUser.setId(keycloakId); // 🔥 Important: mettre à jour l'ID
            existingUser.setEmail(email);
            existingUser.setFirstName(firstName);
            existingUser.setLastName(lastName);
            User updatedUser = userRepository.save(existingUser);
            System.out.println("Utilisateur migré avec nouvel ID: " + updatedUser.getIdentifiant());
            return ResponseEntity.ok(updatedUser);
        }

        // Créer un nouvel utilisateur dans MongoDB avec l'ID Keycloak comme ID MongoDB
        User newUser = new User();
        newUser.setId(keycloakId); // 🔥 TRÈS IMPORTANT: utiliser l'ID Keycloak comme ID MongoDB
        newUser.setIdentifiant(username);
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setRole(User.Role.NEEDY); // Rôle par défaut

        User savedUser = userRepository.save(newUser);
        System.out.println("Nouvel utilisateur créé: " + savedUser.getIdentifiant() + " avec ID: " + savedUser.getId());
        return ResponseEntity.ok(savedUser);
    }

    // -------------------------------
    // Alternative: synchronisation par email
    // -------------------------------
    @PostMapping("/sync-by-email")
    public ResponseEntity<User> syncUserByEmail(@RequestBody Map<String, String> userInfo) {
        String email = userInfo.get("email");
        String keycloakId = userInfo.get("sub"); // 🔥 ID Keycloak

        System.out.println("Synchronisation par email - ID Keycloak: " + keycloakId + ", Email: " + email);

        if (keycloakId == null || keycloakId.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // CORRECTION : Chercher d'abord par ID Keycloak
        Optional<User> existingUserById = userRepository.findById(keycloakId);
        if (existingUserById.isPresent()) {
            User existingUser = existingUserById.get();
            existingUser.setIdentifiant(userInfo.get("username"));
            existingUser.setFirstName(userInfo.get("firstName"));
            existingUser.setLastName(userInfo.get("lastName"));
            User updatedUser = userRepository.save(existingUser);
            return ResponseEntity.ok(updatedUser);
        }

        // Si pas trouvé par ID, chercher par email
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            // Mettre à jour l'ID avec l'ID Keycloak
            existingUser.setId(keycloakId); // 🔥 Important: mettre à jour l'ID
            existingUser.setIdentifiant(userInfo.get("username"));
            existingUser.setFirstName(userInfo.get("firstName"));
            existingUser.setLastName(userInfo.get("lastName"));
            User updatedUser = userRepository.save(existingUser);
            return ResponseEntity.ok(updatedUser);
        }

        // Créer nouvel utilisateur
        User newUser = new User();
        newUser.setId(keycloakId); // 🔥 TRÈS IMPORTANT
        newUser.setEmail(email);
        newUser.setIdentifiant(userInfo.get("username"));
        newUser.setFirstName(userInfo.get("firstName"));
        newUser.setLastName(userInfo.get("lastName"));
        newUser.setRole(User.Role.NEEDY);

        User savedUser = userRepository.save(newUser);
        return ResponseEntity.ok(savedUser);
    }

    // -------------------------------
    // Autres méthodes utiles
    // -------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UserController.java - MODIFIER CETTE MÉTHODE
    // UserController.java - CORRECTION COMPLÈTE
    @GetMapping("/identifiant/{identifiant}")
    public ResponseEntity<User> getUserByIdentifiant(@PathVariable String identifiant) {
        try {
            System.out.println("🔍 DEBUG getUserByIdentifiant: " + identifiant);

            // Vérifier que l'identifiant n'est pas null
            if (identifiant == null || identifiant.trim().isEmpty()) {
                System.out.println("❌ Identifiant null ou vide");
                return ResponseEntity.badRequest().build();
            }

            System.out.println("🔍 Appel repository avec: " + identifiant);

            // Appel simple au repository
            Optional<User> userOpt = userRepository.findByIdentifiant(identifiant);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("✅ Utilisateur trouvé: " + user.getIdentifiant());

                // Retourner l'utilisateur directement
                return ResponseEntity.ok(user);
            } else {
                System.out.println("❌ Utilisateur non trouvé: " + identifiant);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("💥 ERREUR getUserByIdentifiant:");
            System.err.println("   Message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("   Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // 🔹 Méthode de debug pour vérifier les utilisateurs
    @GetMapping("/debug/all")
    public ResponseEntity<List<User>> debugAllUsers() {
        List<User> allUsers = userRepository.findAll();
        System.out.println("=== DEBUG - Tous les utilisateurs ===");
        allUsers.forEach(user -> System.out.println(
                "ID: " + user.getId() +
                        ", Identifiant: " + user.getIdentifiant() +
                        ", Email: " + user.getEmail() +
                        ", Role: " + user.getRole() +
                        ", Date création: " + user.getDateCreation()
        ));
        System.out.println("=== FIN DEBUG ===");
        return ResponseEntity.ok(allUsers);
    }

    // UserController.java - AJOUTER CETTE MÉTHODE
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        Map<String, String> response = new HashMap<>();
        try {
            response.put("status", "OK");
            response.put("message", "Backend fonctionne");
            response.put("timestamp", new Date().toString());

            // Test de la connexion MongoDB
            long userCount = userRepository.count();
            response.put("mongoDB", "CONNECTED");
            response.put("usersCount", String.valueOf(userCount));

            System.out.println("✅ Test endpoint - MongoDB users: " + userCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            System.err.println("❌ Test endpoint error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    // Dans UserController.java - AJOUTER ces méthodes

    // -------------------------------
// 🔥 NOUVEAU : Réinitialiser le mot de passe
// -------------------------------
    // Dans UserController.java - CORRECTION
// 🔥 CHANGER @PostMapping EN @PutMapping
    @PutMapping("/{id}/reset-password")  // 🔥 CORRECTION ICI
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable String id,
            @RequestBody Map<String, String> passwordData) {

        System.out.println("🔄 Réinitialisation mot de passe pour user: " + id);

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            System.out.println("❌ Utilisateur non trouvé: " + id);
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        String newPassword = passwordData.get("newPassword");
        String currentPassword = passwordData.get("currentPassword");

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nouveau mot de passe requis"));
        }

        try {
            // 1. Mettre à jour dans Keycloak
            boolean keycloakUpdated = keycloakAdminService.resetUserPassword(
                    "projet-integration",
                    user.getIdentifiant(),
                    newPassword
            );

            if (keycloakUpdated) {
                System.out.println("✅ Mot de passe mis à jour dans Keycloak pour: " + user.getIdentifiant());

                Map<String, String> response = new HashMap<>();
                response.put("message", "Mot de passe mis à jour avec succès");
                response.put("status", "SUCCESS");

                return ResponseEntity.ok(response);
            } else {
                System.err.println("❌ Échec mise à jour mot de passe Keycloak");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Erreur lors de la mise à jour du mot de passe"));
            }

        } catch (Exception e) {
            System.err.println("💥 Erreur réinitialisation mot de passe: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // -------------------------------
// 🔥 NOUVEAU : Déclencher l'email de réinitialisation
// -------------------------------
    // Dans UserController.java - CORRECTION DE forgotPassword
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        System.out.println("📧 Demande réinitialisation mot de passe pour: " + email);

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email requis"));
        }

        // 🔥 CORRECTION : Utiliser findAll() pour éviter l'erreur de doublons
        List<User> users = userRepository.findAllByEmail(email);

        if (users.isEmpty()) {
            System.out.println("⚠️ Aucun utilisateur trouvé avec cet email");
            return ResponseEntity.ok(Map.of(
                    "message", "Si l'email existe, un lien de réinitialisation a été envoyé",
                    "status", "SUCCESS"
            ));
        }

        // Prendre le premier utilisateur trouvé
        User user = users.get(0);
        System.out.println("✅ Utilisateur trouvé: " + user.getIdentifiant());

        try {
            System.out.println("🔄 Tentative d'envoi d'email via Keycloak...");
            boolean emailSent = keycloakAdminService.triggerPasswordResetEmail(
                    "projet-integration",
                    user.getIdentifiant()
            );

            if (emailSent) {
                System.out.println("✅ Email réinitialisation envoyé avec succès");
                return ResponseEntity.ok(Map.of(
                        "message", "Un lien de réinitialisation a été envoyé à votre email",
                        "status", "SUCCESS"
                ));
            } else {
                System.err.println("❌ Keycloak n'a pas pu envoyer l'email");

                // 🔥 SOLUTION DE SECOURS : Retourner un message avec un lien manuel
                String manualResetUrl = "http://localhost:8080/realms/projet-integration/login-actions/reset-credentials";
                return ResponseEntity.ok(Map.of(
                        "message", "Le service d'email est temporairement indisponible. " +
                                "Veuillez visiter: " + manualResetUrl + " pour réinitialiser votre mot de passe.",
                        "status", "INFO",
                        "manual_url", manualResetUrl
                ));
            }

        } catch (Exception e) {
            System.err.println("💥 Erreur lors de l'envoi d'email: " + e.getMessage());

            // 🔥 SOLUTION DE SECOURS EN CAS D'ERREUR
            String manualResetUrl = "http://localhost:8080/realms/projet-integration/login-actions/reset-credentials";
            return ResponseEntity.ok(Map.of(
                    "message", "Service temporairement indisponible. " +
                            "Veuillez réinitialiser votre mot de passe directement sur: " + manualResetUrl,
                    "status", "INFO",
                    "manual_url", manualResetUrl
            ));
        }
    }


}