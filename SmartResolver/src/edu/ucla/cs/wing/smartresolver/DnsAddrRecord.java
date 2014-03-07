package edu.ucla.cs.wing.smartresolver;

public class DnsAddrRecord {
	
	private String name;
	private String ip;	
	private long ttl;	
	private long createTime;
	private long extendTtl;
	private boolean flushed;
																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																						
	public DnsAddrRecord() {
		extendTtl = 3600000;
	}
	
	public void refresh() {
		
	}
	
	boolean isExpired() {		
		return (System.currentTimeMillis() > createTime + extendTtl);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public long getTtl() {
		return ttl;
	}

	public void setTtl(long ttl) {
		this.ttl = ttl;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getExtendTtl() {
		return extendTtl;
	}

	public void setExtendTtl(long extendTtl) {
		this.extendTtl = extendTtl;
	}

	public boolean isFlushed() {
		return flushed;
	}

	public void setFlushed(boolean flushed) {
		this.flushed = flushed;
	}
	
	

}
