package edu.ucla.cs.wing.smartresolver;

import java.util.HashMap;
import java.util.List;

import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;

public class DnsCache {
		
	private DnsResolver resolver;
	
	private ContentServerPerfDb perfDb;
	
	private String networkId;
	
	private HashMap<String, DnsCacheEntry> entries;
	
	public DnsCache(DnsResolver resolver) {
		this.resolver = resolver;
		entries = new HashMap<String, DnsCacheEntry>();					
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
	
	public boolean hasAnswer(String question, List<Record> answers) {
		if (entries.containsKey(question)) {
			entries.get(question).fullfilAnswer(answers);
			return true;
		} else {
			return false;
		}
	}
	
	public void addRecords(String name, Record[] records) {
		if (entries.containsKey(name)) {
			entries.get(name).addRecords(records);;			
		} else {
			DnsCacheEntry entry = new DnsCacheEntry(name);
			entry.addRecords(records);
			if (entry.size() > 0) {
				entries.put(name, entry);
			}
		}
	}
	
	public void addRecord(String name, Record record) {
		if (entries.containsKey(name)) {
			entries.get(name).addRecord(record);			
		} else {
			DnsCacheEntry entry = new DnsCacheEntry(name);
			entry.addRecord(record);
			if (entry.size() > 0) {
				entries.put(name, entry);
			}
		}
	}
	
	public void clear() {
		entries.clear();
	}
	
}
