package com.myApp.ftp;

import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FTPClientFactory implements PooledObjectFactory<FTPClient> {

	private static Logger log = LoggerFactory.getLogger(FTPClientFactory.class);
	
	private NewFTPConfig newFtpConfig;
	
	public FTPClientFactory(NewFTPConfig newFtpConfig){
		this.newFtpConfig = newFtpConfig;
	}
	
	@Override
	public PooledObject<FTPClient> makeObject() throws Exception {
		String host = newFtpConfig.getHost();
		String port = newFtpConfig.getPort();
		String userName = newFtpConfig.getFtpUserName();
		String password = newFtpConfig.getFtpPassword();
		String path = newFtpConfig.getFtpPath();
		
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect(host, Integer.parseInt(port));
		log.info("Connected to " + newFtpConfig.getHost() + ",replyCode:"+ftpClient.getReplyCode());

		ftpClient.login(userName, password);
		ftpClient.setControlEncoding("UTF-8");
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);// 默认使用binary模式
		// Path is the sub-path of the FTP path
		if (path != null && path.length() != 0) {
			ftpClient.changeWorkingDirectory(path);
		}
		
		if(newFtpConfig.isPassiveMode()){
			log.info("set local passive mode");
			ftpClient.enterLocalPassiveMode();// 开启防火墙后，必须采用被动模式
		}
		else{
			log.info("set local active mode");
			ftpClient.enterLocalActiveMode();
		}
		PooledObject<FTPClient> pooledObject = new FTPPooledObject(ftpClient);
		return pooledObject;
	}

	@Override
	public void destroyObject(PooledObject<FTPClient> p) throws Exception {
		FTPClient ftpClient = p.getObject();
		try{
			if(ftpClient != null && ftpClient.isConnected()){
				ftpClient.logout();
			}
		}
		catch(IOException e){
			log.error(e.getMessage(),e);
		}
		finally{
			try{
				if(ftpClient != null){
					ftpClient.disconnect();
				}
			}
			catch(IOException e){
				log.error(e.getMessage(),e);
			}
		}
	}

	@Override
	public boolean validateObject(PooledObject<FTPClient> p) {
		try{
			FTPClient ftpClient = p.getObject();
			return ftpClient.sendNoOp();
		}
		catch(IOException e){
			log.error(e.getMessage(),e);
		}
		return false;
	}

	@Override
	public void activateObject(PooledObject<FTPClient> p) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void passivateObject(PooledObject<FTPClient> p) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
