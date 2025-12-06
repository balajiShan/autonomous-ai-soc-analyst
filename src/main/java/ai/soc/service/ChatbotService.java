package ai.soc.service;

import ai.soc.entity.Alert;
import ai.soc.service.external.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChatbotService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private AlertService alertService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LlmService llmService;

    public List<Alert> executeDynamicSql(String sql) {
        return jdbcTemplate.query(sql, new RowMapper<Alert>() {
            @Override
            public Alert mapRow(ResultSet rs, int rowNum) throws SQLException {
                Alert alert = new Alert();
                alert.setId(rs.getString("id"));
                alert.setIncidentId(rs.getLong("incident_id"));
                alert.setTitle(rs.getString("title"));
                alert.setSeverity(rs.getString("severity"));
                alert.setCategory(rs.getString("category"));
                alert.setAlertCreationTime(rs.getTimestamp("alert_creation_time").toLocalDateTime());
                alert.setValidity(rs.getString("validity"));
                return alert;
            }
        });
    }

    public Map<String, Object> processQuery(String userQuery) {
        Map<String, Object> response = new HashMap<>();
        response.put("query", userQuery);

        try {
            // Call LLM to generate SQL query
            String sqlQuery = llmService.generateSqlFromLlm(userQuery);

            sqlQuery = sanitizeSqlQuery(sqlQuery);

            // Execute the SQL query
            List<Alert> results = executeDynamicSql(sqlQuery);

            // Enrich with LLM context
            if (!results.isEmpty() && !results.isEmpty()) {
                response.put("additionalInfo", "Found " + results.size() + " alerts");
                response.put("results", results);
            } else {
                response.put("results", List.of());
                response.put("additionalInfo", "Sorry, I couldn't understand your question. Please try a different question.");
            }

        } catch (Exception e) {
            log.error("Error processing query: {}", userQuery, e);
            response.put("results", List.of());
            response.put("additionalInfo", "Failed to process the query. Please try again later");
        }

        return response;
    }

    private String sanitizeSqlQuery(String sqlQuery){
        sqlQuery = sqlQuery
                .replaceAll("\\n", "")
                .replaceAll("WHERE", " WHERE")
                .replaceAll("ORDER", " ORDER")
                .replaceAll("DESC", " DESC")
                .replaceAll("LIMIT", " LIMIT")
                .replaceAll("FROM", " FROM");
        return sqlQuery;
    }

}