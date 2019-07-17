package com.example.firebasechat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firebasechat.FriendlyMessage
import kotlinx.android.synthetic.main.item_message.view.*

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<FriendlyMessage>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return FriendlyMessageVH(inflater, parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val pm = items[position]
        (holder as FriendlyMessageVH).bind(pm)
    }

    override fun getItemCount() = items.size

    fun setItems(items: List<FriendlyMessage>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    private class FriendlyMessageVH(inflater: LayoutInflater, parent: ViewGroup) :
            RecyclerView.ViewHolder(inflater.inflate(R.layout.item_message, parent, false)) {

        fun bind(pm: FriendlyMessage) {
            with(itemView) {
                messageTextView.text = pm.text
                nameTextView.text = pm.name
                Glide.with(photoImageView.context)
                        .load(pm.photoUrl)
                        .into(photoImageView)
            }
        }
    }
}

