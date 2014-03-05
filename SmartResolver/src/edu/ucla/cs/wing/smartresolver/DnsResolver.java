package edu.ucla.cs.wing.smartresolver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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

	public static int TIMEOUT = 500;
	public static int BUF_SIZE = 1024;

	private DatagramSocket internalSocket;

	private Resolver resolver;

	private boolean running;

	private DnsCache defaultCache;

	private DnsCache currentCache = null;

	private HashMap<String, DnsCache> caches;

	private Context context;
	private SharedPreferences prefs;

	private QueryExecutor executor;

	private BlockingQueue<Runnable> pendingQueryTasks;

	private HashMap<String, DnsQueryTask> pendingQueries;

	public DnsResolver(Context context, SharedPreferences prefs) {
		this.context = context;
		this.prefs = prefs;
		
		
		defaultCache = new DnsCache(this);
		caches = new HashMap<String, DnsCache>();
		pendingQueries = new HashMap<String, DnsQueryTask>();

	}

	public void refresh() {
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

		// TODO: refresh resolver
		String[] servers = new String[2];
		for (int i = 0; i < 2; i++) {
			// TODO: add support to wifi dns servers
			servers[i] = MobileInfo.getInstance().getCellularDnsServer(i + 1);
		}
		try {
			resolver = new ExtendedResolver(servers);
		} catch (UnknownHostException e) {
			resolver = null;
		}

	}

	public DnsCache getCurrentDnsCache() {
		return currentCache;
	}

	public void init() {
		refresh();
	}

	public void cleanUp() {
		if (internalSocket != null) {
			internalSocket.close();
		}
	}

	private void handleResolveReq(DatagramPacket pkt) {
		byte[] data = Arrays.copyOf(pkt.getData(), pkt.getLength());
		try {
			org.xbill.DNS.Message msg = new org.xbill.DNS.Message(data);
			DnsQueryTask queryTask = new DnsQueryTask(msg, this);

			synchronized (pendingQueries) {
				if (pendingQueries.containsKey(queryTask.getQuestion())) {
					DnsQueryTask existingTask = pendingQueries.get(queryTask
							.getQuestion());
					existingTask.addObserver(queryTask);
				} else {
					pendingQueries.put(queryTask.getQuestion(), queryTask);
					executor.execute(queryTask);
				}
			}
		} catch (IOException e) {
		}
	}

	public void start() {
		if (!running) {
			EventLog.newLogFile(EventLog.genLogFileName(new String[] { String
					.valueOf(System.currentTimeMillis()) }));

			running = true;

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
				// we only change primary server; leave secondary server as
				// backup
				String cmd = "su -c setprop net.dns1 127.0.0.1";
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				EventLog.write(Type.ERROR, "Fail to change dns server setting");
			}
		}
	}

	public Resolver getResolver() {
		return resolver;
	}

	public void stop() {
		if (running) {
			EventLog.close();
			
			running = false;

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
				if (pendingQueries.containsKey(task.getQuestion())
						&& pendingQueries.get(task.getQuestion()).equals(task)) {
					pendingQueries.remove(task.getQuestion());
				}
			}
		}
	}

}
