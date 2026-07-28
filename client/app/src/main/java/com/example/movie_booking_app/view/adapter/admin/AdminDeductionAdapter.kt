package com.example.movie_booking_app.view.adapter.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.FoodBeverage
import com.example.movie_booking_app.data.model.entity.InventoryDeduction
import com.example.movie_booking_app.databinding.ItemAdminDeductionBinding
import java.util.Locale

class AdminDeductionAdapter : RecyclerView.Adapter<AdminDeductionAdapter.ViewHolder>() {

    private var items: List<FoodBeverage> = emptyList()
    private val selectedDeductions = mutableMapOf<String, Int>() // itemCode to quantity

    fun setSourceItems(allProducts: List<FoodBeverage>, currentDeductions: List<InventoryDeduction>?) {
        this.items = allProducts.filter { it.type == "single" }
        selectedDeductions.clear()
        currentDeductions?.forEach {
            selectedDeductions[it.itemCode] = it.quantity
        }
        notifyDataSetChanged()
    }

    fun getDeductions(): List<InventoryDeduction> {
        return selectedDeductions.filter { it.value > 0 }.map { 
            InventoryDeduction(it.key, it.value) 
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminDeductionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        val itemCode = product.inventoryDeductions?.firstOrNull()?.itemCode 
            ?: product.name.uppercase(Locale.ROOT).replace("\\s+".toRegex(), "_")
            
        holder.bind(product, itemCode, selectedDeductions[itemCode] ?: 0) { newQty ->
            if (newQty > 0) {
                selectedDeductions[itemCode] = newQty
            } else {
                selectedDeductions.remove(itemCode)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemAdminDeductionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: FoodBeverage, itemCode: String, quantity: Int, onQtyChanged: (Int) -> Unit) {
            var currentQty = quantity
            binding.tvItemName.text = product.name
            binding.tvItemCode.text = "Code: $itemCode"
            binding.tvQuantity.text = currentQty.toString()

            binding.btnPlus.setOnClickListener {
                currentQty++
                onQtyChanged(currentQty)
                binding.tvQuantity.text = currentQty.toString()
            }

            binding.btnMinus.setOnClickListener {
                if (currentQty > 0) {
                    currentQty--
                    onQtyChanged(currentQty)
                    binding.tvQuantity.text = currentQty.toString()
                }
            }
        }
    }
}
