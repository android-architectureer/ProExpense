package com.arduia.expense.ui.splash

import android.app.Application
import androidx.lifecycle.*
import com.arduia.expense.data.SettingsRepository
import com.arduia.expense.data.SettingsRepositoryImpl
import com.arduia.expense.ui.common.EventLiveData
import com.arduia.expense.ui.common.EventUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashViewModel(app: Application): AndroidViewModel(app), LifecycleObserver{

    private val _firstTimeEvent = EventLiveData<Unit>()
    val firstTimeEvent = _firstTimeEvent.asLiveData()

    private val _normalUserEvent = EventLiveData<Unit>()
    val normalUserEvent = _normalUserEvent.asLiveData()

    private val settingsRepo: SettingsRepository by lazy {
        SettingsRepositoryImpl(app, viewModelScope)
    }

    private val splashDuration = 1000L

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume(){
        viewModelScope.launch(Dispatchers.IO) {
            delay(splashDuration)
            when(settingsRepo.getFirstUser().first()){
                true -> {
                    settingsRepo.setSelectedLanguage("en")
                    _firstTimeEvent.postValue(EventUnit)
                }
                false -> _normalUserEvent.postValue(EventUnit)
            }
        }
    }

}