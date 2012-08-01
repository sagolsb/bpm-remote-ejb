package com.nick;

import java.util.ArrayList;
import java.util.List;

import oracle.bpel.services.bpm.common.IBPMContext;
import oracle.bpel.services.common.util.XMLUtil;
import oracle.bpel.services.workflow.client.IWorkflowServiceClient;
import oracle.bpel.services.workflow.query.ITaskQueryService;
import oracle.bpel.services.workflow.repos.Column;
import oracle.bpel.services.workflow.repos.Ordering;
import oracle.bpel.services.workflow.repos.Predicate;
import oracle.bpel.services.workflow.task.ITaskService;
import oracle.bpel.services.workflow.task.model.Task;
import oracle.bpel.services.workflow.verification.IWorkflowContext;
import oracle.bpm.services.instancemanagement.IInstanceManagementService;
import oracle.bpm.services.instancemanagement.model.IProcessComment;
import oracle.bpm.services.instancemanagement.model.IProcessInstance;
import oracle.bpm.services.instancemanagement.model.impl.ProcessComment;
import oracle.bpm.services.instancequery.IColumnConstants;
import oracle.bpm.services.instancequery.IInstanceQueryInput;
import oracle.bpm.services.instancequery.IInstanceQueryService;
import oracle.bpm.services.instancequery.impl.InstanceQueryInput;
import oracle.bpm.services.processmetadata.IProcessMetadataService;
import oracle.bpm.services.processmetadata.ProcessMetadataSummary;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.nick.constant.ConnectionConstants;
import com.nick.util.ConnectionUtils;
import com.nick.util.Fixture;

/**
 * Hello world!
 * 
 */
public class App {

	public final static String TASK_NS = "http://xmlns.oracle.com/bpel/workflow/task";

	public static void main(String[] args) {
		App app = new App();
		app.blah();
	}

	private void blah() {
		// testTaskQuery();
		// testInstanceQueryService();
		// testModifyingProcessInstanceState("80001");
		// testProcessMetadataService("aa");

		testSkipping(2);
	}

	private void testSkipping(int taskNumber) {
		final int taskNo = 200380;

		try {
			// Create JAVA WorflowServiceClient
			IWorkflowServiceClient wfSvcClient = ConnectionUtils
					.getWorkflowServiceClient();

			// Get the task query service
			ITaskQueryService querySvc = wfSvcClient.getTaskQueryService();
			ITaskService taskService = wfSvcClient.getTaskService();

			// Login as jstein
			IWorkflowContext ctx = querySvc.authenticate(
					ConnectionConstants.USERNAME,
					ConnectionConstants.PASSWORD.toCharArray(), null);

			if (taskNumber == 1) {
				Task taskDetailsById = querySvc.getTaskDetailsByNumber(ctx,
						taskNo);

				Element payloadElem = generateElement("shouldInitIn", "false");
				taskDetailsById.setPayloadAsElement(payloadElem);

				taskService.updateTaskOutcome(ctx, taskDetailsById, "OK");

				System.out.println("done task 1");
			} else if (taskNumber == 2) {
				Task taskDetailsById = querySvc.getTaskDetailsByNumber(ctx,
						taskNo);

				Element payloadAsElement = taskDetailsById
						.getPayloadAsElement();
				NodeList childNodes = payloadAsElement.getChildNodes();
				Node item = childNodes.item(0);

				Node item2 = item.getChildNodes().item(0);
				String text = item2.getTextContent();

				Element finalElement = null;

				if ("true".equals(text)) {
					finalElement = generateElement("ageIn", "17");
				} else {
					finalElement = generateElement("ageIn", "21");
				}

				taskDetailsById.setPayloadAsElement(finalElement);

				// Task updateTask = taskService.updateTask(ctx,
				// taskDetailsById);

				taskService.updateTaskOutcome(ctx, taskDetailsById, "OK");

				System.out.println("done task 2");
			}

		} catch (Exception e) {
			// Handle any exceptions raised here...
			System.out.println("Caught workflow exception: " + e.getMessage());
		}

	}

