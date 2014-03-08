package edu.ucla.cs.wing.smartresolver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Observer;

import org.xbill.DNS.Flags;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;

import edu.ucla.cs.wing.smartresolver.EventLog.LogType;
import android.database.Observable;
import android.os.Message;
import android.preference.PreferenceActivity.Header;

public class DnsQueryTask extends java.util.Observable implements Observer, Runnable  {	
	
	private long createTime;
	
	private org.xbill.DNS.Message msg;	
	private String name;
	private Record question;
	
	private DnsResolver resolver;
	
	
	private int incomingPort;
	
	private boolean replied = false;
	
	public String getName() {
		return name;
	}

	public void setQuestion(String question) {
		this.name = question;
	}	
	
	public DnsQueryTask(org.xbill.DNS.Message msg, int port, DnsResolver resolver) {
		super();
		this.createTime = System.currentTimeMillis();		
		
		this.resolver = resolver;		
		incomingPort = port;
		this.msg = msg;
		this.question = msg.getQuestion();
	
		name = msg.getQuestion().getName().toString();
		
		EventLog.write(LogType.DEBUG, "DnsQueryTask: " + name);
	}
	
	private void replyToProxy(Record[] records) {
		if (resolver != null && records != null) {
			resolver.reply(msg, Arrays.asList(records), incomingPort);
		}				
	}

	// receive the response for another pending query for the same name
	@Override
	public void update(java.util.Observable observable, Object data) {
		Record[] records = (Record[]) data;
		replyToProxy(records);		
	}	
	
	// send query to resolve
	private void sendQuery() {
		try {
			Lookup lookup = new Lookup(question.getName(), question.getType());
			lookup.setResolver(resolver.getResolver());
			
			Record[] records = lookup.run();
			if (records != null) {
				replyToProxy(records);
				
				for (Record record : records) {
					resolver.getCurrentDnsCache().addRecord(name, record);
				}
				setChanged();
				notifyObservers(records);
			}						
		} catch (Exception e) {
			EventLog.write(LogType.ERROR, "Error in lookup: " + e.toString());
		}		
	}

	@Override
	public void run() {
		sendQuery();		
	}
	
	private void handleError() {
		if (!replied) {
			// TODO: if replied = false, then send error code to resolver
			
		}		
	}
}
