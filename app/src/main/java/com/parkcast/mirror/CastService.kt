package com.parkcast.mirror

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager

/** Foreground screen-capture session. The user must accept the Android capture dialog. */
class CastService : Service() {
    private var projection: MediaProjection? = null
    private var callback: MediaProjection.Callback? = null
    private var capture: CaptureFrames? = null
    private var http: HttpCastServer? = null

    companion object {
        const val ACTION_START = "com.parkcast.mirror.START"
        const val ACTION_STOP = "com.parkcast.mirror.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val NOTIFICATION_ID = 121
        private const val CHANNEL_ID = "parkcast_capture"

        @Volatile var active: Boolean = false
            private set
        @Volatile var url: String? = null
            private set
        @Volatile var lastError: String? = null
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_START -> if (!active && projection == null) startCast(intent)
        }
        return START_NOT_STICKY
    }

    private fun startCast(intent: Intent) {
        try {
            lastError = null
            val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            @Suppress("DEPRECATION")
            val data = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else intent.getParcelableExtra(EXTRA_RESULT_DATA)
            if (code != android.app.Activity.RESULT_OK || data == null) {
                lastError = "Screen sharing was not authorised."
                stopSelf()
                return
            }

            ensureNotificationChannel()
            // Android 14+ requires startForeground before getMediaProjection().
            startForeground(
                NOTIFICATION_ID,
                notification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val media = manager.getMediaProjection(code, data)
                ?: throw IllegalStateException("Android denied screen sharing")
            projection = media

            val frameCapture = CaptureFrames(media, resources.displayMetrics.densityDpi)
            capture = frameCapture
            val stopCallback = object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
                override fun onCapturedContentResize(width: Int, height: Int) {
                    frameCapture.configure(width, height)
                }
            }
            callback = stopCallback
            media.registerCallback(stopCallback, Handler(Looper.getMainLooper()))

            val bounds = if (Build.VERSION.SDK_INT >= 30) {
                (getSystemService(WINDOW_SERVICE) as WindowManager).maximumWindowMetrics.bounds
            } else {
                @Suppress("DEPRECATION")
                resources.displayMetrics.let { android.graphics.Rect(0, 0, it.widthPixels, it.heightPixels) }
            }
            frameCapture.configure(bounds.width(), bounds.height())

            val token = SessionToken.generate()
            val server = HttpCastServer(token, frameCapture::latestJpeg)
            server.start()
            http = server
            val host = LocalAddress.discover()
            url = if (host != null) "http://$host:8765/?token=$token" else null
            active = true
        } catch (e: Exception) {
            lastError = "Could not start casting: ${e.javaClass.simpleName}. Check network and permissions."
            stopSelf()
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ParkCast screen sharing",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Visible while your phone screen is being shared" }
        manager.createNotificationChannel(channel)
    }

    private fun notification(): Notification {
        val stopIntent = Intent(this, CastService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 2, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ParkCast is sharing your screen")
            .setContentText("Local network only · tap Stop to end sharing")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setColor(Color.rgb(52, 211, 153))
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_media_pause, "Stop sharing", stopPending)
            .build()
    }

    override fun onDestroy() {
        active = false
        url = null
        http?.stop()
        http = null
        capture?.close()
        capture = null
        val media = projection
        val cb = callback
        projection = null
        callback = null
        if (cb != null) runCatching { media?.unregisterCallback(cb) }
        runCatching { media?.stop() }
        super.onDestroy()
    }
}
