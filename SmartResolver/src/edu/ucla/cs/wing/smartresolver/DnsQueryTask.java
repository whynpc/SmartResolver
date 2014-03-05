package edu.ucla.cs.wing.smartresolver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Observer;

import org.xbill.DNS.Flags;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;

import edu.ucla.cs.wing.smartresolver.EventLog.Type;
import android.database.Observable;
import android.os.Message;
import android.preference.PreferenceActivity.Header;

public class DnsQueryTask extends java.util.Observable implements Observer, Runnable  {	
	
	private long createTime;
	
	private org.xbill.DNS.Message msg;	
	private String question;
	
	private DnsResolver resolver;
	
	private int incomingPort;
	
	private boolean replied = false;
	
	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}	
	
	public DnsQueryTask(org.xbill.DNS.Message msg, int port, DnsResolver resolver) {
		super();
		this.createTime = System.currentTimeMillis();		
		
		this.resolver = resolver;		
		incomingPort = port;
		this.msg = msg;
		question = msg.getQuestion().getName().toString();
		
		EventLog.write(Type.DEBUG, "DnsQueryTask: " + question);
	}
	
	private void replyToProxy(Record[] records) {
		try {
			org.xbill.DNS.Message response = new org.xbill.DNS.Message(msg.toWire());
			org.xbill.DNS.Header header = response.getHeader();
			header.setFlag(Flags.QR);
			header.setFlag(Flags.RA);
			header.setFlag(Flags.RD);
			response.setHeader(header);
			
			for (Record record : records) {
				response.addRecord(record, Section.ANSWER);				
			}
			
			byte[] data = response.toWire();
			DatagramPacket pkt = new DatagramPacket(data, data.length);
			pkt.setAddress(InetAddress.getLocalHost());
			pkt.setPort(incomingPort);
			if (resolver.reply(pkt)) {
				replied = true;
			}			
		} catch (IOException e) {
		}
	}

	// TODO: try answer with cache
	public void answerWithCache() {
		handleError();
	}

	// receive the response for another pending query for the same name
	@Override
	public void update(java.util.Observable observable, Object data) {
		Record[] records = (Record[]) data;
		replyToProxy(records);
		handleError();
	}
	
	
	// send query to resolve
	private void sendQuery() {
		try {
			Lookup lookup = new Lookup(question);
			lookup.setResolver(resolver.getResolver());			
			Record[] records = lookup.run();			
			replyToProxy(records);
			setChanged();
			notifyObservers(records);			
		} catch (TextParseException e) {		
		}
		handleError();
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
