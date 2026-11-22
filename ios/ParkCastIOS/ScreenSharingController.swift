import Foundation
import SwiftUI
import ScreenCaptureKit
import CoreMedia
import CoreImage
import UIKit

@MainActor
final class ScreenSharingController: NSObject, ObservableObject {
    @Published private(set) var status = "Not sharing"
    @Published private(set) var isSharing = false
    @Published private(set) var viewerURL: URL?

    private let server = LocalMJPEGServer()
    private let frameReceiver: FrameReceiver
    private var stream: SCStream?
    private var pickerRegistered = false

    override init() {
        frameReceiver = FrameReceiver()
        super.init()
        frameReceiver.onJPEG = { [weak server = self.server] jpeg in
            server?.publish(jpeg: jpeg)
        }
    }

    func start() {
        guard !isSharing else { return }
        guard LocalAddress.wifiIPv4() != nil else {
            status = "Connect to Wi-Fi before sharing."
            return
        }
        do {
            try server.start()
            viewerURL = server.viewerURL
            let picker = SCContentSharingPicker.shared
            if !pickerRegistered {
                picker.add(self)
                pickerRegistered = true
            }
            picker.isActive = true
            status = "Choose what to share in Apple's picker"
            picker.present()
        } catch {
            status = "Could not start local viewer: \(error.localizedDescription)"
        }
    }

    func stop() {
        Task {
            if let stream { try? await stream.stopCapture() }
            stream = nil
            server.stop()
            viewerURL = nil
            isSharing = false
            status = "Not sharing"
            if pickerRegistered {
                let picker = SCContentSharingPicker.shared
                picker.remove(self)
                picker.isActive = false
                pickerRegistered = false
            }
        }
    }

    fileprivate func beginCapture(_ filter: SCContentFilter) {
        Task {
            if let stream { try? await stream.stopCapture() }
            stream = nil
            do {
                let config = SCStreamConfiguration()
                config.capturesAudio = false
                let newStream = SCStream(filter: filter, configuration: config, delegate: frameReceiver)
                try newStream.addStreamOutput(frameReceiver, type: .screen,
                                              sampleHandlerQueue: frameReceiver.queue)
                try await newStream.startCapture()
                stream = newStream
                isSharing = true
                status = "Sharing screen — open the viewer link on another device"
            } catch {
                server.stop()
                viewerURL = nil
                isSharing = false
                status = "Capture failed: \(error.localizedDescription)"
            }
        }
    }
}

extension ScreenSharingController: SCContentSharingPickerObserver {
    nonisolated func contentSharingPicker(_ picker: SCContentSharingPicker,
                                         didUpdateWith filter: SCContentFilter,
                                         for stream: SCStream?) {
        Task { @MainActor in self.beginCapture(filter) }
    }

    nonisolated func contentSharingPicker(_ picker: SCContentSharingPicker,
                                         didCancelFor stream: SCStream?) {
        Task { @MainActor in
            if !self.isSharing { self.stop() }
        }
    }

    nonisolated func contentSharingPickerStartDidFailWithError(_ error: Error) {
        Task { @MainActor in
            self.status = "Picker error: \(error.localizedDescription)"
            self.stop()
        }
    }
}

// Frame conversion is kept on a serial queue, at about six frames a second.
private final class FrameReceiver: NSObject, SCStreamOutput, SCStreamDelegate {
    let queue = DispatchQueue(label: "io.parkcast.frame-encoding", qos: .userInitiated)
    var onJPEG: ((Data) -> Void)?
    private let context = CIContext()
    private var lastFrameTime: TimeInterval = 0

    func stream(_ stream: SCStream, didOutputSampleBuffer sampleBuffer: CMSampleBuffer,
                of type: SCStreamOutputType) {
        guard type == .screen, sampleBuffer.isValid,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let now = ProcessInfo.processInfo.systemUptime
        guard now - lastFrameTime >= 0.16 else { return }
        lastFrameTime = now
        var image = CIImage(cvPixelBuffer: pixelBuffer)
        let maxWidth: CGFloat = 960
        if image.extent.width > maxWidth {
            let scale = maxWidth / image.extent.width
            image = image.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        }
        guard let cgImage = context.createCGImage(image, from: image.extent),
              let jpeg = UIImage(cgImage: cgImage).jpegData(compressionQuality: 0.55) else { return }
        onJPEG?(jpeg)
    }

    func stream(_ stream: SCStream, didStopWithError error: Error) {
        NSLog("ParkCast capture ended: %@", error.localizedDescription)
    }
}
