package com.myApp.ftp;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *  @author: xuemc
 *  实现FTPClient对象池
 */
public class FTPClientPool implements ObjectPool<PooledObject<FTPClient>> {

	private static Logger log = LoggerFactory.getLogger(FTPClientPool.class);
	
	private static final int DEFAULT_POOL_SIZE = 5;
	
	private final BlockingQueue<PooledObject<FTPClient>> pool;
	
	private final FTPClientFactory ftpClientFactory;
	
	public FTPClientPool(FTPClientFactory ftpClientFactory){
		this(DEFAULT_POOL_SIZE,ftpClientFactory);
	}
	
	public FTPClientPool(int poolSize,FTPClientFactory ftpClientFactory) {
		if(poolSize <= 0){
			poolSize = DEFAULT_POOL_SIZE;
		}
		pool = new ArrayBlockingQueue<PooledObject<FTPClient>>(poolSize << 1);
		this.ftpClientFactory = ftpClientFactory;
		initPool(poolSize);
	}
	
	private void initPool(int maxPoolSize) {
		for(int i=0;i<maxPoolSize;i++){
			try {
				addObject();
			}
		    catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
	}
	
	@Override
	public PooledObject<FTPClient> borrowObject() {
		try{
			PooledObject<FTPClient> po = pool.take();
			if(po == null){
				po = ftpClientFactory.makeObject();
			}
			else if(!ftpClientFactory.validateObject(po)){
				invalidateObject(po);
				po = ftpClientFactory.makeObject();
			}
			return po;
		}
		catch(Exception e){
			log.error("borrow object异常:"+e.getMessage(),e);
		}
		return null;
	}

	@Override
	public void returnObject(PooledObject<FTPClient> obj) throws Exception {
		if(obj != null){ 
			if(!pool.offer(obj, 3, TimeUnit.SECONDS)){
				ftpClientFactory.destroyObject(obj);
			}
		}
	}

	@Override
	public void invalidateObject(PooledObject<FTPClient> obj) throws Exception {
		if(obj == null){
			return;
		}
	    ftpClientFactory.destroyObject(obj);
	}

	@Override
	public void addObject() throws Exception, IllegalStateException, UnsupportedOperationException {
		pool.offer(ftpClientFactory.makeObject(), 3, TimeUnit.SECONDS);
			
	}

	@Override
	public int getNumIdle() {
		return 0;
	}

	@Override
	public int getNumActive() {
		return 0;
	}

	@Override
	public void clear() throws Exception, UnsupportedOperationException {
		
	}

	@Override
	public void close() {
		try {
			while(pool.iterator().hasNext()){
			    PooledObject<FTPClient> po = pool.take();
			    ftpClientFactory.destroyObject(po);
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}

}
