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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import ove.x0ff5e7db.util.Assert;
import ove.x0ff5e7db.util.Log;

/**
 * @author alphazero
 */
public class Main {
	// ------------------------------------------------------------------------
	// properties
	// ------------------------------------------------------------------------
	public static final Log.Logger log = Log.getLogger("cll-main").setLevel(Level.ALL);
	public static final String DEFAULT_CONF_PATH = "./server.conf";
	
	// ------------------------------------------------------------------------
	// inner types
	// ------------------------------------------------------------------------
	@SuppressWarnings("serial")
	static final class UsageException extends Exception {}
	
	private final static void exit(int status, String info) {
		log.info("%s - exit(%d)", info, status);
		System.exit(status);
	}
	enum Option {
		conf (true, "conf", "server configuration file", "path");
		final String flag;
		final String desc;
		final boolean optional;
		final String form;
		Option(boolean optional, String flag, String desc, String form){
			this.flag = flag;
			this.desc = desc;
			this.form = form;
			this.optional = optional;
		}
	}
	// ------------------------------------------------------------------------
	// 
	// ------------------------------------------------------------------------
	public static final void main(String[] args) {

		// parse args
		Map<Option, String> clargs = null;
		try {
			clargs = parseArgs(args);
		} catch (UsageException e) {
			usage();
			exit(-1, "startup failed - parse args");
		}
		
		// configuration 
		Servant.Configuration conf = null;
		try {
			String cfname = null;
			if((cfname = clargs.get(Option.conf)) == null) { 
				cfname = DEFAULT_CONF_PATH; // REVU: should be in jar 
			}
			conf = Servant.Configuration.Load(cfname);
		} catch (Exception e) {
			e.printStackTrace();
			exit(-2, "startup failed - configuration file load");
		}

		// executive context
		final Servant.Fault[] fault = new Servant.Fault [1];
		Servant.Context context = newServerContext(Thread.currentThread(), conf, fault);

		// instantiate and configure the server
		Servant server = null;
		try {
			server = new Servant(context);
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
				log.log(Level.FINE, "interrupted - e:%s", e);
				e.printStackTrace();
				if(fault[0] != null) {
					log.log(Level.FINEST, "server faulted -- try handling it ...");
					if(!handleFault()){
						log.error("unrecoverable server fault -- will exit.", fault[0]);
						exit(-5, fault[0].info);
					}
					log.log(Level.FINEST, ".. handled -- will continue");
				} else {
					log.log(Level.FINEST, "spurious interrupt -- will continue");
				}
			}
		}
		
	}
	// ------------------------------------------------------------------------
	// inner ops
	// ------------------------------------------------------------------------
	private static final boolean handleFault() {
		return false;
	}
	private static final Servant.Context newServerContext(final Thread owner, final Servant.Configuration config, final Servant.Fault[] fault) {
		return new Servant.Context() {
			final Map<String, Object> map = new HashMap<String, Object>();
			@Override final 
			public void onError(Servant.Fault f) {
				log.error("onError: %s", f.toString());
				fault[0] = f;
				owner.interrupt();
			}
			@Override final 
			public String getProperty(Servant.Property prop) {
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

	private static void usage() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}
	
	/**
	 * @param args
	 * @return
	 * @throws UsageException 
	 */
	private static Map<Option, String> parseArgs(String[] args) throws UsageException {
		log.log(Level.FINEST, "args.length:%d", args.length);
		Map<Option, String> clargs = new HashMap<Option, String>();
		for (int i=0; i< args.length; i++){
			String arg = args[i];
			if(arg.startsWith("--")){
				// directive
			} else if (arg.startsWith("-")) {
				String argv = args[i+1];
				parseArg(clargs, arg, argv);
				i++;
			} else {
				log.severe(arg);
				throw new UsageException();
			}
		}
		return clargs;
	}
	
	private static void parseArg(Map<Option, String> clargs, String arg, String argv) throws IllegalStateException {
		Class<? extends RuntimeException> eclass = IllegalStateException.class;
		Assert.isTrue(arg.charAt(0)=='-', "option flag must begin with '-'", eclass);
		Assert.isTrue(arg.length() > 1, "option flag must be at least 2 chars", eclass);
		Assert.notNull(argv, "option value", eclass);
		Assert.isTrue(!argv.isEmpty(), "option value must not be empty", eclass);
		
		final Option opt = Option.valueOf(arg.substring(1));
		clargs.put(opt, argv);
		log.log(Level.FINEST, "arg %s => %s", arg, argv);
	}
}
