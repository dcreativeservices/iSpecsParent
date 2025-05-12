import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.ispecs.parent.R


fun showUpdateDialog(
    context: Context,
    title: String,
    currentValue: Int,
    minValue: Int,
    maxValue: Int,
    updateValue: (Int) -> Unit
) {
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_value, null)
    val editText = dialogView.findViewById<EditText>(R.id.editTextValue)

    editText.setText(currentValue.toString())

    AlertDialog.Builder(context)
        .setTitle(title)
        .setView(dialogView)
        .setPositiveButton("Update") { _, _ ->
            val newValue = editText.text.toString().toIntOrNull()
            if (newValue != null && newValue in minValue..maxValue) {
                updateValue(newValue)
            } else {
                Toast.makeText(context, "Value must be between $minValue and $maxValue", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}
