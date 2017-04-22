package io.objectbox;

import java.io.Closeable;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import io.objectbox.annotation.apihint.Beta;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.annotation.apihint.Temporary;

@Beta
@Internal
@NotThreadSafe
public abstract class Cursor<T> implements Closeable {
    static final boolean WARN_FINALIZER = false;

    protected static final int PUT_FLAG_FIRST = 1;
    protected static final int PUT_FLAG_COMPLETE = 1 << 1;

    static native void nativeDestroy(long cursor);

    static native void nativeDeleteEntity(long cursor, long key);

    static native void nativeDeleteAll(long cursor);

    static native boolean nativeSeek(long cursor, long key);

    static native Object nativeGetAllEntities(long cursor);

    static native Object nativeGetEntity(long cursor, long key);

    static native Object nativeNextEntity(long cursor);

    static native Object nativeFirstEntity(long cursor);

    static native long nativeCount(long cursor);

    static native List nativeFindScalar(long cursor, String propertyName, long value);

    static native List nativeFindString(long cursor, String propertyName, String value);

    static native List nativeFindScalarPropertyId(long cursor, int propertyId, long value);

    static native List nativeFindStringPropertyId(long cursor, int propertyId, String value);

    // TODO not implemented
    static native long nativeGetKey(long cursor);

    static native long nativeLookupKeyUsingIndex(long cursor, int propertyId, String value);

    static native long nativeRenew(long cursor, long tx);

    protected static native long collect313311(long cursor, long keyIfComplete, int flags,
                                               int idStr1, String valueStr1, int idStr2, String valueStr2,
                                               int idStr3, String valueStr3,
                                               int idBA1, byte[] valueBA1,
                                               int idLong1, long valueLong1, int idLong2, long valueLong2,
                                               int idLong3, long valueLong3,
                                               int idInt1, int valueInt1, int idInt2, int valueInt2,
                                               int idInt3, int valueInt3,
                                               int idFloat1, float valueFloat1, int idDouble1, double valueDouble1
    );

    protected static native long collect430000(long cursor, long keyIfComplete, int flags,
                                               int idStr1, String valueStr1, int idStr2, String valueStr2,
                                               int idStr3, String valueStr3, int idStr4, String valueStr4,
                                               int idBA1, byte[] valueBA1, int idBA2, byte[] valueBA2, int idBA3,
                                               byte[] valueBA3
    );

    protected static native long collect400000(long cursor, long keyIfComplete, int flags,
                                               int idStr1, String valueStr1, int idStr2, String valueStr2,
                                               int idStr3, String valueStr3, int idStr4, String valueStr4
    );

    protected static native long collect002033(long cursor, long keyIfComplete, int flags,
                                               int idLong1, long valueLong1, int idLong2, long valueLong2,
                                               int idFloat1, float valueFloat1, int idFloat2, float valueFloat2,
                                               int idFloat3, float valueFloat3,
                                               int idDouble1, double valueDouble1, int idDouble2, double valueDouble2,
                                               int idDouble3, double valueDouble3
    );

    protected static native long collect004000(long cursor, long keyIfComplete, int flags,
                                               int idLong1, long valueLong1, int idLong2, long valueLong2,
                                               int idLong3, long valueLong3, int idLong4, long valueLong4
    );

    static native int nativePropertyId(long cursor, String propertyValue);

    static native List nativeGetBacklinkEntities(long cursor, int entityId, int propertyId, long key);

    static native void nativeSetBoxStoreForEntities(long cursor, Object boxStore);

    protected Transaction tx;
    protected final long cursor;
    protected final EntityInfo entityInfo;
    protected final BoxStore boxStoreForEntities;

    protected boolean closed;

    private final Throwable creationThrowable;

    protected Cursor(Transaction tx, long cursor, EntityInfo entityInfo, BoxStore boxStore) {
        if (tx == null) {
            throw new IllegalArgumentException("Transaction is null");
        }
        this.tx = tx;
        this.cursor = cursor;
        this.entityInfo = entityInfo;
        this.boxStoreForEntities = boxStore;

        Property[] allProperties = entityInfo.getAllProperties();
        for (Property property : allProperties) {
            if (!property.isIdVerified()) {
                int id = getPropertyId(property.dbName);
                property.verifyId(id);
            }
        }
        creationThrowable = WARN_FINALIZER ? new Throwable() : null;

        nativeSetBoxStoreForEntities(cursor, boxStore);
    }

    @Override
    protected void finalize() throws Throwable {
        if (WARN_FINALIZER && !closed && creationThrowable != null) {
            System.err.println("Cursor was not closed. It was initially created here:");
            creationThrowable.printStackTrace();
        }
        close();
        super.finalize();
    }

    protected abstract long getId(T entity);

    public abstract long put(T entity);

    public EntityInfo getEntityInfo() {
        return entityInfo;
    }

    public T get(long key) {
        return (T) nativeGetEntity(cursor, key);
    }

    public T next() {
        return (T) nativeNextEntity(cursor);
    }

    public T first() {
        return (T) nativeFirstEntity(cursor);
    }

    /** Does not work yet, also probably won't be faster than {@link Box#getAll()}. */
    public List<T> getAll() {
        return (List) nativeGetAllEntities(cursor);
    }

    public void deleteEntity(long key) {
        nativeDeleteEntity(cursor, key);
    }

    public void deleteAll() {
        nativeDeleteAll(cursor);
    }

    public long getKey() {
        return nativeGetKey(cursor);
    }

    public boolean seek(long key) {
        return nativeSeek(cursor, key);
    }

    public long count() {
        return nativeCount(cursor);
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            // tx is null despite check in constructor in some tests (called by finalizer):
            // Null check avoids NPE in finalizer and seems to stabilize Android instrumentation perf tests.
            if (tx != null && !tx.getStore().isClosed()) {
                nativeDestroy(cursor);
            }
        }
    }

    public int getPropertyId(String propertyName) {
        return nativePropertyId(cursor, propertyName);
    }

    @Temporary
    public List<T> find(String propertyName, long value) {
        return nativeFindScalar(cursor, propertyName, value);
    }

    @Temporary
    public List<T> find(String propertyName, String value) {
        return nativeFindString(cursor, propertyName, value);
    }

    @Temporary
    public List<T> find(int propertyId, long value) {
        return nativeFindScalarPropertyId(cursor, propertyId, value);
    }

    @Temporary
    public List<T> find(int propertyId, String value) {
        return nativeFindStringPropertyId(cursor, propertyId, value);
    }

    /**
     * @return key or 0 if not found
     */
    public long lookupKeyUsingIndex(int propertyId, String value) {
        return nativeLookupKeyUsingIndex(cursor, propertyId, value);
    }

    public Transaction getTx() {
        return tx;
    }

    // This cursor may operate on obsolete data (another write TX was committed after this cursor's TX had begun).
    public boolean isObsolete() {
        return tx.isObsolete();
    }

    public boolean isClosed() {
        return closed;
    }

    public void renew(Transaction tx) {
        nativeRenew(cursor, tx.internalHandle());
        this.tx = tx;
    }

    @Internal
    long internalHandle() {
        return cursor;
    }

    List<T> getBacklinkEntities(int entityId, Property relationIdProperty, long key) {
        try {
            return nativeGetBacklinkEntities(cursor, entityId, relationIdProperty.getId(), key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Please check if the given property belongs to a valid @Relation: "
                    + relationIdProperty, e);
        }
    }

    @Override
    public String toString() {
        return "Cursor " + Long.toString(cursor, 16) + (isClosed() ? "(closed)" : "");
    }
}
