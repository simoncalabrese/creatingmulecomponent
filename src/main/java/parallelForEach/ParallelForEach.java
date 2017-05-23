package parallelForEach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.python.modules.thread.thread;

@SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
public class ParallelForEach implements Callable {

	private static final Logger LOGGER = Logger.getLogger(ParallelForEach.class);
	private static final Integer DEFAULT_SIZE = 100;

	@Override
	public Object onCall(MuleEventContext eventContext) throws ParallelForEachException {
		org.mule.api.ThreadSafeAccess.AccessControl.setAssertMessageAccess(false);
		final List payloadAsList;
		final List<Future<MuleEvent>> futureResult;
		if (Collection.class.isAssignableFrom(eventContext.getMessage().getPayload().getClass())) {
			try {
				payloadAsList = eventContext.getMessage().getPayload(List.class);
				final List<SingleFlowThread> threadToExec = new ArrayList<>();
				for (int index = 0; index < payloadAsList.size(); index++) {
					Object payload = payloadAsList.get(index);
					threadToExec
							.add(new SingleFlowThread(eventContext, payload, getFlowToCall(RequestContext.getEvent())));
				}
				futureResult = executeList(threadToExec);
				return collapseAndReturn(futureResult, RequestContext.getEvent());
			} catch (Exception e) {
				throw new ParallelForEachException(e);
			}
		} else {
			throw new ParallelForEachException("Payload is not e Collection to split");
		}
	}

	private List<Future<MuleEvent>> executeList(List<SingleFlowThread> threadToExec) throws Exception {
		final ExecutorService exec = Executors.newFixedThreadPool(threadToExec.size());
		if (threadToExec.size() > DEFAULT_SIZE) {
			LOGGER.info("Pool size longer than DEFAULT. Will being splitted");
			return invokeSplitted(threadToExec, exec);
		}
		LOGGER.info("Execute all in one pool List");
		return exec.invokeAll(threadToExec);

	}

	private List<Future<MuleEvent>> invokeSplitted(List<SingleFlowThread> threadToExec, ExecutorService exec)
			throws Exception {
		final List<Future<MuleEvent>> parallelResult = new ArrayList<>();
		for (List<SingleFlowThread> in : splitCollectionByOffset(threadToExec, DEFAULT_SIZE)) {
			parallelResult.addAll(exec.invokeAll(in));
		}
		return parallelResult;
	}

	private List collapseAndReturn(final List<Future<MuleEvent>> futures, final MuleEvent actualEvent)
			throws ParallelForEachException {
		try {
			if (CollectionUtils.isNotEmpty(futures)) {
				final List payload = new ArrayList();
				final Set<String> flowVarsNames = new HashSet<>();
				for (Future<MuleEvent> f : futures) {
					final MuleEvent event = f.get();
					payload.add(event.getMessage().getPayload());
					flowVarsNames.addAll(event.getFlowVariableNames());
					for (String s : flowVarsNames) {
						actualEvent.setFlowVariable(s, event.getFlowVariable(s));
					}
				}
				return payload;
			} else {
				throw new ParallelForEachException("Returned Parallel Payloads are not e Collection");
			}
		} catch (Exception e) {
			LOGGER.error(e);
			throw new ParallelForEachException(e);
		}

	}

	private <T> List<List<T>> splitCollectionByOffset(final List<T> toSplit, final Integer offset) {
		List<List<T>> parts = new ArrayList<>();
		final int N = toSplit.size();
		for (int i = 0; i < N; i += offset) {
			parts.add(new ArrayList<>(toSplit.subList(i, Math.min(N, i + offset))));
		}
		return parts;
	}

	private String getFlowToCall(final MuleEvent event) throws ParallelForEachException {
		final String flowName = event.getFlowVariable("flowToCall");
		if (flowName == null) {
			throw new ParallelForEachException("Flow are expected");
		}
		return flowName;
	}

}
