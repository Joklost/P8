package dk.aau.sw805f18.ar.common.helpers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Task {
    private static boolean mInit;
    private static ExecutorService mExecutorService;


    public static void run(Runnable runnable) {
        if (!mInit) {
            init();
        }
        mExecutorService.execute(runnable);
    }

    private static void init() {
        int corePoolSize = 2;
        int maxPoolSize = 10;
        long keepAliveTime = 5000;

        mExecutorService =
                new ThreadPoolExecutor(
                        corePoolSize,
                        maxPoolSize,
                        keepAliveTime,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>()
                );
        mInit = true;
    }
}
