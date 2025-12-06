package ai.soc.controller;

import ai.soc.dto.AlertResponse;
import ai.soc.dto.DashboardResponse;
import ai.soc.entity.Alert;
import ai.soc.service.AlertService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/api")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @PostMapping("/alerts")
    public ResponseEntity<AlertResponse> createAlert(@RequestBody String jsonPayload) {
        String validityOfAlert = alertService.saveAlert(jsonPayload);
        AlertResponse ar = new AlertResponse();
        ar.setResponse(validityOfAlert);
        return ResponseEntity.ok(ar);
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable String id) {
        Optional<Alert> alert = alertService.getAlertById(id);
        return alert.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/alerts")
    public List<Alert> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboardData() {
        return ResponseEntity.ok(alertService.getDashboardData());
    }

    @GetMapping("/categories")
    public Map<String, Long> getAlertCategoryCounts() {
        return alertService.getAlertCategoryCounts();
    }

    @GetMapping("/severities")
    public Map<String, Long> getAlertSeverityCounts() {
        return alertService.getAlertSeverityCounts();
    }

    // New endpoint for report generation and download
    @GetMapping("/alerts/{id}/report")
    public ResponseEntity<byte[]> generateAlertReport(@PathVariable String id) {
        Optional<Alert> alert = alertService.getAlertById(id);
        if (alert.isPresent()) {
            try {
                ByteArrayInputStream in = alertService.generateAlertReport(alert.get());
                byte[] bytes = in.readAllBytes();

                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Disposition", "attachment; filename=alert_report_" + id + ".docx");
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            } catch (Exception e) {
                log.error("Error generating report for alert {}", id, e);
                return ResponseEntity.internalServerError().build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/alerts/{alertId}/challenge-recommendation")
    public ResponseEntity<Map<String, String>> challengeRecommendation(
            @PathVariable String alertId,
            @RequestBody Map<String, String> requestBody) throws JsonProcessingException {

        String previousRecommendation = requestBody.get("previousRecommendation");
        String analystComment = requestBody.get("analystComment");

        Map<String, String> response = alertService.challengeRecommendation(alertId, previousRecommendation, analystComment);
        return ResponseEntity.ok(response);
    }

}