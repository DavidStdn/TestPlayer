package davidstdn.com.testplayer.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import davidstdn.com.testplayer.R

/**
 * @author David Studenikin
 */
class AboutDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_about, null)

        view.findViewById<MaterialButton>(R.id.button_github).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/DavidStdn/TestPlayer")
            }

            startActivity(intent)
            dismiss()
        }

        return AlertDialog.Builder(activity, R.style.AppTheme_AlertDialog).apply {
            setView(view)
        }.create()
    }



}