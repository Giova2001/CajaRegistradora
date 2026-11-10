package com.tiendalocal.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;
import java.util.Map;

public class StoreDb extends SQLiteOpenHelper {
    public static final String STORE_NAME = "Tienda El Tobogan";

    public StoreDb(Context c) {
        super(c, "mysql.db", null, 2);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase d) {
        String[] sql = {
                "CREATE TABLE categories(id INTEGER PRIMARY KEY,name TEXT NOT NULL UNIQUE)",
                "CREATE TABLE products(id INTEGER PRIMARY KEY,barcode TEXT NOT NULL UNIQUE,name TEXT NOT NULL,category_id INTEGER REFERENCES categories(id),price_cents INTEGER NOT NULL CHECK(price_cents>=0),cost_cents INTEGER NOT NULL DEFAULT 0,stock INTEGER NOT NULL DEFAULT 0 CHECK(stock>=0),min_stock INTEGER NOT NULL DEFAULT 0,expiration TEXT,kind TEXT NOT NULL DEFAULT 'PRODUCT',active INTEGER NOT NULL DEFAULT 1,description TEXT,alert_days INTEGER NOT NULL DEFAULT 30)",
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
                "CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT)"
        };
        for (String s : sql) d.execSQL(s);
        d.execSQL("INSERT INTO categories(name) VALUES ('General'),('Relojes'),('Alimentos'),('Mobiliario')");
        d.execSQL("INSERT INTO settings(key,value) VALUES ('store_name','" + STORE_NAME + "'),('payables_cents','0')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase d, int old, int next) {
        if (old < 2) {
            d.execSQL("ALTER TABLE products ADD COLUMN description TEXT");
            d.execSQL("ALTER TABLE products ADD COLUMN alert_days INTEGER NOT NULL DEFAULT 30");
            d.execSQL("INSERT OR IGNORE INTO settings(key,value) VALUES ('store_name','" + STORE_NAME + "'),('payables_cents','0')");
        }
    }

    public Cursor products() {
        return getReadableDatabase().rawQuery(
                "SELECT p.id,p.barcode,p.name,p.price_cents,p.cost_cents,p.stock,p.min_stock,p.expiration,p.kind,p.category_id,COALESCE(p.description,''),COALESCE(p.alert_days,30) FROM products p WHERE p.active=1 ORDER BY p.name",
                null);
    }

    public Cursor sales() {
        return getReadableDatabase().rawQuery("SELECT id,created_at,total_cents,payment_method FROM sales ORDER BY id DESC", null);
    }

    public Cursor tags() {
        return getReadableDatabase().rawQuery("SELECT id,name,color,description FROM tags ORDER BY name", null);
    }

    public Cursor assets() {
        return getReadableDatabase().rawQuery("SELECT id,code,name,location,value_cents,status FROM assets ORDER BY name", null);
    }

    public Cursor movements(long id) {
        return getReadableDatabase().rawQuery(
                "SELECT type,quantity,reference,created_at FROM stock_movements WHERE product_id=? ORDER BY id DESC",
                new String[]{"" + id});
    }

    public Cursor lots(long productId) {
        return getReadableDatabase().rawQuery(
                "SELECT id,COALESCE(code,''),COALESCE(expires_on,''),quantity FROM lots WHERE product_id=? ORDER BY CASE WHEN expires_on IS NULL OR expires_on='' THEN 1 ELSE 0 END, expires_on, id",
                new String[]{"" + productId});
    }

    public void saveProduct(long id, String barcode, String name, long price, long cost, int stock, int min, String expiration, String kind, String description, int alertDays) {
        SQLiteDatabase d = getWritableDatabase();
        d.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("barcode", barcode);
            v.put("name", name);
            v.put("price_cents", price);
            v.put("cost_cents", cost);
            v.put("min_stock", min);
            v.put("expiration", expiration == null || expiration.isEmpty() ? null : expiration);
            v.put("kind", kind);
            v.put("description", description == null || description.isEmpty() ? null : description);
            v.put("alert_days", Math.max(0, alertDays));
            if (id == 0) {
                v.put("stock", stock);
                long pid = d.insertOrThrow("products", null, v);
                movement(d, pid, "ENTRADA", stock, "Stock inicial");
            } else {
                int previous = 0;
                try (Cursor c = d.rawQuery("SELECT stock FROM products WHERE id=?", new String[]{"" + id})) {
                    if (c.moveToFirst()) previous = c.getInt(0);
                }
                v.put("stock", stock);
                d.update("products", v, "id=?", new String[]{"" + id});
                if (stock != previous) movement(d, id, "AJUSTE", stock - previous, "Edición de existencias");
            }
            d.setTransactionSuccessful();
        } finally {
            d.endTransaction();
        }
    }

