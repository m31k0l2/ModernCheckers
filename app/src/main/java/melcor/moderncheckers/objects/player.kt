package melcor.neurocheckers.objects

import melcor.neurocheckers.network.Game

object player {
    var gamePlayer = Game()
    var playerColor = 0
    var currentColor = 0
    var gameResult = -1 // 0 - win white, 1 - win black, 2 - ничья

    fun new() {
        gamePlayer = Game()
    }
}