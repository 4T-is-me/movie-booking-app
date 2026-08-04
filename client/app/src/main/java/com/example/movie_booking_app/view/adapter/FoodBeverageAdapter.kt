package com.example.movie_booking_app.view.adapter

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.FoodBeverage
import com.example.movie_booking_app.databinding.ItemFoodBeverageBinding
import com.example.movie_booking_app.utils.Constants
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.util.Locale

class FoodBeverageAdapter(
    private val onQuantityChanged: (FoodBeverage) -> Unit
) : ListAdapter<FoodBeverage, FoodBeverageAdapter.FBViewHolder>(FBDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FBViewHolder {
        val binding = ItemFoodBeverageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FBViewHolder(binding, onQuantityChanged)
    }

    override fun onBindViewHolder(holder: FBViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: FBViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val item = getItem(position)
            holder.updateQuantityOnly(item)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class FBViewHolder(
        private val binding: ItemFoodBeverageBinding,
        private val onQuantityChanged: (FoodBeverage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodBeverage) {
            with(binding) {
                tvProductName.text = item.name
                tvDescription.text = item.description ?: ""
                tvPrice.text = formatCurrency(item.price)
                updateQuantityOnly(item)

                val imageUrl = when {
                    item.imageUrl.isNullOrBlank() -> null
                    item.imageUrl.startsWith("http") -> item.imageUrl
                    else -> "${Constants.BASE_URL.removeSuffix("/")}${item.imageUrl}"
                }

                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imgProduct)

                btnPlus.setOnClickListener {
                    if (item.selectedQuantity < item.stock) {
                        item.selectedQuantity++
                        updateQuantityOnly(item)
                        onQuantityChanged(item)
                    } else {
                        Toast.makeText(itemView.context, itemView.context.getString(R.string.auto_str_maximum_stock_reached), Toast.LENGTH_SHORT).show()
                    }
                }

                btnMinus.setOnClickListener {
                    if (item.selectedQuantity > 0) {
                        item.selectedQuantity--
                        updateQuantityOnly(item)
                        onQuantityChanged(item)
                    }
                }

                tvQuantity.setOnClickListener {
                    showManualInputDialog(item)
                }
            }
        }

        fun updateQuantityOnly(item: FoodBeverage) {
            with(binding) {
                tvQuantity.text = item.selectedQuantity.toString()
                tvStock.text = itemView.context.getString(R.string.fb_in_stock, item.stock)
                btnMinus.isVisible = item.selectedQuantity > 0
                btnPlus.isEnabled = item.selectedQuantity < item.stock
                btnPlus.alpha = if (btnPlus.isEnabled) 1.0f else 0.5f
            }
        }

        private fun showManualInputDialog(item: FoodBeverage) {
            val context = itemView.context
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val padding = (24 * resources.displayMetrics.density).toInt()
                setPadding(padding, 8, padding, 0)
            }

            val til = TextInputLayout(context).apply {
                hint = "Quantity (Max: ${item.stock})"
                errorIconDrawable = null
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(12f, 12f, 12f, 12f)
            }

            val input = TextInputEditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(item.selectedQuantity.toString())
                setSelection(text?.length ?: 0)
            }

            til.addView(input)
            container.addView(til)

            val dialog = AlertDialog.Builder(context)
                .setTitle("Enter Quantity")
                .setMessage("Max available: ${item.stock}")
                .setView(container)
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val value = s.toString().toIntOrNull() ?: 0
                    if (value > item.stock) {
                        til.error = "Exceeds max stock (${item.stock})"
                        okButton.isEnabled = false
                    } else {
                        til.error = null
                        okButton.isEnabled = true
                    }
                }
            })

            okButton.setOnClickListener {
                val value = input.text.toString().toIntOrNull() ?: 0
                item.selectedQuantity = value
                updateQuantityOnly(item)
                onQuantityChanged(item)
                dialog.dismiss()
            }
        }

        private fun formatCurrency(amount: Int): String {
            val formatter = NumberFormat.getInstance(Locale.GERMANY)
            return "${formatter.format(amount)} VND"
        }
    }

    class FBDiffCallback : DiffUtil.ItemCallback<FoodBeverage>() {
        override fun areItemsTheSame(oldItem: FoodBeverage, newItem: FoodBeverage): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FoodBeverage, newItem: FoodBeverage): Boolean = oldItem == newItem
        
        override fun getChangePayload(oldItem: FoodBeverage, newItem: FoodBeverage): Any? {
            return if (oldItem.selectedQuantity != newItem.selectedQuantity) true else null
        }
    }
}
