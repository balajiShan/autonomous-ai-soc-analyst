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
public class VirusTotalService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestClient restClient;

    @Value("${virustotal.api-base}")
    private String virusTotalApiBase;

    @Value("${virustotal.api-key}")
    private String virusTotalApiKey;

    public Long checkVirusTotalReputation(String fileHash) {
        try {
            JsonNode response = restClient.get()
                    .uri(virusTotalApiBase + "/files/" + fileHash)
                    .header("x-apikey", virusTotalApiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() >= HttpStatus.BAD_REQUEST.value(),
                            (req, res) -> {
                                String errorBody = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "No response body";
                                log.error("Failed to fetch VirusTotal reputation for hash {}: HTTP {} - {}", fileHash, res.getStatusCode(), errorBody);
                                throw new HttpClientErrorException(res.getStatusCode(), "Failed to fetch VirusTotal reputation: " + errorBody);
                            })
                    .body(JsonNode.class);
            JsonNode stats = response.path("data").path("attributes").path("last_analysis_stats");
            long maliciousCount = stats.path("malicious").asLong(0);
            log.info("VirusTotal reputation for hash {}: malicious count = {}", fileHash, maliciousCount);
            return maliciousCount;
        } catch (Exception e) {
            log.error("Error checking VirusTotal reputation for hash {}", fileHash, e);
            return null;
        }
    }

}