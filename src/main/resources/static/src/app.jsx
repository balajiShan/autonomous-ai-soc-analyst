import React, { useState, useEffect } from 'react';
import {
  Container, Typography, Grid, Card, CardContent, IconButton, Drawer, Box, List, ListItem, ListItemText,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, CircularProgress, TextField, Button
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import VisibilityIcon from '@mui/icons-material/Visibility';
import GetAppIcon from '@mui/icons-material/GetApp'; // Import for download icon
import CommentIcon from '@mui/icons-material/Comment'; // Replace FeedbackIcon with CommentIcon
import axios from 'axios';
import ChartJS from 'chart.js/auto'; // Auto registers core controllers and scales
import { Chart } from 'react-chartjs-2';

const App = () => {
  const [dashboardData, setDashboardData] = useState({
    counts: { totalAlerts: 0, truePositives: 0, falsePositives: 0, needsAttention: 0 },
    latestAlerts: [],
    avgTimes: { truePositiveAvgTime: 0, falsePositiveAvgTime: 0 }
  });
  const [categoryChartData, setCategoryChartData] = useState({
    labels: [],
    datasets: [{ data: [], backgroundColor: [] }]
  });
  const [severityChartData, setSeverityChartData] = useState({
    labels: [],
    datasets: [{ data: [], backgroundColor: [] }]
  });
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [chatQuery, setChatQuery] = useState('Show me the 10 latest records');
  const [chatResponse, setChatResponse] = useState(null);
  const [loading, setLoading] = useState(false);

  const [showCommentBox, setShowCommentBox] = useState(false);
  const [analystComment, setAnalystComment] = useState('');

  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

const handleChallengeRecommendation = async (alertId, previousRecommendation, comment) => {
  try {
    const response = await axios.post(`/api/alerts/${alertId}/challenge-recommendation`, {
      previousRecommendation,
      analystComment: comment,
    });
    setSelectedAlert((prev) => ({
      ...prev,
      llmRecommendation: response.data.newRecommendation || 'No new recommendation provided.',
    }));
    setShowCommentBox(false); // Hide the comment box after submission
    setAnalystComment(''); // Clear the comment
  } catch (error) {
    console.error('Error challenging recommendation:', error);
  }
};

const renderTextWithLineBreaks = (text) => {
  if (!text || text === 'N/A') return text;
  return text.split('\n').map((line, index) => (
    <span key={index}>
      {line}
      {index < text.split('\n').length - 1 && <br />}
    </span>
  ));
};

  const handleSort = (column) => {
    if (!chatResponse || !chatResponse.results || chatResponse.results.length === 0) return;

    // Clear existing results before sorting
    setChatResponse((prev) => ({ ...prev, results: [] }));

    // Apply sorting after a brief delay to ensure state clears
    setTimeout(() => {
      const sortedResults = [...chatResponse.results].sort((a, b) => {
        let aValue = a[column];
        let bValue = b[column];

        // Handle date comparison for alertCreationTime
        if (column === 'alertCreationTime' && aValue && bValue) {
          aValue = new Date(aValue);
          bValue = new Date(bValue);
        }

        // Handle null or undefined values
        if (aValue == null || aValue === 'N/A') return sortDirection === 'asc' ? 1 : -1;
        if (bValue == null || bValue === 'N/A') return sortDirection === 'asc' ? -1 : 1;

        if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
        if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
        return 0;
      });

      if (sortColumn === column) {
        setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
      } else {
        setSortColumn(column);
        setSortDirection('asc');
      }

      setChatResponse((prev) => ({ ...prev, results: sortedResults }));
    }, 0);
  };

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [dashboardResponse, categoriesResponse, severitiesResponse] = await Promise.all([
        axios.get('/api/dashboard'),
        axios.get('/api/categories'),
        axios.get('/api/severities')
      ]);

      setDashboardData(dashboardResponse.data);

      // Create Doughnut chart data for categories
      const categories = Object.keys(categoriesResponse.data);
      const categoryCounts = Object.values(categoriesResponse.data);
      setCategoryChartData({
        labels: categories,
        datasets: [{
          data: categoryCounts,
          backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40'],
          hoverOffset: 4,
        }]
      });

      // Create Bar chart data for severities
      const severities = Object.keys(severitiesResponse.data);
      const severityCounts = Object.values(severitiesResponse.data);
      const severityColors = severities.map(severity => {
        switch (severity.toLowerCase()) {
          case 'low': return '#32CD32'; // Green
          case 'medium': return '#FFA500'; // Orange
          case 'high': return '#FF0000'; // Red
          case 'informational': return '#4682B4'; // Steel Blue
          default: return '#808080'; // Gray for Unknown
        }
      });

      setSeverityChartData({
        labels: severities,
        datasets: [{
          label: 'Alert Severities',
          data: severityCounts,
          backgroundColor: severityColors,
          borderColor: severityColors,
          borderWidth: 1,
        }]
      });
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  // Auto-refresh every 5 seconds and initial chatbot query
  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 5000);
    handleChatSubmit(); // Trigger default query on load
    return () => clearInterval(interval);
  }, []);

  const handleViewAlert = async (id) => {
    try {
      const response = await axios.get(`/api/alerts/${id}`);
      setSelectedAlert(response.data);
      setDrawerOpen(true);
    } catch (error) {
      console.error('Error fetching alert details:', error);
    }
  };

  const toggleDrawer = (open) => (event) => {
    if (event.type === 'keydown' && (event.key === 'Tab' || event.key === 'Shift')) {
      return;
    }
    setDrawerOpen(open);
  };

  const handleChatSubmit = async () => {
    try {
      setChatResponse(null); // Reset chatResponse to clear previous table data
      const response = await axios.post('/api/chatbot/query', { query: chatQuery });
      setChatResponse(response.data);
    } catch (error) {
      console.error('Error fetching chatbot response:', error);
      setChatResponse({ query: chatQuery, results: [], additionalInfo: 'Error processing your request.' });
    }
  };

  const handleDownloadReport = async (id) => {
    try {
      const response = await axios.get(`/api/alerts/${id}/report`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `alert_${id}.docx`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (error) {
      console.error('Error downloading report:', error);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Header with Refresh Icon on Left */}
      <Grid container alignItems="center" sx={{ mb: 4 }}>
        <Grid item>
          <IconButton onClick={fetchDashboardData} color="primary">
            <RefreshIcon />
          </IconButton>
        </Grid>
        <Grid item sx={{ ml: 1 }}>
          <Typography variant="h1" align="center">
            AI SOC Dashboard
          </Typography>
        </Grid>
      </Grid>

      {loading && <CircularProgress sx={{ display: 'block', mx: 'auto', my: 2 }} />}

      {/* Widget: Alert Counts */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Typography variant="h6">Total Alerts</Typography>
              <Typography variant="h4">{dashboardData.counts.totalAlerts}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Typography variant="h6">True Positives</Typography>
              <Typography variant="h4">{dashboardData.counts.truePositives}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Typography variant="h6">Needs Attention</Typography>
              <Typography variant="h4">{dashboardData.counts.needsAttention}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={3}>
          <Card>
            <CardContent>
              <Typography variant="h6">False Positives</Typography>
              <Typography variant="h4">{dashboardData.counts.falsePositives}</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* KPI: Average Analysis Time */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Average Analysis Time (Seconds)</Typography>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <Typography>True Positives: {dashboardData.avgTimes.truePositiveAvgTime || 'N/A'}</Typography>
            </Grid>
            <Grid item xs={6}>
              <Typography>False Positives: {dashboardData.avgTimes.falsePositiveAvgTime || 'N/A'}</Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Charts: Categories (Doughnut) and Severities (Bar) */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Alert Categories</Typography>
              {categoryChartData.labels.length > 0 ? (
                <div style={{ height: '300px' }}>
                  <Chart
                    type="doughnut"
                    data={categoryChartData}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: {
                        legend: { position: 'top' },
                        tooltip: { enabled: true },
                      },
                      cutout: '60%',
                    }}
                  />
                </div>
              ) : (
                <Typography>No categories available</Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Alert Severities</Typography>
              {severityChartData.labels.length > 0 ? (
                <div style={{ height: '300px' }}>
                  <Chart
                    type="bar"
                    data={severityChartData}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: {
                        legend: { display: true, position: 'top' },
                        tooltip: { enabled: true },
                      },
                      scales: {
                        y: {
                          beginAtZero: true,
                          title: { display: true, text: 'Count' },
                        },
                      },
                    }}
                  />
                </div>
              ) : (
                <Typography>No severities available</Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Chatbot Card with Integrated Table */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>What do you want to know?</Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <TextField
              fullWidth
              label="Ask a question to the LLM"
              value={chatQuery}
              onChange={(e) => setChatQuery(e.target.value)}
              onKeyPress={(e) => {
                if (e.key === 'Enter') {
                  handleChatSubmit();
                }
              }}
              placeholder="Show me the 10 latest alerts"
              sx={{ mr: 2 }}
            />
            <Button variant="contained" onClick={handleChatSubmit}>Submit</Button>
          </Box>
          {chatResponse && (
            <Box sx={{ mt: 2 }}>
              {chatResponse.results && chatResponse.results.length > 0 ? (
                <div>
                  <TableContainer component={Paper} sx={{ mt: 2 }}>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell
                            onClick={() => handleSort('incidentId')}
                            style={{ cursor: 'pointer' }}
                          >
                            Incident Id{' '}
                            {sortColumn === 'incidentId' && (
                              sortDirection === 'asc' ? '↑' : '↓'
                            )}
                          </TableCell>
                          <TableCell
                            onClick={() => handleSort('title')}
                            style={{ cursor: 'pointer' }}
                          >
                            Title{' '}
                            {sortColumn === 'title' && (
                              sortDirection === 'asc' ? '↑' : '↓'
                            )}
                          </TableCell>
                          <TableCell
                            onClick={() => handleSort('severity')}
                            style={{ cursor: 'pointer' }}
                          >
                            Severity{' '}
                            {sortColumn === 'severity' && (
                              sortDirection === 'asc' ? '↑' : '↓'
                            )}
                          </TableCell>
                          <TableCell
                            onClick={() => handleSort('category')}
                            style={{ cursor: 'pointer' }}
                          >
                            Category{' '}
                            {sortColumn === 'category' && (
                              sortDirection === 'asc' ? '↑' : '↓'
                            )}
                          </TableCell>
                          <TableCell
                            onClick={() => handleSort('alertCreationTime')}
                            style={{ cursor: 'pointer' }}
                          >
                            Created Time{' '}
                            {sortColumn === 'alertCreationTime' && (
                              sortDirection === 'asc' ? '↑' : '↓'
                            )}
                          </TableCell>
                          <TableCell
                            onClick={() => handleSort('validity')}
                            style={{ cursor: 'pointer' }}
                          >
                            Validity{' '}
                            {sortColumn === 'validity' && (
                              sortDirection === 'asc' ? '↑' : '↓'
                            )}
                          </TableCell>
                          <TableCell>Action</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {chatResponse.results.map((alert) => (
                          <TableRow key={alert.incidentId}>
                            <TableCell>{alert.incidentId}</TableCell>
                            <TableCell>{alert.title}</TableCell>
                            <TableCell>{alert.severity}</TableCell>
                            <TableCell>{alert.category}</TableCell>
                            <TableCell>{alert.alertCreationTime}</TableCell>
                            <TableCell>{alert.validity || 'N/A'}</TableCell>
                            <TableCell>
                              <IconButton onClick={() => handleViewAlert(alert.id)} color="primary">
                                <VisibilityIcon />
                              </IconButton>
                              <IconButton onClick={() => handleDownloadReport(alert.id)} color="secondary">
                                <GetAppIcon />
                              </IconButton>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </div>
              ) : (
                <Typography>No matching alerts found.</Typography>
              )}
              <Typography>Additional Info: {chatResponse.additionalInfo || 'N/A'}</Typography>
            </Box>
          )}
        </CardContent>
      </Card>

      {/* Right-Side Pullout Pane (Drawer) */}
      <Drawer anchor="right" open={drawerOpen} onClose={toggleDrawer(false)}>
        {selectedAlert ? (
          <Box sx={{ width: 500, p: 2, fontSize: '0.8rem' }}>
            <Typography variant="h6" gutterBottom>Alert Details</Typography>
            <List>
              <ListItem>
                <ListItemText primary="Incident Id" secondary={selectedAlert.incidentId} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Title" secondary={selectedAlert.title} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Description" secondary={renderTextWithLineBreaks(selectedAlert.description)} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Severity" secondary={selectedAlert.severity} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Category" secondary={selectedAlert.category} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Validity (from LLM)" secondary={selectedAlert.validity || 'N/A'} />
              </ListItem>
              <ListItem>
                <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                  <ListItemText
                    primary="Recommendation (from LLM)"
                    secondary={renderTextWithLineBreaks(selectedAlert.llmRecommendation || 'N/A')}
                  />
                  <IconButton
                    onClick={() => setShowCommentBox(!showCommentBox)}
                    color="primary"
                    title="Challenge Recommendation"
                  >
                    <CommentIcon />
                  </IconButton>
                </Box>
              </ListItem>
              {showCommentBox && (
                <ListItem>
                  <Box sx={{ width: '100%' }}>
                    <TextField
                      fullWidth
                      label="Analyst Comment"
                      value={analystComment}
                      onChange={(e) => setAnalystComment(e.target.value)}
                      multiline
                      rows={3}
                      placeholder="Provide feedback to improve the recommendation"
                      sx={{ mb: 1 }}
                    />
                    <Button
                      variant="contained"
                      onClick={() =>
                        handleChallengeRecommendation(
                          selectedAlert.id,
                          selectedAlert.llmRecommendation,
                          analystComment
                        )
                      }
                      disabled={!analystComment.trim()}
                    >
                      Submit Challenge
                    </Button>
                  </Box>
                </ListItem>
              )}
              <ListItem>
                <ListItemText primary="Analysis (from LLM)" secondary={renderTextWithLineBreaks(selectedAlert.llmAnalysis) || 'N/A'} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Virus Total Malicious Count" secondary={selectedAlert.virusTotalMaliciousCount || 'N/A'} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Abuse IP DB Confidence Score" secondary={selectedAlert.abuseIpdbConfidenceScore || 'N/A'} />
              </ListItem>
              <ListItem>
                <ListItemText primary="Created Time" secondary={selectedAlert.alertCreationTime} />
              </ListItem>
            </List>
          </Box>
        ) : (
          <Typography>Loading...</Typography>
        )}
      </Drawer>
    </Container>
  );
};

export default App;