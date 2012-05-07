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

package ove.x0ff5e7db.util.binary;

/**
 * Convert to and from language primitives from provided <code>byte[]</code> buffers.
 * @author joubin <alphazero@sensesay.net>
 */
public interface Codec {
	
	int LONG_BYTES = Long.SIZE / Byte.SIZE;
	int INTEGER_BYTES = Integer.SIZE / Byte.SIZE;
	int SHORT_BYTES = Short.SIZE / Byte.SIZE;

	public interface Binary {
		// ------------------------------------------------------------------------
		// convenience instances 
		// ------------------------------------------------------------------------
		/* inspired by Go's encode/binary */
		Codec.Binary BigEndian = new Binary.BigEndian();
		
		// ------------------------------------------------------------------------
		// api
		// ------------------------------------------------------------------------
		
		void writeLong(final long v, final byte[] b) throws NullPointerException, IllegalArgumentException;
		int writeLong(final long v, final byte[] b, final int off) throws NullPointerException, IllegalArgumentException;
		long readLong(byte[] b);
		long readLong(byte[] b, int off);
		
		void writeInt(final int v, final byte[] b);
		void writeInt(final int v, final byte[] b, final int off);
		int readInt(final byte[] b) throws NullPointerException, IllegalArgumentException;
		int readInt(final byte[] b, final int off) throws NullPointerException, IllegalArgumentException;

		void writeShort(final short v, final byte[] b) throws NullPointerException, IllegalArgumentException ;
		void writeShort(final short v, final byte[] b, final int off) throws NullPointerException, IllegalArgumentException;
		short readShort(final byte[] b) throws NullPointerException, IllegalArgumentException;
		short readShort(final byte[] b, final int off) throws NullPointerException, IllegalArgumentException;

		// ------------------------------------------------------------------------
		// big-endian ref-impl 
		// ------------------------------------------------------------------------
		public static class BigEndian implements Codec.Binary{

			@Override final 
			public void writeLong(final long v, final byte[] b) throws NullPointerException, IllegalArgumentException {
				writeLong(v, b, 0);
			}
			@Override final 
			public int writeLong(final long v, final byte[] b, final int off) throws NullPointerException, IllegalArgumentException {
				if(b==null)
					throw new NullPointerException("b");
				if(b.length - off < LONG_BYTES) 
					throw new IllegalArgumentException(String.format("(b.len:%d, off:%d)", b.length, off).toString());
				b[off]   = (byte)(v >>> 56);
				b[off+1] = (byte)(v >>> 48);
				b[off+2] = (byte)(v >>> 40);
				b[off+3] = (byte)(v >>> 32);
				b[off+4] = (byte)(v >>> 24);
				b[off+5] = (byte)(v >>> 16);
				b[off+6] = (byte)(v >>>  8);
				b[off+7] = (byte)(v >>>  0);
				return LONG_BYTES;
			}
			@Override final 
			public long readLong(byte[] b){
				return readLong(b, 0);
			}
			@Override final 
			public long readLong(byte[] b, int off){
				if(b==null)
					throw new NullPointerException("b");
				if(b.length - off < LONG_BYTES) 
					throw new IllegalArgumentException(String.format("(b.len:%d, off:%d)", b.length, off).toString());
				return ( 
						((long)b[0] << 56) +
						((long)(b[1] & 255) << 48) +
						((long)(b[2] & 255) << 40) +
						((long)(b[3] & 255) << 32) +
						((long)(b[4] & 255) << 24) +
						((b[5] & 255) << 16) +
						((b[6] & 255) <<  8) +
						((b[7] & 255) <<  0)
						);
			}

			@Override final 
			public void writeInt(final int v, final byte[] b){
				writeInt(v, b, 0);
			}
			@Override final 
			public void writeInt(final int v, final byte[] b, final int off){
				if(b==null)
					throw new NullPointerException("b");
				if(b.length - off < INTEGER_BYTES) 
					throw new IllegalArgumentException(String.format("(b.len:%d, off:%d)", b.length, off).toString());
				b[off]   = (byte) ((v >>> 24) & 0xFF);
				b[off+1] = (byte) ((v >>> 16) & 0xFF);
				b[off+2] = (byte) ((v >>>  8) & 0xFF);
				b[off+3] = (byte) ((v >>>  0) & 0xFF);
			}

			@Override final 
			public int readInt(final byte[] b) throws NullPointerException, IllegalArgumentException {
				return readInt(b, 0);
			}
			@Override final 
			public int readInt(final byte[] b, final int off) throws NullPointerException, IllegalArgumentException {
				if(b==null)
					throw new NullPointerException("b");
				if(b.length - off < INTEGER_BYTES) 
					throw new IllegalArgumentException(String.format("(b.len:%d, off:%d)", b.length, off).toString());
				int b1 = b[off] & 0xFF;
				int b2 = b[off+1] & 0xFF;
				int b3 = b[off+2] & 0xFF;
				int b4 = b[off+3] & 0xFF;
				return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0));
			}

			@Override final 
			public void writeShort(final short v, final byte[] b) throws NullPointerException, IllegalArgumentException {
				writeShort(v, b, 0);
			}
			@Override final 
			public void writeShort(final short v, final byte[] b, final int off) throws NullPointerException, IllegalArgumentException {
				if(b==null)
					throw new NullPointerException("b");
				if(b.length - off < SHORT_BYTES) 
					throw new IllegalArgumentException(String.format("(b.len:%d, off:%d)", b.length, off).toString());

				b[off] = (byte) ((v >>> 8) & 0xFF);
				b[off+1] = (byte) ((v >>> 0) & 0xFF);
			}
			@Override final 
			public short readShort(final byte[] b) throws NullPointerException, IllegalArgumentException {
				return readShort(b, 0);
			}
			@Override final 
			public short readShort(final byte[] b, final int off) throws NullPointerException, IllegalArgumentException {
				if(b==null)
					throw new NullPointerException("b");
				if(b.length - off < SHORT_BYTES) 
					throw new IllegalArgumentException(String.format("(b.len:%d, off:%d)", b.length, off).toString());

				int b1 = b[off] & 0xFF;
				int b2 = b[off+1] & 0xFF;
				return (short)((b1 << 8) + (b2 << 0));
			}
		}
	}
}
