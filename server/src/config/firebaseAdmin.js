/**
 * Firebase Admin SDK Initializer
 *
 * Initializes the Firebase Admin SDK using modular syntax for ES Modules.
 * Loads credentials safely from environment variables.
 *
 * Usage:
 *   import { messaging } from '../config/firebaseAdmin.js'
 *   await messaging.send({ token: fcmToken, notification: { title, body } })
 */

import { initializeApp, cert, getApps } from 'firebase-admin/app'
import { getMessaging } from 'firebase-admin/messaging'
import { env } from './environment.js'

let app = null

if (getApps().length === 0) {
    if (!env.FIREBASE_PROJECT_ID || !env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
        console.error('Firebase credentials are missing in the environment configurations.')
    } else {
        try {
            let privateKey = env.FIREBASE_PRIVATE_KEY;

            // 1. Tháo ngoặc kép thừa ở 2 đầu nếu có
            if (privateKey.startsWith('"') && privateKey.endsWith('"')) {
                privateKey = privateKey.slice(1, -1);
            }

            // 2. Chuyển chuỗi literal \n thành xuống dòng thực tế (nếu chưa giải mã)
            // Dùng regex để replace cả \\n lẫn \n cho an toàn
            privateKey = privateKey.replace(/\\n/g, '\n');

            app = initializeApp({
                credential: cert({
                    projectId: env.FIREBASE_PROJECT_ID,
                    clientEmail: env.FIREBASE_CLIENT_EMAIL,
                    privateKey: privateKey
                })
            });
            
            console.log('Firebase Admin SDK initialized successfully via Environment Variables.')
        } catch (error) {
            console.error('Failed to initialize Firebase Admin SDK:', error.message)
        }
    }
} else {
    app = getApps()[0]
}

/**
 * Firebase Cloud Messaging (FCM) messaging instance.
 * Use this to send push notifications to Android/iOS devices.
 * Will be null if Firebase credentials were not provided in the environment.
 */
export const messaging = app ? getMessaging(app) : null

