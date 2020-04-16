package com.myApp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppAttachmentGroup implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String checkDataType;
	
	private List<Attachment> attaches = new ArrayList<>();

	public String getCheckDataType() {
		return checkDataType;
	}

	public void setCheckDataType(String checkDataType) {
		this.checkDataType = checkDataType;
	}

	public List<Attachment> getAttaches() {
		return attaches;
	}

	public void setAttaches(List<Attachment> attaches) {
		this.attaches = attaches;
	}
}
