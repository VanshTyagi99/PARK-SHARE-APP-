const assert = require('assert');
const mongoose = require('mongoose');
const Booking = require('../src/models/Booking');
const ParkingListing = require('../src/models/ParkingListing');

let passed = 0;
let failed = 0;

function assertEqual(name, actual, expected, extra = '') {
  if (actual === expected) {
    console.log(`✓ PASS: ${name}`);
    passed++;
  } else {
    console.error(`✗ FAIL: ${name} (Expected: ${expected}, Got: ${actual}) ${extra}`);
    failed++;
  }
}

function assertTrue(name, condition, extra = '') {
  if (condition) {
    console.log(`✓ PASS: ${name}`);
    passed++;
  } else {
    console.error(`✗ FAIL: ${name} (Condition was false) ${extra}`);
    failed++;
  }
}

console.log('--- Running Sub-Phase 4.1: Booking Model & Concurrency Unit Test Suite ---\n');

// ==========================================
// Group 1: Schema Structure, Defaults & Enums
// ==========================================
console.log('[Group 1: Schema Structure, Defaults & Enums]');

// Test 1.1: Default status is PENDING and bookingCode is auto-generated
const validDoc = new Booking({
  listingId: new mongoose.Types.ObjectId(),
  driverFirebaseUid: 'driver_test_001',
  ownerFirebaseUid: 'owner_test_001',
  startTime: new Date('2026-10-15T10:00:00.000Z'),
  endTime: new Date('2026-10-15T12:00:00.000Z'),
  totalHours: 2,
  hourlyRateSnapshot: 40,
  totalAmount: 80
});

const valError = validDoc.validateSync();
assertEqual('Valid booking document passes validation without errors', valError, undefined);
assertEqual('Default status is PENDING', validDoc.status, 'PENDING');
assertTrue('Booking code is auto-generated in format PS-YYYYMMDD-XXXX', /^PS-\d{8}-[A-F0-9]{6}$/.test(validDoc.bookingCode));
assertEqual('Default vehicleType is Car', validDoc.vehicleType, 'Car');

// Test 1.2: All 10 approved statuses pass enum validation
const approvedStatuses = [
  'PENDING',
  'ACCEPTED',
  'AWAITING_DRIVER_CONFIRMATION',
  'CONFIRMED',
  'CHECKED_IN',
  'CHECKED_OUT',
  'COMPLETED',
  'REJECTED',
  'CANCELLED',
  'EXPIRED'
];

let allStatusesValid = true;
for (const status of approvedStatuses) {
  const doc = new Booking({
    listingId: new mongoose.Types.ObjectId(),
    driverFirebaseUid: 'driver_test_001',
    ownerFirebaseUid: 'owner_test_001',
    startTime: new Date('2026-10-15T10:00:00.000Z'),
    endTime: new Date('2026-10-15T12:00:00.000Z'),
    totalHours: 2,
    hourlyRateSnapshot: 40,
    totalAmount: 80,
    status
  });
  const err = doc.validateSync();
  if (err && err.errors && err.errors.status) {
    allStatusesValid = false;
    console.error(`Status ${status} failed validation:`, err.errors.status.message);
  }
}
assertTrue('All 10 approved lifecycle statuses are valid', allStatusesValid);

// Test 1.3: Illegal status is rejected
const illegalStatusDoc = new Booking({
  listingId: new mongoose.Types.ObjectId(),
  driverFirebaseUid: 'driver_test_001',
  ownerFirebaseUid: 'owner_test_001',
  startTime: new Date('2026-10-15T10:00:00.000Z'),
  endTime: new Date('2026-10-15T12:00:00.000Z'),
  totalHours: 2,
  hourlyRateSnapshot: 40,
  totalAmount: 80,
  status: 'PENDING_APPROVAL'
});
const illegalStatusErr = illegalStatusDoc.validateSync();
assertTrue('Illegal status is rejected by Mongoose schema', !!(illegalStatusErr && illegalStatusErr.errors && illegalStatusErr.errors.status));

// ==========================================
// Group 2: Mandatory Fields & Range Validations
// ==========================================
console.log('\n[Group 2: Mandatory Fields & Range Validations]');

