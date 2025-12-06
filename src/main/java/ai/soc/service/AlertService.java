package ai.soc.service;

import ai.soc.dto.DashboardResponse;
import ai.soc.entity.Alert;
import ai.soc.entity.AlertComment;
import ai.soc.entity.AlertEvidence;
import ai.soc.repository.AlertRepository;
import ai.soc.service.external.AbuseIpService;
import ai.soc.service.external.LlmService;
import ai.soc.service.external.VirusTotalService;
import ch.qos.logback.core.util.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AlertService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;
    private final Pattern ipPattern = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    private final Pattern recommendationPattern = Pattern.compile("Recommendation:\\s*([\\s\\S]*?)(?:\\n\\n|$)", Pattern.DOTALL);
    private final Pattern analysisPattern = Pattern.compile("Analysis:\\s*([\\s\\S]*?)(?:\\n\\n|Recommendation:|$)", Pattern.DOTALL);

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RestClient restClient;

    @Autowired
    private DefenderTokenService defenderTokenService;

    @Autowired
    private LlmService llmService;

    @Autowired
    private AbuseIpService abuseIpService;

    @Autowired
    private VirusTotalService virusTotalService;

    @Value("${defender.api-base}")
    private String defenderApiBase;

    @Value("${defender.suppress-assigned-to}")
    private String suppressAssignedTo;

    public String saveAlert(String jsonPayload) {
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);

            return processAlert(root, null);

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON payload", e);
        }
    }

    public String processAlert(JsonNode root, String bearerToken) throws JsonProcessingException {
        Alert alert = new Alert();
        alert.setId(root.path("id").asText());
        alert.setIncidentId(parseLong(root.path("incidentId")));
        alert.setInvestigationId(parseLong(root.path("investigationId")));
        alert.setAssignedTo(root.path("assignedTo").asText());
        alert.setSeverity(root.path("severity").asText());
        alert.setStatus(root.path("status").asText());
        alert.setClassification(root.path("classification").asText());
        alert.setDetermination(root.path("determination").asText());
        alert.setInvestigationState(root.path("investigationState").asText());
        alert.setDetectionSource(root.path("detectionSource").asText());
        alert.setDetectorId(root.path("detectorId").asText());
        alert.setCategory(root.path("category").asText());
        alert.setThreatFamilyName(root.path("threatFamilyName").asText());
        alert.setTitle(root.path("title").asText());
        alert.setDescription(root.path("description").asText());
        alert.setAlertCreationTime(parseDateTime(root.path("alertCreationTime")));
        alert.setFirstEventTime(parseDateTime(root.path("firstEventTime")));
        alert.setLastEventTime(parseDateTime(root.path("lastEventTime")));
        alert.setLastUpdateTime(parseDateTime(root.path("lastUpdateTime")));
        alert.setResolvedTime(parseDateTime(root.path("resolvedTime")));
        alert.setMachineId(root.path("machineId").asText());
        alert.setComputerDnsName(root.path("computerDnsName").asText());
        alert.setRbacGroupName(root.path("rbacGroupName").asText());
        alert.setAadTenantId(root.path("aadTenantId").asText());
        alert.setThreatName(root.path("threatName").asText());
        alert.setMitreTechniques(objectMapper.writeValueAsString(root.path("mitreTechniques")));
        JsonNode relatedUser = root.path("relatedUser");
        alert.setRelatedUserName(relatedUser.path("userName").asText());
        alert.setRelatedDomainName(relatedUser.path("domainName").asText());

        String text = root.toString();
        alert.setRawJson(text);

        // Parse comments
        JsonNode commentsNode = root.path("comments");
        if (commentsNode.isArray()) {
            Iterator<JsonNode> commentsIter = commentsNode.elements();
            while (commentsIter.hasNext()) {
                JsonNode commentNode = commentsIter.next();
                AlertComment comment = new AlertComment();
                comment.setAlert(alert);
                comment.setComment(commentNode.path("comment").asText());
                comment.setCreatedBy(commentNode.path("createdBy").asText());
                comment.setCreatedTime(parseDateTime(commentNode.path("createdTime")));
                alert.getComments().add(comment);
            }
        }

        // Parse evidence
        JsonNode evidenceNode = root.path("evidence");
        if (evidenceNode.isArray()) {
            Iterator<JsonNode> evidenceIter = evidenceNode.elements();
            while (evidenceIter.hasNext()) {
                JsonNode evNode = evidenceIter.next();
                AlertEvidence evidence = new AlertEvidence();
                evidence.setAlert(alert);
                evidence.setEntityType(evNode.path("entityType").asText());
                evidence.setEvidenceCreationTime(parseDateTime(evNode.path("evidenceCreationTime")));
                evidence.setSha1(evNode.path("sha1").asText());
                evidence.setSha256(evNode.path("sha256").asText());
                evidence.setFileName(evNode.path("fileName").asText());
                evidence.setFilePath(evNode.path("filePath").asText());
                evidence.setProcessId(parseLong(evNode.path("processId")));
                evidence.setProcessCommandLine(evNode.path("processCommandLine").asText());
                evidence.setProcessCreationTime(parseDateTime(evNode.path("processCreationTime")));
                evidence.setParentProcessId(parseLong(evNode.path("parentProcessId")));
                evidence.setParentProcessCreationTime(parseDateTime(evNode.path("parentProcessCreationTime")));
                evidence.setParentProcessFileName(evNode.path("parentProcessFileName").asText());
                evidence.setParentProcessFilePath(evNode.path("parentProcessFilePath").asText());
                evidence.setIpAddress(evNode.path("ipAddress").asText());
                evidence.setUrl(evNode.path("url").asText());
                evidence.setRegistryKey(evNode.path("registryKey").asText());
                evidence.setRegistryHive(evNode.path("registryHive").asText());
                evidence.setRegistryValueType(evNode.path("registryValueType").asText());
                evidence.setRegistryValue(evNode.path("registryValue").asText());
                evidence.setAccountName(evNode.path("accountName").asText());
                evidence.setDomainName(evNode.path("domainName").asText());
                evidence.setUserSid(evNode.path("userSid").asText());
                evidence.setAadUserId(evNode.path("aadUserId").asText());
                evidence.setUserPrincipalName(evNode.path("userPrincipalName").asText());
                evidence.setDetectionStatus(evNode.path("detectionStatus").asText());
                alert.getEvidence().add(evidence);
            }
        }

        // Call LiteLLM to validate alert
        Map<String, String> llmResult = validateAlertWithLiteLLM(root.path("title").asText(), root.path("description").asText(),
                root.path("category").asText(), root.path("severity").asText(), evidenceNode.toString());

        String validity = llmResult.get("validity");

        // Extract file hash from evidence
        String fileHash = extractFileHash(root);
        // Perform VirusTotal check for file-related alerts
        if (fileHash != null) {
            Long maliciousCount = virusTotalService.checkVirusTotalReputation(fileHash);
            alert.setVirusTotalMaliciousCount(maliciousCount);
        }

        // Extract IP from evidence
        String ipAddress = extractIpAddress(root);
        // Perform AbuseIPDB check for IP-related alerts
        if (ipAddress != null) {
            Long confidenceScore = abuseIpService.checkAbuseIpdbReputation(ipAddress);
            alert.setAbuseIpdbConfidenceScore(confidenceScore);
        }

        boolean isLowRisk = (alert.getVirusTotalMaliciousCount() == null || alert.getVirusTotalMaliciousCount() <= 0) &&
                (alert.getAbuseIpdbConfidenceScore() == null || alert.getAbuseIpdbConfidenceScore() < 50);

        if ("FalsePositive".equals(validity) && isLowRisk) {
            suppressAlertInDefender(root.path("id").asText(), bearerToken);
        } else if ("FalsePositive".equals(validity)) {
            validity = "NeedsAttention";
        }

        alert.setValidity(validity);
        alert.setValidity(llmResult.get("validity"));
        alert.setLlmRecommendation(llmResult.get("recommendation"));
        alert.setLlmAnalysis(llmResult.get("llmAnalysis"));
        alertRepository.save(alert);
        return validity;
    }

    private Long parseLong(JsonNode node) {
        return node.isNull() ? null : node.asLong();
    }

    private LocalDateTime parseDateTime(JsonNode node) {
        return node.isNull() || node.asText().isEmpty() ? null : LocalDateTime.parse(node.asText(), isoFormatter);
    }

    private Map<String, String> validateAlertWithLiteLLM(String title, String description, String category, String severity, String evidence) {
        // Construct prompt for LLM
        String prompt = String.format(
                "Analyze the following security alert and determine if it is likely a true positive or false positive. " +
                        " Provide a concise 'Result' as 'TruePositive' or 'FalsePositive' or 'NeedsAttention'. NeedsAttention should be used when you are not able to determine the validity using the provided information." +
                        " Include a detailed 'Analysis' section explaining your reasoning process. " +
                        " Provide a Clear 'Recommendation' to resolve this alert. Alert details are as follows. Title: %s, Description: %s, Category: %s, Severity: %s, Evidence: %s",
                title,
                description,
                category,
                severity,
                evidence
        );

        // Make HTTP POST call to LiteLLM
        try {
            // Parse LLM response
            String llmResponse = llmService.getAnalysisAndRecommendationFromLLM(prompt);

            // Parse validity
            String validity = "Unknown";
            if (llmResponse.contains("FalsePositive")) {
                validity = "FalsePositive";
            } else if (llmResponse.contains("TruePositive")) {
                validity = "TruePositive";
            } else if (llmResponse.contains("NeedsAttention")) {
                validity = "NeedsAttention";
            }

            // Parse recommendation
            String recommendation = "No recommendation provided";
            Matcher recommendationMatcher = recommendationPattern.matcher(llmResponse);
            if (recommendationMatcher.find()) {
                recommendation = recommendationMatcher.group(1).trim();
            }

            // Parse analysis
            String llmAnalysis = "No analysis provided";
            Matcher analysisMatcher = analysisPattern.matcher(llmResponse);
            if (analysisMatcher.find()) {
                llmAnalysis = analysisMatcher.group(1).trim();
            }

            return Map.of("validity", validity, "recommendation", recommendation, "llmAnalysis", llmAnalysis);

        } catch (Exception e) {
            throw new RuntimeException("Failed to validate alert with LiteLLM", e);
        }
    }

    public void suppressAlertInDefender(String alertId, String bearerToken) {

        // Suppress alert in Defender if FalsePositive
        try {

            if (StringUtil.isNullOrEmpty(bearerToken)) {
                bearerToken = defenderTokenService.fetchDefenderToken();
            }

            restClient.patch()
                    .uri(defenderApiBase + "/api/alerts/" + alertId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("assignedTo", suppressAssignedTo))
                    .retrieve()
                    .onStatus(status -> status.value() >= HttpStatus.BAD_REQUEST.value(),
                            (req, res) -> {
                                String errorBody = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "No response body";
                                log.error("Failed to suppress alert {}: HTTP {} - {}", alertId, res.getStatusCode(), errorBody);
                                throw new HttpClientErrorException(res.getStatusCode(), "Failed to suppress alert: " + errorBody);
                            })
                    .toBodilessEntity();
            log.info("Suppressed alert ID {} in Defender by assigning to {}", alertId, suppressAssignedTo);
        } catch (HttpClientErrorException e) {
            log.error("HTTP error suppressing alert {}: {}", alertId, e.getMessage());
        } catch (Exception e) {
            log.error("Error suppressing alert {}", alertId, e);
        }
    }

    private String extractFileHash(JsonNode alertNode) {
        JsonNode evidence = alertNode.path("evidence");
        if (evidence.isArray()) {
            for (JsonNode evidenceNode : evidence) {
                String sha256 = evidenceNode.path("sha256").asText(null);
                if (sha256 != null) {
                    return sha256;
                }
            }
        }
        return null;
    }

    private String extractIpAddress(JsonNode alertNode) {
        JsonNode evidence = alertNode.path("evidence");
        if (evidence.isArray()) {
            for (JsonNode evidenceNode : evidence) {
                String ip = evidenceNode.path("ipAddress").asText(null);
                if (ip != null && ipPattern.matcher(ip).matches()) {
                    return ip;
                }
            }
        }
        return null;
    }

    // Dashboard API logics
    public DashboardResponse getDashboardData() {
        Map<String, Long> counts = getAlertCounts();
        List<Alert> latestAlerts = getLatestAlerts();
        Map<String, Double> avgTimes = getAverageAnalysisTimes();

        DashboardResponse.Counts dtoCounts = new DashboardResponse.Counts(
                counts.get("totalAlerts"),
                counts.get("truePositives"),
                counts.get("needsAttention"),
                counts.get("falsePositives")
        );

        List<DashboardResponse.AlertSummary> alertSummaries = latestAlerts.stream()
                .map(alert -> new DashboardResponse.AlertSummary(
                        alert.getId(),
                        alert.getIncidentId(),
                        alert.getTitle(),
                        alert.getSeverity(),
                        alert.getCategory(),
                        alert.getAlertCreationTime() != null ? alert.getAlertCreationTime().toString() : null,
                        alert.getValidity()
                ))
                .collect(Collectors.toList());

        DashboardResponse.AvgTimes dtoAvgTimes = new DashboardResponse.AvgTimes(
                avgTimes.get("truePositiveAvgTime"),
                avgTimes.get("falsePositiveAvgTime")
        );

        return new DashboardResponse(dtoCounts, alertSummaries, dtoAvgTimes);
    }

    public Map<String, Long> getAlertCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("totalAlerts", getLongValueOrDefault(alertRepository.countTotalAlerts()));
        counts.put("truePositives", getLongValueOrDefault(alertRepository.countByValidity("TruePositive")));
        counts.put("needsAttention", getLongValueOrDefault(alertRepository.countByValidity("NeedsAttention")));
        counts.put("falsePositives", getLongValueOrDefault(alertRepository.countByValidity("FalsePositive")));
        return counts;
    }

    private Long getLongValueOrDefault(Long value) {
        if (value == null)
            return 0L;
        return value;

    }

    public List<Alert> getLatestAlerts() {
        return alertRepository.findTop10ByOrderByAlertCreationTimeDesc();
    }

    public Map<String, Double> getAverageAnalysisTimes() {
        Map<String, Double> times = new HashMap<>();
        Double truePositiveTime = alertRepository.findAverageAnalysisTimeByValidity("TruePositive");
        Double falsePositiveTime = alertRepository.findAverageAnalysisTimeByValidity("FalsePositive");
        times.put("truePositiveAvgTime", truePositiveTime != null ? truePositiveTime : 0.0);
        times.put("falsePositiveAvgTime", falsePositiveTime != null ? falsePositiveTime : 0.0);
        return times;
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public Map<String, Long> getAlertCategoryCounts() {
        List<Alert> alerts = alertRepository.findAll();
        return alerts.stream()
                .collect(Collectors.groupingBy(
                        alert -> alert.getCategory() != null ? alert.getCategory() : "Unknown",
                        Collectors.counting()
                ));
    }

    public Map<String, Long> getAlertSeverityCounts() {
        List<Alert> alerts = alertRepository.findAll();
        return alerts.stream()
                .collect(Collectors.groupingBy(
                        alert -> alert.getSeverity() != null ? alert.getSeverity() : "Unknown",
                        Collectors.counting()
                ));
    }

    public Optional<Alert> getAlertById(String id) {
        return alertRepository.findById(id);
    }

    // New method to generate Word report
    public ByteArrayInputStream generateAlertReport(Alert alert) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();

        // Placeholder content (define later)
        run.setText("Incident ID: " + alert.getIncidentId());
        run.addBreak();
        run.setText("Title: " + alert.getTitle());
        run.addBreak();
        run.setText("Severity: " + alert.getSeverity());
        run.addBreak();
        run.setText("Category: " + alert.getCategory());
        run.addBreak();
        run.setText("Validity: " + alert.getValidity());
        run.addBreak();
        run.setText("Analysis: " + alert.getLlmAnalysis());
        run.addBreak();
        run.setText("Recommendation: " + alert.getLlmRecommendation());
        run.addBreak();
        run.setText("Created Time: " + alert.getAlertCreationTime());

        document.write(out);
        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    public Map<String, String> challengeRecommendation(String alertId, String previousRecommendation, String analystComment)
            throws JsonProcessingException {
        return llmService.challengeRecommendation(alertId, previousRecommendation, analystComment);

    }
}