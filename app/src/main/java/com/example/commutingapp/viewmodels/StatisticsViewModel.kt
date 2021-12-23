package com.example.commutingapp.viewmodels

import androidx.lifecycle.ViewModel
import com.example.commutingapp.data.repositories.CommuteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(repository: CommuteRepository) : ViewModel()