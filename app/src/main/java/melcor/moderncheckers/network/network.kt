package melcor.neurocheckers.network

import java.util.*

class Neuron {
    private val a = 1.0
    private val b = 2.0/3.0
    var weights = mutableListOf<Double>()

    // активационная функция
    fun activate(input: List<Double>): Double {
        if (input.size+1 != weights.size) { // инициализация весов в случае изменения топологии сети
            weights = initWeights(input.size)
        }
        val x = listOf(1.0, *input.toTypedArray()) // добавляем вход 1 для смещения
        return a*Math.tanh(b*sum(x))
    }

    // сумматор
    private fun sum(input: List<Double>) = weights.mapIndexed { i, w -> input[i] * w }.sum()

    // инициализация весов случайными значениями
    private fun initWeights(inputSize: Int) = MutableList(inputSize+1, { 0.5 - Random().nextDouble() })
}

class Layer(size: Int=0) {
    val neurons = MutableList(size, { Neuron() })
    fun activate(input: List<Double>) = neurons.map { it.activate(input) }
}

class Network(vararg layerSize: Int, val id: Int=++Network.genId) {
    companion object {
        var genId = 0
    }
    val layers = MutableList(layerSize.size, { i -> Layer(layerSize[i]) })

    fun multiActivate(x: List<List<Double>>): List<Double> {
        synchronized(this) {
            layers[0].neurons.apply {
                if (size != x.size) {
                    clear()
                    addAll(List(x.size, { Neuron() }))
                }
            }
        }
        var y = layers[0].neurons.mapIndexed { i, neuron ->
            neuron.activate(x[i])
        }
        for (i in 1 until layers.size) {
            y = layers[i].activate(y)
        }
        return y
    }
}