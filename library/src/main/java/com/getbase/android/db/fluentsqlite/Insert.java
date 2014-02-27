package com.getbase.android.db.fluentsqlite;

import static com.google.common.base.Preconditions.checkNotNull;

import com.getbase.android.db.fluentsqlite.QueryBuilder.Query;
import com.getbase.android.db.provider.Utils;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.util.Collections;
import java.util.List;

public class Insert implements InsertTableSelector, InsertFormSelector, InsertValuesBuilder {
  String mTable;
  ContentValues mValues = new ContentValues();
  List<String> mQueryFormColumns = Lists.newArrayList();

  private Insert() {
  }

  public static InsertTableSelector insert() {
    return new Insert();
  }

  public long perform(SQLiteDatabase db) {
    return db.insert(mTable, null, mValues);
  }

  public static class InsertWithSelect {
    private final String mTable;
    private final RawQuery mQuery;
    private final List<String> mQueryFormColumns;

    InsertWithSelect(String table, RawQuery query, List<String> queryFormColumns) {
      mTable = table;
      mQuery = query;
      mQueryFormColumns = queryFormColumns;
    }

    public long perform(SQLiteDatabase db) {
      StringBuilder builder = new StringBuilder();
      builder.append("INSERT INTO ").append(mTable).append(" ");
      if (!mQueryFormColumns.isEmpty()) {
        builder
            .append("(")
            .append(Joiner.on(", ").join(mQueryFormColumns))
            .append(") ");
      }
      builder.append(mQuery.mRawQuery);

      SQLiteStatement statement = db.compileStatement(builder.toString());
      try {
        int argIndex = 1;
        for (String arg : mQuery.mRawQueryArgs) {
          Utils.bindContentValueArg(statement, argIndex++, arg);
        }

        return statement.executeInsert();
      } finally {
        statement.close();
      }
    }
  }

  public static class DefaultValuesInsert {
    final String mTable;
    final String mNullColumnHack;

    private DefaultValuesInsert(String table, String nullColumnHack) {
      mTable = table;
      mNullColumnHack = nullColumnHack;
    }

    public void perform(SQLiteDatabase db) {
      db.insert(mTable, mNullColumnHack, null);
    }
  }

  @Override
  public InsertFormSelector into(String table) {
    mTable = checkNotNull(table);
    return this;
  }

  @Override
  public DefaultValuesInsert defaultValues(String nullColumnHack) {
    return new DefaultValuesInsert(mTable, checkNotNull(nullColumnHack));
  }

  @Override
  public InsertSubqueryForm columns(String... columns) {
    Preconditions.checkArgument(columns != null, "Column list cannot be null");
    Collections.addAll(mQueryFormColumns, columns);

    return this;
  }

  @Override
  public InsertWithSelect resultOf(Query query) {
    checkNotNull(query);

    return new InsertWithSelect(mTable, query.toRawQuery(), mQueryFormColumns);
  }

  @Override
  public Insert values(ContentValues values) {
    mValues.putAll(values);
    return this;
  }

  @Override
  public Insert value(String column, Object value) {
    Utils.addToContentValues(column, value, mValues);
    return this;
  }
}
