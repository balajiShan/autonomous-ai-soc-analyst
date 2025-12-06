package ai.soc.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_evidence")
@Data
public class AlertEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alert_id", nullable = false)
    @JsonBackReference
    private Alert alert;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "evidence_creation_time", nullable = false)
    private LocalDateTime evidenceCreationTime;

    @Column(name = "sha1")
    private String sha1;

    @Column(name = "sha256")
    private String sha256;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "process_command_line", columnDefinition = "TEXT")
    private String processCommandLine;

    @Column(name = "process_creation_time")
    private LocalDateTime processCreationTime;

    @Column(name = "parent_process_id")
    private Long parentProcessId;

    @Column(name = "parent_process_creation_time")
    private LocalDateTime parentProcessCreationTime;

    @Column(name = "parent_process_file_name")
    private String parentProcessFileName;

    @Column(name = "parent_process_file_path", columnDefinition = "TEXT")
    private String parentProcessFilePath;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "registry_key", columnDefinition = "TEXT")
    private String registryKey;

    @Column(name = "registry_hive")
    private String registryHive;

    @Column(name = "registry_value_type")
    private String registryValueType;

    @Column(name = "registry_value", columnDefinition = "TEXT")
    private String registryValue;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "user_sid")
    private String userSid;

    @Column(name = "aad_user_id")
    private String aadUserId;

    @Column(name = "user_principal_name")
    private String userPrincipalName;

    @Column(name = "detection_status")
    private String detectionStatus;
}