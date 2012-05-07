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

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.logging.Level;

import ove.x0ff5e7db.util.Assert;
import ove.x0ff5e7db.util.Log;


/**
 * This component is tasked with managing all network front-end responsibilities
 * of the server.
 * 
 * @author alphazero
 */
class NetworkInterface extends Servant.Component.Base implements Runnable{
	// ------------------------------------------------------------------------
	// properties
	// ------------------------------------------------------------------------
	public static final Log.Logger log = Specification.logger;
	private final Servant.Context context;
	private int ssport;
	private InetSocketAddress inetadd;

	// ------------------------------------------------------------------------
	// Context bindings
	// ------------------------------------------------------------------------
	enum CtxBinding {
		server_socket_chan,
		ssch_accept_handler,
		ssch_read_handler,
		ssch_write_handler,
		ssch_accept_selector, 
		ssch_read_selector, 
		ssch_write_selector;
		private final String id;
		CtxBinding () {
			this.id = this.name().toLowerCase().replace('_', '.');
		}
		public String id() { return id; }
	}
	// REVU: like but error prone ..
	private ServerSocketChannel sschan = null;
	private Selector asel = null;
	private Selector rsel = null;
	private Selector wsel = null;
	private SSChanAcceptHandler ahandler = null;
	private SSChanReadHandler rhandler;
	private SSChanWriteHandler whandler;

	// ------------------------------------------------------------------------
	// initialization concerns
	// ------------------------------------------------------------------------
	NetworkInterface(final Servant.Context context) {
		this.context = createComponentContext(context);
	}

	/** @return the newly created context for this component*/
	private final Servant.Context createComponentContext(final Servant.Context parent) {
		return new Servant.Context() {
			@Override final
			public String getProperty(Servant.Property prop) { return parent.getProperty(prop); }
			@Override final
			public void onError(Servant.Fault fault) { parent.onError(fault); }
			@Override final
			public <V> V bind(String k, V v) {
				Assert.notNull(k, "k", IllegalArgumentException.class);
				Assert.notNull(v, "v", IllegalArgumentException.class);
				/* -- socket channels -- */
				if(k.equals(CtxBinding.server_socket_chan.id) && ServerSocketChannel.class.isAssignableFrom(v.getClass()))
					sschan = (ServerSocketChannel) v;
				/* -- selectors -- */
				else if (k.equals(CtxBinding.ssch_accept_selector.id) && Selector.class.isAssignableFrom(v.getClass()))
					asel = (Selector) v;
				else if (k.equals(CtxBinding.ssch_read_selector.id) && Selector.class.isAssignableFrom(v.getClass()))
					rsel = (Selector) v;
				else if (k.equals(CtxBinding.ssch_write_selector.id) && Selector.class.isAssignableFrom(v.getClass()))
					wsel = (Selector) v;
				/* -- handlers -- */
				else if (k.equals(CtxBinding.ssch_accept_handler.id) && SSChanAcceptHandler.class.isAssignableFrom(v.getClass()))
					ahandler = (NetworkInterface.SSChanAcceptHandler) v;
				else if (k.equals(CtxBinding.ssch_read_handler.id) && SSChanReadHandler.class.isAssignableFrom(v.getClass()))
					rhandler = (NetworkInterface.SSChanReadHandler) v;
				else if (k.equals(CtxBinding.ssch_write_handler.id) && SSChanWriteHandler.class.isAssignableFrom(v.getClass()))
					whandler = (NetworkInterface.SSChanWriteHandler) v;
				// else
				return parent.bind(k, v);
			}

			@SuppressWarnings("unchecked")
			@Override final
			public <V> V get(String k, Class<V> vc) {
				Assert.notNull(k, "k", IllegalArgumentException.class);
				Assert.notNull(vc, "vc", IllegalArgumentException.class);
				/* -- socket channels -- */
				if(k.equals(CtxBinding.server_socket_chan.id) && vc.isAssignableFrom(ServerSocketChannel.class))
					return (V) sschan;
				/* -- selectors -- */
				else if (k.equals(CtxBinding.ssch_accept_selector.id) && vc.isAssignableFrom(Selector.class))
					return (V) asel;
				else if (k.equals(CtxBinding.ssch_read_selector.id) && vc.isAssignableFrom(Selector.class))
					return (V) rsel;
				else if (k.equals(CtxBinding.ssch_write_selector.id) && vc.isAssignableFrom(Selector.class))
					return (V) wsel;
				/* -- handlers -- */
				else if (k.equals(CtxBinding.ssch_accept_handler.id) && vc.isAssignableFrom(NetworkInterface.Handler.class))
					return (V) ahandler;
				else if (k.equals(CtxBinding.ssch_read_handler.id) && vc.isAssignableFrom(NetworkInterface.Handler.class))
					return (V) rhandler;
				else if (k.equals(CtxBinding.ssch_write_handler.id) && vc.isAssignableFrom(NetworkInterface.Handler.class))
					return (V) whandler;
				// else
				return parent.get(k, vc);
			}
		};
	}

