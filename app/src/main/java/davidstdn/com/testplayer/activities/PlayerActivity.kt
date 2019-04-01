package davidstdn.com.testplayer.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerControlView
import davidstdn.com.testplayer.*
import davidstdn.com.testplayer.services.AudioService

/**
 * This class represents [AppCompatActivity] for player window.
 * Binds to [AudioService] to be able to control player states
 * and to receive information about current playlist
 *
 * @property audioService The instance of [AudioService], which provides
 * interaction with player
 *
 * @property connection The object used to connect to [AudioService]
 * and to provide information about current state of connection
 *
 * @author David Studenikin
 */
class PlayerActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var audioService: AudioService

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            audioService = (p1 as AudioService.AudioServiceBinder).getService()
            bindPlayer()
            showTrackInfo(audioService.player.currentWindowIndex)
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.d(LOG_TAG, "Service Disconnected")
        }
    }

    private lateinit var playerView: PlayerControlView

    private lateinit var buttonRepeat: ImageView
    private lateinit var buttonShuffle: ImageView

    private lateinit var textViewTrack: TextView
    private lateinit var textViewArtist: TextView
    private lateinit var textViewAlbum: TextView
    private lateinit var imageViewArtwork: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.player_view)

        buttonRepeat = findViewById(R.id.button_repeat)
        buttonRepeat.setOnClickListener(this)
        initRepeat()

        buttonShuffle = findViewById(R.id.button_shuffle)
        buttonShuffle.setOnClickListener(this)
        initShuffle()

        findViewById<ImageButton>(R.id.button_close).setOnClickListener(this)

        textViewTrack = findViewById(R.id.textview_track)
        textViewArtist = findViewById(R.id.textview_artist)
        textViewAlbum = findViewById(R.id.textview_album)
        imageViewArtwork = findViewById(R.id.artwork)
    }

    /**
     * Initializes icon for [buttonRepeat]
     * based on its previous state, saved to Shared Preferences
     */
    private fun initRepeat() {
        when (getSharedPreferences(PLAYBACK_MODE, Context.MODE_PRIVATE).getInt(REPEAT_MODE, Player.REPEAT_MODE_OFF)) {
            Player.REPEAT_MODE_OFF ->
                buttonRepeat.setImageResource(R.drawable.ic_repeat_black_v24)

            Player.REPEAT_MODE_ALL ->
                buttonRepeat.setImageResource(R.drawable.ic_repeat_white_v24)

            Player.REPEAT_MODE_ONE ->
                buttonRepeat.setImageResource(R.drawable.ic_repeat_once_white_v24)
        }
    }

    /**
     * Initializes icon for [buttonShuffle]
     * based on its previous state, saved to Shared Preferences
     */
    private fun initShuffle() {
        when (getSharedPreferences(PLAYBACK_MODE, Context.MODE_PRIVATE).getBoolean(SHUFFLE_MODE, false)) {
            false -> buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_v24)
            true -> buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_v24)
        }
    }

    /**
     * Binds [PlayerActivity] to [AudioService]
     */
    override fun onStart() {
        super.onStart()

        bindService(
            Intent(this, AudioService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * Unbinds [PlayerActivity] from [AudioService]
     */
    override fun onStop() {
        super.onStop()

        unbindService(connection)
    }

    /**
     * For [buttonRepeat]:
     * Changes repeat mode of player, icon of [buttonRepeat]
     * and saves new state to Shared Preferences
     *
     * For [buttonShuffle]:
     * Changes shuffle mode of player, icon of [buttonShuffle]
     * and saves new state to Shared Preferences
     *
     * For close button:
     * calls finish() method for [PlayerActivity]
     */
    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.button_repeat -> {
                val sharedPreferences = getSharedPreferences(PLAYBACK_MODE, Context.MODE_PRIVATE)

                val previousRepeatMode = sharedPreferences.getInt(REPEAT_MODE, Player.REPEAT_MODE_OFF)
                val nextRepeatMode: Int

                when (previousRepeatMode) {
                    Player.REPEAT_MODE_OFF -> {
                        audioService.player.repeatMode = Player.REPEAT_MODE_ALL
                        nextRepeatMode = Player.REPEAT_MODE_ALL
                        (p0 as ImageView).setImageResource(R.drawable.ic_repeat_white_v24)
                    }

                    Player.REPEAT_MODE_ALL -> {
                        audioService.player.repeatMode = Player.REPEAT_MODE_ONE
                        nextRepeatMode = Player.REPEAT_MODE_ONE
                        (p0 as ImageView).setImageResource(R.drawable.ic_repeat_once_white_v24)
                    }

                    Player.REPEAT_MODE_ONE -> {
                        audioService.player.repeatMode = Player.REPEAT_MODE_OFF
                        nextRepeatMode = Player.REPEAT_MODE_OFF
                        (p0 as ImageView).setImageResource(R.drawable.ic_repeat_black_v24)
                    }

                    else -> {
                        audioService.player.repeatMode = Player.REPEAT_MODE_OFF
                        nextRepeatMode = Player.REPEAT_MODE_OFF
                        (p0 as ImageView).setImageResource(R.drawable.ic_repeat_black_v24)
                    }
                }
                val sharedPreferencesEditor = sharedPreferences.edit()
                sharedPreferencesEditor.putInt(REPEAT_MODE, nextRepeatMode)
                sharedPreferencesEditor.apply()
            }

            R.id.button_shuffle -> {
                val sharedPreferences = getSharedPreferences(PLAYBACK_MODE, Context.MODE_PRIVATE)

                val previousShuffleMode = sharedPreferences.getBoolean(SHUFFLE_MODE, false)
                val nextShuffleMode: Boolean

                when (previousShuffleMode) {
                    false -> {
                        audioService.player.shuffleModeEnabled = true
                        playerView.show()
                        nextShuffleMode = true
                        (p0 as ImageView).setImageResource(R.drawable.ic_shuffle_white_v24)
                    }

                    true -> {
                        audioService.player.shuffleModeEnabled = false
                        playerView.show()
                        nextShuffleMode = false
                        (p0 as ImageView).setImageResource(R.drawable.ic_shuffle_black_v24)
                    }
                }
                val sharedPreferencesEditor = sharedPreferences.edit()
                sharedPreferencesEditor.putBoolean(SHUFFLE_MODE, nextShuffleMode)
                sharedPreferencesEditor.apply()
            }

            R.id.button_close -> {
                finish()
                //audioService.stopSelf()
            }
        }
    }

    /**
     * Adds [Player.EventListener] to player, that tracks a track, which is played
     * to show track metadata
     *
     * Connects [playerView] to player instance
     */
    private fun bindPlayer() {
        audioService.player.addListener(object : Player.EventListener {
            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                super.onTracksChanged(trackGroups, trackSelections)
                showTrackInfo(audioService.player.currentWindowIndex)
            }
        })

        playerView.player = audioService.player
        playerView.showTimeoutMs = -1 // Needed to avoid playerView disappearance
    }

    /**
     * Shows metadata of the track, which is currently played
     */
    private fun showTrackInfo(trackPosition: Int) {
        val currentTrack = audioService.playlist[trackPosition]
        textViewTrack.text = currentTrack.name
        textViewArtist.text = currentTrack.artist
        textViewAlbum.text = currentTrack.album
        imageViewArtwork.setImageResource(currentTrack.artResource)
    }

}
