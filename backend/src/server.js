const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const { connectDB, isDbConnected } = require('./config/db');
const { isFirebaseConfigured } = require('./config/firebase');
const userRoutes = require('./routes/userRoutes');
const parkingRoutes = require('./routes/parkingRoutes');
const bookingRoutes = require('./routes/bookingRoutes');

const app = express();
const PORT = process.env.PORT || 5000;

// Security and utility middleware
app.use(helmet());
app.use(cors());
app.use(morgan('dev'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Health Check Endpoint
app.get('/api/health', (req, res) => {
  const dbStatus = isDbConnected() ? 'connected' : 'disconnected';
  const fbStatus = isFirebaseConfigured() ? 'configured' : 'missing_service_account';

  res.status(200).json({
    status: 'ok',
    service: 'ParkShare Backend API',
    database: dbStatus,
    firebaseAdmin: fbStatus,
    timestamp: new Date().toISOString()
  });
});

// Mount Routes
app.use('/api/users', userRoutes);
app.use('/api/parking', parkingRoutes);
app.use('/api/bookings', bookingRoutes);

// 404 Handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: `Cannot ${req.method} ${req.originalUrl}`
  });
});

// Centralized Error Handler
app.use((err, req, res, next) => {
  console.error(`[Unhandled Error] ${err.stack || err.message}`);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || 'Internal Server Error'
  });
});

// Server Initialization
let server;

const startServer = async () => {
  await connectDB();

  server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`===============================================`);
    console.log(` ParkShare Backend Service`);
    console.log(` Port: ${PORT}`);
    console.log(` Environment: ${process.env.NODE_ENV || 'development'}`);
    console.log(` Database: ${isDbConnected() ? 'Connected' : 'Disconnected (Set MONGODB_URI)'}`);
    console.log(` Firebase: ${isFirebaseConfigured() ? 'Configured' : 'Missing Credentials'}`);
    console.log(`===============================================`);
  });
};

// Graceful Shutdown
const shutdown = () => {
  console.log('\n[Server] Shutting down gracefully...');
  if (server) {
    server.close(() => {
      console.log('[Server] HTTP server closed.');
      process.exit(0);
    });
  } else {
    process.exit(0);
  }
};

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

if (process.env.NODE_ENV !== 'test') {
  startServer();
}

module.exports = app;
