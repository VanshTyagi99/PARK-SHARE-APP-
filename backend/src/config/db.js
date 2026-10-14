const mongoose = require('mongoose');

let isConnected = false;

const connectDB = async () => {
  const uri = process.env.MONGODB_URI;

  if (!uri) {
    console.warn('[Database] WARNING: MONGODB_URI is not set in environment variables. Database operations will fail until configured.');
    isConnected = false;
    return false;
  }

  try {
    const conn = await mongoose.connect(uri, {
      serverSelectionTimeoutMS: 5000
    });
    isConnected = true;
    console.log(`[Database] MongoDB Connected: ${conn.connection.host}/${conn.connection.name}`);
    return true;
  } catch (error) {
    isConnected = false;
    const sanitizedMsg = error.message.replace(/:\/\/([^:]+):([^@]+)@/g, '://<credentials>@');
    console.error(`[Database] Connection Error: ${sanitizedMsg}`);
    return false;
  }
};

mongoose.connection.on('disconnected', () => {
  isConnected = false;
  console.warn('[Database] MongoDB disconnected.');
});

mongoose.connection.on('reconnected', () => {
  isConnected = true;
  console.log('[Database] MongoDB reconnected.');
});

const isDbConnected = () => {
  return mongoose.connection.readyState === 1;
};

module.exports = {
  connectDB,
  isDbConnected
};
