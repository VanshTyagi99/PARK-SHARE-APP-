const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

let isInitialized = false;

const initFirebase = () => {
  if (admin.apps.length > 0) {
    isInitialized = true;
    return admin;
  }

  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_KEY;
  const projectId = process.env.FIREBASE_PROJECT_ID || 'parkshare-537b9';

  try {
    if (serviceAccountPath) {
      const resolvedPath = path.isAbsolute(serviceAccountPath)
        ? serviceAccountPath
        : path.resolve(process.cwd(), serviceAccountPath);

      if (fs.existsSync(resolvedPath)) {
        const serviceAccount = JSON.parse(fs.readFileSync(resolvedPath, 'utf8'));
        admin.initializeApp({
          credential: admin.credential.cert(serviceAccount),
          projectId: serviceAccount.project_id || projectId
        });
        isInitialized = true;
        console.log(`[Firebase Admin] Initialized with Service Account for project: ${serviceAccount.project_id || projectId}`);
        return admin;
      } else {
        console.warn(`[Firebase Admin] Service account file not found at: ${resolvedPath}`);
      }
    }

    // Default initialization with project ID
    admin.initializeApp({
      projectId: projectId
    });
    isInitialized = true;
    console.log(`[Firebase Admin] Initialized with Project ID: ${projectId}`);
    return admin;
  } catch (error) {
    console.error(`[Firebase Admin] Initialization failed: ${error.message}`);
    isInitialized = false;
    return null;
  }
};

initFirebase();

module.exports = {
  admin,
  isFirebaseConfigured: () => isInitialized && admin.apps.length > 0
};
