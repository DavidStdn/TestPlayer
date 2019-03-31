package davidstdn.com.testplayer.model

/**
 * This class is representing a track
 *
 * @author David Studenikin, 2019
 *
 * @property name the title of the track
 * @property artist the artist of the track
 * @property album the album of the track
 * @property artResource the @ResId of the drawable
 * @property link the link to the actual mp3 file
 */
data class Track(
    val name: String,
    val artist: String,
    val album: String,
    val artResource: Int,
    val link: String
)