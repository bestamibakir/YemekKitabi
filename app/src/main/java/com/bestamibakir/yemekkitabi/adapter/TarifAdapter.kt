package com.bestamibakir.yemekkitabi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bestamibakir.yemekkitabi.databinding.RecyclerRowBinding
import com.bestamibakir.yemekkitabi.model.Tarif
import com.bestamibakir.yemekkitabi.view.ListeFragmentDirections

class TarifAdapter(val tarifListesi : List<Tarif>) : RecyclerView.Adapter<TarifAdapter.TarifViewHolder>() {
    class TarifViewHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarifViewHolder {
        val recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return TarifViewHolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
        return tarifListesi.size
    }

    override fun onBindViewHolder(holder: TarifViewHolder, position: Int) {
        holder.binding.recyclerViewTextView.text = tarifListesi[position].isim
        holder.itemView.setOnClickListener {
            val action = ListeFragmentDirections.actionListeFragmentToTarifFragment(bilgi = "eski", tarifListesi[position].id)
            Navigation.findNavController(it).navigate(action)
        }
    }
}