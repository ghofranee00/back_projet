package com.iset.projet_integration.Controller;

import com.iset.projet_integration.dto.PasswordUpdateDto;
import com.iset.projet_integration.dto.UserDto;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.UserRepository;
import com.iset.projet_integration.Service.KeycloakAdminService;
import com.iset.projet_integration.dto.UserUpdateDto;
import com.iset.projet_integration.security.JwtUtils;
import org.keycloak.admin.client.Keycloak;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/keycloak")
public class KeycloakController {

    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final JwtUtils jwtUtils; // ⚠️ AJOUT

    // ⚠️ AJOUT: Injection de JwtUtils
    public KeycloakController(Keycloak keycloak, UserRepository userRepository,
                              KeycloakAdminService keycloakAdminService, JwtUtils jwtUtils) {
        this.keycloak = keycloak;
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.jwtUtils = jwtUtils; // ⚠️ AJOUT
        System.out.println("👉 KEYCLOAK INJECTED = " + keycloak);
    }
    // -------------------------------
    // GET USER PROFILE - NOUVEL ENDPOINT
    // -------------------------------
    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            // Extraire le token du header
            String accessToken = token.replace("Bearer ", "");

            // Décoder le token JWT pour obtenir le username
            String username = JwtUtils.getUsernameFromToken(accessToken);

            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // Trouver l'utilisateur dans MongoDB
            Optional<User> user = userRepository.findByIdentifiant(username);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    // -------------------------------
    // REGISTER USER
    // -------------------------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto dto) {
        // 1️⃣ Créer l'utilisateur dans Keycloak
        ResponseEntity<User> keycloakResponse = keycloakAdminService.createUser(dto);
        if (!keycloakResponse.getStatusCode().is2xxSuccessful()) {
            // Retourner l’erreur Keycloak (conflit, erreur, etc.)
            return ResponseEntity.status(keycloakResponse.getStatusCode())
                    .body(keycloakResponse.getBody());
        }

        // 2️⃣ Si succès, enregistrer dans MongoDB
        User user = new User();
        user.setIdentifiant(dto.getIdentifiant());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPassword(dto.getPassword());
        user.setRole(User.Role.valueOf(dto.getRole().name()));


        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }

    // -------------------------------
    // LOGIN USER
    // -------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=password"
                    + "&client_id=angular-client"
                    + "&username=" + username
                    + "&password=" + password;

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8080/realms/projet-integration/protocol/openid-connect/token",
                    request,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }


    // -------------------------------
// UPDATE USER PROFILE
// -------------------------------
    // CORRECTION : Dans votre méthode updateProfile du contrôleur
    @PutMapping("/profile/{username}")
    public ResponseEntity<User> updateProfile(@PathVariable String username, @RequestBody UserUpdateDto updateDto) {
        try {
            Optional<User> existingUser = userRepository.findByIdentifiant(username);
            if (existingUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            User user = existingUser.get();

            // Mettre à jour les champs
            if (updateDto.getFirstName() != null) user.setFirstName(updateDto.getFirstName());
            if (updateDto.getLastName() != null) user.setLastName(updateDto.getLastName());
            if (updateDto.getEmail() != null) user.setEmail(updateDto.getEmail());

            // 🔥 CORRECTION : Récupérer l'ID Keycloak AVANT la mise à jour
            String keycloakUserId = user.getId(); // Assurez-vous que ce champ existe

            User updatedUser = userRepository.save(user);

            // Mettre à jour dans Keycloak APRÈS avoir sauvegardé en base
            if (keycloakUserId != null) {
                boolean keycloakUpdated = keycloakAdminService.updateUser(
                        "projet-integration",
                        keycloakUserId, // Utiliser l'ID Keycloak
                        updatedUser.getEmail(),
                        username,
                        updatedUser.getFirstName(),
                        updatedUser.getLastName(),
                        null
                );

                if (!keycloakUpdated) {
                    System.err.println("⚠️ Keycloak update failed for user: " + username);
                    // Ne pas retourner une erreur ici, car la base de données est déjà mise à jour
                }
            }

            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            System.err.println("💥 Error updating profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    // -------------------------------
// UPDATE PASSWORD
// -------------------------------
    @PutMapping("/profile/{username}/password")
    public ResponseEntity<?> updatePassword(@PathVariable String username, @RequestBody PasswordUpdateDto passwordDto) {
        try {
            Optional<User> existingUser = userRepository.findByIdentifiant(username);
            if (existingUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Mettre à jour le mot de passe dans Keycloak
            boolean passwordUpdated = keycloakAdminService.resetUserPassword(
                    "projet-integration",
                    username,
                    passwordDto.getNewPassword()
            );

            if (passwordUpdated) {
                return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to update password"));
            }

        } catch (Exception e) {
            System.err.println("💥 Error updating password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating password"));
        }
    }

    // -------------------------------
// LOGOUT USER (optionnel - généralement géré côté client)
// -------------------------------
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Avec Keycloak, la déconnexion se fait généralement côté client
        // en supprimant le token localement
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }


    // -------------------------------
    // TEST KEYCLOAK CONNECTION
    // -------------------------------
    @GetMapping("/test-keycloak")
    public ResponseEntity<String> testKeycloak() {
        try {
            System.out.println("🧪 Testing Keycloak connection...");

            // Test 1: Lister les realms
            var realms = keycloak.realms().findAll();
            System.out.println("✅ Realms count: " + realms.size());

            // Test 2: Accéder au realm projet-integration
            var realmResource = keycloak.realm("projet-integration");
            var realmInfo = realmResource.toRepresentation();
            System.out.println("✅ Realm found: " + realmInfo.getRealm());

            // Test 3: Lister les rôles
            var roles = realmResource.roles().list();
            System.out.println("✅ Roles available: " + roles.stream().map(r -> r.getName()).collect(Collectors.toList()));

            // Test 4: Vérifier les clients
            var clients = realmResource.clients().findAll();
            System.out.println("✅ Clients count: " + clients.size());

            return ResponseEntity.ok("Keycloak connection OK! Realm: " + realmInfo.getRealm());

        } catch (Exception e) {
            System.err.println("❌ Keycloak test failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Keycloak error: " + e.getMessage());
        }
    }

}
