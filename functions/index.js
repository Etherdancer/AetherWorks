const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Triggered when a content receives enough reports from the global_blacklist collection
exports.promoteToBlacklist = functions.firestore
    .document('global_blacklist/{hash}/reports/{token}')
    .onWrite(async (change, context) => {
        const hash = context.params.hash;
        
        // Count total reports for this hash
        const reportsSnapshot = await admin.firestore()
            .collection(`global_blacklist/${hash}/reports`)
            .get();
            
        if (reportsSnapshot.size > 20) {
            await admin.firestore()
                .collection('blacklisted_hashes')
                .doc(hash)
                .set({ hash: hash, timestamp: Date.now() });
        }
    });
