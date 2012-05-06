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

package ove.x0FF5E3DB.util;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A perfect excuse to not work on the real code .. but it works ;)
 */
public class Log {
	
	// ------------------------------------------------------------------------
	// Log access
	// ------------------------------------------------------------------------
	/** dumps {@link Throwable} stack (if applicable) */
	public static Logger getLogger(final String name) {
		return getLogger(name, true);
	}
	/** @param dumpstack if <code>true</code>, dumps {@link Throwable} stack (if applicable) */
	public static Logger getLogger(final String name, final boolean dumpstack) {
		// note: can't use assert - circular dep.
		if(name == null) throw new IllegalArgumentException("name is null");
		if(name.isEmpty()) throw new IllegalArgumentException("name is blank");

		String lname = null;
		lname = String.format("%8s", name).substring(0, 8);
		java.util.logging.Logger stdlog = java.util.logging.Logger.getLogger(lname);
		stdlog.setUseParentHandlers(false);
		final Handler handler = new Log.Handler();
		final Formatter formatter = new Log.Formatter();
		handler.trySetFormatter(formatter);
		stdlog.addHandler(handler);
		return new Logger(stdlog, dumpstack);
	}
	// ------------------------------------------------------------------------
	// Log.Logger
	// ------------------------------------------------------------------------
	public static class Logger {
		private final java.util.logging.Logger stdlog;
		private final boolean dumpstack;
		Logger(java.util.logging.Logger stdlog, boolean dumpstack){
			this.stdlog = stdlog;
			this.dumpstack = dumpstack;
		}
		final public void error (String msg){
			stdlog.severe(msg);
		}
		final public void error (String fmt, Object...args){
			stdlog.severe(String.format(fmt, args));
		}
		final public void error (String msg, Throwable t){
			final String ttype = t.getClass().getSimpleName();
			String tmsg =  t.getMessage();
			if(t.getMessage() == null || tmsg.isEmpty()) 
				tmsg = String.format("- cause: %s", ttype);
			else
				tmsg = String.format("- cause: %s - %s", ttype, tmsg);
			
			stdlog.severe(String.format("%s %s", msg, tmsg));

			if (dumpstack) t.printStackTrace();
		}
		
		final public void warning (String msg){
			stdlog.warning(msg);
		}
		final public void warning (String fmt, Object...args){
			stdlog.warning(String.format(fmt, args));
		}
		final public void warning (String msg, Throwable t){
			final String ttype = t.getClass().getSimpleName();
			String tmsg =  t.getMessage();
			if(t.getMessage() == null || tmsg.isEmpty()) 
				tmsg = String.format("- cause: %s", ttype);
			else
				tmsg = String.format("- cause: %s - %s", ttype, tmsg);
			
			stdlog.warning(String.format("%s %s", msg, tmsg));

			if (dumpstack) t.printStackTrace();
		}
		
		final public void info (String msg){
			stdlog.info(msg);
		}
		final public void info (String fmt, Object...args){
			stdlog.info(String.format(fmt, args));
		}
		final public void info (String msg, Throwable t){
			final String ttype = t.getClass().getSimpleName();
			String tmsg =  t.getMessage();
			if(t.getMessage() == null || tmsg.isEmpty()) 
				tmsg = String.format("- cause: %s", ttype);
			else
				tmsg = String.format("- cause: %s - %s", ttype, tmsg);
			
			stdlog.info(String.format("%s %s", msg, tmsg));
			if (dumpstack) t.printStackTrace();
		}
		
		final public Logger setLevel(Level level) { 
			stdlog.setLevel(level);
			return this;
		}
		final public void trace(Level level, String msg){
			stdlog.log(level, msg);
		}
		final public void trace(Level level, String fmt, Object...args){
			stdlog.log(level, fmt, args);
		}
		final public void trace(Level level, String msg, Throwable t){
			stdlog.log(level, msg, t);
		}
	}
//	public enum Category { INFO, DEBUG, ERROR, PROBLEM, BUG }
	
	// ------------------------------------------------------------------------
	// Log.Handler
	// ------------------------------------------------------------------------
	public static class Handler extends java.util.logging.Handler {
		/**
		 * Try and set the formatter -- may not be possible if
		 * run in containers, etc. due to security checks.
		 * @param fmt 
		 */
		private java.util.logging.Formatter formatter;
		final void trySetFormatter(Formatter fmt){
			try {
				super.setFormatter(fmt);
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				formatter = getFormatter();
			}
		}
		@Override final
		public void publish(LogRecord record) {
			System.err.print(formatter.format(record));
			flush();
		}
		@Override final
		public void flush() {
			System.err.flush();
		}
		@Override final
		public void close() throws SecurityException {
			flush();
		}
	}
	
	// ------------------------------------------------------------------------
	// Log.Formatter
	// ------------------------------------------------------------------------
	/**  simple formatter for a clean single line log out. */
	public static class Formatter extends java.util.logging.Formatter {
		final String LINESEP = System.getProperty("line.separator");
		@SuppressWarnings("boxing")
		@Override final
		public String format(LogRecord record) {
			// TODO: clean up the mess above and fix this.
			final Level level = record.getLevel();
			final String logger = record.getLoggerName();
			final String msg = record.getMessage();
			final Object[] msgparams = record.getParameters();
			final int tid = record.getThreadID();
			final long time = record.getMillis();
			
			String _msg = null;
			if(msgparams != null && msgparams.length > 0){
				_msg = String.format(msg, msgparams);
			}
			else {
				_msg = msg;
			}
			
			final Date d = new Date(time);
			return String.format("%014d %s [%s][tid:%d] %-7s - %s%s", time, d, logger, tid, level.getLocalizedName(), _msg, LINESEP);
		}
	}
	
	// ------------------------------------------------------------------------
	/* -- ADHOC TEST - REMOVE AT WILL */
	// ------------------------------------------------------------------------
	public static void main(String[] args) {
		final Logger logger = Log.getLogger("testlog");
		logger.info("This is getting better");
		logger.error("oops");
		try {
			Log.getLogger(null);
		} catch (Exception e) {
			logger.warning("oops", e);
		}
		try {
			Log.getLogger("");
		} catch (Exception e) { 
			logger.error("oops", e);
		}
		logger.warning("%s %s %s %s %s", "you", "are", "taking", "too", "loong");
		
		Throwable tnomsg = new Exception();
		logger.warning("oops", tnomsg);
		final Logger logger2 = Log.getLogger("testlog2");

		logger2.error("oops", tnomsg);
		
		logger2.setLevel(Level.ALL);
		logger2.trace(Level.FINEST, "how fine it is");
		
	}
	/* -- ADHOC TEST - REMOVE AT WILL */
}