	@Override final
	public <T> T initialize(Class<T> vt) throws Throwable {
		String propPort = context.getProperty(Servant.Property.DB_SERVER_PORT);

		
		log.log(Level.FINER, "NET - initialize SSChans ");
		try {
			ssport = Integer.parseInt(propPort);
			inetadd = new InetSocketAddress(ssport);
			
			final ServerSocketChannel ssch = ServerSocketChannel.open();
			context.bind(CtxBinding.server_socket_chan.id(), ssch);
			ssch.socket().bind(inetadd);
			
			log.log(Level.FINER, "NET - %s bound to %s", sschan, inetadd);
		} catch (Throwable e) {
			log.error("sschan initialize failed", e);
			throw e;
		} 

		
		log.log(Level.FINER, "NET - initialize sschan-op-selectors ");
		try {
			context.bind(CtxBinding.ssch_accept_selector.id(), Selector.open());
			context.bind(CtxBinding.ssch_read_selector.id(), Selector.open());
			context.bind(CtxBinding.ssch_write_selector.id(), Selector.open());

		} catch (Exception e) {
			log.error("selectors init failed", e);
			throw e;
		}


		log.log(Level.FINER, "NET - initialize sschan-op-handlers ");
		try {
			final class sschh_spec {
				final Servant.Component handler;
				final CtxBinding binding;
				sschh_spec (Servant.Component h, CtxBinding binding){
					this.binding = binding;
					this.handler = (Handler.Base) h;
				}
			}
			final sschh_spec hspecs[] = {
					new sschh_spec(new SSChanAcceptHandler(), CtxBinding.ssch_accept_handler),
					new sschh_spec(new SSChanReadHandler(), CtxBinding.ssch_read_handler),
					new sschh_spec(new SSChanWriteHandler(), CtxBinding.ssch_write_handler)
				};
			for(sschh_spec hspec : hspecs){
				hspec.handler.setContext(context);
				hspec.handler.initialize(hspec.handler.getClass());
				context.bind(hspec.binding.id(), hspec.handler);
				log.log(Level.FINEST, "NET - handler %s initialized and bound", hspec.handler);
			}
		} catch (Throwable e) {
			log.error("handlers init failed", e);
		}
		log.log(Level.FINER, "NET - handler %s initialized and bound", whandler);

		
		try {
			final ServerSocketChannel ssch = context.get(CtxBinding.server_socket_chan.id(), ServerSocketChannel.class);
			ssch.configureBlocking(false); 
			ssch.register(asel, SelectionKey.OP_ACCEPT, ahandler);
		} catch (Exception e) {
			log.error("sschan registration (OP_ACCEPT) failed", e);
		}
		return (T) this;
	}

	// ------------------------------------------------------------------------
	// component execution
	// ------------------------------------------------------------------------
	
