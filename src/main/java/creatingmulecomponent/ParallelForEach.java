package creatingmulecomponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

@SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
public class ParallelForEach implements Callable {

	private static final Logger LOGGER = Logger.getLogger(ParallelForEach.class);

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception {
		org.mule.api.ThreadSafeAccess.AccessControl.setAssertMessageAccess(false);
		final List payloadAsList;
		final List<Future<MuleEvent>> futureResult;
		if (Collection.class.isAssignableFrom(eventContext.getMessage().getPayload().getClass())) {
			payloadAsList = eventContext.getMessage().getPayload(List.class);
			final List<SingleFlowThread> threadToExec = new ArrayList<>();
			for (int index = 0; index < payloadAsList.size(); index++) {
				Object payload = payloadAsList.get(index);
				threadToExec.add(new SingleFlowThread(eventContext, payload, getFlowToCall(RequestContext.getEvent())));
			}
			final ExecutorService exec = Executors.newFixedThreadPool(threadToExec.size());
			futureResult = exec.invokeAll(threadToExec);
			return collapseAndReturn(futureResult, RequestContext.getEvent());
		} else {
			throw new ParallelForEachException("Payload is not e Collection to split");
		}
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

	private String getFlowToCall(final MuleEvent event) throws ParallelForEachException {
		final String flowName = event.getFlowVariable("flowToCall");
		if(flowName == null) {
			throw new ParallelForEachException("Flow are expected");
		}
		return flowName;
	}

}
