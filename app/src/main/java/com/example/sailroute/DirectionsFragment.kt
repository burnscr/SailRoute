package com.example.sailroute

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.sailroute.databinding.FragmentDirectionsBinding
import com.example.sailroute.sailing.Route
import java.text.DecimalFormat
import kotlin.math.roundToInt

/**
 * A simple [Fragment] subclass.
 */
class DirectionsFragment : Fragment(R.layout.fragment_directions) {

    private var _binding: FragmentDirectionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectionsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(MapsViewModel::class.java)
        binding.model = viewModel

        viewModel.currentRoute.observe(viewLifecycleOwner, this::updateDirections)
        viewModel.primaryIconId.observe(viewLifecycleOwner, this::updatePrimaryIcon)
        viewModel.windHeading.observe(viewLifecycleOwner) {updateDirections(viewModel.currentRoute.value)}
        viewModel.windVelocity.observe(viewLifecycleOwner) {updateDirections(viewModel.currentRoute.value)}
        viewModel.lastKnownLocation.observe(viewLifecycleOwner) {updateDirections(viewModel.currentRoute.value)}
        viewModel.selectedDestination.observe(viewLifecycleOwner) {updateDirections(viewModel.currentRoute.value)}

        updateDirections(viewModel.currentRoute.value)
        updatePrimaryIcon(viewModel.primaryIconId.value)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun updateDirections(route: Route?) {
        if (route != null) {
            // check if we are close to our destination
            if (route.duration < 10) {
                viewModel.setIconId(R.drawable.anchor)
                binding.primaryDirectionText.text = "You have reached your destination"
                viewModel.setPrimaryVisible(true)
                viewModel.setSecondaryVisible(false)
            } else {
                val departureAngleString = formatAngleString(route.departureWindBearing)
                val arrivalAngleString = formatAngleString(route.arrivalWindBearing)
                val departureDistanceString = metersToDistanceString(route.departureDistance)
                val primaryDirection = "Sail <big><b>$departureAngleString</b></big> to wind for $departureDistanceString<br><small>Compass heading <b>${route.departureHeading}°</b></small>"
                val arrivalDirection = "Then $arrivalAngleString (${route.arrivalHeading}°)"

                binding.primaryDirectionText.text = Html.fromHtml(primaryDirection, Html.FROM_HTML_MODE_COMPACT)
                binding.secondaryDirectionText.text = arrivalDirection

                viewModel.setIconId(R.drawable.ic_baseline_explore_24)
                viewModel.setPrimaryVisible(true)
                viewModel.setSecondaryVisible(route.arrivalDistance > 1)
            }
        } else {
            // check if there's a problem with location services
            if (viewModel.lastKnownLocation.value == null || !viewModel.requestingLocationUpdates.get()) {
                viewModel.setIconId(R.drawable.gps_off)
                binding.primaryDirectionText.text = "Location services are unavailable"
                viewModel.setSecondaryVisible(false)
                viewModel.setPrimaryVisible(true)
            }

            // check if there's a problem with wind heading
            else if (viewModel.windHeading.value == null) {
                viewModel.setIconId(R.drawable.explore_off)
                binding.primaryDirectionText.text = "Wind heading has not been set"
                binding.secondaryDirectionText.text = "Swipe up on bottom drawer to fix"
                viewModel.setSecondaryVisible(true)
                viewModel.setPrimaryVisible(true)
            }

            // check if there's a problem with wind velocity
            else if (viewModel.windVelocity.value == null) {
                viewModel.setIconId(R.drawable.cloud_off)
                binding.primaryDirectionText.text = "Wind speed has not been set"
                binding.secondaryDirectionText.text = "Swipe up on bottom drawer to fix"
                viewModel.setSecondaryVisible(true)
                viewModel.setPrimaryVisible(true)
            }

            // check if no destination has been set
            else if (viewModel.selectedDestination.value == null) {
                viewModel.setIconId(R.drawable.no_destination)
                binding.primaryDirectionText.text = "Destination has not been set"
                binding.secondaryDirectionText.text = "Long click the map to fix"
                viewModel.setSecondaryVisible(true)
                viewModel.setPrimaryVisible(true)
            }

            // default
            else {
                viewModel.setIconId(R.drawable.announcement)
                binding.primaryDirectionText.text = "Something went wrong"
                viewModel.setSecondaryVisible(false)
                viewModel.setPrimaryVisible(true)
            }
        }
    }

    private fun updatePrimaryIcon(resourceId: Int?) {
        if (resourceId == null) {
            if (binding.primaryIcon.isVisible)
                binding.primaryIcon.visibility = View.GONE
        } else {
            binding.primaryIcon.setImageResource(resourceId)
            if (!binding.primaryIcon.isVisible)
                binding.primaryIcon.visibility = View.VISIBLE
        }
    }

    // Formatting Utilities

    private fun formatAngleString(angle: Int): String = when {
        angle == -180 || angle == 180 -> "180°"
        angle > 0 -> "+$angle°"
        else -> "$angle°"
    }

    private fun metersToDistanceString(meters: Double): String {
        val miles = meters / 1609
        val feet = meters * 3.281
        return when {
            miles >= 100 -> "${miles.toInt()} mi"
            miles >= 0.1 -> "${DecimalFormat("#.#").format(miles)} mi"
            feet > 0 -> "${feet.roundToInt()} ft"
            else -> "0 ft"
        }
    }

}