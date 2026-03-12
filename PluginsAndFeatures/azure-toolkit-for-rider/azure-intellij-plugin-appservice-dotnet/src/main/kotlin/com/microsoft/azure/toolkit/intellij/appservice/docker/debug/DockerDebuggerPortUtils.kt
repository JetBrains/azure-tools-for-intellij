package com.microsoft.azure.toolkit.intellij.appservice.docker.debug

import com.intellij.util.NetworkUtils
import com.jetbrains.rider.plugins.appender.docker.deployment.TransformedDeploymentPortPair
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val BASE_FRONTEND_PORT = 57000
private const val BASE_BACKEND_PORT = 57200
private const val BASE_ROSLYN_WORKER_PORT = 57400
private const val PERIOD_LENGTH = 200

private var currentFrontendHostPort = BASE_FRONTEND_PORT
private var currentBackendHostPort = BASE_BACKEND_PORT
private var currentRoslynWorkerHostPort = BASE_ROSLYN_WORKER_PORT

private val mutex = Mutex()

internal suspend fun getAvailableDebuggerPorts(
    containerPortsInUse: Set<Int>,
    hostPortsInUse: Set<Int>,
): DockerDebuggerPorts = mutex.withLock {
    val frontendContainerPort = findAvailableContainerPort(containerPortsInUse, BASE_FRONTEND_PORT)
    val backendContainerPort = findAvailableContainerPort(containerPortsInUse, BASE_BACKEND_PORT)

    val frontendHostPort = NetworkUtils.findFreePort(getNextFrontendHostPort(), hostPortsInUse)
    val backendHostPort =
        NetworkUtils.findFreePort(getNextBackendHostPort(), hostPortsInUse.union(listOf(frontendHostPort)))

    //For Roslyn worker we need the same port for server and client
    val roslynWorkerPort = NetworkUtils.findFreePort(
        getNextRoslynWorkerHostPort(),
        hostPortsInUse.union(listOf(frontendHostPort, backendHostPort))
    )

    return DockerDebuggerPorts(
        TransformedDeploymentPortPair(frontendContainerPort, frontendHostPort),
        TransformedDeploymentPortPair(backendContainerPort, backendHostPort),
        TransformedDeploymentPortPair(roslynWorkerPort, roslynWorkerPort)
    )
}

private fun findAvailableContainerPort(containerPortsInUse: Set<Int>, basePort: Int): Int {
    var result = basePort
    while (containerPortsInUse.contains(result)) {
        result++
    }

    return result
}

private fun getNextFrontendHostPort(): Int {
    val port = currentFrontendHostPort++
    if (currentFrontendHostPort >= BASE_FRONTEND_PORT + PERIOD_LENGTH)
        currentFrontendHostPort = BASE_FRONTEND_PORT

    return port
}

private fun getNextBackendHostPort(): Int {
    val port = currentBackendHostPort++
    if (currentBackendHostPort >= BASE_BACKEND_PORT + PERIOD_LENGTH)
        currentBackendHostPort = BASE_BACKEND_PORT

    return port
}

private fun getNextRoslynWorkerHostPort(): Int {
    val port = currentRoslynWorkerHostPort++
    if (currentRoslynWorkerHostPort >= BASE_ROSLYN_WORKER_PORT + PERIOD_LENGTH)
        currentRoslynWorkerHostPort = BASE_ROSLYN_WORKER_PORT

    return port
}