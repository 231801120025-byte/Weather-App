package com.example.practical.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.practical.R
import com.example.practical.model.FavoriteCity

class FavoritesAdapter(
    private var favorites: List<FavoriteCity> = emptyList(),
    private val onCitySelected: (FavoriteCity) -> Unit,
    private val onRemoveFavorite: (FavoriteCity) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    fun updateFavorites(newFavorites: List<FavoriteCity>) {
        favorites = newFavorites
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_city, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(favorites[position])
    }

    override fun getItemCount(): Int = favorites.size

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        private val btnRemoveFavorite: ImageButton = itemView.findViewById(R.id.btnRemoveFavorite)

        fun bind(city: FavoriteCity) {
            tvCityName.text = city.name
            itemView.setOnClickListener {
                onCitySelected(city)
            }
            btnRemoveFavorite.setOnClickListener {
                onRemoveFavorite(city)
            }
        }
    }
}
