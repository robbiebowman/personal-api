package com.robbiebowman.personalapi.service

import org.springframework.stereotype.Service
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy





@Service
class AsyncService {

    private var executorService: ExecutorService? = null

    @PostConstruct
    private fun create() {
        executorService = Executors.newSingleThreadExecutor()
    }

    fun process(operation: Runnable?) {
        executorService!!.submit(operation)
    }

    @PreDestroy
    private fun destroy() {
        executorService!!.shutdown()
    }
}