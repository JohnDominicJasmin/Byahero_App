package com.example.commutingapp.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.commutingapp.R.drawable.*
import com.example.commutingapp.R.string.*
import com.example.commutingapp.databinding.AdaptersIntroSlidersBinding
import com.example.commutingapp.utils.others.Constants.SLIDER_ITEM_COUNTS


/* Adapters provide a binding from an
 app-specific data set to views that are
 displayed within a RecyclerView.*/

class IntroSliderAdapter(
    private val inflater: LayoutInflater,
    private val _context: Context
    ) : RecyclerView.Adapter<IntroSliderAdapter.IntroSliderViewHolder>() {

    private var binding: AdaptersIntroSlidersBinding? = null
    private val images = arrayListOf(
        ic_create_account,
        ic_traveller,
        ic_map,
        ic_weather
    )
    private val headerText = arrayListOf(
        headerTextCreateAccountMessage,
        headerTextEnjoyTripsMessage,
        headerTextChooseDestinationMessage,
        headerTextAccurateWeatherMessage
    )
    private val descriptionText = arrayListOf(
        descriptionTextCreateAccountMessage,
        descriptionTextEnjoyTripMessage,
        descriptionTextChooseDestinationMessage,
        descriptionTextAccurateWeatherMessage

    )



    /* A ViewHolder describes an
    item view and metadata about
    its place within the RecyclerView.*/

    inner class IntroSliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bindAttributes(position: Int) {

            binding?.imageViewDisplaySliders?.setImageResource(elements(position).image)
            binding?.headerTextViewSliders?.text = elements(position).header
            binding?.descriptionTextViewSliders?.text = elements(position).description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroSliderViewHolder {

        binding = AdaptersIntroSlidersBinding.inflate(inflater, parent, false)

        return IntroSliderViewHolder(binding!!.root)

    }


    override fun onBindViewHolder(holder: IntroSliderViewHolder, position: Int) {
        holder.bindAttributes(position)
    }

    private fun elements(position: Int) = object {

        val image = images[position]
        val header = _context.getString(headerText[position])
        val description = _context.getString(descriptionText[position])
    }

    override fun getItemCount() = SLIDER_ITEM_COUNTS

    fun destroyIntroSliderAdapterBinding() {
        binding = null
    }
}