/*
 * Decompiled with CFR 0.152.
 */
package org.eclipse.jetty.util;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jetty.util.MemoryUtils;

public class BlockingArrayQueue<E>
extends AbstractList<E>
implements BlockingQueue<E> {
    private static final int HEAD_OFFSET = MemoryUtils.getIntegersPerCacheLine() - 1;
    private static final int TAIL_OFFSET = HEAD_OFFSET + MemoryUtils.getIntegersPerCacheLine();
    public static final int DEFAULT_CAPACITY = 128;
    public static final int DEFAULT_GROWTH = 64;
    private final int _maxCapacity;
    private final int _growCapacity;
    private final int[] _indexes = new int[TAIL_OFFSET + 1];
    private final Lock _tailLock = new ReentrantLock();
    private final AtomicInteger _size = new AtomicInteger();
    private final Lock _headLock = new ReentrantLock();
    private final Condition _notEmpty = this._headLock.newCondition();
    private Object[] _elements;

    public BlockingArrayQueue() {
        this._elements = new Object[128];
        this._growCapacity = 64;
        this._maxCapacity = Integer.MAX_VALUE;
    }

    public BlockingArrayQueue(int maxCapacity) {
        this._elements = new Object[maxCapacity];
        this._growCapacity = -1;
        this._maxCapacity = maxCapacity;
    }

    public BlockingArrayQueue(int capacity, int growBy) {
        this._elements = new Object[capacity];
        this._growCapacity = growBy;
        this._maxCapacity = Integer.MAX_VALUE;
    }

    public BlockingArrayQueue(int capacity, int growBy, int maxCapacity) {
        if (capacity > maxCapacity) {
            throw new IllegalArgumentException();
        }
        this._elements = new Object[capacity];
        this._growCapacity = growBy;
        this._maxCapacity = maxCapacity;
    }

    @Override
    public void clear() {
        this._tailLock.lock();
        try {
            this._headLock.lock();
            try {
                this._indexes[BlockingArrayQueue.HEAD_OFFSET] = 0;
                this._indexes[BlockingArrayQueue.TAIL_OFFSET] = 0;
                this._size.set(0);
            }
            finally {
                this._headLock.unlock();
            }
        }
        finally {
            this._tailLock.unlock();
        }
    }

    @Override
    public int size() {
        return this._size.get();
    }

    @Override
    public Iterator<E> iterator() {
        return this.listIterator();
    }

    @Override
    public E poll() {
        if (this._size.get() == 0) {
            return null;
        }
        Object e = null;
        this._headLock.lock();
        try {
            if (this._size.get() > 0) {
                int head = this._indexes[HEAD_OFFSET];
                e = this._elements[head];
                this._elements[head] = null;
                this._indexes[BlockingArrayQueue.HEAD_OFFSET] = (head + 1) % this._elements.length;
                if (this._size.decrementAndGet() > 0) {
                    this._notEmpty.signal();
                }
            }
        }
        finally {
            this._headLock.unlock();
        }
        return (E)e;
    }

    @Override
    public E peek() {
        if (this._size.get() == 0) {
            return null;
        }
        Object e = null;
        this._headLock.lock();
        try {
            if (this._size.get() > 0) {
                e = this._elements[this._indexes[HEAD_OFFSET]];
            }
        }
        finally {
            this._headLock.unlock();
        }
        return (E)e;
    }

    @Override
    public E remove() {
        E e = this.poll();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    @Override
    public E element() {
        E e = this.peek();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        boolean notEmpty = false;
        this._tailLock.lock();
        try {
            int size = this._size.get();
            if (size >= this._maxCapacity) {
                boolean bl = false;
                return bl;
            }
            if (size == this._elements.length) {
                this._headLock.lock();
                try {
                    if (!this.grow()) {
                        boolean bl = false;
                        return bl;
                    }
                }
                finally {
                    this._headLock.unlock();
                }
            }
            int tail = this._indexes[TAIL_OFFSET];
            this._elements[tail] = e;
            this._indexes[BlockingArrayQueue.TAIL_OFFSET] = (tail + 1) % this._elements.length;
            notEmpty = this._size.getAndIncrement() == 0;
        }
        finally {
            this._tailLock.unlock();
        }
        if (notEmpty) {
            this._headLock.lock();
            try {
                this._notEmpty.signal();
            }
            finally {
                this._headLock.unlock();
            }
        }
        return true;
    }

    @Override
    public boolean add(E e) {
        if (this.offer(e)) {
            return true;
        }
        throw new IllegalStateException();
    }

    @Override
    public void put(E o) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public E take() throws InterruptedException {
        Object e = null;
        this._headLock.lockInterruptibly();
        try {
            try {
                while (this._size.get() == 0) {
                    this._notEmpty.await();
                }
            }
            catch (InterruptedException ie) {
                this._notEmpty.signal();
                throw ie;
            }
            int head = this._indexes[HEAD_OFFSET];
            e = this._elements[head];
            this._elements[head] = null;
            this._indexes[BlockingArrayQueue.HEAD_OFFSET] = (head + 1) % this._elements.length;
            if (this._size.decrementAndGet() > 0) {
                this._notEmpty.signal();
            }
        }
        finally {
            this._headLock.unlock();
        }
        return (E)e;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public E poll(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        Object e = null;
        this._headLock.lockInterruptibly();
        try {
            while (this._size.get() == 0) {
                if (nanos <= 0L) {
                    E e2 = null;
                    return e2;
                }
                try {
                    nanos = this._notEmpty.awaitNanos(nanos);
                }
                catch (InterruptedException x) {
                    this._notEmpty.signal();
                    throw x;
                }
            }
            int head = this._indexes[HEAD_OFFSET];
            e = this._elements[head];
            this._elements[head] = null;
            this._indexes[BlockingArrayQueue.HEAD_OFFSET] = (head + 1) % this._elements.length;
            if (this._size.decrementAndGet() > 0) {
                this._notEmpty.signal();
            }
        }
        finally {
            this._headLock.unlock();
        }
        return (E)e;
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean remove(Object o) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [10[DOLOOP]], but top level block is 5[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public int remainingCapacity() {
        this._tailLock.lock();
        try {
            this._headLock.lock();
            try {
                int n = this.getCapacity() - this.size();
                this._headLock.unlock();
                return n;
            }
            catch (Throwable throwable) {
                this._headLock.unlock();
                throw throwable;
            }
        }
        finally {
            this._tailLock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public E get(int index) {
        this._tailLock.lock();
        try {
            Object object;
            this._headLock.lock();
            try {
                if (index < 0 || index >= this._size.get()) {
                    throw new IndexOutOfBoundsException("!(0<" + index + "<=" + this._size + ")");
                }
                int i = this._indexes[HEAD_OFFSET] + index;
                int capacity = this._elements.length;
                if (i >= capacity) {
                    i -= capacity;
                }
                object = this._elements[i];
                this._headLock.unlock();
            }
            catch (Throwable throwable) {
                this._headLock.unlock();
                throw throwable;
            }
            return (E)object;
        }
        finally {
            this._tailLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void add(int index, E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        this._tailLock.lock();
        try {
            this._headLock.lock();
            try {
                int size = this._size.get();
                if (index < 0 || index > size) {
                    throw new IndexOutOfBoundsException("!(0<" + index + "<=" + this._size + ")");
                }
                if (index == size) {
                    this.add(e);
                } else {
                    if (this._indexes[TAIL_OFFSET] == this._indexes[HEAD_OFFSET] && !this.grow()) {
                        throw new IllegalStateException("full");
                    }
                    int i = this._indexes[HEAD_OFFSET] + index;
                    int capacity = this._elements.length;
                    if (i >= capacity) {
                        i -= capacity;
                    }
                    this._size.incrementAndGet();
                    int tail = this._indexes[TAIL_OFFSET];
                    this._indexes[BlockingArrayQueue.TAIL_OFFSET] = tail = (tail + 1) % capacity;
                    if (i < tail) {
                        System.arraycopy(this._elements, i, this._elements, i + 1, tail - i);
                        this._elements[i] = e;
                    } else {
                        if (tail > 0) {
                            System.arraycopy(this._elements, 0, this._elements, 1, tail);
                            this._elements[0] = this._elements[capacity - 1];
                        }
                        System.arraycopy(this._elements, i, this._elements, i + 1, capacity - i - 1);
                        this._elements[i] = e;
                    }
                }
            }
            finally {
                this._headLock.unlock();
            }
        }
        finally {
            this._tailLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public E set(int index, E e) {
        Objects.requireNonNull(e);
        this._tailLock.lock();
        try {
            Object object;
            this._headLock.lock();
            try {
                if (index < 0 || index >= this._size.get()) {
                    throw new IndexOutOfBoundsException("!(0<" + index + "<=" + this._size + ")");
                }
                int i = this._indexes[HEAD_OFFSET] + index;
                int capacity = this._elements.length;
                if (i >= capacity) {
                    i -= capacity;
                }
                Object old = this._elements[i];
                this._elements[i] = e;
                object = old;
                this._headLock.unlock();
            }
            catch (Throwable throwable) {
                this._headLock.unlock();
                throw throwable;
            }
            return (E)object;
        }
        finally {
            this._tailLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public E remove(int index) {
        this._tailLock.lock();
        try {
            Object object;
            this._headLock.lock();
            try {
                if (index < 0 || index >= this._size.get()) {
                    throw new IndexOutOfBoundsException("!(0<" + index + "<=" + this._size + ")");
                }
                int i = this._indexes[HEAD_OFFSET] + index;
                int capacity = this._elements.length;
                if (i >= capacity) {
                    i -= capacity;
                }
                Object old = this._elements[i];
                int tail = this._indexes[TAIL_OFFSET];
                if (i < tail) {
                    System.arraycopy(this._elements, i + 1, this._elements, i, tail - i);
                    int n = TAIL_OFFSET;
                    this._indexes[n] = this._indexes[n] - 1;
                } else {
                    System.arraycopy(this._elements, i + 1, this._elements, i, capacity - i - 1);
                    this._elements[capacity - 1] = this._elements[0];
                    if (tail > 0) {
                        System.arraycopy(this._elements, 1, this._elements, 0, tail);
                        int n = TAIL_OFFSET;
                        this._indexes[n] = this._indexes[n] - 1;
                    } else {
                        this._indexes[BlockingArrayQueue.TAIL_OFFSET] = capacity - 1;
                    }
                    this._elements[this._indexes[BlockingArrayQueue.TAIL_OFFSET]] = null;
                }
                this._size.decrementAndGet();
                object = old;
                this._headLock.unlock();
            }
            catch (Throwable throwable) {
                this._headLock.unlock();
                throw throwable;
            }
            return (E)object;
        }
        finally {
            this._tailLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        this._tailLock.lock();
        try {
            this._headLock.lock();
            try {
                Object[] elements = new Object[this.size()];
                if (this.size() > 0) {
                    int head = this._indexes[HEAD_OFFSET];
                    int tail = this._indexes[TAIL_OFFSET];
                    if (head < tail) {
                        System.arraycopy(this._elements, head, elements, 0, tail - head);
                    } else {
                        int chunk = this._elements.length - head;
                        System.arraycopy(this._elements, head, elements, 0, chunk);
                        System.arraycopy(this._elements, 0, elements, chunk, tail);
                    }
                }
                Itr itr = new Itr(elements, index);
                this._headLock.unlock();
                return itr;
            }
            catch (Throwable throwable) {
                this._headLock.unlock();
                throw throwable;
            }
        }
        finally {
            this._tailLock.unlock();
        }
    }

    public int getCapacity() {
        this._tailLock.lock();
        try {
            int n = this._elements.length;
            return n;
        }
        finally {
            this._tailLock.unlock();
        }
    }

    public int getMaxCapacity() {
        return this._maxCapacity;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean grow() {
        if (this._growCapacity <= 0) {
            return false;
        }
        this._tailLock.lock();
        try {
            this._headLock.lock();
            try {
                int newTail;
                int head = this._indexes[HEAD_OFFSET];
                int tail = this._indexes[TAIL_OFFSET];
                int capacity = this._elements.length;
                Object[] elements = new Object[capacity + this._growCapacity];
                if (head < tail) {
                    newTail = tail - head;
                    System.arraycopy(this._elements, head, elements, 0, newTail);
                } else if (head > tail || this._size.get() > 0) {
                    newTail = capacity + tail - head;
                    int cut = capacity - head;
                    System.arraycopy(this._elements, head, elements, 0, cut);
                    System.arraycopy(this._elements, 0, elements, cut, tail);
                } else {
                    newTail = 0;
                }
                this._elements = elements;
                this._indexes[BlockingArrayQueue.HEAD_OFFSET] = 0;
                this._indexes[BlockingArrayQueue.TAIL_OFFSET] = newTail;
                boolean bl = true;
                this._headLock.unlock();
                return bl;
            }
            catch (Throwable throwable) {
                this._headLock.unlock();
                throw throwable;
            }
        }
        finally {
            this._tailLock.unlock();
        }
    }

    private class Itr
    implements ListIterator<E> {
        private final Object[] _elements;
        private int _cursor;

        public Itr(Object[] elements, int offset) {
            this._elements = elements;
            this._cursor = offset;
        }

        @Override
        public boolean hasNext() {
            return this._cursor < this._elements.length;
        }

        @Override
        public E next() {
            return this._elements[this._cursor++];
        }

        @Override
        public boolean hasPrevious() {
            return this._cursor > 0;
        }

        @Override
        public E previous() {
            return this._elements[--this._cursor];
        }

        @Override
        public int nextIndex() {
            return this._cursor + 1;
        }

        @Override
        public int previousIndex() {
            return this._cursor - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }
}

