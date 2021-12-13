package com.ethernom.maintenance.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ethernom.maintenance.R
import com.ethernom.maintenance.databinding.ItemDebugProcessBgBinding
import com.ethernom.maintenance.databinding.ItemDebugProcessBinding
import com.ethernom.maintenance.model.CapsuleOAModel
import java.lang.IllegalArgumentException

class CapsuleAOAdapter(ctx: Context, array: MutableList<CapsuleOAModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val context: Context = ctx
    private val arrayList: MutableList<CapsuleOAModel> = array
    companion object{
        const val FIRST_ROW_TYPE = 0
        const val SECOND_ROW_TYPE = 1
    }

    inner class DebugProcessViewHolder(private val itemViewBinding: ItemDebugProcessBinding) :
        RecyclerView.ViewHolder(itemViewBinding.root) {
        @SuppressLint("ResourceAsColor")
        fun bind(context: Context, item: CapsuleOAModel) {
            itemViewBinding.tvAo.text = item.ao
            itemViewBinding.tvCs.text = item.cs
            itemViewBinding.tvEnt.text = item.event.toString()
        }
    }

    inner class DebugProcessBgViewHolder(private val itemDebugProcessBgBinding: ItemDebugProcessBgBinding):
        RecyclerView.ViewHolder(itemDebugProcessBgBinding.root){
            fun bind(context: Context, item: CapsuleOAModel) {
                itemDebugProcessBgBinding.tvAo.text = item.ao
                itemDebugProcessBgBinding.tvCs.text = item.cs
                itemDebugProcessBgBinding.tvEnt.text = item.event.toString()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            FIRST_ROW_TYPE -> DebugProcessViewHolder(ItemDebugProcessBinding.inflate(LayoutInflater.from(context), parent, false))
            SECOND_ROW_TYPE -> DebugProcessBgViewHolder(ItemDebugProcessBgBinding.inflate(LayoutInflater.from(context), parent, false))
            else -> throw IllegalArgumentException("Error view not found!!!")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(position % 2 == 0){
            (holder as DebugProcessBgViewHolder).bind(context, arrayList[position])
        } else {
            (holder as DebugProcessViewHolder).bind(context, arrayList[position])

        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(position % 2 == 0) SECOND_ROW_TYPE
        else FIRST_ROW_TYPE
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addAOs(array: MutableList<CapsuleOAModel>){
        arrayList.addAll(array)
        notifyDataSetChanged()
    }


}