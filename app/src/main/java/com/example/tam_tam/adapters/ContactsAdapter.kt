package com.example.tam_tam.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tam_tam.R
import com.example.tam_tam.models.Contact

class ContactsAdapter(
    private var contacts: MutableList<Contact>, // Utilisation d'une liste mutable
    private val clickListener: (Contact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position], clickListener)
    }

    override fun getItemCount(): Int = contacts.size

    fun updateData(newContacts: List<Contact>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContactName: TextView = itemView.findViewById(R.id.tvContactName)
        private val tvContactPhone: TextView = itemView.findViewById(R.id.tvContactPhone)

        fun bind(contact: Contact, clickListener: (Contact) -> Unit) {
            tvContactName.text = contact.name
            tvContactPhone.text = contact.phoneNumber
            itemView.setOnClickListener { clickListener(contact) }
        }
    }
}
