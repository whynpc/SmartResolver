package edu.ucla.cs.wing.smartresolver;

import java.util.HashMap;

import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;

public class DnsCache {
		
	private DnsResolver resolver;
	
	private ContentServerPerfDb perfDb;
	
	private String networkId;
	
	public DnsCache(DnsResolver resolver) {
		this.resolver = resolver;
		
		
					
	}	
	
	public String getNetworkId() {
		return networkId;
	}
	
	public ContentServerPerfDb getPerfDb() {
		return perfDb;
	}

	public void setPerfDb(ContentServerPerfDb perfDb) {
		this.perfDb = perfDb;
	}

	public boolean resolveQueryTask(DnsQueryTask queryTask) {		
		return false;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}
	
	public boolean hasAnswer(String question) {
		
		return false;
	}
	
	private void deleteCacheAuto() {
		
	}
	
}
