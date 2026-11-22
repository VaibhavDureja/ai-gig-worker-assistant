import AppKit
import AVFoundation
import CoreGraphics
import UniformTypeIdentifiers

@MainActor
final class Wallpaper {
    let window: NSWindow
    let player = AVQueuePlayer()
    let looper: AVPlayerLooper

    init(screen: NSScreen, asset: AVAsset) {
        player.isMuted = true
        looper = AVPlayerLooper(player: player, templateItem: AVPlayerItem(asset: asset))

        let view = NSView(frame: NSRect(origin: .zero, size: screen.frame.size))
        let layer = AVPlayerLayer(player: player)
        layer.videoGravity = .resizeAspectFill
        view.wantsLayer = true
        view.layer = layer

        window = NSWindow(contentRect: screen.frame, styleMask: .borderless,
                          backing: .buffered, defer: false)
        window.setFrame(screen.frame, display: false)
        window.contentView = view
        window.backgroundColor = .black
        window.hasShadow = false
        window.ignoresMouseEvents = true
        window.isExcludedFromWindowsMenu = true
        window.isReleasedWhenClosed = false
        window.animationBehavior = .none
        window.level = NSWindow.Level(rawValue: Int(CGWindowLevelForKey(.desktopWindow)) + 1)
        window.collectionBehavior = [.canJoinAllSpaces, .stationary, .ignoresCycle]
        window.orderFrontRegardless()
    }

    func stop() {
        player.pause()
        looper.disableLooping()
        window.orderOut(nil)
    }
}

@MainActor
final class LidLoop: NSObject, NSApplicationDelegate {
    private let status = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
    private var wallpapers: [Wallpaper] = []
    private var asset: AVURLAsset?
    private var source: URL?
    private var paused = UserDefaults.standard.bool(forKey: "paused")
    private var sleeping = false
    private var displaysAsleep = false
    private var rebuild: Task<Void, Never>?
    private var loading: Task<Void, Never>?
    private var observations: [NSObjectProtocol] = []
    private var playbackObservations: [NSKeyValueObservation] = []
    private var playbackError: String?

    func applicationDidFinishLaunching(_ notification: Notification) {
        status.button?.image = NSImage(systemSymbolName: "play.rectangle", accessibilityDescription: "LidLoop")
        refreshMenu()
        let workspace = NSWorkspace.shared.notificationCenter
        observe(workspace, NSWorkspace.willSleepNotification) { app in
            app.sleeping = true
            app.updatePlayback()
        }
        observe(workspace, NSWorkspace.didWakeNotification) { app in
            app.sleeping = false
            app.displaysAsleep = false
            app.scheduleRebuild()
        }
        observe(workspace, NSWorkspace.screensDidSleepNotification) { app in
            app.displaysAsleep = true
            app.updatePlayback()
        }
        observe(workspace, NSWorkspace.screensDidWakeNotification) { app in
            app.displaysAsleep = false
            app.scheduleRebuild()
        }
        observe(workspace, NSWorkspace.sessionDidBecomeActiveNotification) {
            $0.scheduleRebuild()
        }
        observe(NotificationCenter.default, NSApplication.didChangeScreenParametersNotification) {
            $0.scheduleRebuild()
        }

        if let bookmark = UserDefaults.standard.data(forKey: "video") {
            do {
                var stale = false
                let url = try URL(resolvingBookmarkData: bookmark, options: [.withoutUI, .withoutMounting],
                                  relativeTo: nil, bookmarkDataIsStale: &stale)
                load(url, startingUp: true)
            } catch {
                playbackError = "Choose the video again to restore playback."
                refreshMenu()
            }
        }
    }

