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

public interface Hash {
	/**
	 * Provider is not required to test for nulls, etc.  Could possibly 
	 * throw {@link RuntimeException}
	 * @throws RuntimeException 
	 * @param b byte buffer to be hashed
	 * @return hash of b
	 */
	int hash(byte[] b) ;
	
	int bitsize();

	// ------------------------------------------------------------------------
	// Convenience instances
	// ------------------------------------------------------------------------
	/** */
	Hash MBUint32 = new ModifiedBernstein.Uint32();
	/** */
	Hash MBInt32 = new ModifiedBernstein.Int32();
	
	// ------------------------------------------------------------------------
	// Providers 
	// ------------------------------------------------------------------------
	
	enum Provider {
		MBInt32 (Hash.MBInt32, ModifiedBernstein.class),
		MBUint32 (Hash.MBUint32,  ModifiedBernstein.class);
		final public String id;
		final public Hash provider;
		Provider(Hash provider, Class<?> algogroup){
			this.provider = provider;
			this.id = String.format("%s.%s", algogroup.getSimpleName(), provider.getClass().getSimpleName());
		}
	}

	/** based on the various copies of implementation found on the net. Name is not canonical. */
	public interface ModifiedBernstein {
		public static class Int32 implements Hash{
			@Override public int bitsize() { return 32;}
			@Override public int hash(byte[] b) {
				int h = 5381;
				for(int i=0; i<b.length; i++) {
					h = ((h << 5) - h) + b[i];
				}
				return h;
			}
		}
		/** Masks output of {@link Int32} with {@link Integer#MAX_VALUE} */
		public static final class Uint32 extends Int32{
			@Override final public int bitsize() { return 31;}
			@Override final public int hash(byte[] b) {
 				return super.hash(b) & Integer.MAX_VALUE;  
			}
		}
	}
}
