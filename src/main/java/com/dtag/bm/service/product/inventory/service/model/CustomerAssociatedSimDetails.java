package com.dtag.bm.service.product.inventory.service.model;

import java.util.Set;

public class CustomerAssociatedSimDetails {
	
	private String custId;
	private String custName;
	private String custType;
	private Set<Object> sims;

	public Set<Object> getSims() {
		return sims;
	}

	public void setSims(Set<Object> sims) {
		this.sims = sims;
	}

	public String getCustId() {
		return custId;
	}

	public void setCustId(String custId) {
		this.custId = custId;
	}

	public String getCustName() {
		return custName;
	}

	public void setCustName(String custName) {
		this.custName = custName;
	}

	public String getCustType() {
		return custType;
	}

	public void setCustType(String custType) {
		this.custType = custType;
	}
	
	@Override
	public String toString() {
		return "CustomerAssociatedSims [custId=" + custId + ", custName=" + custName + ", custType=" + custType + ", sims=" + sims + "]";
	}


}
