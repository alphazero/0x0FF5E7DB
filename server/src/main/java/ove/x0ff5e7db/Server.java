/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */
/*                          ~!!! Al-Aziz Al-Hakeem !!!~                        */
/*                          ~!!!  Ahura    Mazda   !!!~                        */
/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! */

/*
 *   Copyright 2012 Joubin Houshyar.  All rights are reserved.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ove.x0ff5e7db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ove.x0ff5e7db.util.Assert;
import ove.x0ff5e7db.util.Log;


/**
 * @author alphazero
 */
public class Server implements Runnable {
	
	// ------------------------------------------------------------------------
	// properties
	// ------------------------------------------------------------------------
	public static final Log.Logger log = Specification.logger;

	private final Context context;
	private NetworkInterface netcomp;
	
	// ------------------------------------------------------------------------
	// api
	// ------------------------------------------------------------------------
	public Server(Context context) throws IllegalArgumentException {
		Assert.notNull(context, "context", IllegalArgumentException.class);
		this.context = new Server.Context.Tree(context);
		
		configure();
	}
	private Server.Fault configure() {
		try {
			String levelstr = context.getProperty(Property.LOG_LEVEL);
			Log.setLoggerLevel(log, levelstr);
		} catch (Exception e) {
			String err = "configure log level error";
			log.error(err, e);
			return new Server.Fault(this, e, err);
		}
		
		return  null;
	}

	/**
	 * Server general architecture:
	 * 
	 * - networking thread:s
	 * 		- accept connections
	 * 		- process client requests => db thread
	 * 		- process server responses <= db thread
	 * 
	 * 	- fs thread (possibly):
	 * 		- deal with file IO (not entirely sure)
	 * 
	 * - db thread: 
	 * 		- process db requests
	 * 
	 * - background tasks thread:
	 * 		- possibly handle non-db related request e.g. metrics, info, etc.
	 * 		- garbage collections
	 * 
	 * 
	 */
	@Override final public void run() {
		Thread.yield();	// REVU: stinko
		
		final Thread thread = Thread.currentThread();
		final String id = String.format("{%s:%d}", thread.getName(), thread.getId());
		log.log(Level.FINEST, "server thread id is %s - started ...", id);

		// assemble system and initialize 
		try {
			Fault fault = null;
			if((fault = initializeComponents()) != null) {
				log.error("failed initialize components - %s", fault);
				context.onError(fault);
				return;
			}
			log.log(Level.FINER, "server components assembled and initialized");
		} catch (Throwable rte) {
			log.error("unexpected fault on component intialization", rte);
			context.onError(new Fault(this, rte, id));
			return;
		}
		
		// bootup -- TODO: 
		
		// get busy
		try {
			ExecutorService netex = Executors.newSingleThreadExecutor();
			netex.execute(netcomp);
			log.log(Level.FINEST, "network interface executor %s started", netex);
		} catch (Exception e) {
			log.error("fault on network interface executor intialization", e);
			context.onError(new Fault(this, e, id));
			return;
		}
		
		// REVU: likely want to block on a latch and wait for interrupts on error
	}
	// ------------------------------------------------------------------------
	// internal ops
	// ------------------------------------------------------------------------
	protected final Server.Fault initializeComponents() {

		// 1 - netcomp
		netcomp = new NetworkInterface(context);
		try {
			netcomp.initialize();
			context.bind(CtxBinding.network_interface.id(), netcomp);
			log.log(Level.FINEST, "network interface %s initialized and bound",  netcomp);
		} catch (Throwable e) {
			String err = "failed to initialize netcomp";
			log.error(err, e);
			return new Server.Fault(this, e, err);
		}

		// TODO: fscomp
		// TODO: cachecomp
		
		return null;
	}
	protected final void bootup() {
		// TODO: Server#bootup -- May 5, 2012
		throw new RuntimeException("Server#bootup is not implemented!");
	}
	
	// ========================================================================
	// INNER TYPES
	// ========================================================================

	// ------------------------------------------------------------------------
	// Context bindings
	// ------------------------------------------------------------------------
	enum CtxBinding {
		network_interface,
		network_interface_executor;
		private final String id;
		CtxBinding () {
			this.id = this.name().toLowerCase().replace('_', '.');
		}
		public String id() { return id; }
	}
	// ------------------------------------------------------------------------
	// Server.Context 
	// ------------------------------------------------------------------------

