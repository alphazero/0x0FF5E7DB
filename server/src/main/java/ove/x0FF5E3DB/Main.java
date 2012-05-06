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

package ove.x0FF5E3DB;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import ove.x0FF5E3DB.Server.Configuration;
import ove.x0FF5E3DB.util.Assert;
import ove.x0FF5E3DB.util.Log;


/**
 * @author alphazero
 */
public class Main {
	public static final Log.Logger log = Specification.logger;
	
	@SuppressWarnings("serial")
	static final class UsageException extends Exception {
	}
	
	public static final String DEFAULT_CONF_PATH = "./server.conf";
	private final static void exit(int status, String info) {
		log.info("%s - exit(%d)", info, status);
		System.exit(status);
	}
	public static final void main(String[] args) {

		// parse args
		Map<String, String> clargs = null;
		try {
			clargs = parseArgs(args);
		} catch (UsageException e) {
			usage();
			exit(-1, "startup failed - parse args");
		}
		
		
		// configuration 
		Server.Configuration conf = null;
		try {
			String cfname = null;
			if((cfname = clargs.get("-conf")) == null) { cfname = DEFAULT_CONF_PATH; }
			conf = Server.Configuration.Load(cfname);
		} catch (Exception e) {
			e.printStackTrace();
			exit(-2, "startup failed - configuration file load");
		}

		temptest(conf);
		// executive context
		final Server.Fault[] fault = new Server.Fault [1];
		Server.Context context = newServerContext(Thread.currentThread(), conf, fault);

		// instantiate and configure the server
		Server server = null;
		try {
			server = new Server(context);
		} catch (Exception e) {
			e.printStackTrace();
			exit(-3, "startup failed - server config");
		}
		
		// start it up
		Thread srvthread = new Thread(server, "0ff5e3db-server");
		srvthread.start();

		// hang out and wait until server is shutdown
		for (;;){
			try {
				if(!Thread.interrupted()) {
					srvthread.join();
				}
				else {
					// interrupted!
				}
			} catch (InterruptedException e) {	// via context onError()
				log.trace(Level.FINE, "interrupted - e:%s", e);
				e.printStackTrace();
				if(fault[0] != null) {
					log.trace(Level.FINEST, "server faulted -- try handling it ...");
					if(!handleFault()){
						log.error("unrecoverable server fault -- will exit.", fault[0]);
						exit(-5, fault[0].info);
					}
					log.trace(Level.FINEST, ".. handled -- will continue");
				} else {
					log.trace(Level.FINEST, "spurious interrupt -- will continue");
				}
			}
		}
		
	}
	private static final boolean handleFault() {
		return false;
	}
	private static final Server.Context newServerContext(final Thread owner, final Server.Configuration config, final Server.Fault[] fault) {
		return new Server.Context() {
			final Map<String, Object> map = new HashMap<String, Object>();
			@Override final 
			public void onError(Server.Fault f) {
				log.error("onError: %s", f.toString());
				fault[0] = f;
				owner.interrupt();
			}
			@Override final 
			public String getProperty(Server.Property prop) {
				return config.get(prop);
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
				return (V) map.get(k);
			}
		};
	}
	
	/**
	 * @param conf
	 */
	private static void temptest(Configuration conf) {
		for (Server.Property p : Server.Property.values()){
			log.trace(Level.FINEST, conf.get(p) + " check");
		}
	}
	private static void usage() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}
	
	/**
	 * @param args
	 * @return
	 * @throws UsageException 
	 */
	private static Map<String, String> parseArgs(String[] args) throws UsageException {
		Map<String, String> clargs = new HashMap<String, String>();
		for (int i=0; i< args.length; i++){
			String arg = args[i];
			if(arg.startsWith("--")){
				// directive
			} else if (arg.startsWith("-")) {
				String argv = args[i+1];
				parseArg(clargs, arg, argv);
				i++;
			} else {
				log.error(arg);
				throw new UsageException();
			}
		}
		return clargs;
	}
	
	private static void parseArg(Map<String, String> clargs, String arg, String argv) throws IllegalStateException {
		Class<? extends RuntimeException> eclass = IllegalStateException.class;
		Assert.notNull(clargs, "clargs", eclass);
		Assert.isTrue(arg.charAt(0)=='-', "option flag must begin with '-'", eclass);
		Assert.isTrue(arg.length() > 1, "option flag must be at least 2 chars", eclass);
		Assert.notNull(argv, "option value", eclass);
		Assert.isTrue(!argv.isEmpty(), "option value must not be empty", eclass);
		clargs.put(arg.substring(1), argv);
	}
}
