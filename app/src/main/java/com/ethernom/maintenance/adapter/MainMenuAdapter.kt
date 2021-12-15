package com.ethernom.maintenance.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ethernom.maintenance.R
import com.ethernom.maintenance.databinding.ItemMainMenuBinding
import com.ethernom.maintenance.model.MenuModel

class MainMenuAdapter(ctx: Context, menuSelected: (Int) -> Unit): RecyclerView.Adapter<MainMenuAdapter.MainMenuViewHolder>() {

    private val context: Context = ctx
    private val itemClick: (Int) -> Unit = menuSelected
    private val menus = mutableListOf(
        MenuModel(R.string.menu_reset, icon = R.drawable.img_reset),
        MenuModel(R.string.menu_debug_process, icon = R.drawable.img_debug),
        MenuModel(R.string.menu_read_qr, icon = R.drawable.ic_qr_code),
        MenuModel(R.string.menu_about, icon = R.drawable.img_about)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainMenuViewHolder {
        return MainMenuViewHolder(ItemMainMenuBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MainMenuViewHolder, position: Int) {
        holder.bind(menus[position])
    }

    override fun getItemCount(): Int = menus.size

    inner class MainMenuViewHolder(private val itemBinding: ItemMainMenuBinding): RecyclerView.ViewHolder(itemBinding.root){
        fun bind(item: MenuModel){
            itemBinding.apply {
                ivMenu.setImageResource(item.icon)
                tvMenu.text = context.getString(item.title)
                root.setOnClickListener {
                    itemClick.invoke(adapterPosition)
                }

//                when(absoluteAdapterPosition){
//                    0 -> cardMenu.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_factory_reset))
//                    1 -> cardMenu.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_debug_process))
//                    2 -> cardMenu.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_read_qr))
//                    3 -> cardMenu.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_about))
//                }
            }
        }
    }
}