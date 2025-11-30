package com.iset.projet_integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

@Component
public class JwtUtils {

    public static String getUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> json = mapper.readValue(payload, Map.class);
            return (String) json.get("preferred_username");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}