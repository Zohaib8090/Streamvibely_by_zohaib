package app.streamvibely.theme

import app.streamvibely.domain.ThemeRepository
import app.streamvibely.domain.ThemeSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Keeps the theme in memory only. Used by the fake services (web, desktop, previews). */
class InMemoryThemeRepository(initial: ThemeSpec = ThemeSpec()) : ThemeRepository {
    private val _spec = MutableStateFlow(initial)
    override val spec: StateFlow<ThemeSpec> = _spec.asStateFlow()

    override suspend fun update(transform: (ThemeSpec) -> ThemeSpec) = _spec.update(transform)

    override suspend fun reset() { _spec.value = ThemeSpec() }
}
