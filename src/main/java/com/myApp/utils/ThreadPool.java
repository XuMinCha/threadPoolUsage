/**
 * 
 */
package com.myApp.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xuemc
 */
public class ThreadPool {

	private ThreadPoolExecutor threadPool;
	private ScheduledThreadPoolExecutor shceduledPool;

	private ThreadPool() {
		int cpuNum = Runtime.getRuntime().availableProcessors();

		if (cpuNum > 1) {
			cpuNum = cpuNum - 1 ;
		}

		threadPool = new ThreadPoolExecutor(cpuNum, cpuNum << 1, 3, TimeUnit.SECONDS, new SynchronousQueue<>(),
				new HBThreadFactory(false), new ThreadPoolExecutor.CallerRunsPolicy());
		shceduledPool = new ScheduledThreadPoolExecutor(cpuNum*2,new HBThreadFactory(true),new ThreadPoolExecutor.AbortPolicy());
	}

	private static ThreadPool instance = null;

	public static ThreadPool getInstance() {
		if (instance == null) {
			instance = new ThreadPool();
		}
		return instance;
	}

	public void execute(Runnable _runnable) {
		threadPool.execute(_runnable);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return threadPool.submit(task);
	}

	public void execute(Runnable _runnable, long _delay, TimeUnit _timeUnit) {
		shceduledPool.schedule(_runnable, _delay, _timeUnit);
	}

	public void shutDown() {
		threadPool.shutdown();
		shceduledPool.shutdown();
	}
    
	public static class HBThreadFactory implements ThreadFactory{
		
		private AtomicInteger threadNum = new AtomicInteger();
		
		private String namePrefix;
		
		public HBThreadFactory(boolean scheduled){
			if(scheduled){
				namePrefix = "scheduled-thread-";
			}
			else{
				namePrefix = "common-thread-";
			}
		}
		
		public void setNamePrefix(String namePrefix){
			this.namePrefix = namePrefix;
		}
		
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r,namePrefix + threadNum.getAndIncrement());
			if(t.isDaemon()){
				t.setDaemon(false);
			}
			if(t.getPriority() != Thread.NORM_PRIORITY){
				t.setPriority(Thread.NORM_PRIORITY);
			}
		    return t;
		}
	}
	
	public ExecutorService getThreadPoolExecutor(){
		return this.threadPool;
	}
	
	public ScheduledExecutorService getScheduledExecutorService(){
		return this.shceduledPool;
	}
}
