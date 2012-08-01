package com.nick.util;

import java.util.HashMap;
import java.util.Map;

import com.nick.constant.ConnectionConstants;

import oracle.bpel.services.workflow.client.IWorkflowServiceClient;
import oracle.bpel.services.workflow.client.WorkflowServiceClientFactory;
import oracle.bpel.services.workflow.client.IWorkflowServiceClientConstants.CONNECTION_PROPERTY;

public class ConnectionUtils {

	private static IWorkflowServiceClient workflowServiceClient;

	private ConnectionUtils() {
	}

	public static IWorkflowServiceClient getWorkflowServiceClient() {
		if (workflowServiceClient == null) {
			Map<CONNECTION_PROPERTY, String> properties = new HashMap<>();
			properties.put(CONNECTION_PROPERTY.EJB_PROVIDER_URL, "t3://"
					+ ConnectionConstants.IP_ADDRESS + ":7001");

			workflowServiceClient = WorkflowServiceClientFactory
					.getWorkflowServiceClient(
							WorkflowServiceClientFactory.REMOTE_CLIENT,
							properties, null);
		}

		return workflowServiceClient;
	}

}