// Test 2.1: Missing required fields
const emptyDoc = new Booking({});
const emptyErr = emptyDoc.validateSync();
assertTrue('Missing listingId triggers validation error', !!(emptyErr && emptyErr.errors && emptyErr.errors.listingId));
assertTrue('Missing driverFirebaseUid triggers validation error', !!(emptyErr && emptyErr.errors && emptyErr.errors.driverFirebaseUid));
assertTrue('Missing ownerFirebaseUid triggers validation error', !!(emptyErr && emptyErr.errors && emptyErr.errors.ownerFirebaseUid));
assertTrue('Missing startTime triggers validation error', !!(emptyErr && emptyErr.errors && emptyErr.errors.startTime));
assertTrue('Missing endTime triggers validation error', !!(emptyErr && emptyErr.errors && emptyErr.errors.endTime));
assertTrue('Missing totalHours triggers validation error', !!(emptyErr && emptyErr.errors && emptyErr.errors.totalHours));
assertTrue('Missing hourlyRateSnapshot triggers validation error', !!(emptyErr && emptyErr.errors && emptyErr.errors.hourlyRateSnapshot));
assertTrue('Missing totalAmount triggers validation error', !!(emptyErr && emptyErr.errors && emptyErr.errors.totalAmount));

// Test 2.2: Negative or zero totalHours
const zeroHoursDoc = new Booking({
  listingId: new mongoose.Types.ObjectId(),
  driverFirebaseUid: 'd1',
  ownerFirebaseUid: 'o1',
  startTime: new Date('2026-10-15T10:00:00.000Z'),
  endTime: new Date('2026-10-15T12:00:00.000Z'),
  totalHours: 0,
  hourlyRateSnapshot: 40,
  totalAmount: 0
});
const zeroHoursErr = zeroHoursDoc.validateSync();
assertTrue('Zero totalHours triggers min validation error', !!(zeroHoursErr && zeroHoursErr.errors && zeroHoursErr.errors.totalHours));

// Test 2.3: Negative hourlyRateSnapshot or totalAmount
const negativeAmountDoc = new Booking({
  listingId: new mongoose.Types.ObjectId(),
  driverFirebaseUid: 'd1',
  ownerFirebaseUid: 'o1',
  startTime: new Date('2026-10-15T10:00:00.000Z'),
  endTime: new Date('2026-10-15T12:00:00.000Z'),
  totalHours: 1,
  hourlyRateSnapshot: -10,
  totalAmount: -10
});
const negativeAmountErr = negativeAmountDoc.validateSync();
assertTrue('Negative hourlyRateSnapshot triggers validation error', !!(negativeAmountErr && negativeAmountErr.errors && negativeAmountErr.errors.hourlyRateSnapshot));
assertTrue('Negative totalAmount triggers validation error', !!(negativeAmountErr && negativeAmountErr.errors && negativeAmountErr.errors.totalAmount));

// ==========================================
// Group 3: Start Time & End Time Ordering
// ==========================================
console.log('\n[Group 3: Start Time & End Time Ordering]');

// Test 3.1: startTime === endTime is rejected
const equalTimeDoc = new Booking({
  listingId: new mongoose.Types.ObjectId(),
  driverFirebaseUid: 'd1',
  ownerFirebaseUid: 'o1',
  startTime: new Date('2026-10-15T10:00:00.000Z'),
  endTime: new Date('2026-10-15T10:00:00.000Z'),
  totalHours: 1,
  hourlyRateSnapshot: 40,
  totalAmount: 40
});
const equalTimeErr = equalTimeDoc.validateSync();
assertTrue('startTime equal to endTime is rejected', !!(equalTimeErr && equalTimeErr.errors && equalTimeErr.errors.endTime));

// Test 3.2: startTime > endTime is rejected
const invertedTimeDoc = new Booking({
  listingId: new mongoose.Types.ObjectId(),
  driverFirebaseUid: 'd1',
  ownerFirebaseUid: 'o1',
  startTime: new Date('2026-10-15T14:00:00.000Z'),
  endTime: new Date('2026-10-15T10:00:00.000Z'),
  totalHours: 1,
  hourlyRateSnapshot: 40,
  totalAmount: 40
});
const invertedTimeErr = invertedTimeDoc.validateSync();
assertTrue('startTime after endTime is rejected', !!(invertedTimeErr && invertedTimeErr.errors && invertedTimeErr.errors.endTime));

// ==========================================
// Group 4: Half-Open Interval Overlap Math [S, E)
// ==========================================
console.log('\n[Group 4: Half-Open Interval Overlap Math [S, E)]');

// Interval A: 10:00 to 12:00
const sA = '2026-10-15T10:00:00.000Z';
const eA = '2026-10-15T12:00:00.000Z';

// Test 4.1: Identical intervals overlap
assertEqual(
  'Identical intervals [10:00, 12:00) vs [10:00, 12:00) overlap',
  Booking.hasTimeOverlap(sA, eA, sA, eA),
  true
);

