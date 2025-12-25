package com.av.pixel.helper;

import com.av.pixel.exception.Error;
import com.av.pixel.exception.IdeogramException;
import com.av.pixel.exception.IdeogramServerException;
import com.av.pixel.exception.IdeogramUnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AsyncUtil {

    @Autowired
    @Qualifier("taskExecutor")
    private TaskExecutor taskExecutor;

    public <T> CompletableFuture<T> executeAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        taskExecutor.execute(() -> {
            try {
                T result = supplier.get();
                future.complete(result);
            } catch (Exception e) {
                log.error("Error executing async task", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }


    public CompletableFuture<Void> executeAsync(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        taskExecutor.execute(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Exception e) {
                log.error("Error executing async task", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    public <T> CompletableFuture<List<T>> executeAllAsync(List<Supplier<T>> suppliers) {
        List<CompletableFuture<T>> futures = suppliers.stream()
                .map(this::executeAsync)
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
}
