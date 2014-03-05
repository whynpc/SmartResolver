package edu.ucla.cs.wing.smartresolver;

import java.util.HashMap;

import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;

public class DnsCache {
	
	public static class DnsRecord {
		
	}
	
	private DnsResolver resolver;
	
	private String networkId;	
	
	private HashMap<String, DnsRecord> cache;
	
	public DnsCache(DnsResolver resolver) {
		this.resolver = resolver;
		cache = new HashMap<String, DnsCache.DnsRecord>();			
	}
	
	
	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}
	
	public boolean hasAnswer(String question) {
		return false;
	}

	
	
	
}
