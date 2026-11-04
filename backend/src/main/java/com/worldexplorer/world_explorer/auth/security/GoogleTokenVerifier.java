
package com.worldexplorer.world_explorer.auth.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(
            @Value("${google.oauth.client-id}") String clientId
    ) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new IllegalArgumentException("Google ID Token không hợp lệ");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String googleId = payload.getSubject();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            if (googleId == null || email == null || !emailVerified) {
                throw new IllegalArgumentException(
                        "Không thể xác minh thông tin tài khoản Google"
                );
            }

            return new GoogleUserInfo(
                    googleId,
                    email,
                    name,
                    pictureUrl
            );

        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(
                    "Không thể xác minh Google ID Token",
                    e
            );
        }
    }

    public record GoogleUserInfo(
            String googleId,
            String email,
            String name,
            String pictureUrl
    ) {
    }
}
