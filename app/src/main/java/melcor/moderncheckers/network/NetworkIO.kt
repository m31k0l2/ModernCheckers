package melcor.neurocheckers.network

class NetworkIO {
    fun load(lines: List<String>): Network? {
        val nw = Network()
        var layer: Layer? = null
        var neuron: Neuron? = null
        lines.forEach { line ->
            when (line) {
                "layer" -> {
                    layer?.let {
                        it.neurons.add(neuron!!)
                        nw.layers.add(it)
                        neuron = null
                    }
                    layer = Layer()
                }
                "neuron" -> {
                    neuron?.let { layer!!.neurons.add(it) }
                    neuron = Neuron()
                }
                "end" -> {
                    layer!!.neurons.add(neuron!!)
                    nw.layers.add(layer!!)
                }
                else -> neuron!!.weights.add(line.toDouble())
            }
        }
        return nw
    }
}