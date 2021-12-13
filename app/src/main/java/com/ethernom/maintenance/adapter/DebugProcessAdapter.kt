package com.ethernom.maintenance.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ethernom.maintenance.databinding.ItemCapsuleAoListBinding
import com.ethernom.maintenance.databinding.ItemCapsuleStatusListBinding
import com.ethernom.maintenance.model.DebugProcessSealed

class DebugProcessAdapter(ctx: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val context: Context = ctx
    private val mutableList: MutableList<DebugProcessSealed> = mutableListOf()

    companion object{
        const val CAPSULE_STATUS = 0
        const val CAPSULE_AO = 1
    }

    inner class CapsuleStatusViewHolder(private val itemBinding: ItemCapsuleStatusListBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: DebugProcessSealed.CapsuleStatus) {
            itemBinding.rcvCtStatus.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = CapsuleStatusAdapter(context, item.capsuleStatusList)
            }
        }
    }

    inner class CapsuleAOViewHolder(private val itemBinding: ItemCapsuleAoListBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(item: DebugProcessSealed.CapsuleAOs) {
            itemBinding.rcvDebugProcess.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = CapsuleAOAdapter(context, item.capsuleAOList)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            CAPSULE_STATUS -> CapsuleStatusViewHolder(ItemCapsuleStatusListBinding.inflate(LayoutInflater.from(context), parent, false))
            CAPSULE_AO -> CapsuleAOViewHolder(ItemCapsuleAoListBinding.inflate(LayoutInflater.from(context), parent, false))
            else -> throw IllegalAccessException("Invalid view type!!!")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val item = mutableList[position]){
            is DebugProcessSealed.CapsuleStatus -> (holder as CapsuleStatusViewHolder).bind(item)
            is DebugProcessSealed.CapsuleAOs -> (holder as CapsuleAOViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = mutableList.size

    override fun getItemViewType(position: Int): Int {
        return when(mutableList[position]) {
            is DebugProcessSealed.CapsuleStatus -> CAPSULE_STATUS
            is DebugProcessSealed.CapsuleAOs -> CAPSULE_AO
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addDataList(debugProcessSealed: DebugProcessSealed){
        mutableList.add(debugProcessSealed)
        notifyDataSetChanged()
    }
}