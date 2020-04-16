package com.myApp.ftp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
/**
 * 新影像平台对应的ftp 
 */
@Component
public class NewFTPConfig {

	/** 主机ip **/
	@Value("${newImageSystem.FtpIp}")
	private String host;

	/** 端口号 **/
	@Value("${newImageSystem.FtpPort}")
	private String port;

	/** ftp用户名 **/
	@Value("${newImageSystem.FtpUserName}")
	private String ftpUserName;

	/** ftp密码 **/
	@Value("${newImageSystem.FtpPassword}")
	private String ftpPassword;

	/** ftp中的目录 **/
	@Value("${newImageSystem.FtpFileDir}")
	private String ftpPath;

	/** ftp中文件后缀 **/
	// @Value("${ftp.ftpFileSuffix}")
	private String ftpFileSuffix;

	/** ftp处理后，备份路径 （为空时，不备份） **/
	private String toPath;

	/**
	 * 是否以被动模式连接
	 */
	@Value("${newImageSystem.isPassiveMode}")
	private boolean isPassiveMode = true;

	public String getHost() {
		return host;
	}

	public void setHost(String ftpServer) {
		this.host = ftpServer;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String ftpPort) {
		this.port = ftpPort;
	}

	public String getFtpUserName() {
		return ftpUserName;
	}

	public void setFtpUserName(String ftpUserName) {
		this.ftpUserName = ftpUserName;
	}

	public String getFtpPassword() {
		return ftpPassword;
	}

	public void setFtpPassword(String ftpPassword) {
		this.ftpPassword = ftpPassword;
	}

	public String getFtpPath() {
		return ftpPath;
	}

	public void setFtpPath(String ftpPath) {
		this.ftpPath = ftpPath;
	}

	public String getFtpFileSuffix() {
		return ftpFileSuffix;
	}

	public void setFtpFileSuffix(String ftpFileSuffix) {
		this.ftpFileSuffix = ftpFileSuffix;
	}

	public String getToPath() {
		return toPath;
	}

	public void setToPath(String toPath) {
		this.toPath = toPath;
	}

	public boolean isPassiveMode() {
		return isPassiveMode;
	}

	public void setPassiveMode(boolean isPassiveMode) {
		this.isPassiveMode = isPassiveMode;
	}

}
