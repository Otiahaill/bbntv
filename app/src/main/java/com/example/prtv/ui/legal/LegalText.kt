package com.example.prtv.ui.legal

data class LegalSection(val title: String, val body: String)

object LegalText {
    const val YOUTUBE_TERMS_URL = "https://www.youtube.com/t/terms"
    const val GOOGLE_PRIVACY_URL = "http://www.google.com/policies/privacy"

    val terms: List<LegalSection> = listOf(
        LegalSection("Using this app", "This app plays videos from YouTube channels and playlists as a scheduled, TV-style programme. Everyone who opens the app at the same moment sees the same video at the same point. It is a scheduled replay, not a live broadcast."),
        LegalSection("YouTube Terms of Service", "This app uses YouTube API Services. By using this app you agree to be bound by the YouTube Terms of Service: https://www.youtube.com/t/terms"),
        LegalSection("Third-party content and trademarks", "All videos, titles, thumbnails and channel names belong to their creators and owners and are provided by YouTube. This app is not affiliated with, endorsed by or sponsored by YouTube, Google, or the owners of the channels it shows. YouTube is a trademark of Google LLC. All other names and marks belong to their respective owners."),
        LegalSection("Your responsibilities", "Use the app lawfully. Do not try to download, record, modify or block YouTube videos, ads or the player."),
        LegalSection("No warranty", "The app is provided as is. Videos can become unavailable at any time and are outside our control.")
    )

    fun privacy(contactEmail: String): List<LegalSection> = listOf(
        LegalSection("Summary", "This app has no accounts, no analytics, no advertising and no servers of its own. It does not collect personal information. The details below explain what is stored on your device and what YouTube and Google receive."),
        LegalSection("YouTube API Services", "This app uses YouTube API Services (the YouTube embedded player and the YouTube Data API). Google's use of information is described in the Google Privacy Policy: http://www.google.com/policies/privacy"),
        LegalSection("What is stored on your device", "Your consent choice; the channel and playlist you entered; an API key if you typed one in (stored encrypted); and video details fetched from YouTube (titles, thumbnail links, lengths). Video details are refreshed regularly and deleted no later than 29 days after they were fetched. No video or audio is ever downloaded or stored."),
        LegalSection("What YouTube and Google receive", "When the player loads or plays a video, YouTube and Google receive information such as your IP address, device and playback details, as described in the Google Privacy Policy. Videos may start automatically, so this can happen as soon as the player appears. The player is loaded in YouTube's privacy-enhanced mode and third-party cookies are switched off, but the embedded player may still store or read information on your device (for example local storage) to work."),
        LegalSection("Ads and third-party content", "This app does not show ads. YouTube may show ads inside videos; we do not control them. Third parties do not serve content in this app other than YouTube."),
        LegalSection("Children", "The app may be enjoyed by children. It collects no personal information from anyone, has no sign-in, comments, purchases or sharing, and treats videos marked Made for Kids as YouTube requires."),
        LegalSection("Deleting your data", "Open Settings and choose Clear stored data to delete everything this app keeps on your device. This does not change data that YouTube or Google hold; manage that through YouTube and your Google account."),
        LegalSection("Contact", "Questions about privacy: " + contactEmail)
    )
}
