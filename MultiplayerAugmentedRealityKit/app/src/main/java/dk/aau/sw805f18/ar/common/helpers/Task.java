package dk.aau.sw805f18.ar.common.helpers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Task {
    private static boolean mInit;
    private static ExecutorService mExecutorService;

    public static void run(Callable callable) {
        if (!mInit) {
            init();
        }
        mExecutorService.submit(callable);
    }

    private static void init() {
        int corePoolSize = 2;

        mExecutorService = Executors.newFixedThreadPool(corePoolSize);
        mInit = true;
    }
}

