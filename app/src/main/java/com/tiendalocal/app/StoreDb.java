package com.tiendalocal.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public class StoreDb extends SQLiteOpenHelper {
 public StoreDb(Context c){super(c,"mysql.db",null,1);setWriteAheadLoggingEnabled(true);}
 @Override public void onConfigure(SQLiteDatabase db){super.onConfigure(db);db.setForeignKeyConstraintsEnabled(true);}
 @Override public void onCreate(SQLiteDatabase d){
  String[] sql={
   "CREATE TABLE categories(id INTEGER PRIMARY KEY,name TEXT NOT NULL UNIQUE)",
   "CREATE TABLE products(id INTEGER PRIMARY KEY,barcode TEXT NOT NULL UNIQUE,name TEXT NOT NULL,category_id INTEGER REFERENCES categories(id),price_cents INTEGER NOT NULL CHECK(price_cents>=0),cost_cents INTEGER NOT NULL DEFAULT 0,stock INTEGER NOT NULL DEFAULT 0 CHECK(stock>=0),min_stock INTEGER NOT NULL DEFAULT 0,expiration TEXT,kind TEXT NOT NULL DEFAULT 'PRODUCT',active INTEGER NOT NULL DEFAULT 1)",
   "CREATE TABLE tags(id INTEGER PRIMARY KEY,name TEXT NOT NULL UNIQUE,color TEXT DEFAULT '#673DE6',description TEXT)",
   "CREATE TABLE product_tags(product_id INTEGER REFERENCES products(id) ON DELETE CASCADE,tag_id INTEGER REFERENCES tags(id) ON DELETE CASCADE,PRIMARY KEY(product_id,tag_id))",
   "CREATE TABLE lots(id INTEGER PRIMARY KEY,product_id INTEGER NOT NULL REFERENCES products(id),code TEXT,expires_on TEXT,quantity INTEGER NOT NULL DEFAULT 0)",
   "CREATE TABLE sales(id INTEGER PRIMARY KEY,created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),total_cents INTEGER NOT NULL,payment_method TEXT NOT NULL DEFAULT 'Efectivo',notes TEXT)",
   "CREATE TABLE sale_items(id INTEGER PRIMARY KEY,sale_id INTEGER NOT NULL REFERENCES sales(id),product_id INTEGER NOT NULL REFERENCES products(id),quantity INTEGER NOT NULL CHECK(quantity>0),unit_price_cents INTEGER NOT NULL,unit_cost_cents INTEGER NOT NULL DEFAULT 0)",
   "CREATE TABLE stock_movements(id INTEGER PRIMARY KEY,product_id INTEGER NOT NULL REFERENCES products(id),type TEXT NOT NULL,quantity INTEGER NOT NULL,reference TEXT,created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')))",
   "CREATE TABLE assets(id INTEGER PRIMARY KEY,code TEXT UNIQUE,name TEXT NOT NULL,location TEXT,value_cents INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'Activo')",
   "CREATE TABLE asset_movements(id INTEGER PRIMARY KEY,asset_id INTEGER NOT NULL REFERENCES assets(id),type TEXT NOT NULL,notes TEXT,created_at TEXT DEFAULT (datetime('now','localtime')))",
   "CREATE TABLE expenses(id INTEGER PRIMARY KEY,description TEXT NOT NULL,amount_cents INTEGER NOT NULL,date TEXT NOT NULL)",
   "CREATE TABLE accounts(id INTEGER PRIMARY KEY,code TEXT UNIQUE,name TEXT NOT NULL,type TEXT NOT NULL)",
   "CREATE TABLE entries(id INTEGER PRIMARY KEY,date TEXT NOT NULL,description TEXT)",
   "CREATE TABLE entry_lines(id INTEGER PRIMARY KEY,entry_id INTEGER REFERENCES entries(id),account_id INTEGER REFERENCES accounts(id),debit_cents INTEGER DEFAULT 0,credit_cents INTEGER DEFAULT 0)",
   "CREATE TABLE cash_closings(id INTEGER PRIMARY KEY,date TEXT NOT NULL,opening_cents INTEGER DEFAULT 0,expected_cents INTEGER DEFAULT 0,counted_cents INTEGER DEFAULT 0)",
   "CREATE TABLE data_operations(id INTEGER PRIMARY KEY,operation TEXT NOT NULL,format TEXT NOT NULL,records INTEGER DEFAULT 0,created_at TEXT DEFAULT (datetime('now','localtime')))",
   "CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT)"};
  for(String s:sql)d.execSQL(s);
  d.execSQL("INSERT INTO categories(name) VALUES ('General'),('Relojes'),('Alimentos'),('Mobiliario')");
 }
 @Override public void onUpgrade(SQLiteDatabase d,int old,int next){}
 public Cursor products(){return getReadableDatabase().rawQuery("SELECT p.id,p.barcode,p.name,p.price_cents,p.cost_cents,p.stock,p.min_stock,p.expiration,p.kind,p.category_id FROM products p WHERE p.active=1 ORDER BY p.name",null);}
 public Cursor sales(){return getReadableDatabase().rawQuery("SELECT id,created_at,total_cents,payment_method FROM sales ORDER BY id DESC",null);}
 public Cursor tags(){return getReadableDatabase().rawQuery("SELECT id,name,color,description FROM tags ORDER BY name",null);}
 public Cursor assets(){return getReadableDatabase().rawQuery("SELECT id,code,name,location,value_cents,status FROM assets ORDER BY name",null);}
 public Cursor movements(long id){return getReadableDatabase().rawQuery("SELECT type,quantity,reference,created_at FROM stock_movements WHERE product_id=? ORDER BY id DESC",new String[]{""+id});}
 public void saveProduct(long id,String barcode,String name,long price,long cost,int stock,int min,String expiration,String kind){
  SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
   ContentValues v=new ContentValues();v.put("barcode",barcode);v.put("name",name);v.put("price_cents",price);v.put("cost_cents",cost);v.put("min_stock",min);v.put("expiration",expiration.isEmpty()?null:expiration);v.put("kind",kind);
   if(id==0){v.put("stock",stock);long pid=d.insertOrThrow("products",null,v);movement(d,pid,"ENTRADA",stock,"Stock inicial");}
   else {int previous=0;try(Cursor c=d.rawQuery("SELECT stock FROM products WHERE id=?",new String[]{""+id})){if(c.moveToFirst())previous=c.getInt(0);}v.put("stock",stock);d.update("products",v,"id=?",new String[]{""+id});if(stock!=previous)movement(d,id,"AJUSTE",stock-previous,"Edición de existencias");}
   d.setTransactionSuccessful();
  }finally{d.endTransaction();}
 }
 public void archiveProduct(long id){ContentValues v=new ContentValues();v.put("active",0);getWritableDatabase().update("products",v,"id=?",new String[]{""+id});}
 public void adjust(long id,int delta,String reason){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
  d.execSQL("UPDATE products SET stock=stock+? WHERE id=? AND stock+?>=0",new Object[]{delta,id,delta});
  long changed=android.database.DatabaseUtils.longForQuery(d,"SELECT changes()",null);if(changed!=1)throw new IllegalStateException("Existencias insuficientes");movement(d,id,delta>=0?"ENTRADA":"SALIDA",delta,reason);d.setTransactionSuccessful();
 }finally{d.endTransaction();}}
 private void movement(SQLiteDatabase d,long pid,String type,int qty,String ref){ContentValues v=new ContentValues();v.put("product_id",pid);v.put("type",type);v.put("quantity",qty);v.put("reference",ref);d.insertOrThrow("stock_movements",null,v);}
 public long checkout(Map<Long,Integer> cart,String method,String notes){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
  long total=0;Map<Long,long[]> rows=new LinkedHashMap<>();for(Map.Entry<Long,Integer> e:cart.entrySet()){
   try(Cursor c=d.rawQuery("SELECT price_cents,cost_cents,stock,kind FROM products WHERE id=? AND active=1",new String[]{""+e.getKey()})){
    if(!c.moveToFirst()||c.getInt(2)<e.getValue()||!"PRODUCT".equals(c.getString(3)))throw new IllegalStateException("Stock insuficiente o producto no válido");
    rows.put(e.getKey(),new long[]{c.getLong(0),c.getLong(1)});total=Math.addExact(total,Math.multiplyExact(c.getLong(0),e.getValue()));
   }
  }
  ContentValues sale=new ContentValues();sale.put("total_cents",total);sale.put("payment_method",method);sale.put("notes",notes);long sid=d.insertOrThrow("sales",null,sale);
  for(Map.Entry<Long,Integer> e:cart.entrySet()){
   ContentValues item=new ContentValues();item.put("sale_id",sid);item.put("product_id",e.getKey());item.put("quantity",e.getValue());item.put("unit_price_cents",rows.get(e.getKey())[0]);item.put("unit_cost_cents",rows.get(e.getKey())[1]);d.insertOrThrow("sale_items",null,item);
   d.execSQL("UPDATE products SET stock=stock-? WHERE id=?",new Object[]{e.getValue(),e.getKey()});movement(d,e.getKey(),"VENTA",-e.getValue(),"Venta #"+sid);
  }
  d.setTransactionSuccessful();return sid;
 }finally{d.endTransaction();}}
 public void saveTag(long id,String name,String color,String description){ContentValues v=new ContentValues();v.put("name",name);v.put("color",color);v.put("description",description);if(id==0)getWritableDatabase().insertOrThrow("tags",null,v);else getWritableDatabase().update("tags",v,"id=?",new String[]{""+id});}
 public void deleteTag(long id){getWritableDatabase().delete("tags","id=?",new String[]{""+id});}
 public void setTag(long pid,long tid,boolean enabled){if(enabled){ContentValues v=new ContentValues();v.put("product_id",pid);v.put("tag_id",tid);getWritableDatabase().insertWithOnConflict("product_tags",null,v,SQLiteDatabase.CONFLICT_IGNORE);}else getWritableDatabase().delete("product_tags","product_id=? AND tag_id=?",new String[]{""+pid,""+tid});}
 public boolean hasTag(long pid,long tid){try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM product_tags WHERE product_id=? AND tag_id=?",new String[]{""+pid,""+tid})){return c.moveToFirst();}}
 public void saveAsset(long id,String code,String name,String location,long value,String status){ContentValues v=new ContentValues();v.put("code",code);v.put("name",name);v.put("location",location);v.put("value_cents",value);v.put("status",status);if(id==0)getWritableDatabase().insertOrThrow("assets",null,v);else getWritableDatabase().update("assets",v,"id=?",new String[]{""+id});}
 public void deleteAsset(long id){getWritableDatabase().delete("assets","id=?",new String[]{""+id});}
 public long scalar(String sql,String...args){return android.database.DatabaseUtils.longForQuery(getReadableDatabase(),sql,args);}
}
