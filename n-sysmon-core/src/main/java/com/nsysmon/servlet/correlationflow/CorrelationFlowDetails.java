package com.nsysmon.servlet.correlationflow;

import com.nsysmon.data.ACorrelationId;

import java.util.Set;

public class CorrelationFlowDetails {
	private final ACorrelationId aCorrelationId;
	private final Set<ACorrelationId> childs;

	public CorrelationFlowDetails(ACorrelationId aCorrelationId, Set<ACorrelationId> childs){
		this.aCorrelationId = aCorrelationId;
		this.childs = childs;
	}

	public ACorrelationId getaCorrelationId() {
		return aCorrelationId;
	}

	public Set<ACorrelationId> getChilds() {
		return childs;
	}
}
