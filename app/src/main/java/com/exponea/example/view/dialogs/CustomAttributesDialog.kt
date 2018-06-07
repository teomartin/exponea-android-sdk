package com.exponea.example.view.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.exponea.example.R
import com.exponea.example.utils.asJson
import com.exponea.sdk.models.PropertiesList

class CustomAttributesDialog : DialogFragment() {

    private lateinit var onUpdate: (PropertiesList) -> Unit
    private val attributes = HashMap<String, Any>()

    companion object {
        const val TAG = "CustomAttributesDialog"

        fun show(fragmentManager: FragmentManager, onUpdate: (PropertiesList) -> Unit) {
            val fragment = fragmentManager.findFragmentByTag(TAG)
                    as? CustomAttributesDialog ?: CustomAttributesDialog()

            fragment.onUpdate = onUpdate
            fragment.show(fragmentManager, TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context, R.style.MyDialogTheme)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_custom_attributes, null, false)
        builder.setView(view)
        initListeners(view)
        return builder.create()
    }


    private fun initListeners(view: View) {
        val nameView = view.findViewById(R.id.editTextName) as EditText
        val valueView = view.findViewById(R.id.editTextValue) as EditText
        val propertiesView = view.findViewById(R.id.textViewAttributes) as TextView
        propertiesView.text = attributes.asJson()
        view.findViewById<Button>(R.id.buttonAddAttr).setOnClickListener {
            if (!nameView.text.isEmpty() && !valueView.text.isEmpty() ) {
                attributes[nameView.text.toString()] = valueView.text.toString()
                propertiesView.text = attributes.asJson()
            }
        }

        view.findViewById<Button>(R.id.buttonUpdate).setOnClickListener {
            val properties = PropertiesList(attributes)
            onUpdate(properties)
            dismiss()
        }


    }

}