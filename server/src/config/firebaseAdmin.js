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

// Prevent re-initializing if the module is imported multiple times
if (getApps().length === 0) {
    if (!env.FIREBASE_PROJECT_ID || !env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
        console.error('Firebase credentials are missing in the environment configurations.')
    } else {
        try {
            // Replace literal \n string with actual newline characters
            const privateKey = env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
            
            app = initializeApp({
                credential: cert({
                    projectId: env.FIREBASE_PROJECT_ID,
                    clientEmail: env.FIREBASE_CLIENT_EMAIL,
                    privateKey: privateKey
                })
            })
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

