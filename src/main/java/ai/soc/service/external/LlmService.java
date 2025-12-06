package ai.soc.service.external;

import ai.soc.entity.Alert;
import ai.soc.repository.AlertRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LlmService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestClient restClient;

    @Autowired
    private AlertRepository alertRepository;

    @Value("${litellm.api-base}")
    private String litellmApiBase;

    @Value("${litellm.api-key}")
    private String litellmApiKey;

    @Value("${litellm.model}")
    private String litellmModel;

    public String getAnalysisAndRecommendationFromLLM(String prompt) throws JsonProcessingException {
        // Prepare LiteLLM request payload

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> messageMap = new HashMap<>();
        messageMap.put("role", "user");
        messageMap.put("content", prompt);
        messages.add(messageMap);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", litellmModel);
        requestBody.put("messages", messages);

        log.info("Request sent to LLM : {}", objectMapper.writeValueAsString(requestBody));

        // Make HTTP POST call to LiteLLM
        try {
            JsonNode response = restClient.post()
                    .uri(litellmApiBase + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + litellmApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            log.info("Response from LLM : {}", response.toString());

            // Parse LLM response
            return response.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to validate alert with LiteLLM", e);
        }
    }

    public String generateSqlFromLlm(String userQuery) {
        String prompt = "As a SOC expert familiar with Microsoft Defender and MySQL, use fields like id, incident_id, investigation_id, assigned_to, severity, status, classification, determination, investigation_state, " +
                " detection_source, detector_id, category, threat_family_name, title, description, alert_creation_time, first_event_time, last_event_time, last_update_time, resolved_time, machine_id, computer_dns_name, " +
                " rbac_group_name,  aad_tenant_id, threat_name, mitre_techniques, related_user_name, related_domain_name, raw_json, validity, virustotal_malicious_count, abuseipdb_confidence_score, llm_recommendation " +
                " from the 'dagger.alerts' table to frame a SELECT query matching the user's question: '[" + userQuery + "]'. Return only the SQL statement; if no specific match is possible, use 'SELECT * FROM dagger.alerts'";


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + litellmApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", litellmModel); // Adjust as needed
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant."),
                Map.of("role", "user", "content", prompt)
        ));

        try {

            log.info("Request sent to LLM : {}", objectMapper.writeValueAsString(requestBody));

            JsonNode response = restClient.post()
                    .uri(litellmApiBase + "/chat/completions")
                    .header("Authorization", "Bearer " + litellmApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            log.info("Response from LLM : {}", response.toString());
            return response.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Error generating SQL from LLM", e);
            return "select * from alerts";
        }
    }


    public Map<String, String> challengeRecommendation(String alertId, String previousRecommendation, String analystComment)
            throws JsonProcessingException {
        // Fetch alert details for context
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert with ID " + alertId + " not found"));

        // Construct a detailed prompt for the LLM
        String prompt = String.format(
                "As a SOC expert, review the following alert and provide an improved recommendation based on the analyst's feedback. " +
                        "Alert Details: Title=%s, Description=%s, Severity=%s, Category=%s. Previous result: %s  " +
                        "Previous Recommendation: %s. Analyst Feedback: %s. " +
                        "Provide a concise, actionable recommendation to address the alert, incorporating the analyst's feedback. ",
                alert.getTitle(), alert.getDescription(), alert.getSeverity(), alert.getCategory(), alert.getValidity(), previousRecommendation, analystComment
        );

        // Prepare LiteLLM request payload
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "You are a SOC expert assistant specializing in cybersecurity alert analysis."),
                Map.of("role", "user", "content", prompt)
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", litellmModel);
        requestBody.put("messages", messages);

        log.info("Request sent to LLM for challenge recommendation: {}", objectMapper.writeValueAsString(requestBody));

        // Make HTTP POST call to LiteLLM
        try {
            JsonNode response = restClient.post()
                    .uri(litellmApiBase + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + litellmApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            log.info("Response from LLM: {}", response.toString());

            // Parse LLM response
            String newRecommendation = response.path("choices").get(0).path("message").path("content").asText();

            alert.setLlmRecommendation(alert.getLlmRecommendation() + "\n\n *** This was challenged by the user at " + LocalDateTime.now() + " with comments " + analystComment +
                    ". \n\n *** New Recommendation from LLM after a challenge: \n\n" + newRecommendation);
            alertRepository.save(alert);

            // Return response in expected format
            Map<String, String> result = new HashMap<>();
            result.put("newRecommendation", alert.getLlmRecommendation());
            return result;

        } catch (Exception e) {
            log.error("Error challenging recommendation for alert ID {}: {}", alertId, e.getMessage());
            throw new RuntimeException("Failed to challenge recommendation with LiteLLM", e);
        }
    }

}