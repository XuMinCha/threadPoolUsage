package com.myApp.model;

import java.io.Serializable;

/**
 * 附件资料 
 * 
 */
public class Attachment implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * 主键
	 */
	private String serialNo;
	/**
	 * 申请单号
	 */
	private String objectNo;
	/**
	 * 资料类型
	 */
	private String checkDataType;
	/**
	 * 子类型
	 */
	private String subType;
	/**
	 * S3上的路径
	 */
	private String fileLocation;

	/**
	 * 文件名
	 */
	private String fileName;
	/**
	 * S3上的下载URL
	 */
	private String url;
	/**
	 * 默认 1
	 */
	private Integer custPageCount;
	/**
	 * 是否必填 默认否
	 */
	private Boolean isMust;
	/**
	 * 是否上传 默认是
	 */
	private Boolean isSupply;
	/**
	 * 是否已核查 默认是
	 */
	private Boolean checkList;
	/**
	 * 审批结果
	 */
	private Boolean auditingResult;
	public String getSerialNo() {
		return serialNo;
	}
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	public String getObjectNo() {
		return objectNo;
	}
	public void setObjectNo(String objectNo) {
		this.objectNo = objectNo;
	}
	public String getCheckDataType() {
		return checkDataType;
	}
	public void setCheckDataType(String checkDataType) {
		this.checkDataType = checkDataType;
	}
	public String getSubType() {
		return subType;
	}
	public void setSubType(String subType) {
		this.subType = subType;
	}
	public String getFileLocation() {
		return fileLocation;
	}
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Integer getCustPageCount() {
		return custPageCount;
	}
	public void setCustPageCount(Integer custPageCount) {
		this.custPageCount = custPageCount;
	}
	public Boolean getIsMust() {
		return isMust;
	}
	public void setIsMust(Boolean isMust) {
		this.isMust = isMust;
	}
	public Boolean getIsSupply() {
		return isSupply;
	}
	public void setIsSupply(Boolean isSupply) {
		this.isSupply = isSupply;
	}
	public Boolean getCheckList() {
		return checkList;
	}
	public void setCheckList(Boolean checkList) {
		this.checkList = checkList;
	}
	public Boolean getAuditingResult() {
		return auditingResult;
	}
	public void setAuditingResult(Boolean auditingResult) {
		this.auditingResult = auditingResult;
	}
}
