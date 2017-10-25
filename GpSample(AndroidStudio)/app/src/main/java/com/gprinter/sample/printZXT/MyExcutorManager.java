package com.gprinter.sample.printZXT;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author libohan
 *         邮箱:76681287@qq.com
 *         create on 2017/10/25.
 */

public class MyExcutorManager {
    private static MyExcutorManager manager;
    private ThreadPoolExecutor executorService;
    private MyExcutorManager()
    {
        init();
    }

    private void init() {
        int threadCount=Runtime.getRuntime().availableProcessors()*2+1;
        executorService= new ThreadPoolExecutor(2,Math.min(threadCount,4),3, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(3),new ThreadPoolExecutor.DiscardOldestPolicy());
    }
    public static MyExcutorManager getInstance()
    {
        if (manager==null)
        {
            synchronized (MyExcutorManager.class)
            {
                if (manager==null) {
                    manager = new MyExcutorManager();
                    return manager;
                }
            }
        }
        return manager;
    }
    public ThreadPoolExecutor getExecutor(){
        return this.executorService;
    }
    public void execute(Runnable runnable){
        this.executorService.execute(runnable);
    }
}
