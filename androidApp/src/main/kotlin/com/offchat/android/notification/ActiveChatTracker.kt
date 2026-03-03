package com.offchat.android.notification

/**
 * Simple singleton that tracks which chat screen is currently visible.
 * Used by the background service to suppress notifications for the active conversation.
 */
object ActiveChatTracker {
    /** The peerId of the chat currently being viewed, or null if none. */
    @Volatile
    var activePeerId: String? = null
}
