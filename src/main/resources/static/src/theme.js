import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2', // Deep blue for glossy effect
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          background: 'linear-gradient(135deg, #ffffff 0%, #e3f2fd 100%)', // Glossy gradient
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.1)',
          borderRadius: '12px',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: '8px',
          textTransform: 'none',
          background: 'linear-gradient(45deg, #1976d2 30%, #42a5f5 90%)',
          color: '#ffffff',
          boxShadow: '0 3px 5px rgba(0, 0, 0, 0.2)',
        },
      },
    },
    MuiTable: {
      styleOverrides: {
        root: {
          background: '#ffffff',
          boxShadow: '0 2px 10px rgba(0, 0, 0, 0.05)',
        },
      },
    },
  },
  typography: {
    h1: {
      fontWeight: 700,
      color: '#333',
    },
    h6: {
      fontWeight: 600,
    },
  },
});

export default theme;