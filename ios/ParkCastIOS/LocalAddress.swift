import Foundation
import Darwin

enum LocalAddress {
    static func wifiIPv4() -> String? {
        var first: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&first) == 0, let first else { return nil }
        defer { freeifaddrs(first) }
        var cursor: UnsafeMutablePointer<ifaddrs>? = first
        while let item = cursor {
            defer { cursor = item.pointee.ifa_next }
            guard let raw = item.pointee.ifa_addr,
                  raw.pointee.sa_family == UInt8(AF_INET),
                  String(cString: item.pointee.ifa_name) == "en0" else { continue }
            var address = raw.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee.sin_addr }
            var buffer = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
            guard inet_ntop(AF_INET, &address, &buffer, socklen_t(INET_ADDRSTRLEN)) != nil else { continue }
            return String(cString: buffer)
        }
        return nil
    }
}
