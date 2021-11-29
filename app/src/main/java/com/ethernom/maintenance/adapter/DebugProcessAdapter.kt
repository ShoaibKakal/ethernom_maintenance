package com.ethernom.maintenance.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ethernom.maintenance.R
import com.ethernom.maintenance.databinding.ItemDebugProcessBinding
import com.ethernom.maintenance.model.CapsuleOAModel

class DebugProcessAdapter(ctx: Context, array: MutableList<CapsuleOAModel>) :
    RecyclerView.Adapter<DebugProcessAdapter.DebugProcessViewHolder>() {

    private val context: Context = ctx
    private val arrayList: MutableList<CapsuleOAModel> = array

    inner class DebugProcessViewHolder(private val itemViewBinding: ItemDebugProcessBinding) :
        RecyclerView.ViewHolder(itemViewBinding.root) {
        @SuppressLint("ResourceAsColor")
        fun bind(context: Context, item: CapsuleOAModel) {
            itemViewBinding.tvAo.text = item.ao.toString()
            itemViewBinding.tvCs.text = item.cs.toString()
            itemViewBinding.tvEnt.text = item.event.toString()
            if(adapterPosition % 2 == 0) {
                itemViewBinding.lnRow.background = ContextCompat.getDrawable(context, R.drawable.bg_row_capsule_gray)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugProcessViewHolder {
        return DebugProcessViewHolder(ItemDebugProcessBinding.inflate(LayoutInflater.from(context), parent, false))

    }

    override fun onBindViewHolder(holder: DebugProcessViewHolder, position: Int) {
        holder.bind(context, arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }


}