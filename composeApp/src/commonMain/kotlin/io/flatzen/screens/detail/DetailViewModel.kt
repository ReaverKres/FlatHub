package io.flatzen.kmpapp.screens.detail

import androidx.lifecycle.ViewModel
import io.flatzen.data.MuseumObject
import io.flatzen.data.MuseumRepository
import kotlinx.coroutines.flow.Flow

class DetailViewModel(private val museumRepository: MuseumRepository) : ViewModel() {
    fun getObject(objectId: Int): Flow<MuseumObject?> =
        museumRepository.getObjectById(objectId)
}
