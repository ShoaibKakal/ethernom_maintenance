package com.ethernom.maintenance.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ethernom.maintenance.R
import com.ethernom.maintenance.databinding.ItemDeviceBinding
import com.ethernom.maintenance.model.DeviceModel

class DeviceAdapter(ctx:Context, deviceList: MutableList<DeviceModel>, itemCallback: (DeviceModel) -> Unit): RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val context: Context = ctx
    private val devices: MutableList<DeviceModel> = deviceList
    private val itemClickCallback: (DeviceModel) -> Unit = itemCallback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(ItemDeviceBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    inner class DeviceViewHolder(private val itemBinding: ItemDeviceBinding): RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: DeviceModel){
            itemBinding.txtCardName.text = item.name
            itemBinding.txtCardSn.text = "SN:${item.sn}"
            itemBinding.root.setOnClickListener {
                itemClickCallback.invoke(item)
            }
        }
    }
}