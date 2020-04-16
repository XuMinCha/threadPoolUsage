package com.myApp.service;

import com.alibaba.fastjson.JSONObject;

public interface MQMessageHandler {

	String getMessageSource();
	
	String isPotentialCustomer();
	
	void handleMessage(JSONObject receivedJson);
}
