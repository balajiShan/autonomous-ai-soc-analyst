package ai.soc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {
    private Counts counts;
    private List<AlertSummary> latestAlerts;
    private AvgTimes avgTimes;

    @Setter
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Counts {
        private long totalAlerts;
        private long truePositives;
        private Long needsAttention;
        private long falsePositives;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlertSummary {
        private String id;
        private Long incidentId;
        private String title;
        private String severity;
        private String category;
        private String alertCreationTime;
        private String validity;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AvgTimes {
        private Double truePositiveAvgTime;
        private Double falsePositiveAvgTime;
    }
}