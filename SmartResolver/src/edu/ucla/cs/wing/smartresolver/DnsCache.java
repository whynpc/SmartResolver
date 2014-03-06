package edu.ucla.cs.wing.smartresolver;

import java.util.HashMap;

import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;

public class DnsCache {
	
	public static class DnsRecord {		
	}
	
	private DnsResolver resolver;
	
	private ContentServerPerfDb perfDb;
	
	private String networkId;	
	
	private HashMap<String, DnsRecord> cache;
	
	public DnsCache(DnsResolver resolver) {
		this.resolver = resolver;
		cache = new HashMap<String, DnsCache.DnsRecord>();			
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
		if (cache.containsKey(question)) {
			return true;
		}
		return false;
	}
	
	private void deleteCacheAuto() {
		
	}
	
}
