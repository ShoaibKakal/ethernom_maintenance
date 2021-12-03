package com.ethernom.maintenance.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ethernom.maintenance.R
import com.ethernom.maintenance.ao.link.LinkDescriptor
import com.ethernom.maintenance.databinding.ItemDeviceBinding
import com.ethernom.maintenance.model.DeviceModel

class DeviceAdapter(ctx:Context, itemCallback: (LinkDescriptor, Int) -> Unit): RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val context: Context = ctx
    private val devices: MutableList<LinkDescriptor> = mutableListOf()
    private val itemClickCallback: (LinkDescriptor, Int) -> Unit = itemCallback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(ItemDeviceBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearAllDevice(){
        devices.clear()
        notifyDataSetChanged()
    }

    fun addDevice(device: LinkDescriptor) {
        devices.add(device)
        notifyItemInserted(devices.size)
    }

    inner class DeviceViewHolder(private val itemBinding: ItemDeviceBinding): RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: LinkDescriptor){
            itemBinding.txtCardName.text = item.deviceName
            itemBinding.txtCardSn.text = "SN:${item.mfgSN}"
            itemBinding.root.setOnClickListener {
                itemClickCallback.invoke(item, adapterPosition)
            }
        }
    }
}