	/** context for server and its components  */
	public interface Context {
		/** @return the value of the prop {@link Property} */
		String getProperty(Server.Property prop);
		/** notify context owner of {@link Fault} */
		void onError(Fault fault);
		<V extends Object> V bind(String k, V v);
		<V extends Object> V get(String k, Class<V> vc);
		
		/** A hierarchical context */
		public static final class Tree implements Context {
			final Map<String, Object> map = new HashMap<String, Object>();
			private final Context parent;
			public Tree(Server.Context parent){
				this.parent = parent;
			}
			@Override final 
			public void onError(Server.Fault f) {
				log.error("onError: %s", f.toString());
				parent.onError(f);
			}
			@Override final 
			public String getProperty(Server.Property prop) {
				return parent.getProperty(prop);
			}
			@SuppressWarnings("unchecked")
			@Override final
			public <V> V bind(String k, V v) {
				Object prev = map.put(k, v);
				return (V) prev;
			}
			@SuppressWarnings("unchecked")
			@Override final
			public <V> V get(String k, Class<V> vc) {
				Object v = null;
				if((v = map.get(k)) == null)
					v = parent.get(k, vc);
				return (V) v;
			}
		}
	}
	
	// ------------------------------------------------------------------------
	// Server.Component
	// ------------------------------------------------------------------------
	public interface Component<T> {
		/**  @param context for the component */
		void setContext(Context context);
		/**  @return self */
		T initialize() throws Throwable;
		/** support base for specialized comps. */
		static class Base<T> implements Component<T> {
			protected Context context;
			@Override final public void setContext(Context context) {
				this.context = context;
			}
			@SuppressWarnings("unchecked")
			@Override
			public T initialize() throws Throwable { return (T) this;/* nop convenience impl */ }
		}
	}
	
	// ------------------------------------------------------------------------
	// Server.Fault
	// ------------------------------------------------------------------------
	
	/** non-throwable wrapper of internally trapped faults  */
	public static final class Fault {
		final public Throwable t;
		final public String info;
		final public Server source;
		Fault(Server source, Throwable t, String info) {
			this.t = t;
			this.info = info;
			this.source = source;
		}
		public final String toString() {
			return String.format("%s faulted - %s - cause: %s",source, info, t);
		}
	}
	
	// ------------------------------------------------------------------------
	// Server.Property
	// ------------------------------------------------------------------------
	
	/** Server configuration properties and their default values. */
	public enum Property {
		LOG_LEVEL ("FINE"),
		DB_SERVER_PORT ("7727"), 
		DB_IMAGE_ROOT ("db/image"),
		DB_CACHE_BLOCK_SIZE ("4096");
		Property (String defval) {
			this.defval = defval;
			this.k = this.name().toLowerCase().replace('_', '.');
		}
		private final String k;
		private final String defval;
		public String key() { return k;}
		public String defval() { return defval; }
	}

	// ------------------------------------------------------------------------
	// Server.Config
	// ------------------------------------------------------------------------
	public static class Configuration {
		/** */
		private final Properties userprops;
		
		public String get(Server.Property prop) throws IllegalArgumentException{ 
			Assert.notNull(prop, "key", IllegalArgumentException.class);
			return userprops.getProperty(prop.key(), prop.defval()); 
		}
		
		public static final Configuration Load(String path) throws Exception {
			Configuration conf = null;
			try {
				Assert.notNull(path, "path", IllegalArgumentException.class);
				File fconf = new File(path);
				if(!fconf.exists()) {
					String msg = String.format("configuration file <%s> does not exist", fconf.getAbsolutePath());
					log.severe(msg);
					throw new Exception(msg);
				}
				BufferedReader r = new BufferedReader(new FileReader(fconf));
				conf = new Configuration(r);
			} catch (Exception e) {
				log.error("failed to load configuration from " + path, e);
				throw e;
			}
			return conf;
		}
		private Configuration(Reader reader) throws IllegalArgumentException, Exception{
			Assert.notNull(reader, "reader", IllegalArgumentException.class);
			this.userprops = new Properties();

			try {
				userprops.load(reader);
				for (Entry<Object, Object> e : userprops.entrySet()) {
					System.out.format("%s -> %s\n", e.getKey(), e.getValue());
				}
			} catch (Exception e) {
				log.error("on properties.load()", e);
			}
		}
	}
}
