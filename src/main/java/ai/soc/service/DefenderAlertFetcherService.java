package ai.soc.service;

import ai.soc.repository.AlertRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class DefenderAlertFetcherService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private DefenderTokenService defenderTokenService;

    @Autowired
    private RestClient restClient;

    @Value("${defender.api-base}")
    private String defenderApiBase;

    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    public void fetchDefenderAlerts() {
        try {

            String bearerToken = defenderTokenService.fetchDefenderToken();

            // Calculate time window (last 10 seconds)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoMonthsAgo = now.minusSeconds(10);
            String filter = String.format("alertCreationTime ge %s", twoMonthsAgo.format(isoFormatter));

            // Fetch alerts from Microsoft Defender for Endpoint
            JsonNode response = restClient.get()
                    .uri(defenderApiBase + "/api/alerts?$filter=" + filter + "Z")
                    .header("Authorization", "Bearer " + bearerToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() >= HttpStatus.BAD_REQUEST.value(),
                            (req, res) -> {
                                log.error("Failed to fetch alerts: HTTP {}", res.getStatusCode());
                                throw new RestClientException("Failed to fetch alerts: HTTP " + res.getStatusCode());
                            })
                    .body(JsonNode.class);

            // Process alerts
            JsonNode alerts = response.path("value");
            if (alerts.isArray()) {
                for (JsonNode alertNode : alerts) {
                    String alertId = alertNode.path("id").asText();
                    if (alertRepository.existsById(alertId)) {
                        log.debug("Skipping duplicate alert ID: {}", alertId);
                        continue;
                    }

                    alertService.processAlert(alertNode, bearerToken);

                    log.info("Processed new alert ID: {}", alertId);
                }
            } else {
                log.debug("No new alerts found in the last 10 seconds");
            }
        } catch (Exception e) {
            log.error("Error fetching Defender alerts", e);
        }
    }
}