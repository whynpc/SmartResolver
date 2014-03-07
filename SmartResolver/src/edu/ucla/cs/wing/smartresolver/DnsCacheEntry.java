package edu.ucla.cs.wing.smartresolver;

import java.util.LinkedList;
import java.util.List;

public class DnsCacheEntry { 
	
	private String name;	
	private long lastUsedTime;	
	private int usedCnt;
	
	private List<DnsAddrRecord> addrRecords;
	
	
	public DnsCacheEntry() {
		addrRecords = new LinkedList<DnsAddrRecord>();
		
	}
	
	
	
	

}
