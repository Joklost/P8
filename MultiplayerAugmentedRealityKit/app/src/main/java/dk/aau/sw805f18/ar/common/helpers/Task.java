package dk.aau.sw805f18.ar.common.helpers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class Task {
    private static boolean mInit;
    private static ExecutorService mExecutorService;


    public static void run(Runnable runnable) {
        if (!mInit) {
            init();
        }
        mExecutorService.submit(runnable);
    }

    public static <T> Future<T> getResult(Callable<T> callable) {
        if (!mInit) {
            init();
        }
        return mExecutorService.submit(callable);
    }

    private static void init() {
        int corePoolSize = 2;
        mExecutorService = Executors.newFixedThreadPool(corePoolSize);
        mInit = true;
    }
}

