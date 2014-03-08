package edu.ucla.cs.wing.smartresolver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.xbill.DNS.Cache;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TSIG;

import edu.ucla.cs.wing.smartresolver.EventLog.LogType;
import edu.ucla.cs.wing.smartresolver.R.string;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;

public class DnsResolver {

	public static final int TIMEOUT = 5000;
	public static final int BUF_SIZE = 1024;

	private DatagramSocket internalSocket;

	private ExtendedResolver stubResovler;
	private String[] currentServers;

	private boolean running;
	private boolean disableLegacyCache;

	private DnsCache defaultCache;
	private HashMap<String, DnsCache> caches;
	private DnsCache currentCache = null;

	private ContentServerPerfDb contentServerPerfDb;

	private Context context;
	private SharedPreferences prefs;

	private QueryExecutor executor;
	private BlockingQueue<Runnable> pendingQueryTasks;
	private HashMap<String, DnsQueryTask> pendingQueries;
	
	private Timer timer;

	public DnsResolver(Context context, SharedPreferences prefs) {
		this.context = context;
		this.prefs = prefs;

		contentServerPerfDb = new ContentServerPerfDb();
		caches = new HashMap<String, DnsCache>();
		pendingQueries = new HashMap<String, DnsQueryTask>();
		defaultCache = createCache(null, contentServerPerfDb);
		currentCache = defaultCache;
		timer = new Timer();
	}

	public void init() {
		// TODO: init the content of contentServerPerfDb
		// TODO: init cache based on current network

		refresh();
	}

	private DnsCache createCache(String networkId,
			ContentServerPerfDb contentServerPerfDb) {
		DnsCache dnsCache = new DnsCache(this);
		dnsCache.setNetworkId(networkId);
		dnsCache.setPerfDb(contentServerPerfDb);
		return dnsCache;
	}

	private DnsCache createDnsCache(String network) {
		DnsCache dnsCache = new DnsCache(this);
		return null;
	}

	public void refresh() {
		// refresh parameter
		disableLegacyCache = prefs.getBoolean("disable_legacy_cache", true);

		// refresh socket to recv query from proxy
		if (internalSocket != null) {
			internalSocket.close();
			internalSocket = null;
		}

		short port = Short.parseShort(prefs.getString("interval_port",
				context.getString(R.string.pref_default_internal_port)));
		try {
			internalSocket = new DatagramSocket(port);
			internalSocket.setSoTimeout(TIMEOUT);
		} catch (SocketException e) {
			EventLog.write(LogType.ERROR, "Fail to open internal socket");
		}

		// refresh thread pool to handle queries
		pendingQueryTasks = new LinkedBlockingQueue<Runnable>();
		int corePoolSize, maxPoolSize;
		corePoolSize = Integer.parseInt(prefs.getString("core_pool_size",
				context.getString(R.string.pref_default_core_pool_size)));
		maxPoolSize = Integer.parseInt(prefs.getString("max_pool_size",
				context.getString(R.string.pref_default_max_pool_size)));
		executor = new QueryExecutor(corePoolSize, maxPoolSize, 1,
				TimeUnit.SECONDS, pendingQueryTasks);

		// TODO: refresh DNS server
		refreshStubResolver();
	}

	private void refreshStubResolver() {
		String[] servers = new String[2];
		for (int i = 0; i < 2; i++) {
			servers[i] = MobileInfo.getInstance().isConnectingWifi() ? MobileInfo
					.getInstance().getWifiDnsServer(i + 1) : MobileInfo
					.getInstance().getCellularDnsServer(i + 1);
			EventLog.write(LogType.DEBUG, servers[i]);

		}
		// check whether server[0] available
		if (servers[0] == null) {
			return;
		}
		// check whether servers are the same
		if (currentServers != null && servers[0].equals(currentServers[0])
				&& servers[1].equals(currentServers[1])) {
			return;
		}
		EventLog.write(LogType.DEBUG, "Reset resolver to: " + servers[0] + ";"
				+ servers[1]);

		try {
			stubResovler = new ExtendedResolver(servers);
		} catch (UnknownHostException e) {
			stubResovler = null;
			EventLog.write(LogType.ERROR, "Failt to init resovler");
		}
	}

	public DnsCache getCurrentDnsCache() {
		return currentCache;
	}

	public void cleanUp() {
		if (internalSocket != null) {
			internalSocket.close();
		}
		DnsProxy.restoreDnsServerSetting();
		DnsProxy.stopDnsProxy();
	}

