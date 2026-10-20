package com.jjkay03.classgraph.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.Disposer
import java.util.concurrent.CopyOnWriteArrayList

/** Project-level settings store. Mutate [values], then call [changed] so views refresh. */
@Service(Service.Level.PROJECT)
@State(name = "ClassGraphSettings", storages = [Storage("classGraph.xml")])
class GraphSettings : PersistentStateComponent<GraphSettingsState> {
    private var current = GraphSettingsState()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    val values: GraphSettingsState get() = current

    /** Function to hand the current settings to the IDE for saving. */
    override fun getState(): GraphSettingsState = current

    /** Function to restore saved settings when the project opens. */
    override fun loadState(state: GraphSettingsState) {
        current = state
    }

    /** Function to restore every setting to its default and tell the graph views. */
    fun resetToDefaults() {
        current = GraphSettingsState()
        changed()
    }

    /** Function to tell every open graph view that a setting changed. */
    fun changed() = listeners.forEach { it() }

    /** Function to register [listener] for setting changes until [parent] is disposed. */
    fun addListener(parent: Disposable, listener: () -> Unit) {
        listeners.add(listener)
        Disposer.register(parent) { listeners.remove(listener) }
    }
}
