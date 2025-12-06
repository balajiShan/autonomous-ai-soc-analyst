package ai.soc.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class AbuseIpService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestClient restClient;

    @Value("${abuseipdb.api-base}")
    private String abuseIpdbApiBase;

    @Value("${abuseipdb.api-key}")
    private String abuseIpdbApiKey;

    public Long checkAbuseIpdbReputation(String ipAddress) {
        try {
            JsonNode response = restClient.get()
                    .uri(abuseIpdbApiBase + "/check?ipAddress=" + ipAddress + "&maxAgeInDays=90")
                    .header("Key", abuseIpdbApiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() >= HttpStatus.BAD_REQUEST.value(),
                            (req, res) -> {
                                String errorBody = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "No response body";
                                log.error("Failed to fetch AbuseIPDB reputation for IP {}: HTTP {} - {}", ipAddress, res.getStatusCode(), errorBody);
                                throw new HttpClientErrorException(res.getStatusCode(), "Failed to fetch AbuseIPDB reputation: " + errorBody);
                            })
                    .body(JsonNode.class);
            long confidenceScore = response.path("data").path("abuseConfidenceScore").asLong(0);
            log.info("AbuseIPDB reputation for IP {}: confidence score = {}", ipAddress, confidenceScore);
            return confidenceScore;
        } catch (Exception e) {
            log.error("Error checking AbuseIPDB reputation for IP {}", ipAddress, e);
            return null;
        }
    }

}