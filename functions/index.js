const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

exports.notifyChildOnNewTask = functions.firestore
    .document("tasks/{taskId}")
    .onCreate(async (snap, context) => {
        const task = snap.data();
        const childId = task.assignedToId;

        const userDoc = await admin.firestore().collection("users").doc(childId).get();
        if (!userDoc.exists) return null;
        
        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return console.log("Brak tokenu dla dziecka");

        const message = {
            notification: {
                title: "Nowe zadanie! 📝",
                body: `Rodzic dodał zadanie: ${task.title}. Do zdobycia: ${task.points} pkt!`
            },
            token: fcmToken
        };

        return admin.messaging().send(message);
    });

exports.notifyParentOnReward = functions.firestore
    .document("redemptions/{redemptionId}")
    .onCreate(async (snap, context) => {
        const redemption = snap.data();
        const parentId = redemption.parentId;

        const userDoc = await admin.firestore().collection("users").doc(parentId).get();
        if (!userDoc.exists) return null;

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return console.log("Brak tokenu dla rodzica");

        const message = {
            notification: {
                title: "Dziecko odbiera nagrodę! 🎁",
                body: `Nagroda: ${redemption.rewardTitle} oczekuje na realizację!`
            },
            token: fcmToken
        };

        return admin.messaging().send(message);
    });