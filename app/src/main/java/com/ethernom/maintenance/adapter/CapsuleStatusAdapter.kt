package com.ethernom.maintenance.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ethernom.maintenance.R
import com.ethernom.maintenance.databinding.ItemCtStatusBinding
import com.ethernom.maintenance.model.CapsuleStatusModel
import com.ethernom.maintenance.model.DebugProcessSealed

class CapsuleStatusAdapter(ctx: Context, array: MutableList<CapsuleStatusModel>) :
    RecyclerView.Adapter<CapsuleStatusAdapter.CapsuleStatusViewHolder>() {

    private val context: Context = ctx
    private val arrayList: MutableList<CapsuleStatusModel> = array

    inner class CapsuleStatusViewHolder(private val itemViewBinding: ItemCtStatusBinding) :
        RecyclerView.ViewHolder(itemViewBinding.root) {
        @SuppressLint("ResourceAsColor")
        fun bind(context: Context, item: CapsuleStatusModel) {
            itemViewBinding.tvCt.text = item.ctStatus
            itemViewBinding.tvStatus.text = item.ctValue
            if(adapterPosition % 2 == 0){
                itemViewBinding.lnRow.background = ContextCompat.getDrawable(context, R.drawable.bg_row_capsule_gray)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CapsuleStatusViewHolder {
        return CapsuleStatusViewHolder(ItemCtStatusBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: CapsuleStatusViewHolder, position: Int) {
        holder.bind(context, arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addCapsuleStatus(array: MutableList<CapsuleStatusModel>){
        arrayList.addAll(array)
        notifyDataSetChanged()
    }


}