package com.iset.projet_integration.Controller;

import com.iset.projet_integration.Entities.Comment;
import com.iset.projet_integration.Entities.Post;
import com.iset.projet_integration.Entities.User;
import com.iset.projet_integration.Repository.UserRepository;
import com.iset.projet_integration.Service.CommentService;
import com.iset.projet_integration.Service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:4200")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    // ----------------- Posts -----------------
    @GetMapping
    public List<Post> getAllPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable String id) {
        try {
            Post post = postService.getPostById(id);
            return ResponseEntity.ok(post);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likePost(
            @PathVariable String postId,
            Authentication authentication) {

        try {
            // Même logique : utiliser l'ID Keycloak
            String keycloakUserId = authentication.getName();

            System.out.println("❤️ LIKE - Recherche utilisateur ID: " + keycloakUserId);

            // Chercher par ID, pas par identifiant
            User user = userRepository.findById(keycloakUserId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + keycloakUserId));

            System.out.println("✅ Utilisateur pour like: " + user.getEmail());

            return ResponseEntity.ok(postService.toggleLike(postId, user.getId()));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{postId}/like/status")
    public ResponseEntity<?> getLikeStatus(
            @PathVariable String postId,
            @RequestParam String userId) {

        boolean hasLiked = postService.hasUserLiked(postId, userId);
        return ResponseEntity.ok(Map.of("hasLiked", hasLiked));
    }

    // ----------------- Commentaires -----------------
    @GetMapping("/{postId}/comments")
    public List<Comment> getComments(@PathVariable String postId) {
        return commentService.findByPostId(postId);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable String postId,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        String contenu = payload.get("contenu");

        if(contenu == null || contenu.isEmpty()) {
            return ResponseEntity.badRequest().body("Comment content is required");
        }

        try {
            // MODIFICATION ICI : authentication.getName() retourne le "sub" du token
            // qui est l'ID Keycloak = l'ID dans votre MongoDB
            String keycloakUserId = authentication.getName(); // C'est "f07e0388-320e-4f6e-898e-5daef93f95d0"

            System.out.println("🔍 Recherche utilisateur avec ID: " + keycloakUserId);

            // MODIFICATION ICI : Utilisez findById() au lieu de findByIdentifiant()
            User user = userRepository.findById(keycloakUserId)
                    .orElseThrow(() -> {
                        System.out.println("❌ Utilisateur non trouvé avec ID: " + keycloakUserId);
                        return new RuntimeException("User not found with ID: " + keycloakUserId);
                    });

            System.out.println("✅ Utilisateur trouvé: " + user.getEmail() + " (" + user.getIdentifiant() + ")");

            Comment comment = commentService.addComment(postId, user, contenu);
            return ResponseEntity.ok(comment);

        } catch (RuntimeException e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable String commentId,
            @RequestBody Map<String, String> payload) {

        String contenu = payload.get("contenu");
        try {
            Comment updated = commentService.updateComment(commentId, contenu);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable String commentId,
            Authentication authentication) {

        try {
            // Récupérer l'ID utilisateur depuis le token
            String userId = authentication.getName();

            // Récupérer le commentaire
            Comment comment = commentService.getCommentById(commentId);

            // DEBUG: Afficher les infos
            System.out.println("🔍 Delete comment - User ID from token: " + userId);
            System.out.println("🔍 Delete comment - Comment owner ID: " + comment.getUser().getId());
            System.out.println("🔍 Delete comment - Comment ID: " + commentId);

            // Vérifier que l'utilisateur est le propriétaire du commentaire
            if (!comment.getUser().getId().equals(userId)) {
                System.out.println("❌ Not authorized: User " + userId + " cannot delete comment of user " + comment.getUser().getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Not authorized to delete this comment");
            }

            // Supprimer le commentaire
            commentService.deleteComment(commentId);

            return ResponseEntity.ok(Map.of(
                    "deleted", true,
                    "message", "Comment deleted successfully"
            ));

        } catch (RuntimeException e) {
            System.out.println("❌ Error deleting comment: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }}
}