	private Element generateElement(String name, String value) {
		// // set payload
		Document document = null;
		try {
			document = createDocument();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		Element payloadElem = document.createElementNS(TASK_NS, "payload");
		Element ageElem = document.createElementNS(TASK_NS, name);
		ageElem.appendChild(document.createTextNode(value));
		payloadElem.appendChild(ageElem);
		document.appendChild(payloadElem);

		return payloadElem;
	}

	private void testTaskQuery() {
		try {
			// Create JAVA WorflowServiceClient
			IWorkflowServiceClient wfSvcClient = ConnectionUtils
					.getWorkflowServiceClient();

			// Get the task query service
			ITaskQueryService querySvc = wfSvcClient.getTaskQueryService();

			// Login as jstein
			IWorkflowContext ctx = querySvc.authenticate(
					ConnectionConstants.USERNAME,
					ConnectionConstants.PASSWORD.toCharArray(), null);
			// Set up list of columns to query
			List queryColumns = new ArrayList();
			queryColumns.add("TASKID");
			queryColumns.add("TASKNUMBER");
			queryColumns.add("TITLE");
			queryColumns.add("OUTCOME");

			// Query a list of tasks assigned to jstein
			List tasks = querySvc.queryTasks(ctx, queryColumns, null, // Do not
																		// query
																		// additional
																		// info
					ITaskQueryService.AssignmentFilter.ALL, null, // No keywords
					null, // No custom predicate
					null, // No special ordering
					0, // Do not page the query result
					0);
			// Get the task service
			ITaskService taskSvc = wfSvcClient.getTaskService();
			// Loop over the tasks, outputting task information, and approving
			// any
			// tasks whose outcome has not been set...

			System.out.println("There are " + tasks.size() + " task\n");

			for (int i = 0; i < tasks.size(); i++) {
				Task task = (Task) tasks.get(i);
				int taskNumber = task.getSystemAttributes().getTaskNumber();
				String title = task.getTitle();
				String taskId = task.getSystemAttributes().getTaskId();
				String outcome = task.getSystemAttributes().getOutcome();
				// if (outcome == null) {
				// outcome = "APPROVED";
				// taskSvc.updateTaskOutcome(ctx, taskId, outcome);
				// }
				System.out.println("Task id " + taskId + " (" + title + ") is "
						+ outcome);

				if (taskNumber == 200258) {

					Task taskDetailsById = querySvc.getTaskDetailsById(ctx,
							taskId);

					taskSvc.addComment(ctx, taskDetailsById, "FARK HY!!!!!!!");
				}
			}

		} catch (Exception e) {
			// Handle any exceptions raised here...
			System.out.println("Caught workflow exception: " + e.getMessage());
		}
	}

	private void testInstanceQueryService() {
		try {
			IInstanceQueryService queryService = Fixture.getBPMServiceClient()
					.getInstanceQueryService();
			IBPMContext bpmContext = Fixture.getIBPMContext(
					ConnectionConstants.USERNAME, ConnectionConstants.PASSWORD);

			List<Column> columns = new ArrayList<Column>();
			columns.add(IColumnConstants.PROCESS_ID_COLUMN);
			columns.add(IColumnConstants.PROCESS_NUMBER_COLUMN);
			columns.add(IColumnConstants.PROCESS_STATE_COLUMN);
			columns.add(IColumnConstants.PROCESS_TITLE_COLUMN);
			columns.add(IColumnConstants.PROCESS_CREATOR_COLUMN);
			columns.add(IColumnConstants.PROCESS_CREATEDDATE_COLUMN);

			Ordering ordering = new Ordering(
					IColumnConstants.PROCESS_NUMBER_COLUMN, true, true);
			Predicate pred = new Predicate(
					IColumnConstants.PROCESS_STATE_COLUMN, Predicate.OP_EQ,
					"OPEN");
			IInstanceQueryInput input = new InstanceQueryInput();
			input.setAssignmentFilter(IInstanceQueryInput.AssignmentFilter.ALL);

			List<IProcessInstance> processInstances = queryService
					.queryInstances(bpmContext, columns, null, ordering, input);
			System.out
					.println("ProcessId\tProcess#\tState\tTitle\t\t\t\t\tCreator\tCreatedDate");
			for (IProcessInstance instance : processInstances) {
				System.out.println(instance.getSystemAttributes()
						.getProcessInstanceId()
						+ "\t"
						+ instance.getSystemAttributes().getProcessNumber()
						+ "\t"
						+ instance.getSystemAttributes().getState()
						+ "\t"
						+ instance.getTitle()
						+ "\t"
						+ instance.getCreator()
						+ "\t"
						+ instance.getSystemAttributes().getCreatedDate()
								.getTime());
			}
			if (processInstances.isEmpty()) {
				System.out.println("no result");
			}
		} catch (Exception ex) {
			System.out.println("error!");
		}
	}

	private void testModifyingProcessInstanceState(String processInstanceId) {

		try {
			IInstanceQueryService queryService = Fixture.getBPMServiceClient()
					.getInstanceQueryService();
			IBPMContext bpmContext = Fixture.getIBPMContext(
					ConnectionConstants.USERNAME, ConnectionConstants.PASSWORD);

			IProcessInstance processInstance = queryService.getProcessInstance(
					bpmContext, processInstanceId);

			IInstanceManagementService instanceManagementService = Fixture
					.getBPMServiceClient().getInstanceManagementService();

			IProcessComment processComment = new ProcessComment();
			processComment.setComment("fark HY");

			instanceManagementService.addComment(bpmContext, processInstance,
					processComment);

			// String message = null;
			//
			// if (processInstance.getSystemAttributes().getState()
			// .equals("SUSPENDED")) {
			// instanceManagementService.resumeInstance(bpmContext,
			// processInstance);
			// message = "Changed status from SUSPENDED TO OPEN";
			// } else {
			// instanceManagementService.suspendInstance(bpmContext,
			// processInstance);
			// message = "Changed status from OPEN TO SUSPENDED";
			// }
			//
			// System.out.println(message);

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	private void testProcessMetadataService(String processInstanceId) {
		try {
			IInstanceQueryService queryService = Fixture.getBPMServiceClient()
					.getInstanceQueryService();
			IBPMContext bpmContext = Fixture.getIBPMContext(
					ConnectionConstants.USERNAME, ConnectionConstants.PASSWORD);

			IProcessInstance processInstance = queryService.getProcessInstance(
					bpmContext, processInstanceId);

			IProcessMetadataService processMetadataService = Fixture
					.getBPMServiceClient().getProcessMetadataService();

			List<ProcessMetadataSummary> initiatableProcesses = processMetadataService
					.getInitiatableProcesses(bpmContext);

			for (ProcessMetadataSummary processMetadataSummary : initiatableProcesses) {
				String out = processMetadataSummary.getProjectName() + "\t"
						+ processMetadataSummary.getProcessName() + "\n";
				System.out.println(out);
			}

		} catch (Exception ex) {
			System.out.println("error!");
		}
	}

	public Document createDocument() throws Exception {
		return XMLUtil.createDocument();
	}
}
