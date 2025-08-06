package com.akaes.applimpieza

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

class MensajeAdapter(private val mensajes: MutableList<Mensaje>,
                    private val currentUserId: String):
    RecyclerView.Adapter<MensajeAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textoMensaje: TextView = itemView.findViewById(R.id.tvMensaje)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.sent_message_item, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mensaje = mensajes[position]
        holder.textoMensaje.text = mensaje.texto

        val params = holder.textoMensaje.layoutParams as FrameLayout.LayoutParams

        // Mensaje enviado
        if (mensaje.senderId == currentUserId) {
            params.gravity = Gravity.END
            holder.textoMensaje.setBackgroundTintList(
                ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, R.color.secundario)
            ))
        // Mensaje recibido
        } else {
            params.gravity = Gravity.START
            holder.textoMensaje.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, R.color.gris)
            ))
            holder.textoMensaje.setTextColor(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, R.color.black)
            ))
        }

        holder.textoMensaje.layoutParams = params
    }

    override fun getItemCount(): Int = mensajes.size

    fun agregarMensaje(mensaje: Mensaje) {
        mensajes.add(mensaje)
        notifyItemInserted(mensajes.size - 1)
    }
}