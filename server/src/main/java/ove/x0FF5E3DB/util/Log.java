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
 * REVU: Intent here is to have clean log out and zero dependencies.  Per
 * review, only value added (outside of formatter/handler) is exception
 * related.  
 * TODO: mirror standard API method names to allow for future revert to stdlib. 
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
		// REVU: this is buggy 
		// TODO: should only happen once per named logger
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

	/**
	 * @param log
	 * @param levelstr
	 */
	public static void setLogLevel(Logger log, final String levelname) throws IllegalArgumentException{
		if(levelname == null) throw new IllegalArgumentException("levelname is null");
		if(levelname.isEmpty()) throw new IllegalArgumentException("levelname is blank");
		Level level = null;
		if(levelname.equalsIgnoreCase(Level.ALL.getName()))
			level = Level.ALL;
		else if(levelname.equalsIgnoreCase(Level.CONFIG.getName()))
			level = Level.CONFIG;
		else if(levelname.equalsIgnoreCase(Level.INFO.getName()))
			level = Level.INFO;
		else if(levelname.equalsIgnoreCase(Level.OFF.getName()))
			level = Level.OFF;
		else if(levelname.equalsIgnoreCase(Level.SEVERE.getName()))
			level = Level.SEVERE;
		else if(levelname.equalsIgnoreCase(Level.WARNING.getName()))
			level = Level.WARNING;
		else if(levelname.equalsIgnoreCase(Level.FINE.getName()))
			level = Level.FINE;
		else if(levelname.equalsIgnoreCase(Level.FINER.getName()))
			level = Level.FINER;
		else if(levelname.equalsIgnoreCase(Level.FINEST.getName()))
			level = Level.FINEST;
		else
			throw new IllegalArgumentException("Log#setLogLevel: unrecognized log level: " + levelname);
		
		log.setLevel(level);
	}
}
