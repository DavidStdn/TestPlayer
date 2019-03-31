package davidstdn.com.testplayer.adapters

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import davidstdn.com.testplayer.R
import davidstdn.com.testplayer.TRACK_POSITION
import davidstdn.com.testplayer.model.Track
import davidstdn.com.testplayer.activities.PlayerActivity
import davidstdn.com.testplayer.services.AudioService
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for [RecyclerView], containing tracks
 *
 * @author David Studenikin
 */
class TrackAdapter(private val tracks: List<Track>) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_track, parent, false)
        )

    override fun getItemCount(): Int = tracks.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = tracks[position]

        holder.textViewTrack.text = track.name
        holder.textViewArtist.text = track.artist
        holder.textViewDuration.text = track.album
        holder.imageViewArtwork.setImageResource(track.artResource)

        holder.view.setOnClickListener {
            val intent = Intent(it.context, AudioService::class.java)
            intent.putExtra(TRACK_POSITION, position)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.context.startForegroundService(intent)
            else it.context.startService(intent)

            Intent(it.context, PlayerActivity::class.java).also { intent ->
                it.context.startActivity(intent)
            }
        }
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textViewTrack: TextView = view.findViewById(R.id.textview_track)
        val textViewArtist: TextView = view.findViewById(R.id.textview_artist)
        val textViewDuration: TextView = view.findViewById(R.id.textview_album)
        val imageViewArtwork: CircleImageView = view.findViewById(R.id.artwork)
    }
}