	@Override final
	public void run() {
		final Selector asel = context.get(CtxBinding.ssch_accept_selector.id(), Selector.class);
		final Selector rsel = context.get(CtxBinding.ssch_read_selector.id(), Selector.class);
		final Selector wsel = context.get(CtxBinding.ssch_write_selector.id(), Selector.class);
		final Selector[] selectors = {
				Assert.notNull(wsel, "wsel", IllegalArgumentException.class), 
				Assert.notNull(rsel, "rsel", IllegalArgumentException.class), 
				Assert.notNull(asel, "asel", IllegalArgumentException.class) 
		}; // order is meaningful

		// REVU: should be constrained e.g. take n selection from A then ... etc.
		// where n is a small-ish number
		for(;;) {
			boolean idling = true;
			for(Selector s : selectors){
				log.log(Level.FINER, "check selector %s", s);
				try {
					if(s.selectNow() == 0)
						continue;
					
					Set<SelectionKey> selections = s.selectedKeys();
					for(SelectionKey k : selections){
						log.log(Level.FINEST, "selected: %s %s", k, k.attachment());
						Handler hdlr = (Handler) k.attachment();
						hdlr.handle(k);
						selections.remove(k);
					}
					idling = false;
				} catch (Exception e) {
					log.error("in select loop", e);
				}
			}

			// REVU: this needs a better solution to the idle.
			try {
				if(idling){
					Thread.sleep(1000);
					log.log(Level.FINER, "nothing to do ..");
				}
			} catch (Exception e) {
				log.error("in idle sleep", e);
			}
		}
	}

	// ------------------------------------------------------------------------
	// Network OPs handler
	// ------------------------------------------------------------------------
	private interface Handler{
		void handle(SelectionKey key) throws Exception;
		abstract static class Base extends Servant.Component.Base implements NetworkInterface.Handler {}
	}

	// ------------------------------------------------------------------------
	// NetworkInterface.AcceptHandler
	// ------------------------------------------------------------------------
	private static class SSChanAcceptHandler extends NetworkInterface.Handler.Base {
		@Override final
		public void handle(final SelectionKey key) throws Exception {
			Assert.notNull(key, "key", IllegalArgumentException.class);
			Assert.isTrue(key.isAcceptable(), "key should be acceptable", IllegalArgumentException.class);

			ServerSocketChannel chan = null;
			chan = context.get(CtxBinding.server_socket_chan.id(), ServerSocketChannel.class);

			SocketChannel sch = chan.accept();
			log.log(Level.FINE, "Accepted connection - %s", sch);

			Selector rsel = context.get(CtxBinding.ssch_read_selector.id(), Selector.class);
			SSChanReadHandler reqhdlr = context.get(CtxBinding.ssch_read_handler.id(), SSChanReadHandler.class);
			sch.configureBlocking(false);
			sch.register(rsel, SelectionKey.OP_READ, reqhdlr);
			log.log(Level.FINEST, "%s registered with %s OP_READs to be handled by - %s", sch, rsel, reqhdlr);

			log.warning("did not register for OP_WRITE -- IMPLEMENT WRITE HANDLER ..");
		}
	}

	// ------------------------------------------------------------------------
	// NetworkInterface.RequestHandler
	// ------------------------------------------------------------------------
	private static class SSChanReadHandler extends NetworkInterface.Handler.Base{
		@Override final
		public void handle(SelectionKey key) throws Exception {
			Assert.notNull(key, "key", IllegalArgumentException.class);
			Assert.isTrue(key.isReadable(), "key should be readable", IllegalArgumentException.class);

			key.channel().close(); // TEMP
			log.warning("closed socket channel -- IMPLEMENT READ HANDLER ..");
			throw new RuntimeException("Handler#handle NOT IMPLEMENTED");
		}
	}

	// ------------------------------------------------------------------------
	// NetworkInterface.ResponseHandler
	// ------------------------------------------------------------------------
	private static class SSChanWriteHandler extends NetworkInterface.Handler.Base {
		@Override final
		public void handle(SelectionKey key) throws Exception {
			throw new RuntimeException("Handler#handle NOT IMPLEMENTED");
		}
	}
}
