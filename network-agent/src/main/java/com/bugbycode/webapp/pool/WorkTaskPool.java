package com.bugbycode.webapp.pool;

import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 线程池
 */
public class WorkTaskPool extends ThreadGroup {

	private final Logger logger = LogManager.getLogger(WorkTaskPool.class);
	
	private LinkedList<Runnable> queue;
	
	private boolean isClosed = true;
	
	private int max_thread = 1;
	
	public WorkTaskPool(String name,int max_thread) {
		super(name);
		this.queue = new LinkedList<Runnable>();
		this.max_thread = max_thread;
		this.start();
		logger.info(name + " start.");
	}
	
	public synchronized void add(Runnable task) {
		if(task == null || isClosed) {
			return;
		}
		this.queue.addLast(task);
		this.notifyAllTask();
	}
	
	public synchronized void notifyAllTask() {
		this.notifyAll();
	}

	public synchronized Runnable getTask() throws InterruptedException {
		while(queue.isEmpty()) {
			wait();
			if(this.isClosed) {
				throw new InterruptedException("Thread pool closed.");
			}
		}
		return this.queue.removeFirst();
	}
	
	private void start() {
		for(int index = 0;index < max_thread;index++) {
			new WorkThread().start();
		}
		this.isClosed = false;
	}
	
	private class WorkThread extends Thread {

		@Override
		public void run() {
			try {
				while(!isClosed) {
					Runnable task = getTask();
					task.run();
				}
			}catch (Exception e) {
				logger.error(e.getLocalizedMessage());
			}
		}
		
	}
}
