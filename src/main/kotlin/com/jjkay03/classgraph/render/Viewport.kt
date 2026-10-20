package com.jjkay03.classgraph.render

/** World ↔ screen transform with pan and cursor-anchored zoom. */
class Viewport {
    var scale = 1.0
        private set
    var offsetX = 0.0
        private set
    var offsetY = 0.0
        private set
    private var centered = false

    /** Function to convert a world x coordinate to a screen x coordinate. */
    fun screenX(worldX: Double) = worldX * scale + offsetX
    /** Function to convert a world y coordinate to a screen y coordinate. */
    fun screenY(worldY: Double) = worldY * scale + offsetY
    /** Function to convert a screen x coordinate to a world x coordinate. */
    fun worldX(screenX: Double) = (screenX - offsetX) / scale
    /** Function to convert a screen y coordinate to a world y coordinate. */
    fun worldY(screenY: Double) = (screenY - offsetY) / scale

    /** Put the world origin in the middle of the component the first time it has a size. */
    fun centerOnce(width: Int, height: Int) {
        if (centered || width <= 0) return
        offsetX = width / 2.0
        offsetY = height / 2.0
        centered = true
    }

    /** Function to re-center the origin on the next paint, used when a different graph loads. */
    fun resetCentering() { centered = false }

    /** Function to move the view by [dx],[dy] screen pixels. */
    fun panBy(dx: Int, dy: Int) {
        offsetX += dx
        offsetY += dy
    }

    /** Function to zoom by [factor] while keeping the world point under the cursor fixed. */
    fun zoomAt(screenX: Int, screenY: Int, factor: Double) {
        val newScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        val k = newScale / scale
        offsetX = screenX - (screenX - offsetX) * k
        offsetY = screenY - (screenY - offsetY) * k
        scale = newScale
    }

    private companion object {
        const val MIN_SCALE = 0.05
        const val MAX_SCALE = 6.0
    }
}
