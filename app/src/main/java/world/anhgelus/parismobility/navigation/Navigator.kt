package world.anhgelus.parismobility.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(val page: NavKey, val top: Set<NavKey>) {
    private val stack: ArrayDeque<NavKey> = ArrayDeque(listOf(page))
    fun go(key: NavKey) {
        stack.addLast(key)
    }

    fun back() {
        stack.removeLast()
    }

    fun current(): NavKey {
        return stack.last()
    }

    fun backStack(): List<NavKey> {
        return stack
    }
}