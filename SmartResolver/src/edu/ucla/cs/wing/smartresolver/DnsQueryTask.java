package edu.ucla.cs.wing.smartresolver;

import java.io.IOException;
import java.util.Observer;

import org.xbill.DNS.Flags;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import android.database.Observable;
import android.os.Message;
import android.preference.PreferenceActivity.Header;

public class DnsQueryTask extends java.util.Observable implements Observer, Runnable  {	
	
	private long createTime;
	
	private org.xbill.DNS.Message msg;	
	private String question;
	
	private DnsResolver resolver;
	
	private boolean replied = false;
	
	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}	
	
	public DnsQueryTask(org.xbill.DNS.Message msg, DnsResolver resolver) {
		super();
		this.createTime = System.currentTimeMillis();		
		this.msg = msg;
		this.resolver = resolver;		
	}
	
	private void replyToProxy(Record[] records) {
		try {
			org.xbill.DNS.Message response = new org.xbill.DNS.Message(msg.toWire());
			org.xbill.DNS.Header header = response.getHeader();
			header.setFlag(Flags.QR);
			header.setFlag(Flags.RA);
			header.setFlag(Flags.RD);
			response.setHeader(header);
			
			
			
			replied = true;			
		} catch (IOException e) {
		}		
		
	}
	
	public void answerWithCache() {
		// TODO: try answer with cache
		handleError();
	}

	@Override
	public void update(java.util.Observable observable, Object data) {
		// receive the response for another pending query for the same name
		Record[] records = (Record[]) data;
		replyToProxy(records);
		handleError();
	}

	@Override
	public void run() {
		// send query to get answers
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
	
	private void handleError() {
		if (!replied) {
			// TODO: send error code to resolver
			
		}		
	}
}
