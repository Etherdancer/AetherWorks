const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Triggered when a content receives enough reports from the global_blacklist collection
exports.promoteToBlacklist = functions.firestore
    .document('global_blacklist/{hash}')
    .onWrite(async (change, context) => {
        const data = change.after.data();
        if (!data) return;
        if (data.reportCount > 20) {
            await admin.firestore()
                .collection('blacklisted_hashes')
                .doc(context.params.hash)
                .set({ hash: context.params.hash, timestamp: Date.now() });
        }
    });
