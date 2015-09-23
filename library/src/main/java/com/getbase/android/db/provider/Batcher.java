package com.getbase.android.db.provider;

import com.google.common.collect.Multimap;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

public abstract class Batcher {

  public static Batcher begin() {
    return new BatcherImpl();
  }

  public abstract BackRefBuilder append(ConvertibleToOperation... convertibles);

  public abstract BackRefBuilder append(Iterable<ConvertibleToOperation> convertibles);

  public abstract ArrayList<ContentProviderOperation> operations();

  public final ContentProviderResult[] applyBatch(ContentProvider provider) {
    return applyBatchOrThrow(null, new ContentProviderCrudHandler(provider));
  }

  public final ContentProviderResult[] applyBatch(ContentProviderClient providerClient) throws RemoteException, OperationApplicationException {
    return applyBatch(null, new ContentProviderClientCrudHandler(providerClient));
  }

  public final ContentProviderResult[] applyBatch(String authority, ContentResolver resolver) throws RemoteException, OperationApplicationException {
    return applyBatch(authority, new ContentResolverCrudHandler(resolver));
  }

  public final ContentProviderResult[] applyBatch(String authority, CrudHandler crudHandler) throws RemoteException, OperationApplicationException {
    return crudHandler.applyBatch(authority, operations());
  }

  public final ContentProviderResult[] applyBatchOrThrow(String authority, ContentProvider provider) {
    return applyBatchOrThrow(authority, new ContentProviderCrudHandler(provider));
  }

  public final ContentProviderResult[] applyBatchOrThrow(String authority, ContentResolver resolver) {
    return applyBatchOrThrow(authority, new ContentResolverCrudHandler(resolver));
  }

  public final ContentProviderResult[] applyBatchOrThrow(ContentProviderClient client) {
    return applyBatchOrThrow(null, new ContentProviderClientCrudHandler(client));
  }

  public final ContentProviderResult[] applyBatchOrThrow(String authority, CrudHandler crudHandler) {
    try {
      return applyBatch(authority, crudHandler);
    } catch (RemoteException | OperationApplicationException e) {
      throw new RuntimeException("An Exception was returned from applyBatch", e);
    }
  }

  static class BackRef {

    final Insert parent;
    final String column;

    BackRef(Insert parent, String column) {
      this.parent = parent;
      this.column = column;
    }
  }

  protected abstract Multimap<ConvertibleToOperation, BackRef> getBackRefsMultimap();
  protected abstract List<ConvertibleToOperation> getConvertibles();
}
