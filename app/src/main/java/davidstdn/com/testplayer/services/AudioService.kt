package davidstdn.com.testplayer.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import davidstdn.com.testplayer.*
import davidstdn.com.testplayer.R
import davidstdn.com.testplayer.activities.PlayerActivity
import davidstdn.com.testplayer.model.MusicLibrary
import davidstdn.com.testplayer.model.Track

/**
 * Service for playing audio in background (foreground)
 *
 * @property intent Intent, sent from the caller of the service,
 * where track position is transferred
 *
 * @property binder Object that allows other application components
 * to bind to the [AudioService]
 *
 * @property notificationId Id of the [notification]
 * @property notification Notification, displayed in foreground
 *
 * @property player Instance of the player
 *
 * @property playerNotificationManager object that displays notification and
 * allows [AudioService] to run in foreground
 *
 * @property playlist List of [Track] in current playlist
 *
 * @author David Studenikin
 */
class AudioService : Service() {

    private lateinit var intent: Intent
    private val binder: AudioServiceBinder = AudioServiceBinder()

    private var notificationId: Int = 0
    private var notification: Notification? = null

    lateinit var player: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager

    val playlist = ArrayList<Track>()

    /**
     * Initializes [player] and [playerNotificationManager]
     */
    override fun onCreate() {
        super.onCreate()

        initializePlayer()
        initializeNotificationManager()
    }

    /**
     * Initializes [player].
     *
     * Sets [AudioAttributes] to player in order to handle audio focus properly.
     *
     * Adds [Player.EventListener] to [player], which tracks its current play state.
     * When [player] starts playing audio, transfers [AudioService] to foreground and
     * disallows user to dismiss [notification] and system to stop [AudioService].
     * When [player] pauses playing audio, transfers [AudioService] to background and
     * allows user to dismiss [notification] and system to stop [AudioService] if needed.
     *
     * Sets repeat mode and shuffle mode for player,
     * based on their previous state, saved in Shared Preferences
     */
    private fun initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultRenderersFactory(this),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )

        player.setAudioAttributes(
            AudioAttributes.DEFAULT,
            true
        )

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playWhenReady && playbackState == Player.STATE_READY) {
                    startForeground(notificationId, notification)
                    playerNotificationManager.setOngoing(true)
                } else if (!playWhenReady) {
                    stopForeground(false)
                    playerNotificationManager.setOngoing(false)
                }
            }
        })

        val sharedPreferences = getSharedPreferences(PLAYBACK_MODE, Context.MODE_PRIVATE)
        player.repeatMode = sharedPreferences.getInt(REPEAT_MODE, Player.REPEAT_MODE_OFF)
        player.shuffleModeEnabled = sharedPreferences.getBoolean(SHUFFLE_MODE, false)
    }

    /**
     * Initializes [playerNotificationManager].
     *
     * Indicates data that will be shown in [notification].
     *
     * Adds listener to [playerNotificationManager] that tracks current state of [notification].
     * When [notification] is cancelled stops [AudioService].
     * When [notification] is started  moves [AudioService] to foreground and disallows system to stop it.
     *
     * Connects [notification] with [player]
     */
    private fun initializeNotificationManager() {
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            this,
            PLAYBACK_CHANNEL_ID,
            R.string.playback_channel_name,
            PLAYBACK_NOTIFICATION_ID,
            object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                    val intent = Intent(this@AudioService, PlayerActivity::class.java)
                    return PendingIntent.getActivity(this@AudioService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                }

                override fun getCurrentContentText(player: Player?): String? {
                    return if (player != null) playlist[player.currentWindowIndex].artist
                    else null
                }

                override fun getCurrentContentTitle(player: Player?): String {
                    return if (player != null) playlist[player.currentWindowIndex].name
                    else ""
                }

                override fun getCurrentLargeIcon(
                    player: Player?,
                    callback: PlayerNotificationManager.BitmapCallback?
                ): Bitmap? {
                    return if (player != null)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            (resources.getDrawable(
                                playlist[player.currentWindowIndex].artResource,
                                theme
                            ) as BitmapDrawable).bitmap
                        else null
                    else null
                }
            }
        )

        playerNotificationManager.setNotificationListener(object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationCancelled(notificationId: Int) = stopSelf()

            override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
                this@AudioService.notificationId = notificationId
                this@AudioService.notification = notification

                startForeground(notificationId, notification)
            }
        })

        /*  The methods below remove a fast forward button and a rewind button.
            The removal of these buttons, adds a stop button (God knows why)
            and an explicit removal of it is required (cause a stop action may crash an app
            in some cases). But an explicit removal of stop button causes another issue:
            Dismissing notification doesn't call an onNotificationCancelled method and thus
            service doesn't stop and runs in background until system will need to reclaim more memory. */

        playerNotificationManager.setFastForwardIncrementMs(0)
        playerNotificationManager.setRewindIncrementMs(0)
        playerNotificationManager.setStopAction(null)

        playerNotificationManager.setPlayer(player)
    }

    /**
     * Initializes [playlist] based on track position, transferred through [intent]
     */
    private fun initializePlaylist() {
        val trackPosition = intent.getIntExtra(TRACK_POSITION, 0)

        playlist.clear()
        for (i in trackPosition..(MusicLibrary.trackList.size - 1))
            playlist.add(MusicLibrary.trackList[i])
    }

    /**
     * Sets media sources to [player].
     * Starts playing audio.
     */
    private fun startPlayer() {
        player.prepare(preparePlaylist(), true, true)

        player.seekTo(0, 0)
        player.playWhenReady = true
    }

    /**
     * Initializes [ConcatenatingMediaSource] for [player] based on [playlist]
     */
    private fun preparePlaylist(): MediaSource {
        val mediaSources = Array(playlist.size) {
            buildMediaSource(Uri.parse(playlist[it].link))
        }

        return ConcatenatingMediaSource(*mediaSources)
    }

    /**
     * Builds [MediaSource] from URL
     */
    private fun buildMediaSource(uri: Uri): MediaSource =
        ExtractorMediaSource.Factory(DefaultHttpDataSourceFactory(getString(R.string.user_agent))).createMediaSource(uri)

    /**
     * Initializes [playlist] and starts playing audio
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) stopSelf()
        else this.intent = intent

        initializePlaylist()
        startPlayer()

        return START_STICKY
    }

    /**
     * Provides application components, that want to connect to [AudioService],
     * with [binder]
     */
    override fun onBind(intent: Intent): IBinder = binder

    /**
     * Returns true, which allows application components to call onRebind instead of onBind
     * if they were connected to [AudioService] in the past
     */
    override fun onUnbind(intent: Intent?): Boolean = true

    /**
     * Disconnects [playerNotificationManager] from [player] and releases [player]
     */
    override fun onDestroy() {
        playerNotificationManager.setPlayer(null)
        player.release()

        super.onDestroy()
    }

    /**
     * Binder class that provides application components with [AudioService] instance
     */
    inner class AudioServiceBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }

}
