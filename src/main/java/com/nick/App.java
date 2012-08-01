package com.nick;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import oracle.bpm.project.SequenceFlowImpl;
import oracle.bpm.project.model.ProjectObject;
import oracle.bpm.services.client.IBPMServiceClient;
import oracle.bpm.services.instancemanagement.IInstanceManagementService;
import oracle.bpm.services.instancemanagement.model.IActivityInfo;
import oracle.bpm.services.instancemanagement.model.IFlowChangeItem;
import oracle.bpm.services.instancemanagement.model.IGrabInstanceRequest;
import oracle.bpm.services.instancemanagement.model.IGrabInstanceResponse;
import oracle.bpm.services.instancemanagement.model.IProcessComment;
import oracle.bpm.services.instancemanagement.model.IProcessInstance;
import oracle.bpm.services.instancemanagement.model.impl.ProcessComment;
import oracle.bpm.services.instancemanagement.model.impl.alterflow.ActivityInfo;
import oracle.bpm.services.instancemanagement.model.impl.alterflow.FlowChangeItem;
import oracle.bpm.services.instancemanagement.model.impl.alterflow.GrabInstanceRequest;
import oracle.bpm.services.instancequery.IAuditInstance;
import oracle.bpm.services.instancequery.IColumnConstants;
import oracle.bpm.services.instancequery.IInstanceQueryInput;
import oracle.bpm.services.instancequery.IInstanceQueryService;
import oracle.bpm.services.instancequery.impl.InstanceQueryInput;
import oracle.bpm.services.internal.processmodel.model.IProcessModelPackage;
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

	private final int taskNo = 200380;

	public static void main(String[] args) {
		App app = new App();
		app.blah();
	}

	private void blah() {
		// testTaskQuery();
		// testInstanceQueryService();
		// testModifyingProcessInstanceState("80001");
		// testProcessMetadataService("aa");

		// testSkipping(2);
//		 testaaa();
		// testFindingOutNewActivities();
		testGrab();
	}

	private void testaaa() {
		try {
			String instanceId = "140002";

			// get the BPMServiceClient
			IBPMServiceClient bpmServiceClient = Fixture.getBPMServiceClient();

			// authenticate to the BPM engine
			IBPMContext bpmContext = Fixture.getIBPMContext(
					ConnectionConstants.USERNAME, ConnectionConstants.PASSWORD);

			// get details of the process instance
			IInstanceQueryService instanceQueryService = bpmServiceClient
					.getInstanceQueryService();
			IProcessInstance processInstance = instanceQueryService
					.getProcessInstance(bpmContext, instanceId);

			List<IAuditInstance> auditInstances = bpmServiceClient
					.getInstanceQueryService().queryAuditInstanceByProcessId(
							bpmContext, instanceId);

			Set<IAuditInstance> aaa = new HashSet<IAuditInstance>(
					auditInstances);

			for (IAuditInstance a1 : aaa) {

				if (a1.getActivityName().equals("USER_TASK")) {
					System.out.println(a1.getActivityId() + " ("
							+ a1.getLabel() + ")");
				}

			}

		} catch (Exception ex) {

		}
	}

	private void testFindingOutNewActivities() {
		try {

			String instanceId = "140002";

			// get the BPMServiceClient
			IBPMServiceClient bpmServiceClient = Fixture.getBPMServiceClient();

			// authenticate to the BPM engine
			IBPMContext bpmContext = Fixture.getIBPMContext(
					ConnectionConstants.USERNAME, ConnectionConstants.PASSWORD);

			// get details of the process instance
			IInstanceQueryService instanceQueryService = bpmServiceClient
					.getInstanceQueryService();
			IProcessInstance processInstance = instanceQueryService
					.getProcessInstance(bpmContext, instanceId);

			if (processInstance == null) {
				System.out.println("Could not find instance, aborting");
				System.exit(0);
			}

			// get details of the process (not a specific instance of it,
			// but the actual process definition itself)
			// WARNING WARNING WARNING
			// The ProcessModelService is an UNDOCUMENTED API - this means
			// that it could (and probably will) change in some future
			// release - you SHOULD NOT build any code that relies on it,
			// unless you understand and accept the risks of using an
			// undocumented API.
			IProcessModelPackage processModelPackage = bpmServiceClient
					.getProcessModelService().getProcessModel(bpmContext,
							processInstance.getSca().getCompositeDN(),
							processInstance.getSca().getComponentName());

			// get a list of the audit events that have occurred in this
			// instance
			List<IAuditInstance> auditInstances = bpmServiceClient
					.getInstanceQueryService().queryAuditInstanceByProcessId(
							bpmContext, instanceId);

			// work out which activities have not finished
			List<IAuditInstance> started = new ArrayList<IAuditInstance>();
			for (IAuditInstance a1 : auditInstances) {
				if (a1.getAuditInstanceType().compareTo("START") == 0) {
					// ingore the process instance itself, we only care
					// about tasks in the process
					if (a1.getActivityName().compareTo("PROCESS") != 0) {
						started.add(a1);
					}
				}
			}

			for (IAuditInstance a2 : auditInstances) {
				if (a2.getAuditInstanceType().compareTo("END") == 0) {
					for (int i = 0; i < started.size(); i++) {
						if (a2.getActivityId().compareTo(
								started.get(i).getActivityId()) == 0) {
							started.remove(i);
						}
					}
				}
			}
			System.out
					.println("\n\nLooks like the following have started but not ended:");
			for (IAuditInstance s : started) {
				System.out.println(s.getActivityId() + "\nwhich is a "
						+ s.getActivityName() + "\ncalled " + s.getLabel()
						+ "\n");
			}

			// now we need to find what is after these activities...
			// WARNING WARNING WARNING
			// The ProcessModel, ProcessObject, etc. are UNDOCUMENTED APIs -
			// this means that they could (and probably will) change
			// in some future release - you SHOULD NOT build any code
			// that relies on them, unless you understand and
			// accept the risks of using undocumented APIs.
			List<ProjectObject> nextActivities = new ArrayList<ProjectObject>();
			for (ProjectObject po : processModelPackage.getProcessModel()
					.getChildren()) {
				if (po instanceof SequenceFlowImpl) {
					for (IAuditInstance s2 : started) {
						if (((SequenceFlowImpl) po).getSource().getId()
								.compareTo(s2.getActivityId()) == 0) {
							nextActivities.add(po);
						}
					}
				}
			}

			System.out.println("\n\nLooks like the next activities are:");
			for (ProjectObject po2 : nextActivities) {
				System.out.println(((SequenceFlowImpl) po2).getTarget().getId()
						+ "\nwhich is a "
						+ ((SequenceFlowImpl) po2).getTarget().getBpmnType()
						+ "\ncalled "
						+ ((SequenceFlowImpl) po2).getTarget()
								.getDefaultLabel() + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void testGrab() {
		String processInstanceId = "140002";

		try {
			IBPMServiceClient bpmServiceClient = Fixture.getBPMServiceClient();

			IInstanceManagementService instanceManagementService = bpmServiceClient
					.getInstanceManagementService();

			IBPMContext bpmContext = Fixture.getIBPMContext(
					ConnectionConstants.USERNAME, ConnectionConstants.PASSWORD);

			IInstanceQueryService queryService = Fixture.getBPMServiceClient()
					.getInstanceQueryService();

			IProcessInstance processInstance = queryService.getProcessInstance(
					bpmContext, processInstanceId);

			IGrabInstanceRequest grabInstanceRequest = new GrabInstanceRequest();
			grabInstanceRequest.setProcessInstance(processInstance);

			// change flow

			String activityId = processInstance.getSystemAttributes()
					.getActivityId();

			IActivityInfo sourceActivity = ActivityInfo.create(activityId,
					"ccc");
			IActivityInfo targetActivity = ActivityInfo.create(
					"ABSTRACT_ACTIVITY4213107856266", "bbb");
			
			IFlowChangeItem flowChangeItem = FlowChangeItem.create(
					sourceActivity, targetActivity);
			List<IFlowChangeItem> items = new ArrayList<IFlowChangeItem>();
			items.add(flowChangeItem);
			grabInstanceRequest.setRequestedFlowChanges(items);

			IGrabInstanceResponse grabInstance = instanceManagementService
					.grabInstance(bpmContext, grabInstanceRequest);

			if (grabInstance.getInstanceSummary().isSuccessfullyUpdated()) {
				System.out.println("Success!");
			}

		} catch (Exception ex) {
			System.out.println("error!");
		}
	}

	private void testSkipping(int taskNumber) {

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
