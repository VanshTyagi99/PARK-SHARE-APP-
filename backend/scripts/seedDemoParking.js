const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const ParkingListing = require('../src/models/ParkingListing');
const User = require('../src/models/User');

const DEMO_OWNER_UID = 'demo_partner_ncr_01';

const demoListings = [
  // --- Noida ---
  {
    demoSeedId: 'noida-sec18-driveway',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Sec 18 Atta Market Gated Driveway',
    address: 'B-Block, Near Wave Silver Tower, Sector 18, Noida',
    description: 'Covered and secure private driveway parking spot just 2 minutes walking distance from Sector 18 Metro & Mall of India. CCTV monitored with 24/7 guard.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 40,
    availableFrom: '08:00 AM',
    availableUntil: '11:00 PM',
    latitude: 28.5708,
    longitude: 77.3260,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'noida-sec62-itpark',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Sector 62 Tech Park Covered Space',
    address: 'Plot C-56, Institutional Area, Sector 62, Noida',
    description: 'Spacious basement parking spot suitable for office employees and visitors. High-speed EV charging available on request. RFID gate access.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 35,
    availableFrom: '07:00 AM',
    availableUntil: '10:00 PM',
    latitude: 28.6280,
    longitude: 77.3649,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'noida-sec15-metro',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Sector 15 Metro Quick Park Spot',
    address: 'Near Metro Gate 2, Pocket A, Sector 15, Noida',
    description: 'Paved two-wheeler and hatchback parking space right beside Sector 15 Metro Station. Perfect for daily commuters hopping onto Blue Line.',
    parkingType: 'Car + Bike',
    vehicleCompatibility: 'CAR_AND_BIKE',
    pricePerHour: 25,
    availableFrom: '06:00 AM',
    availableUntil: '11:30 PM',
    latitude: 28.5855,
    longitude: 77.3117,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'noida-botanical-garden',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Botanical Garden Interchange Parking',
    address: 'Block E, Sector 37, Near Botanical Garden Metro, Noida',
    description: 'Wide secure residential parking space suitable for SUVs and sedans. 3 min walk to Magenta and Blue Line interchange station.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 50,
    availableFrom: '06:00 AM',
    availableUntil: '11:00 PM',
    latitude: 28.5644,
    longitude: 77.3344,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'noida-sec50-bike',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Sector 50 Central Market Bike Spot',
    address: 'Near Amrapali Crystal Market, Sector 50, Noida',
    description: 'Dedicated shade parking for bikes and scooters. Guarded society entry with quick street access.',
    parkingType: 'Bike',
    vehicleCompatibility: 'BIKE',
    pricePerHour: 15,
    availableFrom: '09:00 AM',
    availableUntil: '10:00 PM',
    latitude: 28.5830,
    longitude: 77.3680,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'noida-sec137-expressway',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Noida Expressway Sector 137 Spot',
    address: 'Tower A Ground Bay, Paras Tierea, Sector 137, Noida',
    description: 'Gated society parking spot right off the Noida-Greater Noida Expressway. Next to Metro Station, CCTV enabled.',
    parkingType: 'Car + Bike',
    vehicleCompatibility: 'CAR_AND_BIKE',
    pricePerHour: 30,
    availableFrom: '24/7',
    availableUntil: '24/7',
    latitude: 28.5098,
    longitude: 77.4082,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'noida-sec16-filmcity',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Film City Sector 16 Media Hub Parking',
    address: 'Studio Row, FC-24, Sector 16A Film City, Noida',
    description: 'Dedicated open-air parking space in commercial media district. Secure security guard presence.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 45,
    availableFrom: '08:00 AM',
    availableUntil: '09:00 PM',
    latitude: 28.5670,
    longitude: 77.3180,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'noida-wave-city-center',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Wave City Center Sector 32 Covered Bay',
    address: 'Sector 32, Opposite City Center Metro, Noida',
    description: 'Indoor basement parking spot with direct lift access. Clean, well-lit, and sheltered from sun and rain.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 40,
    availableFrom: '07:00 AM',
    availableUntil: '11:00 PM',
    latitude: 28.5746,
    longitude: 77.3562,
    isActive: true,
    isDemoData: true
  },

  // --- Indirapuram ---
  {
    demoSeedId: 'indirapuram-shipramall',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Shipra Mall Area Gated Driveway',
    address: 'Vaibhav Khand 1, Near Shipra Mall, Indirapuram, Ghaziabad',
    description: 'Avoid crowded mall basements. Safe villa driveway spot just 100 meters from Shipra Mall entry.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 35,
    availableFrom: '10:00 AM',
    availableUntil: '11:00 PM',
    latitude: 28.6387,
    longitude: 77.3705,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'indirapuram-ahinsa-khand',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Ahinsa Khand 2 Family Bay',
    address: 'Near Jaipuria Sunrise Plaza, Ahinsa Khand 2, Indirapuram',
    description: 'Wide concrete porch parking for both sedan and scooter. Automated lighting and CCTV.',
    parkingType: 'Car + Bike',
    vehicleCompatibility: 'CAR_AND_BIKE',
    pricePerHour: 30,
    availableFrom: '08:00 AM',
    availableUntil: '10:00 PM',
    latitude: 28.6350,
    longitude: 77.3820,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'indirapuram-swarna-jayanti',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Swarna Jayanti Park Morning Park Bay',
    address: 'Niti Khand 3, Facing Swarna Jayanti Park, Indirapuram',
    description: 'Convenient early morning and day parking for park walkers and shoppers visiting Niti Khand market.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 20,
    availableFrom: '05:30 AM',
    availableUntil: '09:00 PM',
    latitude: 28.6480,
    longitude: 77.3690,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'indirapuram-windsor-bike',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Windsor Street Food Lane Bike Spot',
    address: 'Windsor Park Commercial Lane, Vaibhav Khand, Indirapuram',
    description: 'Fast and convenient scooter/motorcycle parking near popular eateries and bakeries on Windsor Street.',
    parkingType: 'Bike',
    vehicleCompatibility: 'BIKE',
    pricePerHour: 15,
    availableFrom: '11:00 AM',
    availableUntil: '11:30 PM',
    latitude: 28.6420,
    longitude: 77.3750,
    isActive: true,
    isDemoData: true
  },

  // --- Vaishali ---
  {
    demoSeedId: 'vaishali-metro-station',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Vaishali Metro Station Priority Spot',
    address: 'Sector 4, Within 150m of Vaishali Blue Line Metro, Ghaziabad',
    description: 'Skip the long metro parking queues! Dedicated private slot with easy walk to station turnstiles.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 45,
    availableFrom: '06:00 AM',
    availableUntil: '11:00 PM',
    latitude: 28.6492,
    longitude: 77.3396,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'vaishali-mahagun-mall',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Mahagun Metro Mall Side Bay',
    address: 'Plot VC-3, Sector 4, Opposite Mahagun Mall, Vaishali',
    description: 'Easy-access outdoor parking right across from Mahagun Mall. Suitable for compact cars and sedans.',
    parkingType: 'Car + Bike',
    vehicleCompatibility: 'CAR_AND_BIKE',
    pricePerHour: 30,
    availableFrom: '09:00 AM',
    availableUntil: '10:30 PM',
    latitude: 28.6475,
    longitude: 77.3450,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'vaishali-sec1-peaceful',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Sector 1 Vaishali Residential Driveway',
    address: 'House 142, Sector 1, Vaishali, Ghaziabad',
    description: 'Quiet, shaded residential car porch. Host is an EV owner with standard 15A socket access available.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 25,
    availableFrom: '08:00 AM',
    availableUntil: '08:00 PM',
    latitude: 28.6530,
    longitude: 77.3350,
    isActive: true,
    isDemoData: true
  },

  // --- Ghaziabad ---
  {
    demoSeedId: 'ghaziabad-rajnagar-ext',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Raj Nagar Extension High-Street Space',
    address: 'Main Meerut Bypass Road, Raj Nagar Extension, Ghaziabad',
    description: 'Secure commercial complex front parking with wide driveway and 24/7 security booth.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 25,
    availableFrom: '07:00 AM',
    availableUntil: '11:00 PM',
    latitude: 28.7050,
    longitude: 77.4320,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'ghaziabad-rdc-commercial',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'RDC Raj Nagar Prime Commercial Spot',
    address: 'RDC Business Hub, Sector 15, Raj Nagar, Ghaziabad',
    description: 'Prime parking spot in the heart of Ghaziabad commercial district (banks, coaching, restaurants). Avoid street congestion.',
    parkingType: 'Car + Bike',
    vehicleCompatibility: 'CAR_AND_BIKE',
    pricePerHour: 40,
    availableFrom: '08:30 AM',
    availableUntil: '09:30 PM',
    latitude: 28.6810,
    longitude: 77.4420,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'ghaziabad-kavinagar-market',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Kavi Nagar C-Block Community Parking',
    address: 'Near Diamond Palace, C-Block, Kavi Nagar, Ghaziabad',
    description: 'Clean paved parking slot inside a gated colony. High safety rating with night security guards.',
    parkingType: 'Car',
    vehicleCompatibility: 'CAR',
    pricePerHour: 30,
    availableFrom: '08:00 AM',
    availableUntil: '10:00 PM',
    latitude: 28.6670,
    longitude: 77.4560,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'ghaziabad-mohannagar-metro',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Mohan Nagar Red Line Metro Bay',
    address: 'Near Mohan Nagar Junction & Red Line Metro, Ghaziabad',
    description: 'Quick access parking spot right by Mohan Nagar Temple and Red Line metro terminal. Ideal for Sahibabad & Delhi travel.',
    parkingType: 'Car + Bike',
    vehicleCompatibility: 'CAR_AND_BIKE',
    pricePerHour: 30,
    availableFrom: '06:00 AM',
    availableUntil: '11:00 PM',
    latitude: 28.6780,
    longitude: 77.3980,
    isActive: true,
    isDemoData: true
  },
  {
    demoSeedId: 'ghaziabad-old-bus-stand',
    ownerFirebaseUid: DEMO_OWNER_UID,
    title: 'Old Bus Stand GT Road Two-Wheeler Bay',
    address: 'Opposite GDA Tower, GT Road, Ghaziabad',
    description: 'Affordable two-wheeler parking in downtown Ghaziabad market hub.',
    parkingType: 'Bike',
    vehicleCompatibility: 'BIKE',
    pricePerHour: 15,
    availableFrom: '09:00 AM',
    availableUntil: '09:00 PM',
    latitude: 28.6650,
    longitude: 77.4350,
    isActive: true,
    isDemoData: true
  }
];

