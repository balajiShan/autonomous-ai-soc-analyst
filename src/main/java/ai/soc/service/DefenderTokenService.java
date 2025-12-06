package ai.soc.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class DefenderTokenService {


    @Value("${defender.token-endpoint}")
    private String tokenEndpoint;

    @Value("${defender.tenant-id}")
    private String tenantId;

    @Value("${defender.client-id}")
    private String clientId;

    @Value("${defender.client-secret}")
    private String clientSecret;

    @Value("${defender.scope}")
    private String scope;

    @Autowired
    private RestClient restClient;

    private String accessToken;
    private Long tokenExpiryTime;

    public String fetchDefenderToken() {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("scope", scope);

            JsonNode response = restClient.post()
                    .uri(tokenEndpoint + "/" + tenantId + "/oauth2/v2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() >= HttpStatus.BAD_REQUEST.value(),
                            (req, res) -> {
                                String errorBody = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "No response body";
                                log.error("Failed to fetch access token: HTTP {} - {}", res.getStatusCode(), errorBody);
                                throw new HttpClientErrorException(res.getStatusCode(), "Failed to fetch access token: " + errorBody);
                            })
                    .body(JsonNode.class);
            String accessToken = response.path("access_token").asText();
            long expiresIn = response.path("expires_in").asLong(3600); // Default to 3600 seconds if not provided
            Long tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000);
            log.debug("Obtained new access token, expires at: {}", new java.util.Date(tokenExpiryTime));
            return accessToken;
        } catch (Exception e) {
            log.error("Error fetching access token", e);
            throw new RuntimeException("Failed to obtain access token", e);
        }
    }
}