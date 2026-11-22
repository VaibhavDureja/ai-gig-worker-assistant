package com.parkcast.mirror

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Tiny, offline MJPEG server. No cloud account, tracker, or third-party library. */
internal class HttpCastServer(
    private val sessionToken: String,
    private val jpegFrame: () -> ByteArray?,
    private val port: Int = 8765,
) {
    private val running = AtomicBoolean(false)
    private val viewers = AtomicInteger(0)
    private val openSockets = ConcurrentHashMap.newKeySet<Socket>()
    private val workers = Executors.newCachedThreadPool()
    private var listener: ServerSocket? = null

    fun start() {
        val socket = ServerSocket(port)
        socket.reuseAddress = true
        listener = socket
        running.set(true)
        Thread({
            while (running.get()) {
                try {
                    val client = socket.accept()
                    openSockets.add(client)
                    workers.execute { serve(client) }
                } catch (_: Exception) {
                    if (!running.get()) break
                }
            }
        }, "ParkCastHttpAccept").apply { isDaemon = true; start() }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        runCatching { listener?.close() }
        openSockets.forEach { runCatching { it.close() } }
        openSockets.clear()
        workers.shutdownNow()
    }

    private fun serve(socket: Socket) {
        try {
            socket.use { client ->
                client.soTimeout = 5000
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII))
                val requestLine = reader.readLine() ?: return
                if (requestLine.length > 4096) return
                val components = requestLine.split(' ')
                if (components.size < 2 || components[0] != "GET") {
                    sendText(client.getOutputStream(), "405 Method Not Allowed", "GET required")
                    return
                }
                val rawTarget = components[1]
                val path = rawTarget.substringBefore('?')
                val query = rawTarget.substringAfter('?', "")
                val supplied = query.split('&').firstOrNull { it.startsWith("token=") }?.substringAfter('=', "") ?: ""
                if (!SessionToken.matches(sessionToken, supplied)) {
                    sendText(client.getOutputStream(), "403 Forbidden", "Wrong or missing session code")
                    return
                }
                if (path == "/") {
                    sendHtml(client.getOutputStream())
                } else if (path == "/stream") {
                    if (viewers.incrementAndGet() > 2) {
                        viewers.decrementAndGet()
                        sendText(client.getOutputStream(), "503 Service Unavailable", "Maximum two viewers")
                        return
                    }
                    try {
                        client.soTimeout = 0
                        stream(client.getOutputStream())
                    } finally {
                        viewers.decrementAndGet()
                    }
                } else {
                    sendText(client.getOutputStream(), "404 Not Found", "Not found")
                }
            }
        } catch (_: Exception) {
            // Browser closed, local Wi-Fi disconnected, or screen-casting session stopped.
        } finally {
            openSockets.remove(socket)
        }
    }

    private fun stream(out: OutputStream) {
        val header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: multipart/x-mixed-replace; boundary=parkcastframe\r\n" +
            "Cache-Control: no-store, no-cache\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        var lastFrame: ByteArray? = null
        while (running.get() && !Thread.currentThread().isInterrupted) {
            val frame = jpegFrame()
            if (frame != null && frame !== lastFrame) {
                out.write(("--parkcastframe\r\nContent-Type: image/jpeg\r\nContent-Length: " +
                    frame.size + "\r\n\r\n").toByteArray(StandardCharsets.US_ASCII))
                out.write(frame)
                out.write("\r\n".toByteArray(StandardCharsets.US_ASCII))
                out.flush()
                lastFrame = frame
            }
            Thread.sleep(120L)
        }
    }

    private fun sendHtml(out: OutputStream) {
        val page = """<!doctype html>
<html lang="en"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/>
<meta name="referrer" content="no-referrer"/>
<title>ParkCast · Local screen viewer</title><style>
*{box-sizing:border-box}body{margin:0;min-height:100vh;background:#090e1b;color:#e8f1ff;font:16px system-ui, sans-serif;display:flex;flex-direction:column;align-items:center}
header{width:100%;padding:16px 20px;background:#121c34;border-bottom:1px solid #233351;display:flex;justify-content:space-between;gap:12px;align-items:center}
strong{color:#fff}.pill{border-radius:99px;padding:7px 11px;background:#14362e;color:#aef5d2;font-size:12px}
main{width:100%;max-width:1160px;flex:1;padding:15px;display:grid;place-items:center}
img{display:block;max-width:100%;max-height:calc(100vh - 170px);object-fit:contain;border-radius:12px;background:#070b13;box-shadow:0 12px 45px #0008}
footer{padding:12px 16px;text-align:center;color:#a9bbd8;font-size:13px}
</style></head><body><header><strong>◉ ParkCast</strong><span class="pill">Local session · parked only</span></header>
<main><img src="/stream?token=$sessionToken" alt="Live phone screen; please wait for the next frame"></main>
<footer>Use only while parked. This is a view-only stream. Audio and car controls are not supported.</footer>
</body></html>"""
        val content = page.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
            "Content-Length: ${content.size}\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\n" +
            "Content-Security-Policy: default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'\r\n" +
            "X-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.US_ASCII))
        out.write(content)
        out.flush()
    }

    private fun sendText(out: OutputStream, status: String, message: String) {
        val bytes = message.toByteArray(StandardCharsets.UTF_8)
        out.write(("HTTP/1.1 $status\r\nContent-Type: text/plain; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray(StandardCharsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }
}