const seedDemoData = async () => {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    console.error('✗ ERROR: MONGODB_URI is not set in backend/.env');
    process.exit(1);
  }

  try {
    console.log('Connecting to MongoDB Atlas...');
    await mongoose.connect(uri, { serverSelectionTimeoutMS: 5000 });
    console.log('✓ Connected to MongoDB Atlas');

    // Step 1: Ensure demo partner owner user exists in User collection
    console.log('Ensuring demo partner owner profile exists...');
    await User.findOneAndUpdate(
      { firebaseUid: DEMO_OWNER_UID },
      {
        $setOnInsert: {
          firebaseUid: DEMO_OWNER_UID,
          email: 'ncr.partner@parkshare.demo',
          fullName: 'NCR Demo Parking Partner',
          phone: '+91 98765 43210',
          role: 'user',
          listingCount: demoListings.length
        }
      },
      { upsert: true, new: true }
    );
    console.log(`✓ Demo owner verified: ${DEMO_OWNER_UID}`);

    // Step 2: Seed / Upsert demo listings (Idempotent: matches on demoSeedId)
    console.log(`Seeding ${demoListings.length} demo parking listings across Noida, Ghaziabad, Indirapuram, Vaishali...`);

    let insertedCount = 0;
    let updatedCount = 0;

    for (const item of demoListings) {
      // Find existing by title & address, or demoSeedId
      const existing = await ParkingListing.findOne({
        $or: [
          { address: item.address },
          { title: item.title, ownerFirebaseUid: DEMO_OWNER_UID }
        ]
      });

      if (!existing) {
        const newListing = new ParkingListing(item);
        await newListing.save();
        insertedCount++;
      } else {
        // Update fields to keep demo data fresh
        Object.assign(existing, item);
        await existing.save();
        updatedCount++;
      }
    }

    console.log('\n==================================================');
    console.log(`✓ SEEDING COMPLETE!`);
    console.log(`  Newly Inserted: ${insertedCount}`);
    console.log(`  Updated/Synced: ${updatedCount}`);
    console.log(`  Total Active Demo Listings: ${demoListings.length}`);
    console.log('==================================================\n');

    await mongoose.disconnect();
    console.log('✓ Disconnected cleanly from MongoDB Atlas.');
    process.exit(0);
  } catch (error) {
    console.error('✗ Seed error:', error.message);
    process.exit(1);
  }
};

seedDemoData();
