package edu.ucla.cs.wing.smartresolver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.xbill.DNS.Cache;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TSIG;

import edu.ucla.cs.wing.smartresolver.EventLog.Type;
import edu.ucla.cs.wing.smartresolver.R.string;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;

public class DnsResolver {

	public static final int TIMEOUT = 500;
	public static final int BUF_SIZE = 1024;
	public static final String CMD_LAUNCH_PROXY = "su -c ./data/local/dnsproxy";
	public static final String CMD_STOP_PROXY = "su -c killall dnsproxy";

	private DatagramSocket internalSocket;

	private Resolver resolver;

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

	public DnsResolver(Context context, SharedPreferences prefs) {
		this.context = context;
		this.prefs = prefs;

		contentServerPerfDb = new ContentServerPerfDb();
		caches = new HashMap<String, DnsCache>();
		pendingQueries = new HashMap<String, DnsQueryTask>();
		defaultCache = createCache(null, contentServerPerfDb);
		currentCache = defaultCache;
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
		// DnsCache dnsCache = new DnsCache(this, serverPerfDb)
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
			EventLog.write(Type.ERROR, "Fail to open internal socket");
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
		String[] servers = new String[2];
		for (int i = 0; i < 2; i++) {
			// TODO: add support to wifi dns servers
			servers[i] = MobileInfo.getInstance().getCellularDnsServer(i + 1);
		}
		try {
			resolver = new ExtendedResolver(servers);
		} catch (UnknownHostException e) {
			resolver = null;
			EventLog.write(Type.ERROR, "Failt to init resovler");
		}
	}

	public DnsCache getCurrentDnsCache() {
		return currentCache;
	}

	public void cleanUp() {
		if (internalSocket != null) {
			internalSocket.close();
		}
	}

	private void handleResolveReq(DatagramPacket pkt) {
		try {
			EventLog.write(Type.DEBUG, "Incoming query from proxy at port = "
					+ pkt.getPort());

			byte[] data = Arrays.copyOf(pkt.getData(), pkt.getLength());
			org.xbill.DNS.Message msg = new org.xbill.DNS.Message(data);
			DnsQueryTask queryTask = new DnsQueryTask(msg, pkt.getPort(), this);

			if (getCurrentDnsCache().resolveQueryTask(queryTask)) {
				EventLog.write(Type.DEBUG, "Query handled by cache: "
						+ queryTask.getQuestion());
			} else {
				synchronized (pendingQueries) {
					if (pendingQueries.containsKey(queryTask.getQuestion())) {
						EventLog.write(Type.DEBUG,
								"Query duplicate to pending queries: "
										+ queryTask.getQuestion());
						DnsQueryTask existingTask = pendingQueries
								.get(queryTask.getQuestion());
						existingTask.addObserver(queryTask);
					} else {
						EventLog.write(Type.DEBUG, "Query to server: "
								+ queryTask.getQuestion());
						pendingQueries.put(queryTask.getQuestion(), queryTask);
						executor.execute(queryTask);
					}
				}
			}
		} catch (IOException e) {
		}
	}

	public void start() {
		if (!running) { // avoid duplicate start
			EventLog.newLogFile(EventLog.genLogFileName(new String[] { String
					.valueOf(System.currentTimeMillis()) }));

			running = true;

			try {
				Runtime.getRuntime().exec(CMD_LAUNCH_PROXY);
			} catch (IOException e2) {
				EventLog.write(Type.ERROR, "Fail to launch proxy");
			}

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
							EventLog.write(Type.ERROR,
									"IO error when recving pkt from proxy");
						}
					}
				}
			}.start();

			// change DNS server
			try {
				// only change primary server; keep secondary server as backup
				String cmd = "su -c setprop net.dns1 127.0.0.1";
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				EventLog.write(Type.ERROR, "Fail to change dns server setting");
			}
		}
	}

	public boolean reply(DatagramPacket pkt) {
		if (internalSocket != null) {
			try {
				EventLog.write(Type.DEBUG,
						"Reply to proxy at port = " + pkt.getPort());
				internalSocket.send(pkt);
			} catch (IOException e) {
				EventLog.write(Type.ERROR, "IOException when replying to proxy");
				return false;
			}
		}
		return true;
	}

	public Resolver getResolver() {
		return resolver;
	}

	public boolean isDisableLegacyCache() {
		return disableLegacyCache;
	}

	public void stop() {
		EventLog.close();

		running = false;
		try {
			Runtime.getRuntime().exec(CMD_STOP_PROXY);
		} catch (IOException e1) {
			EventLog.write(Type.ERROR, "Fail to stop proxy");
		}

		// restore DNS server
		// TODO: add wifi support
		String cmd = "su -c setprop net.dns1 "
				+ MobileInfo.getInstance().getCellularDnsServer(1);
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			EventLog.write(Type.ERROR, "Fail to restore dns server setting");
		}
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
				if (pendingQueries.containsKey(task.getQuestion())
						&& pendingQueries.get(task.getQuestion()).equals(task)) {
					pendingQueries.remove(task.getQuestion());
				}
			}
		}
	}

}