// Test 4.2: Nested interval overlaps
assertEqual(
  'Nested interval [10:00, 14:00) vs [11:00, 13:00) overlap',
  Booking.hasTimeOverlap('2026-10-15T10:00:00.000Z', '2026-10-15T14:00:00.000Z', '2026-10-15T11:00:00.000Z', '2026-10-15T13:00:00.000Z'),
  true
);

// Test 4.3: Overlapping start (front overlap)
assertEqual(
  'Front overlapping [10:00, 12:00) vs [11:00, 13:00) overlap',
  Booking.hasTimeOverlap('2026-10-15T10:00:00.000Z', '2026-10-15T12:00:00.000Z', '2026-10-15T11:00:00.000Z', '2026-10-15T13:00:00.000Z'),
  true
);

// Test 4.4: Overlapping end (back overlap)
assertEqual(
  'Back overlapping [11:00, 13:00) vs [10:00, 12:00) overlap',
  Booking.hasTimeOverlap('2026-10-15T11:00:00.000Z', '2026-10-15T13:00:00.000Z', '2026-10-15T10:00:00.000Z', '2026-10-15T12:00:00.000Z'),
  true
);

// Test 4.5: ADJACENT intervals do NOT overlap (Half-open interval requirement [S, E))
// Slot 1: [10:00, 12:00)
// Slot 2: [12:00, 14:00)
// Since Slot 1 ends at 12:00 and Slot 2 starts at 12:00, they must NOT conflict!
assertEqual(
  'Adjacent intervals [10:00, 12:00) and [12:00, 14:00) do NOT overlap (half-open [S, E))',
  Booking.hasTimeOverlap(sA, eA, '2026-10-15T12:00:00.000Z', '2026-10-15T14:00:00.000Z'),
  false
);

// Test 4.6: Reverse adjacent intervals do NOT overlap
assertEqual(
  'Reverse adjacent intervals [12:00, 14:00) and [10:00, 12:00) do NOT overlap',
  Booking.hasTimeOverlap('2026-10-15T12:00:00.000Z', '2026-10-15T14:00:00.000Z', sA, eA),
  false
);

// Test 4.7: Disjoint intervals with gap do NOT overlap
assertEqual(
  'Disjoint intervals [10:00, 12:00) and [14:00, 16:00) do NOT overlap',
  Booking.hasTimeOverlap(sA, eA, '2026-10-15T14:00:00.000Z', '2026-10-15T16:00:00.000Z'),
  false
);

// ==========================================
// Group 5: Status Sets & Partitioning
// ==========================================
console.log('\n[Group 5: Status Sets & Partitioning]');

const blocking = Booking.ACTIVE_BLOCKING_STATUSES;
const nonBlocking = Booking.TERMINAL_OR_NON_BLOCKING_STATUSES;
const all = Booking.ALL_STATUSES;

assertEqual('Active blocking statuses count is 5', blocking.length, 5);
assertEqual('Terminal / non-blocking statuses count is 5', nonBlocking.length, 5);
assertEqual('All statuses count is 10', all.length, 10);

assertTrue('PENDING is an active blocking status', blocking.includes('PENDING'));
assertTrue('ACCEPTED is an active blocking status', blocking.includes('ACCEPTED'));
assertTrue('AWAITING_DRIVER_CONFIRMATION is an active blocking status', blocking.includes('AWAITING_DRIVER_CONFIRMATION'));
assertTrue('CONFIRMED is an active blocking status', blocking.includes('CONFIRMED'));
assertTrue('CHECKED_IN is an active blocking status', blocking.includes('CHECKED_IN'));

assertTrue('REJECTED is non-blocking', nonBlocking.includes('REJECTED'));
assertTrue('CANCELLED is non-blocking', nonBlocking.includes('CANCELLED'));
assertTrue('EXPIRED is non-blocking', nonBlocking.includes('EXPIRED'));
assertTrue('CHECKED_OUT is non-blocking', nonBlocking.includes('CHECKED_OUT'));
assertTrue('COMPLETED is non-blocking', nonBlocking.includes('COMPLETED'));

// Verify sets are strictly disjoint
const intersection = blocking.filter(s => nonBlocking.includes(s));
assertEqual('Blocking and non-blocking statuses have zero intersection', intersection.length, 0);

// ==========================================
// Group 6: Confirmation Deadline Calculation
// ==========================================
console.log('\n[Group 6: Confirmation Deadline Calculation]');

// Test 6.1: Standard case: Accept at 10:00, Start at 12:00
// 30m after accept = 10:30. 15m before start = 11:45.
// Min is 10:30.
const normalResult = Booking.calculateConfirmationDeadline(
  new Date('2026-10-15T10:00:00.000Z'),
  new Date('2026-10-15T12:00:00.000Z')
);
assertEqual('Standard case can accept is true', normalResult.canAccept, true);
assertEqual('Deadline is 30 mins after accept', normalResult.deadline.toISOString(), '2026-10-15T10:30:00.000Z');

