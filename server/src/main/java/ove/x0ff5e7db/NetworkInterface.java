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

package ove.x0FF5E7DB;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.logging.Level;

import ove.x0ff5e7db.Server.Context;
import ove.x0ff5e7db.util.Assert;
import ove.x0ff5e7db.util.Log;


/**
 * This component is tasked with managing all network front-end responsibilities
 * of the server.
 * 
 * @author alphazero
 */
class NetworkInterface extends Server.Component.Base<NetworkInterface> implements Runnable{
	// ------------------------------------------------------------------------
	// properties
	// ------------------------------------------------------------------------
	public static final Log.Logger log = Specification.logger;
	private final Context context;
	private int ssport;
	private InetSocketAddress inetadd;
	// ------------------------------------------------------------------------
	// initialization concerns
	// ------------------------------------------------------------------------
	NetworkInterface(Server.Context context) {
		this.context = new Server.Context.Tree(context);
	}
	@Override final
	public NetworkInterface initialize() throws Throwable {
		String propPort = context.getProperty(Server.Property.DB_SERVER_PORT);
		
		ServerSocketChannel sschan = null;
		try {
			ssport = Integer.parseInt(propPort);
			sschan = ServerSocketChannel.open();
			inetadd = new InetSocketAddress(ssport);
			sschan.socket().bind(inetadd);
			context.bind(CtxBinding.server_socket_chan.id(), sschan);
			
			log.trace(Level.FINER, "NET - %s bound to %s", sschan, inetadd);
			
		} catch (Throwable e) {
			log.error("sschan initialize failed", e);
			throw e;
		} 
		
		Selector asel = null;
		try {
			asel = Selector.open();
			context.bind(CtxBinding.accept_selector.id(), asel);
			
			Selector selReader = Selector.open();
			context.bind(CtxBinding.read_selector.id(), selReader);
			
			Selector selWriter = Selector.open();
			context.bind(CtxBinding.write_selector.id(), selWriter);

		} catch (Exception e) {
			log.error("selectors init failed", e);
			throw e;
		}
		
		AcceptHandler acceptHandler = null;
		try {
			acceptHandler = new AcceptHandler();
			acceptHandler.setContext(context);
			acceptHandler.initialize();
			context.bind(CtxBinding.accept_handler.id(), acceptHandler);
			log.trace(Level.FINEST, "NET - handler %s initialized and bound", acceptHandler);
			
			RequestHandler requestHandler = new RequestHandler();
			requestHandler.setContext(context);
			requestHandler.initialize();
			context.bind(CtxBinding.request_handler.id(), requestHandler);
			log.trace(Level.FINEST, "NET - handler %s initialized and bound", requestHandler);
			
			ResponseHandler responseHandler = new ResponseHandler();
			responseHandler.setContext(context);
			responseHandler.initialize();
			context.bind(CtxBinding.response_handler.id(), responseHandler);
			log.trace(Level.FINEST, "NET - handler %s initialized and bound", responseHandler);
			
		} catch (Throwable e) {
			log.error("handlers init failed", e);
		}

		try {
			sschan = context.get(CtxBinding.server_socket_chan.id(), ServerSocketChannel.class);
			sschan.configureBlocking(false); 
			sschan.register(asel, SelectionKey.OP_ACCEPT, acceptHandler);
		} catch (Exception e) {
			log.error("sschan registration (OP_ACCEPT) failed", e);
		}
		return this;
	}
	
	@Override final
	public void run() {
		/* check selectors in the following order
		 * - write
		 * - read
		 * - accept
		 */
		Selector csel = context.get(CtxBinding.accept_selector.id(), Selector.class);
		Selector rsel = context.get(CtxBinding.read_selector.id(), Selector.class);
		Selector wsel = context.get(CtxBinding.write_selector.id(), Selector.class);
		Selector[] selectors = {wsel, rsel, csel}; // order is meaningful
		
		for(;;) {
			boolean idling = true;
			for(Selector s : selectors){
				log.trace(Level.FINEST, "check selector %s", s);
				try {
					if(s.selectNow() == 0){
						continue;
					}
					Set<SelectionKey> selections = s.selectedKeys();
					for(SelectionKey k : selections){
						log.trace(Level.FINER, "selected: %s %s", k, k.attachment());
						Handler hdlr = (Handler) k.attachment();
						hdlr.handle(k);
						selections.remove(k);
					}
					idling = false;
				} catch (Exception e) {
					log.error("in select loop", e);
				}
			}
			
			try {
				if(idling){
					Thread.sleep(1000);
					log.trace(Level.FINEST, "nothing to do ..");
				}
			} catch (Exception e) {
				log.error("in idle sleep", e);
			}
		}
	}
	
	// ------------------------------------------------------------------------
	// Context bindings
	// ------------------------------------------------------------------------
	enum CtxBinding {
		server_socket_chan,
		accept_handler,
		request_handler,
		response_handler,
		accept_selector, 
		read_selector, 
		write_selector;
		private final String id;
		CtxBinding () {
			this.id = this.name().toLowerCase().replace('_', '.');
		}
		public String id() { return id; }
	}
	
	// ------------------------------------------------------------------------
	// Network OPs handler
	// ------------------------------------------------------------------------
	private interface Handler{
		void handle(SelectionKey key) throws Exception;
		abstract static class Base<T> extends Server.Component.Base<T> implements Handler {}
	}
	
	// ------------------------------------------------------------------------
	// NetworkInterface.AcceptHandler
	// ------------------------------------------------------------------------
	private static class AcceptHandler extends Handler.Base<AcceptHandler> {
		@Override final
		public void handle(final SelectionKey key) throws Exception {
			Assert.notNull(key, "key", IllegalArgumentException.class);
			Assert.isTrue(key.isAcceptable(), "key should be acceptable", IllegalArgumentException.class);
			
			ServerSocketChannel chan = null;
			chan = context.get(CtxBinding.server_socket_chan.id(), ServerSocketChannel.class);
			
			SocketChannel sch = chan.accept();
			log.trace(Level.FINE, "Accepted connection - %s", sch);
			
			Selector rsel = context.get(CtxBinding.read_selector.id(), Selector.class);
			RequestHandler reqhdlr = context.get(CtxBinding.request_handler.id(), RequestHandler.class);
			sch.configureBlocking(false);
			sch.register(rsel, SelectionKey.OP_READ, reqhdlr);
			log.trace(Level.FINEST, "%s registered with %s OP_READs to be handled by - %s", sch, rsel, reqhdlr);
			
			log.warning("did not register for OP_WRITE -- IMPLEMENT WRITE HANDLER ..");
		}
	}
	
	// ------------------------------------------------------------------------
	// NetworkInterface.RequestHandler
	// ------------------------------------------------------------------------
	private static class RequestHandler extends Handler.Base<RequestHandler>{
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
	private static class ResponseHandler extends Handler.Base<ResponseHandler> {
		@Override final
		public void handle(SelectionKey key) throws Exception {
			throw new RuntimeException("Handler#handle NOT IMPLEMENTED");
		}
	}
}
