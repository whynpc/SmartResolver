package edu.ucla.cs.wing.smartresolver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Record;

public class DnsCacheEntry { 
	
	public static final int MAX_ANSWERS = 5;
	public static final int MAX_SIZE = 10;
	
	private String name;	
	private long lastUsedTime;	
	private int usedCnt;
	
	//private List<DnsAddrRecord> addrRecords;
	private Queue<Record> records;
	
	
	public DnsCacheEntry(String name) {
		//addrRecords = new LinkedList<DnsAddrRecord>();
		this.name = name;
		records = new LinkedList<Record>();
	}
	
	public int size() {
		return records.size();
	}
	
	public void addRecord(Record record) {
		if (record instanceof ARecord) {
			records.add(record);			
			if (records.size() > MAX_SIZE) {
				records.poll();
			}
		}				
	}
	
	public void addRecords(Record[] newrecords) {
		for (Record record : newrecords) {
			addRecord(record);
		}
		while (records.size() > MAX_SIZE) {
			records.poll();
		}
	}
	
	public void fullfilAnswer(List<Record> answers) {
		for (Record record : records) {
			answers.add(record);
			if (answers.size() > MAX_ANSWERS) {
				break;
			}
		}
	}

}
