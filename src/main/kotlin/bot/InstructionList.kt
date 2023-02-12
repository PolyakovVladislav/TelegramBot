package bot

class InstructionList(
    private val onListChanged: (List<Instruction>) -> Unit
) {

    private val set: MutableSet<Instruction> = sortedSetOf(Comparator { o1, o2 ->
        if (o1.id == o2.id) return@Comparator 0
        val comparingTime = o1.executionTime - o2.executionTime
        val comparingPriority = o1.priority - o2.priority
        return@Comparator if (comparingTime != 0L) {
            comparingTime.toInt()
        }
        else if (comparingPriority == 0) {
            1
        } else {
            comparingPriority
        }
    })
    val list = set.toList()

    fun add(instruction: Instruction) {
        set.add(instruction)
        onListChanged(set.toList())
    }

    fun addAll(list: List<Instruction>) {
        this.set.addAll(list)
        onListChanged(list)
    }

    fun remove(instruction: Instruction) {
        set.remove(instruction)
        onListChanged(set.toList())
    }

    fun remove(id: Int) {
        set.removeIf { it.id == id }
        onListChanged(set.toList())
    }

    fun first(): Instruction {
        return set.first()
    }

    fun isEmpty(): Boolean {
        return set.isEmpty()
    }
}