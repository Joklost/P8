package dk.aau.sw805f18.ar.common.helpers;

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

public class RunnableExecutor implements Executor {
    private static RunnableExecutor instance;

    public static RunnableExecutor getInstance() {
        if (instance == null)
            instance = new RunnableExecutor();
        return instance;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        new Thread(command).start();
    }
}