    public int importProducts(List<String[]> rows) {
        SQLiteDatabase d = getWritableDatabase();
        d.beginTransaction();
        try {
            int line = 1;
            for (String[] a : rows) {
                line++;
                if (a.length < 8) throw new IllegalArgumentException("Fila incompleta en la línea " + line);
                String barcode = a[0].trim();
                String name = a[1].trim();
                if (barcode.isEmpty() || name.isEmpty()) {
                    throw new IllegalArgumentException("Código o nombre vacío en la línea " + line);
                }
                long price;
                long cost;
                int stock;
                int min;
                try {
                    price = Long.parseLong(a[2].trim());
                    cost = Long.parseLong(a[3].trim());
                    stock = Integer.parseInt(a[4].trim());
                    min = Integer.parseInt(a[5].trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Número inválido en la línea " + line);
                }
                String exp = a[6].trim();
                String kind = a[7].trim().isEmpty() ? "PRODUCT" : a[7].trim();
                if (price < 0 || cost < 0 || stock < 0 || min < 0) {
                    throw new IllegalArgumentException("Hay valores negativos en la línea " + line);
                }
                long id = 0;
                try (Cursor c = d.rawQuery("SELECT id FROM products WHERE barcode=?", new String[]{barcode})) {
                    if (c.moveToFirst()) id = c.getLong(0);
                }
                ContentValues v = new ContentValues();
                v.put("barcode", barcode);
                v.put("name", name);
                v.put("price_cents", price);
                v.put("cost_cents", cost);
                v.put("stock", stock);
                v.put("min_stock", min);
                v.put("expiration", exp.isEmpty() ? null : exp);
                v.put("kind", kind);
                v.put("active", 1);
                if (id == 0) {
                    long pid = d.insertOrThrow("products", null, v);
                    movement(d, pid, "ENTRADA", stock, "Importación CSV");
                } else {
                    int previous = 0;
                    try (Cursor c = d.rawQuery("SELECT stock FROM products WHERE id=?", new String[]{"" + id})) {
                        if (c.moveToFirst()) previous = c.getInt(0);
                    }
                    d.update("products", v, "id=?", new String[]{"" + id});
                    if (stock != previous) movement(d, id, "AJUSTE", stock - previous, "Importación CSV");
                }
            }
            ContentValues op = new ContentValues();
            op.put("operation", "import");
            op.put("format", "csv");
            op.put("records", rows.size());
            d.insert("data_operations", null, op);
            d.setTransactionSuccessful();
            return rows.size();
        } finally {
            d.endTransaction();
        }
    }

    public void archiveProduct(long id) {
        ContentValues v = new ContentValues();
        v.put("active", 0);
        getWritableDatabase().update("products", v, "id=?", new String[]{"" + id});
    }

    public void adjust(long id, int delta, String reason) {
        SQLiteDatabase d = getWritableDatabase();
        d.beginTransaction();
        try {
            d.execSQL("UPDATE products SET stock=stock+? WHERE id=? AND stock+?>=0", new Object[]{delta, id, delta});
            long changed = android.database.DatabaseUtils.longForQuery(d, "SELECT changes()", null);
            if (changed != 1) throw new IllegalStateException("Existencias insuficientes");
            movement(d, id, delta >= 0 ? "ENTRADA" : "SALIDA", delta, reason);
            d.setTransactionSuccessful();
        } finally {
            d.endTransaction();
        }
    }

    private void movement(SQLiteDatabase d, long pid, String type, int qty, String ref) {
        ContentValues v = new ContentValues();
        v.put("product_id", pid);
        v.put("type", type);
        v.put("quantity", qty);
        v.put("reference", ref);
        d.insertOrThrow("stock_movements", null, v);
    }

    public long checkout(Map<Long, Integer> cart, String method, String notes) {
        SQLiteDatabase d = getWritableDatabase();
        d.beginTransaction();
        try {
            long total = 0;
            java.util.Map<Long, long[]> rows = new java.util.LinkedHashMap<>();
            for (Map.Entry<Long, Integer> e : cart.entrySet()) {
                try (Cursor c = d.rawQuery("SELECT price_cents,cost_cents,stock,kind FROM products WHERE id=? AND active=1", new String[]{"" + e.getKey()})) {
                    if (!c.moveToFirst() || c.getInt(2) < e.getValue() || !"PRODUCT".equals(c.getString(3))) {
                        throw new IllegalStateException("Stock insuficiente o producto no válido");
                    }
                    rows.put(e.getKey(), new long[]{c.getLong(0), c.getLong(1)});
                    total = Math.addExact(total, Math.multiplyExact(c.getLong(0), e.getValue()));
                }
            }
            ContentValues sale = new ContentValues();
            sale.put("total_cents", total);
            sale.put("payment_method", method);
            sale.put("notes", notes);
            long sid = d.insertOrThrow("sales", null, sale);
            for (Map.Entry<Long, Integer> e : cart.entrySet()) {
                ContentValues item = new ContentValues();
                item.put("sale_id", sid);
                item.put("product_id", e.getKey());
                item.put("quantity", e.getValue());
                item.put("unit_price_cents", rows.get(e.getKey())[0]);
                item.put("unit_cost_cents", rows.get(e.getKey())[1]);
                d.insertOrThrow("sale_items", null, item);
                d.execSQL("UPDATE products SET stock=stock-? WHERE id=?", new Object[]{e.getValue(), e.getKey()});
                consumeLots(d, e.getKey(), e.getValue());
                movement(d, e.getKey(), "VENTA", -e.getValue(), "Venta #" + sid);
            }
            d.setTransactionSuccessful();
            return sid;
        } finally {
            d.endTransaction();
        }
    }

    private void consumeLots(SQLiteDatabase d, long productId, int qty) {
        try (Cursor c = d.rawQuery(
                "SELECT id, quantity FROM lots WHERE product_id=? AND quantity>0 ORDER BY CASE WHEN expires_on IS NULL OR expires_on='' THEN 1 ELSE 0 END, expires_on, id",
                new String[]{"" + productId})) {
            while (qty > 0 && c.moveToNext()) {
                int have = c.getInt(1);
                int take = Math.min(have, qty);
                d.execSQL("UPDATE lots SET quantity=quantity-? WHERE id=?", new Object[]{take, c.getLong(0)});
                qty -= take;
            }
        }
    }

    public void saveLot(long id, long productId, String code, String expires, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("La cantidad del lote no puede ser negativa");
        SQLiteDatabase d = getWritableDatabase();
        d.beginTransaction();
        try {
            int previous = 0;
            if (id > 0) {
                try (Cursor c = d.rawQuery("SELECT quantity, product_id FROM lots WHERE id=?", new String[]{"" + id})) {
                    if (!c.moveToFirst()) throw new IllegalArgumentException("El lote ya no existe");
                    previous = c.getInt(0);
                    productId = c.getLong(1);
                }
            }
            ContentValues v = new ContentValues();
            v.put("product_id", productId);
            v.put("code", code == null || code.isEmpty() ? null : code);
            v.put("expires_on", expires == null || expires.isEmpty() ? null : expires);
            v.put("quantity", quantity);
            if (id == 0) d.insertOrThrow("lots", null, v);
            else d.update("lots", v, "id=?", new String[]{"" + id});
            int delta = quantity - previous;
            if (delta != 0) {
                d.execSQL("UPDATE products SET stock=stock+? WHERE id=? AND stock+?>=0", new Object[]{delta, productId, delta});
                long changed = android.database.DatabaseUtils.longForQuery(d, "SELECT changes()", null);
                if (changed != 1) throw new IllegalStateException("El ajuste del lote dejaría el stock en negativo");
                movement(d, productId, delta >= 0 ? "ENTRADA" : "SALIDA", delta, "Lote " + (expires == null || expires.isEmpty() ? code : expires));
            }
            d.setTransactionSuccessful();
        } finally {
            d.endTransaction();
        }
    }

    public void deleteLot(long id) {
        SQLiteDatabase d = getWritableDatabase();
        d.beginTransaction();
        try {
            long productId;
            int qty;
            try (Cursor c = d.rawQuery("SELECT product_id, quantity FROM lots WHERE id=?", new String[]{"" + id})) {
                if (!c.moveToFirst()) {
                    d.setTransactionSuccessful();
                    return;
                }
                productId = c.getLong(0);
                qty = c.getInt(1);
            }
            if (qty > 0) {
                d.execSQL("UPDATE products SET stock=stock-? WHERE id=? AND stock-?>=0", new Object[]{qty, productId, qty});
                long changed = android.database.DatabaseUtils.longForQuery(d, "SELECT changes()", null);
                if (changed != 1) throw new IllegalStateException("No se puede quitar el lote porque el stock quedaría negativo");
                movement(d, productId, "SALIDA", -qty, "Eliminación de lote");
            }
            d.delete("lots", "id=?", new String[]{"" + id});
            d.setTransactionSuccessful();
        } finally {
            d.endTransaction();
        }
    }

    public void saveTag(long id, String name, String color, String description) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("color", color);
        v.put("description", description);
        if (id == 0) getWritableDatabase().insertOrThrow("tags", null, v);
        else getWritableDatabase().update("tags", v, "id=?", new String[]{"" + id});
    }

