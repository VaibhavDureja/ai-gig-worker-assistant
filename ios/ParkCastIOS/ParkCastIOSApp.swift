import SwiftUI

@main
struct ParkCastIOSApp: App {
    @StateObject private var sharing = ScreenSharingController()

    var body: some Scene {
        WindowGroup {
            HomeView()
                .environmentObject(sharing)
        }
    }
}
