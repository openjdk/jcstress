/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.incorta.conc.atomicity;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.openjdk.jcstress.util.UnsafeHolder.UNSAFE;

public class OffHeapUtils {
    private static final long addressOffset;
    private static final int PAGE_BYTE_SIZE = 1024;
    
    static {
        try {
            addressOffset = UNSAFE.objectFieldOffset(Buffer.class.getDeclaredField("address"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long getAddress(ByteBuffer buffy) {
        // steal the value of address field from Buffer, holding the memory address for this ByteBuffer
        return UNSAFE.getLong(buffy, addressOffset);
    }

    public static ByteBuffer allocateAlignedByteBuffer(int capacity, long align) {
        // Power of 2 --> single bit, none power of 2 alignments are not allowed.  
        if (Long.bitCount(align) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of 2");
        }
        // We over allocate by the alignment so we know we can have a large enough aligned
        // block of memory to use. Also set order to native while we are here.
        ByteBuffer buffy = ByteBuffer.allocateDirect((int) (capacity + align));
        long address = getAddress(buffy);
        // check if we got lucky and the address is already aligned
        if ((address & (align - 1)) == 0) {
            // set the new limit to intended capacity
            buffy.limit(capacity);
            // the slice is now an aligned buffer of the required capacity
            return buffy.slice().order(ByteOrder.nativeOrder());
        } else {
            // we need to shift the start position to an aligned address --> address + (align - (address % align))
            // the modulo replacement with the & trick is valid for power of 2 values only
            int newPosition = (int) (align - (address & (align - 1)));
            // change the position
            buffy.position(newPosition);
            int newLimit = newPosition + capacity;
            // set the new limit to accomodate offset + intended capacity
            buffy.limit(newLimit);
            // the slice is now an aligned buffer of the required capacity
            return buffy.slice().order(ByteOrder.nativeOrder());
        }
    }
    
    public static final long allocatePage(int size) {
        
        long address = UNSAFE.allocateMemory(size);
        
        // Initializing page
        UNSAFE.setMemory(address, size, (byte) 0);
        return address;
    }

    public static final void setLong(final long page, final long offsetByte, final long value) {
        final long address = page + offsetByte;
        UNSAFE.putLong(address, value);
    }

    public static final long getLong(final long page, final long offsetByte) {
        final long address = page + offsetByte;
        return UNSAFE.getLong(address);
    }
}