    public void deleteTag(long id) {
        getWritableDatabase().delete("tags", "id=?", new String[]{"" + id});
    }

    public void setTag(long pid, long tid, boolean enabled) {
        if (enabled) {
            ContentValues v = new ContentValues();
            v.put("product_id", pid);
            v.put("tag_id", tid);
            getWritableDatabase().insertWithOnConflict("product_tags", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        } else {
            getWritableDatabase().delete("product_tags", "product_id=? AND tag_id=?", new String[]{"" + pid, "" + tid});
        }
    }

    public boolean hasTag(long pid, long tid) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM product_tags WHERE product_id=? AND tag_id=?", new String[]{"" + pid, "" + tid})) {
            return c.moveToFirst();
        }
    }

    public void saveAsset(long id, String code, String name, String location, long value, String status) {
        ContentValues v = new ContentValues();
        v.put("code", code);
        v.put("name", name);
        v.put("location", location);
        v.put("value_cents", value);
        v.put("status", status);
        if (id == 0) getWritableDatabase().insertOrThrow("assets", null, v);
        else getWritableDatabase().update("assets", v, "id=?", new String[]{"" + id});
    }

    public void deleteAsset(long id) {
        getWritableDatabase().delete("assets", "id=?", new String[]{"" + id});
    }

    public void addExpense(String description, long amount, String date) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del gasto es obligatoria");
        }
        if (amount < 0) throw new IllegalArgumentException("El monto no puede ser negativo");
        ContentValues v = new ContentValues();
        v.put("description", description.trim());
        v.put("amount_cents", amount);
        v.put("date", date);
        getWritableDatabase().insertOrThrow("expenses", null, v);
    }

    public String setting(String key, String def) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT value FROM settings WHERE key=?", new String[]{key})) {
            if (c.moveToFirst() && c.getString(0) != null) return c.getString(0);
        }
        return def;
    }

    public void setSetting(String key, String value) {
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("value", value);
        getWritableDatabase().insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long payables() {
        try {
            return Long.parseLong(setting("payables_cents", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public long scalar(String sql, String... args) {
        return android.database.DatabaseUtils.longForQuery(getReadableDatabase(), sql, args);
    }
}
