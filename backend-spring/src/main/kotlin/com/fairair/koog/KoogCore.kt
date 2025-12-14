package com.fairair.koog

import kotlin.annotation.AnnotationTarget.CLASS

@Target(CLASS)
annotation class KoogAgent

open class KoogException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

interface Node {
    val name: String
    suspend fun execute(context: MutableMap<String, Any>): MutableMap<String, Any>
}

class ToolNode(
    override val name: String,
    private val action: suspend (MutableMap<String, Any>) -> MutableMap<String, Any>
) : Node {
    override suspend fun execute(context: MutableMap<String, Any>): MutableMap<String, Any> {
        return action(context)
    }
}

class AIAgentGraph(private val nodes: List<Node>) {
    suspend fun execute(initialInput: Map<String, Any>): Map<String, Any> {
        val context = initialInput.toMutableMap()
        for (node in nodes) {
            val result = node.execute(context)
            context.putAll(result)
        }
        return context
    }
}

class AgentGraphBuilder {
    private val nodes = mutableListOf<Node>()

    fun addNode(name: String, action: suspend (MutableMap<String, Any>) -> MutableMap<String, Any>): AgentGraphBuilder {
        nodes.add(ToolNode(name, action))
        return this
    }
    
    fun build(): AIAgentGraph {
        return AIAgentGraph(nodes)
    }
}
