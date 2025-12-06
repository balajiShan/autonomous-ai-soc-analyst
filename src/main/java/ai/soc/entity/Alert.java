package ai.soc.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alerts")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Alert {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "investigation_id")
    private Long investigationId;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "classification")
    private String classification;

    @Column(name = "determination")
    private String determination;

    @Column(name = "investigation_state", nullable = false)
    private String investigationState;

    @Column(name = "detection_source", nullable = false)
    private String detectionSource;

    @Column(name = "detector_id", nullable = false)
    private String detectorId;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "threat_family_name")
    private String threatFamilyName;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "alert_creation_time")
    private LocalDateTime alertCreationTime;

    @Column(name = "first_event_time")
    private LocalDateTime firstEventTime;

    @Column(name = "last_event_time")
    private LocalDateTime lastEventTime;

    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;

    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    @Column(name = "machine_id", nullable = false)
    private String machineId;

    @Column(name = "computer_dns_name", nullable = false)
    private String computerDnsName;

    @Column(name = "rbac_group_name")
    private String rbacGroupName;

    @Column(name = "aad_tenant_id", nullable = false)
    private String aadTenantId;

    @Column(name = "threat_name")
    private String threatName;

    @Column(name = "mitre_techniques", columnDefinition = "JSON")
    private String mitreTechniques;  // Stored as JSON string

    @Column(name = "related_user_name")
    private String relatedUserName;

    @Column(name = "related_domain_name")
    private String relatedDomainName;

    @Column(name = "raw_json", columnDefinition = "JSON", nullable = false)
    private String rawJson;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<AlertComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<AlertEvidence> evidence = new ArrayList<>();

    @Column(name = "validity")
    private String validity;  // New field for LLM validity assessment

    @Column(name = "virustotal_malicious_count")
    private Long virusTotalMaliciousCount;

    @Column(name = "abuseipdb_confidence_score")
    private Long abuseIpdbConfidenceScore;

    @Column(name = "llm_recommendation", columnDefinition = "TEXT")
    private String llmRecommendation;

    @Column(name = "llm_analysis", columnDefinition = "TEXT")
    private String llmAnalysis;

}