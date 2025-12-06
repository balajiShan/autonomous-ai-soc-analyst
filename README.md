# Autonomous AI SOC Analyst 

A full-stack AI-powered Security Operations Center (SOC) platform built with Spring Boot (backend) and React (frontend), seamlessly integrated with Microsoft Defender for Endpoint, LiteLLM, VirusTotal, and AbuseIPDB.

This autonomous system transforms traditional SOC operations by leveraging Large Language Models (LLM) to perform intelligent alert triage, enrich alerts with real-time threat intelligence, automatically classify alerts as True Positive / False Positive, generate actionable recommendations, and deliver in-depth root-cause analysis — dramatically reducing analyst workload, accelerating incident response, and enabling a proactive, AI-driven security posture.

## Key Features

- Real-time ingestion of Microsoft Defender alerts
- LLM-powered alert classification (True/False Positive)
- Automated recommendation & root cause analysis using LLM
- Threat intelligence enrichment via VirusTotal & AbuseIPDB
- Interactive chatbot with natural language querying (e.g., "Show me high-severity malware alerts from last 24 hours")
- Dynamic SQL generation via LLM for advanced alert search
- Visualizations: Alert categories (Doughnut), Severities (Bar), KPIs
- Detailed alert view with LLM analysis
- One-click Word document report generation per alert
- Sortable, searchable, and responsive alert table
- Right-side drawer for alert details

## Tech Stack

### Backend (Spring Boot)
- Java 21
- Spring Boot 3.5.6
- Spring Data JPA + MySQL
- Spring Security
- Apache POI (for .docx report generation)

### Frontend (React)
- React 18 + Vite
- Material-UI (MUI) v5
- Chart.js + react-chartjs-2

### Integrations
- Microsoft Defender for Endpoint API
- LiteLLM (supports OpenAI, Anthropic, etc.)
- VirusTotal API
- AbuseIPDB API

## Prerequisites
- Java 21 or higher
- Node.js 18+
- MySQL 8.0+
- Microsoft Defender for Endpoint access
- API keys for:
    - LiteLLM (OpenAI / Claude / etc.)
    - VirusTotal
    - AbuseIPDB

## Setup & Installation

### 1. Clone the Repository
```bash
  git clone https://github.com/yourusername/soc-alert-dashboard.git
  cd soc-alert-dashboard
```

### 2. Backend Setup
Update src/main/resources/application.yaml with your credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/default?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: MyRootPassword123
    
defender:
  api-base: https://api.securitycenter.microsoft.com
  token-endpoint: https://login.microsoftonline.com
  tenant-id: test-tenant-id
  client-id: test-client-id
  client-secret: test-client-secret
  scope: https://api.securitycenter.microsoft.com/.default
  suppress-assigned-to: suppressor@client.com

virustotal:
  api-base: https://www.virustotal.com/api/v3
  api-key: virut-total-api-key

abuseipdb:
  api-base: https://api.abuseipdb.com/api/v2
  api-key: abuse-ip-db-api-key

litellm:
  api-base: https://your-litellm-endpoint.com/
  api-key: your-litellm-api-key
  model: azure/gpt-4.1-nano
```
### 3. Frontend Setup

``` bash
  cd src/main/resources/static
  npm install
  npm run dev
```

### 4. Build for Production
```Bash
  npm run build
  ./mvnw clean package
  java -jar target/alert-dashboard-0.0.1-SNAPSHOT.jar
```
Access the app at: http://localhost:7095

---

Built with passion for smarter SOC operations

Last updated: December 2025
