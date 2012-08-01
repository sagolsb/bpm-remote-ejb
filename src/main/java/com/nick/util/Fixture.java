package com.nick.util;

import java.util.HashMap;
import java.util.Map;

import com.nick.constant.ConnectionConstants;

import oracle.bpel.services.bpm.common.IBPMContext;
import oracle.bpel.services.workflow.client.IWorkflowServiceClient;
import oracle.bpel.services.workflow.client.IWorkflowServiceClientConstants;
import oracle.bpel.services.workflow.client.WorkflowServiceClientFactory;
import oracle.bpm.client.BPMServiceClientFactory;
import oracle.bpm.services.client.IBPMServiceClient;

public class Fixture {

	private static String url = "t3://" + ConnectionConstants.IP_ADDRESS
			+ ":7001";

	public static BPMServiceClientFactory getBPMServiceClientFactory() {
		Map<IWorkflowServiceClientConstants.CONNECTION_PROPERTY, String> properties = new HashMap<IWorkflowServiceClientConstants.CONNECTION_PROPERTY, String>();

		properties
				.put(IWorkflowServiceClientConstants.CONNECTION_PROPERTY.CLIENT_TYPE,
						WorkflowServiceClientFactory.REMOTE_CLIENT);
		properties
				.put(IWorkflowServiceClientConstants.CONNECTION_PROPERTY.EJB_PROVIDER_URL,
						url);
		properties
				.put(IWorkflowServiceClientConstants.CONNECTION_PROPERTY.EJB_INITIAL_CONTEXT_FACTORY,
						"weblogic.jndi.WLInitialContextFactory");
		return BPMServiceClientFactory.getInstance(properties, null, null);
	}

	public static IBPMContext getIBPMContext(String username, String password)
			throws Exception {
		return getBPMServiceClientFactory().getBPMUserAuthenticationService()
				.authenticate(username, password.toCharArray(), null);
	}

	public static IWorkflowServiceClient getIWorkflowServiceClient() {
		return getBPMServiceClientFactory().getWorkflowServiceClient();
	}

	public static IBPMServiceClient getBPMServiceClient() {
		return getBPMServiceClientFactory().getBPMServiceClient();
	}

}
