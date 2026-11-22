import Foundation
import Network

/// A token-protected local MJPEG server. HTTP is unencrypted; use trusted Wi-Fi only.
final class LocalMJPEGServer {
    private let queue = DispatchQueue(label: "io.parkcast.local-server")
    private let port: UInt16 = 8765
    private var listener: NWListener?
    private var clients: [UUID: ViewerClient] = [:]
    let token = UUID().uuidString.replacingOccurrences(of: "-", with: "") +
                UUID().uuidString.replacingOccurrences(of: "-", with: "")

    private final class ViewerClient {
        let connection: NWConnection
        var awaitingSend = false
        init(_ connection: NWConnection) { self.connection = connection }
    }

    var viewerURL: URL? {
        guard let ip = LocalAddress.wifiIPv4() else { return nil }
        return URL(string: "http://\(ip):\(port)/?token=\(token)")
    }

    func start() throws {
        guard listener == nil else { return }
        let listener = try NWListener(using: .tcp, on: NWEndpoint.Port(rawValue: port)!)
        self.listener = listener
        listener.newConnectionHandler = { [weak self] connection in
            self?.accept(connection)
        }
        listener.stateUpdateHandler = { [weak self] state in
            if case .failed = state { self?.stop() }
        }
        listener.start(queue: queue)
    }

    func stop() {
        queue.async {
            self.listener?.cancel()
            self.listener = nil
            for client in self.clients.values { client.connection.cancel() }
            self.clients.removeAll()
        }
    }

    private func accept(_ connection: NWConnection) {
        connection.start(queue: queue)
        connection.receive(minimumIncompleteLength: 1, maximumLength: 8192) { [weak self] data, _, _, error in
            guard let self, error == nil, let data,
                  let request = String(data: data, encoding: .utf8),
                  let line = request.components(separatedBy: "\r\n").first else {
                connection.cancel()
                return
            }
            self.handle(firstLine: line, connection: connection)
        }
    }

    private func handle(firstLine: String, connection: NWConnection) {
        let tokens = firstLine.split(separator: " ")
        guard tokens.count >= 2, tokens[0] == "GET",
              let components = URLComponents(string: "http://localhost\(tokens[1])"),
              components.queryItems?.first(where: { $0.name == "token" })?.value == token else {
            reply(connection, status: "403 Forbidden", type: "text/plain", body: Data("Access denied".utf8))
            return
        }

        if components.path == "/" {
            let html = """
            <!doctype html><html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <meta name="referrer" content="no-referrer">
            <title>ParkCast viewer</title>
            <style>body{font:16px system-ui;background:#0c111c;color:#f8fafc;margin:0;text-align:center}
            header{padding:16px} img{display:block;max-width:100vw;max-height:86vh;margin:auto}
            small{color:#a7b0c2}</style></head><body>
            <header><strong>ParkCast</strong> — private local viewer<br>
            <small>Video only. Stop sharing from your phone.</small></header>
            <img alt="Live screen" src="/stream?token=\(token)"></body></html>
            """
            reply(connection, status: "200 OK", type: "text/html; charset=utf-8", body: Data(html.utf8))
            return
        }

        guard components.path == "/stream" else {
            reply(connection, status: "404 Not Found", type: "text/plain", body: Data("Not found".utf8))
            return
        }
        let id = UUID()
        clients[id] = ViewerClient(connection)
        connection.stateUpdateHandler = { [weak self] state in
            if case .failed = state { self?.remove(id) }
            if case .cancelled = state { self?.remove(id) }
        }
        let headers = "HTTP/1.1 200 OK\r\nContent-Type: multipart/x-mixed-replace; boundary=frame\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n"
        connection.send(content: Data(headers.utf8), completion: .contentProcessed { [weak self] error in
            if error != nil { self?.remove(id) }
        })
    }

    private func reply(_ connection: NWConnection, status: String, type: String, body: Data) {
        let header = "HTTP/1.1 \(status)\r\nContent-Type: \(type)\r\nContent-Length: \(body.count)\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n"
        var data = Data(header.utf8)
        data.append(body)
        connection.send(content: data, completion: .contentProcessed { _ in connection.cancel() })
    }

    private func remove(_ id: UUID) {
        queue.async {
            self.clients.removeValue(forKey: id)?.connection.cancel()
        }
    }

    func publish(jpeg: Data) {
        queue.async {
            guard !self.clients.isEmpty else { return }
            let header = "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: \(jpeg.count)\r\n\r\n"
            var data = Data(header.utf8)
            data.append(jpeg)
            data.append(Data("\r\n".utf8))
            for (id, client) in self.clients where !client.awaitingSend {
                client.awaitingSend = true
                client.connection.send(content: data, completion: .contentProcessed { [weak self] error in
                    guard let self else { return }
                    if error != nil { self.remove(id) }
                    else { self.clients[id]?.awaitingSend = false }
                })
            }
        }
    }
}