// Test 6.2: Tight window: Accept at 10:00, Start at 10:35
// 30m after accept = 10:30. 15m before start = 10:20.
// Min is 10:20 (15 min before start time).
const tightResult = Booking.calculateConfirmationDeadline(
  new Date('2026-10-15T10:00:00.000Z'),
  new Date('2026-10-15T10:35:00.000Z')
);
assertEqual('Tight window can accept is true', tightResult.canAccept, true);
assertEqual('Deadline is 15 mins before start time', tightResult.deadline.toISOString(), '2026-10-15T10:20:00.000Z');

// Test 6.3: Start time too close: Accept at 10:00, Start at 10:10
// 15m before start is 09:55 <= accept time 10:00.
// Cannot accept because deadline is already in the past!
const tooCloseResult = Booking.calculateConfirmationDeadline(
  new Date('2026-10-15T10:00:00.000Z'),
  new Date('2026-10-15T10:10:00.000Z')
);
assertEqual('Start time too close results in canAccept = false', tooCloseResult.canAccept, false);
assertTrue('Explains reason for inability to accept', typeof tooCloseResult.reason === 'string');

// Test 6.4: Start time identical to accept time
const identicalTimeResult = Booking.calculateConfirmationDeadline(
  new Date('2026-10-15T10:00:00.000Z'),
  new Date('2026-10-15T10:00:00.000Z')
);
assertEqual('Start time equal to accept time results in canAccept = false', identicalTimeResult.canAccept, false);

// ==========================================
// Group 7: QR Token Generator & Booking Code
// ==========================================
console.log('\n[Group 7: QR Token Generator & Booking Code]');

const qr1 = Booking.generateQrToken();
const qr2 = Booking.generateQrToken();

assertEqual('QR token is 48 hex characters long (24 bytes)', qr1.length, 48);
assertTrue('QR token matches hex pattern', /^[a-f0-9]{48}$/.test(qr1));
assertTrue('Consecutive QR tokens are unique', qr1 !== qr2);

const code1 = Booking.generateBookingCode();
const code2 = Booking.generateBookingCode();
assertTrue('Booking code has PS prefix', code1.startsWith('PS-'));
assertTrue('Consecutive booking codes are unique', code1 !== code2);

// ==========================================
// Group 8: ParkingListing bookingLockVersion
// ==========================================
console.log('\n[Group 8: ParkingListing bookingLockVersion]');

assertTrue(
  'ParkingListing schema defines bookingLockVersion',
  'bookingLockVersion' in ParkingListing.schema.paths
);

const testListing = new ParkingListing({
  ownerFirebaseUid: 'owner_123',
  title: 'Test Concurrency Space',
  address: '123 Test Road',
  pricePerHour: 50,
  availableFrom: '08:00',
  availableUntil: '20:00'
});

assertEqual('Default bookingLockVersion is 0', testListing.bookingLockVersion, 0);

// ==========================================
// Group 9: Status History Recording
// ==========================================
console.log('\n[Group 9: Status History Recording]');

const historyDoc = new Booking({
  listingId: new mongoose.Types.ObjectId(),
  driverFirebaseUid: 'd1',
  ownerFirebaseUid: 'o1',
  startTime: new Date('2026-10-15T10:00:00.000Z'),
  endTime: new Date('2026-10-15T12:00:00.000Z'),
  totalHours: 2,
  hourlyRateSnapshot: 40,
  totalAmount: 80
});

historyDoc.recordStatusChange('ACCEPTED', 'Owner confirmed availability');
assertEqual('Status updated to ACCEPTED', historyDoc.status, 'ACCEPTED');
assertEqual('History recorded 1 transition', historyDoc.statusHistory.length, 1);
assertEqual('History entry has correct status', historyDoc.statusHistory[0].status, 'ACCEPTED');
assertEqual('History entry has correct note', historyDoc.statusHistory[0].note, 'Owner confirmed availability');

let caughtError = false;
try {
  historyDoc.recordStatusChange('INVALID_STATUS', 'bad');
} catch (e) {
  caughtError = true;
}
assertTrue('recordStatusChange throws on invalid status', caughtError);

// ==========================================
// Summary
// ==========================================
console.log('\n========================================');
console.log(`Sub-Phase 4.1 Test Results: ${passed} passed, ${failed} failed`);
console.log('========================================\n');

process.exit(failed > 0 ? 1 : 0);
