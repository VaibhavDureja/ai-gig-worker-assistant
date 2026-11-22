import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var sharing: ScreenSharingController

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    VStack(alignment: .leading, spacing: 8) {
                        Image(systemName: "airplay.video")
                            .font(.system(size: 52, weight: .medium))
                            .foregroundStyle(.blue)
                        Text("ParkCast").font(.largeTitle.bold())
                        Text("Share your iPhone screen to a browser on the same trusted Wi-Fi network.")
                            .foregroundStyle(.secondary)
                    }
                    GroupBox("Screen sharing") {
                        VStack(alignment: .leading, spacing: 14) {
                            Label(sharing.status, systemImage: sharing.isSharing ? "record.circle.fill" : "circle.dotted")
                                .foregroundStyle(sharing.isSharing ? Color.red : Color.primary)

                            Button {
                                sharing.start()
                            } label: {
                                Label("Choose screen to share", systemImage: "display")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .disabled(sharing.isSharing)

                            if sharing.isSharing {
                                Button(role: .destructive) {
                                    sharing.stop()
                                } label: {
                                    Label("Stop sharing", systemImage: "stop.fill")
                                        .frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.bordered)
                            }

                            if let url = sharing.viewerURL {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text("Private viewer address").font(.headline)
                                    Text(url.absoluteString)
                                        .textSelection(.enabled)
                                        .font(.caption.monospaced())
                                    ShareLink(item: url) {
                                        Label("Share viewer link", systemImage: "square.and.arrow.up")
                                    }
                                    Text("Anyone on your Wi-Fi with this link may view your screen. Do not share it publicly.")
                                        .font(.footnote).foregroundStyle(.orange)
                                }
                            }
                        }
                        .padding(.vertical, 8)
                    }
                    GroupBox("How to use") {
                        VStack(alignment: .leading, spacing: 12) {
                            Label("Connect your iPhone and viewer to the same Wi-Fi.", systemImage: "wifi")
                            Label("Tap Choose screen and approve Apple's sharing picker.", systemImage: "checkmark.shield")
                            Label("Open the private URL in Safari or another browser.", systemImage: "safari")
                            Label("Tap Stop sharing when finished.", systemImage: "stop.circle")
                        }
                        .font(.subheadline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                    }
                    Text("Prototype · iOS 27+ · Video only · No audio · No CarPlay integration. Protected video may be blanked. Do not use while driving.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
                .padding()
            }
            .navigationTitle("ParkCast")
        }
    }
}
