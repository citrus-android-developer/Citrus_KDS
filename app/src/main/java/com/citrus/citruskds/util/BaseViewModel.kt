package com.citrus.citruskds.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * UiState is current state of views.
 */
interface UiState

/**
 * UiEvent is the user actions.
 */
interface UiEvent

/**
 * UiEffect is the side effects like error messages which we want to show only once.
 */
interface UiEffect

data class ShowAlertEffect(
    val title: UiText? = UiText.DynamicString(""),
    val message: UiText? = UiText.DynamicString(""),
) : UiEffect


abstract class BaseViewModel<Event : UiEvent, State : UiState, Effect : UiEffect> : ViewModel() {

    // Create Initial State of View
    private val initialState: State by lazy { createInitialState() }
    abstract fun createInitialState(): State

    // Get Current State
    val currentState: State
        get() = uiState.value


    var uiState: MutableState<State> = mutableStateOf(initialState)
        private set


    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    val event = _event.asSharedFlow()

    private val _effect: Channel<Effect> = Channel()
    val effect = _effect.receiveAsFlow()


    init {
        subscribeEvents()
    }

    /**
     * Start listening to Event
     */
    private fun subscribeEvents() {
        viewModelScope.launch {
            event.collect {
                handleEvent(it)
            }
        }
    }

    /**
     * Handle each event
     */
    abstract fun handleEvent(event: Event)

    /**
     * Set new Event
     */
    fun setEvent(event: Event) {
        val newEvent = event
        viewModelScope.launch { _event.emit(newEvent) }
    }

    /**
     * Set new Ui State
     */
    protected fun setState(reduce: State.() -> State) {
        val newState = currentState.reduce()
        uiState.value = newState
    }

    /**
     * Set new Effect
     */
    protected fun setEffect(builder: () -> Effect) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }
}


//open class BaseViewModel : ViewModel() {
//    private var jobs = mutableListOf<Job>()
//
//    override fun onCleared() {
//        super.onCleared()
//        jobs.forEach { it.cancel() }
//    }
//}