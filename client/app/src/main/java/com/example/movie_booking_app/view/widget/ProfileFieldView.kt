package com.example.movie_booking_app.view.widget

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ItemProfileFieldBinding

class ProfileFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ItemProfileFieldBinding

    init {
        binding = ItemProfileFieldBinding.inflate(LayoutInflater.from(context), this, true)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProfileFieldView, defStyleAttr, 0)
        try {
            val iconRes = typedArray.getResourceId(R.styleable.ProfileFieldView_fieldIcon, R.drawable.ic_profile)
            binding.ivFieldIcon.setImageResource(iconRes)

            val label = typedArray.getString(R.styleable.ProfileFieldView_fieldLabel) ?: "Label"
            binding.tvFieldLabel.text = label

            val hint = typedArray.getString(R.styleable.ProfileFieldView_fieldHint) ?: ""
            binding.etFieldValue.hint = hint

            val value = typedArray.getString(R.styleable.ProfileFieldView_fieldValue) ?: ""
            binding.etFieldValue.setText(value)

            val editable = typedArray.getBoolean(R.styleable.ProfileFieldView_fieldEditable, true)
            setEditable(editable)
        } finally {
            typedArray.recycle()
        }
    }

    fun setIcon(resId: Int) {
        binding.ivFieldIcon.setImageResource(resId)
    }

    fun setLabel(label: String) {
        binding.tvFieldLabel.text = label
    }

    fun getValue(): String {
        return binding.etFieldValue.text.toString().trim()
    }

    fun setValue(value: String) {
        binding.etFieldValue.setText(value)
    }

    fun getEditText(): EditText {
        return binding.etFieldValue
    }

    fun setEditable(editable: Boolean) {
        binding.etFieldValue.isFocusable = editable
        binding.etFieldValue.isFocusableInTouchMode = editable
        binding.etFieldValue.isCursorVisible = editable
        if (!editable) {
            binding.etFieldValue.inputType = InputType.TYPE_NULL
        }
    }

    fun setFieldOnClickListener(listener: OnClickListener?) {
        // When field is not editable (e.g. dropdown, date picker), clicking any part of the view
        // (including the edit text itself) should trigger the action.
        setOnClickListener(listener)
        binding.etFieldValue.setOnClickListener(listener)
    }
}
