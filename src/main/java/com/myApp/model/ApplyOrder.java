package com.myApp.model;

import java.io.Serializable;
import java.util.List;

public class ApplyOrder implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String serialNo;
	
	private List<AppAttachmentGroup> appAttachmentGroupList;

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	public List<AppAttachmentGroup> getAppAttachmentGroupList() {
		return appAttachmentGroupList;
	}

	public void setAppAttachmentGroupList(List<AppAttachmentGroup> appAttachmentGroupList) {
		this.appAttachmentGroupList = appAttachmentGroupList;
	}
	
}