    private func observe(_ center: NotificationCenter, _ name: Notification.Name,
                         _ handler: @escaping @MainActor (LidLoop) -> Void) {
        observations.append(center.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor in
                if let self { handler(self) }
            }
        })
    }

    private func refreshMenu() {
        let menu = NSMenu()
        func add(_ title: String, _ action: Selector, key: String = "") -> NSMenuItem {
            let item = menu.addItem(withTitle: title, action: action, keyEquivalent: key)
            item.target = self
            return item
        }

        if let title = playbackError ?? source?.lastPathComponent {
            menu.addItem(withTitle: title, action: nil, keyEquivalent: "")
            menu.addItem(.separator())
        }
        _ = add("Choose Video…", #selector(chooseVideo), key: "o")
        if source != nil {
            _ = add(paused ? "Resume Wallpaper" : "Pause Wallpaper", #selector(togglePause))
        }
        if source != nil || UserDefaults.standard.data(forKey: "video") != nil {
            _ = add("Remove Wallpaper", #selector(removeWallpaper))
        }
        menu.addItem(.separator())
        _ = add("Quit LidLoop", #selector(quit), key: "q")
        status.menu = menu
    }

    @objc private func chooseVideo() {
        let panel = NSOpenPanel()
        panel.title = "Choose a Wallpaper Video"
        panel.allowedContentTypes = [.movie, .mpeg4Movie, .quickTimeMovie]
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        NSApp.activate(ignoringOtherApps: true)
        if panel.runModal() == .OK, let url = panel.url { load(url, startingUp: false) }
    }

    private func load(_ url: URL, startingUp: Bool) {
        loading?.cancel()
        loading = Task { [weak self] in
            guard let self else { return }
            do {
                let candidate = AVURLAsset(url: url)
                let playable = try await candidate.load(.isPlayable)
                let tracks = try await candidate.loadTracks(withMediaType: .video)
                let duration = try await candidate.load(.duration)
                try Task.checkCancellation()
                guard playable, !tracks.isEmpty, duration.seconds.isFinite, duration.seconds > 0 else {
                    throw CocoaError(.fileReadCorruptFile)
                }
                let bookmark = try url.bookmarkData()
                UserDefaults.standard.set(bookmark, forKey: "video")
                if !startingUp {
                    paused = false
                    UserDefaults.standard.set(false, forKey: "paused")
                }
                source = url
                asset = candidate
                rebuildWallpaper()
                refreshMenu()
            } catch {
                guard !Task.isCancelled else { return }
                if startingUp {
                    playbackError = "Saved video unavailable. Choose it again."
                    refreshMenu()
                } else {
                    let alert = NSAlert()
                    alert.alertStyle = .warning
                    alert.messageText = "Couldn’t use this video"
                    alert.informativeText = "Choose a local MP4 or MOV video that opens in QuickTime Player."
                    NSApp.activate(ignoringOtherApps: true)
                    alert.runModal()
                }
            }
        }
    }

    private func scheduleRebuild() {
        rebuild?.cancel()
        rebuild = Task { [weak self] in
            do {
                try await Task.sleep(nanoseconds: 750_000_000)
            } catch { return }
            guard let self, self.asset != nil, !self.sleeping, !self.displaysAsleep else { return }
            // The display list can still be empty immediately after wake.
            for _ in 0..<3 {
                if !NSScreen.screens.isEmpty {
                    self.rebuildWallpaper()
                    return
                }
                do {
                    try await Task.sleep(nanoseconds: 750_000_000)
                } catch { return }
                guard !self.sleeping, !self.displaysAsleep else { return }
            }
        }
    }

    private func rebuildWallpaper() {
        guard !sleeping, !displaysAsleep, let asset, !NSScreen.screens.isEmpty else { return }
        clearWindows()
        playbackError = nil
        wallpapers = NSScreen.screens.map { Wallpaper(screen: $0, asset: asset) }
        for wallpaper in wallpapers {
            playbackObservations.append(wallpaper.looper.observe(\.status, options: [.initial, .new]) {
                [weak self, weak wallpaper] looper, _ in
                guard looper.status == .failed else { return }
                DispatchQueue.main.async {
                    guard let self, let wallpaper, self.wallpapers.contains(where: { $0 === wallpaper }) else {
                        return
                    }
                    self.playbackError = "Playback failed. Choose the video again."
                    self.clearWindows()
                    self.refreshMenu()
                }
            })
        }
        updatePlayback()
        refreshMenu()
    }

    private func clearWindows() {
        playbackObservations.removeAll()
        for wallpaper in wallpapers { wallpaper.stop() }
        wallpapers.removeAll()
    }

    private func updatePlayback() {
        for wallpaper in wallpapers {
            if paused || sleeping || displaysAsleep {
                wallpaper.player.pause()
                if paused { wallpaper.window.orderOut(nil) }
            } else {
                wallpaper.window.orderFrontRegardless()
                wallpaper.player.play()
            }
        }
    }

    @objc private func togglePause() {
        paused.toggle()
        UserDefaults.standard.set(paused, forKey: "paused")
        if !paused && wallpapers.isEmpty { rebuildWallpaper() }
        updatePlayback()
        refreshMenu()
    }

    @objc private func removeWallpaper() {
        loading?.cancel()
        rebuild?.cancel()
        clearWindows()
        asset = nil
        source = nil
        playbackError = nil
        paused = false
        UserDefaults.standard.removeObject(forKey: "video")
        UserDefaults.standard.removeObject(forKey: "paused")
        refreshMenu()
    }

    @objc private func quit() { NSApp.terminate(nil) }
}

@main
struct LidLoopMain {
    @MainActor
    static func main() {
        let app = NSApplication.shared
        let delegate = LidLoop()
        app.delegate = delegate
        app.setActivationPolicy(.accessory)
        withExtendedLifetime(delegate) { app.run() }
    }
}