	private void handleResolveReq(DatagramPacket pkt) {
		try {
			EventLog.write(LogType.DEBUG,
					"Incoming query from proxy at port = " + pkt.getPort());

			byte[] data = Arrays.copyOf(pkt.getData(), pkt.getLength());
			org.xbill.DNS.Message msg = new org.xbill.DNS.Message(data);
			DnsQueryTask queryTask = new DnsQueryTask(msg, pkt.getPort(), this);

			List<Record> records = new ArrayList<Record>();
			if (getCurrentDnsCache()
					.hasAnswer(queryTask.getName(), records)) {
				EventLog.write(LogType.DEBUG, "Query handled by cache: "
						+ queryTask.getName());
				reply(msg, records, pkt.getPort());
			} else {
				synchronized (pendingQueries) {
					// if (pendingQueries.containsKey(queryTask.getQuestion()))
					// {
					if (false) {
						EventLog.write(LogType.DEBUG,
								"Query duplicate to pending queries: "
										+ queryTask.getName());
						DnsQueryTask existingTask = pendingQueries
								.get(queryTask.getName());
						existingTask.addObserver(queryTask);
					} else {
						EventLog.write(LogType.DEBUG, "Query to server: "
								+ queryTask.getName());
						pendingQueries.put(queryTask.getName(), queryTask);
						executor.execute(queryTask);
					}
				}
			}
		} catch (IOException e) {
		}
	}

	public void start() {
		if (!running) { // avoid duplicate start
			running = true;

			DnsProxy.launchDnsProxy();
			DnsProxy.changeDnsServerSetting();

			new Thread() {
				@Override
				public void run() {
					while (running) {
						DatagramPacket pkt = new DatagramPacket(
								new byte[BUF_SIZE], BUF_SIZE);
						try {
							internalSocket.receive(pkt);
							handleResolveReq(pkt);
						} catch (SocketTimeoutException e1) {
						} catch (IOException e) {
							EventLog.write(LogType.ERROR,
									"IO error when recving pkt from proxy");
						}
					}
				}
			}.start();

		}
	}

	public boolean reply(org.xbill.DNS.Message request, List<Record> answers,
			int incomingPort) {

		org.xbill.DNS.Message response = null;
		try {
			response = new org.xbill.DNS.Message(request.toWire());
		} catch (IOException e1) {
			return false;
		}
		org.xbill.DNS.Header header = response.getHeader();
		header.setFlag(Flags.QR);
		header.setFlag(Flags.RA);
		header.setFlag(Flags.RD);
		response.setHeader(header);

		if (answers != null) {
			for (Record record : answers) {
				if (isDisableLegacyCache()) {
					// TODO: set TTL of record to 0
				}
				response.addRecord(record, Section.ANSWER);
			}

			byte[] data = response.toWire();
			DatagramPacket pkt = new DatagramPacket(data, data.length);
			try {
				pkt.setAddress(InetAddress.getLocalHost());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pkt.setPort(incomingPort);
			return reply(pkt);
		} else {
			return false;
		}
	}

	public boolean reply(DatagramPacket pkt) {
		if (internalSocket != null) {
			try {
				EventLog.write(LogType.DEBUG,
						"Reply to proxy at port = " + pkt.getPort());
				internalSocket.send(pkt);
			} catch (IOException e) {
				EventLog.write(LogType.ERROR,
						"IOException when replying to proxy");
				return false;
			}
		}
		return true;
	}

	public Resolver getResolver() {
		return stubResovler;
	}

	public boolean isDisableLegacyCache() {
		return disableLegacyCache;
	}

	public void stop() {
		running = false;

		DnsProxy.restoreDnsServerSetting();
		DnsProxy.stopDnsProxy();
	}

	public class QueryExecutor extends ThreadPoolExecutor {
		public QueryExecutor(int corePoolSize, int maximumPoolSize,
				long keepAliveTime, TimeUnit unit,
				BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			DnsQueryTask task = (DnsQueryTask) r;
			synchronized (pendingQueries) {
				// XXX: whether also do pendingQueries.remove() with Observer?
				if (pendingQueries.containsKey(task.getName())
						&& pendingQueries.get(task.getName()).equals(task)) {
					pendingQueries.remove(task.getName());
				}
			}
		}
	}

	public void clearCache() {
		getCurrentDnsCache().clear();
		try {
			Runtime.getRuntime().exec("su -c nds resolver flushdefaultif");
		} catch (IOException e) {

		}
	}

	public void onNetworkChange() {
		if (running) {
			timer.schedule(new TimerTask() {				
				@Override
				public void run() {
					DnsProxy.changeDnsServerSetting();
					refreshStubResolver();
				}
			}, 1000);
			
		}
	}

}
