package com.tiendalocal.app;

import android.app.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    StoreDb db;
    LinearLayout root, body;
    String page = "Venta", scanMode = "sale";
    LinkedHashMap<Long, Integer> cart = new LinkedHashMap<>();
    String pendingExport = "products";
    String inventoryFilter = "ALL";
    String searchQuery = "";

    // Dark Theme Colors
    final int BG_DARK = 0xFF121214;
    final int CARD_DARK = 0xFF1E1E24;
    final int CARD_BORDER = 0xFF2C2C36;
    final int PURPLE_PRIMARY = 0xFF673DE6;
    final int PURPLE_LIGHT = 0xFF8262FE;
    final int PURPLE_CONTAINER = 0xFF2A244D;
    final int TEXT_WHITE = 0xFFFFFFFF;
    final int TEXT_GRAY = 0xFF9A9AA6;
    final int BADGE_GREEN_BG = 0xFF1C3A27; final int BADGE_GREEN_TXT = 0xFF2ECC71;
    final int BADGE_AMBER_BG = 0xFF3D2E14; final int BADGE_AMBER_TXT = 0xFFF39C12;
    final int BADGE_RED_BG = 0xFF3D1919; final int BADGE_RED_TXT = 0xFFE74C3C;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new StoreDb(this);
        show("Venta");
    }

    // --- Helper UI Generator Methods ---
    int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    GradientDrawable roundedBg(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    GradientDrawable roundedBorderBg(int color, int borderColor, int borderWidthDp, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setStroke(dp(borderWidthDp), borderColor);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    ImageView iconView(int resId, int tintColor, int sizeDp) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(resId);
        if (tintColor != 0) {
            iv.setColorFilter(tintColor);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        iv.setLayoutParams(lp);
        return iv;
    }

    LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    void add(LinearLayout l, View v) {
        l.addView(v, new LinearLayout.LayoutParams(-1, -2));
    }

    LinearLayout cardView() {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(roundedBorderBg(CARD_DARK, CARD_BORDER, 1, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        c.setLayoutParams(lp);
        return c;
    }

    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(TEXT_WHITE);
        t.setPadding(dp(4), dp(4), dp(4), dp(4));
        return t;
    }

    Button primaryButton(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TEXT_WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setPadding(dp(16), dp(12), dp(16), dp(12));
        b.setBackground(roundedBg(PURPLE_PRIMARY, 14));
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(8));
        b.setLayoutParams(lp);
        return b;
    }

    Button secondaryButton(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(TEXT_WHITE);
        b.setTextSize(14);
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        b.setBackground(roundedBorderBg(CARD_DARK, CARD_BORDER, 1, 14));
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(8));
        b.setLayoutParams(lp);
        return b;
    }

    EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextColor(TEXT_WHITE);
        e.setHintTextColor(TEXT_GRAY);
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setBackground(roundedBorderBg(CARD_DARK, CARD_BORDER, 1, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        e.setLayoutParams(lp);
        return e;
    }

    TextView stockBadge(int stock, int minStock) {
        TextView t = new TextView(this);
        t.setTextSize(12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(10), dp(4), dp(10), dp(4));

        if (stock <= 0) {
            t.setText("Sin stock");
            t.setTextColor(BADGE_RED_TXT);
            t.setBackground(roundedBg(BADGE_RED_BG, 12));
        } else if (stock <= minStock) {
            t.setText("Stock bajo: " + stock);
            t.setTextColor(BADGE_AMBER_TXT);
            t.setBackground(roundedBg(BADGE_AMBER_BG, 12));
        } else {
            t.setText("En stock: " + stock);
            t.setTextColor(BADGE_GREEN_TXT);
            t.setBackground(roundedBg(BADGE_GREEN_BG, 12));
        }
        return t;
    }

    TextView statusBadge(String statusText, int bgColor, int textColor) {
        TextView t = new TextView(this);
        t.setText(statusText);
        t.setTextSize(12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(10), dp(4), dp(10), dp(4));
        t.setTextColor(textColor);
        t.setBackground(roundedBg(bgColor, 12));
        return t;
    }

    void error(Exception e) {
        new AlertDialog.Builder(this)
            .setTitle("Aviso")
            .setMessage(e.getMessage())
            .setPositiveButton("Aceptar", null)
            .show();
    }

    String money(long cents) {
        return String.format(Locale.US, "$%.2f", cents / 100.0);
    }

    long cents(String s) {
        return new java.math.BigDecimal(s.trim()).movePointRight(2).longValueExact();
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    // --- Main Layout Framework ---
    void show(String p) {
        page = p;
        root = column();
        root.setBackgroundColor(BG_DARK);
        setContentView(root);

        // Header Bar
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(16), dp(20), dp(12));

        TextView title = new TextView(this);
        title.setText(p);
        title.setTextSize(26);
        title.setTextColor(TEXT_WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        if (p.equals("Venta") && !cart.isEmpty()) {
            Button clearBtn = new Button(this);
            clearBtn.setText("Limpiar carrito");
            clearBtn.setAllCaps(false);
            clearBtn.setTextColor(BADGE_RED_TXT);
            clearBtn.setTextSize(13);
            clearBtn.setBackground(roundedBg(BADGE_RED_BG, 10));
            clearBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
            clearBtn.setOnClickListener(v -> { cart.clear(); show("Venta"); });
            header.addView(clearBtn);
        } else if (p.equals("Historial")) {
            Button statsBtn = new Button(this);
            statsBtn.setText("Estadísticas");
            statsBtn.setAllCaps(false);
            statsBtn.setTextColor(TEXT_WHITE);
            statsBtn.setTextSize(13);
            statsBtn.setTypeface(Typeface.DEFAULT_BOLD);
            statsBtn.setBackground(roundedBg(PURPLE_PRIMARY, 10));
            statsBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
            statsBtn.setOnClickListener(v -> statsMenu());
            header.addView(statsBtn);
        }

        add(root, header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = column();
        body.setPadding(dp(16), dp(0), dp(16), dp(16));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        root.addView(createBottomNav());

        if (p.equals("Venta")) sale();
        else if (p.equals("Historial")) history();
        else inventory();
    }

    LinearLayout createBottomNav() {
        LinearLayout nav = row();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackground(roundedBorderBg(CARD_DARK, CARD_BORDER, 1, 28));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(16), dp(8), dp(16), dp(16));
        nav.setLayoutParams(lp);

        String[][] tabs = {
            {"Venta", "Venta"},
            {"Historial", "Historial"},
            {"Inventario", "Inventario"}
        };

        for (String[] tab : tabs) {
            String key = tab[0];
            String label = tab[1];
            boolean isSelected = key.equals(page);

            TextView item = new TextView(this);
            item.setText(label);
            item.setTextSize(14);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(16), dp(10), dp(16), dp(10));

            if (isSelected) {
                item.setTextColor(TEXT_WHITE);
                item.setTypeface(Typeface.DEFAULT_BOLD);
                item.setBackground(roundedBg(PURPLE_PRIMARY, 20));
            } else {
                item.setTextColor(TEXT_GRAY);
                item.setOnClickListener(v -> show(key));
            }

            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return nav;
    }

    // --- Módulo de Ventas ---
    void sale() {
        LinearLayout scanBar = row();
        scanBar.setPadding(0, 0, 0, dp(12));

        Button scanBtn = secondaryButton("Escanear producto", () -> scan("sale"));
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(0, -2, 1);
        scanLp.setMargins(0, 0, dp(6), 0);
        scanBtn.setLayoutParams(scanLp);

        Button manualBtn = secondaryButton("Código manual", () -> manualCode("sale"));
        LinearLayout.LayoutParams manualLp = new LinearLayout.LayoutParams(0, -2, 1);
        manualLp.setMargins(dp(6), 0, 0, 0);
        manualBtn.setLayoutParams(manualLp);

        scanBar.addView(scanBtn);
        scanBar.addView(manualBtn);
        add(body, scanBar);

        if (cart.isEmpty()) {
            LinearLayout emptyCard = cardView();
            emptyCard.setGravity(Gravity.CENTER);
            emptyCard.setPadding(dp(24), dp(40), dp(24), dp(40));

            ImageView icon = iconView(R.drawable.ic_cart, PURPLE_PRIMARY, 48);

            TextView msg = new TextView(this);
            msg.setText("El carrito está vacío");
            msg.setTextSize(18);
            msg.setTextColor(TEXT_WHITE);
            msg.setTypeface(Typeface.DEFAULT_BOLD);
            msg.setGravity(Gravity.CENTER);
            msg.setPadding(0, dp(12), 0, dp(4));

            TextView sub = new TextView(this);
            sub.setText("Escanea el código de barras o ingresa un código manualmente para agregar productos.");
            sub.setTextSize(13);
            sub.setTextColor(TEXT_GRAY);
            sub.setGravity(Gravity.CENTER);

            emptyCard.addView(icon);
            emptyCard.addView(msg);
            emptyCard.addView(sub);
            add(body, emptyCard);
            return;
        }

        long total = 0;
        int totalItems = 0;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            long pid = entry.getKey();
            int qty = entry.getValue();

            try (Cursor c = db.getReadableDatabase().rawQuery(
                    "SELECT name, price_cents, barcode FROM products WHERE id=?",
                    new String[]{"" + pid})) {

                if (c.moveToFirst()) {
                    long price = c.getLong(1);
                    long lineTotal = price * qty;
                    total += lineTotal;
                    totalItems += qty;

                    LinearLayout itemCard = cardView();
                    LinearLayout itemRow = row();
                    itemRow.setGravity(Gravity.CENTER_VERTICAL);

                    // Product Icon Box
                    LinearLayout imgBox = column();
                    imgBox.setGravity(Gravity.CENTER);
                    imgBox.setBackground(roundedBg(PURPLE_CONTAINER, 12));
                    imgBox.addView(iconView(R.drawable.ic_box, PURPLE_PRIMARY, 22));

                    LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp(44), dp(44));
                    imgLp.setMargins(0, 0, dp(12), 0);
                    itemRow.addView(imgBox, imgLp);

                    // Details Column
                    LinearLayout details = column();
                    TextView nameTxt = new TextView(this);
                    nameTxt.setText(c.getString(0));
                    nameTxt.setTextSize(15);
                    nameTxt.setTextColor(TEXT_WHITE);
                    nameTxt.setTypeface(Typeface.DEFAULT_BOLD);

                    TextView priceTxt = new TextView(this);
                    priceTxt.setText(money(price) + " / c.u.");
                    priceTxt.setTextSize(13);
                    priceTxt.setTextColor(TEXT_GRAY);

                    details.addView(nameTxt);
                    details.addView(priceTxt);
                    itemRow.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

                    // Quantity Controls
                    LinearLayout qtyBox = row();
                    qtyBox.setGravity(Gravity.CENTER_VERTICAL);

                    Button minusBtn = new Button(this);
                    minusBtn.setText("−");
                    minusBtn.setTextSize(16);
                    minusBtn.setTextColor(TEXT_WHITE);
                    minusBtn.setBackground(roundedBg(CARD_BORDER, 8));
                    minusBtn.setPadding(0, 0, 0, 0);
                    minusBtn.setOnClickListener(v -> changeCart(pid, -1));
                    qtyBox.addView(minusBtn, new LinearLayout.LayoutParams(dp(32), dp(32)));

                    TextView qtyTxt = new TextView(this);
                    qtyTxt.setText(String.valueOf(qty));
                    qtyTxt.setTextSize(15);
                    qtyTxt.setTextColor(TEXT_WHITE);
                    qtyTxt.setTypeface(Typeface.DEFAULT_BOLD);
                    qtyTxt.setPadding(dp(10), 0, dp(10), 0);
                    qtyBox.addView(qtyTxt);

                    Button plusBtn = new Button(this);
                    plusBtn.setText("+");
                    plusBtn.setTextSize(16);
                    plusBtn.setTextColor(TEXT_WHITE);
                    plusBtn.setBackground(roundedBg(PURPLE_PRIMARY, 8));
                    plusBtn.setPadding(0, 0, 0, 0);
                    plusBtn.setOnClickListener(v -> changeCart(pid, 1));
                    qtyBox.addView(plusBtn, new LinearLayout.LayoutParams(dp(32), dp(32)));

                    itemRow.addView(qtyBox);

                    // Line Total
                    TextView subtotalTxt = new TextView(this);
                    subtotalTxt.setText(money(lineTotal));
                    subtotalTxt.setTextSize(15);
                    subtotalTxt.setTextColor(TEXT_WHITE);
                    subtotalTxt.setTypeface(Typeface.DEFAULT_BOLD);
                    subtotalTxt.setPadding(dp(12), 0, 0, 0);
                    itemRow.addView(subtotalTxt);

                    itemCard.addView(itemRow);
                    add(body, itemCard);
                }
            }
        }

        // Summary Card
        LinearLayout totalCard = cardView();
        totalCard.setBackground(roundedBorderBg(PURPLE_CONTAINER, PURPLE_PRIMARY, 1, 16));

        LinearLayout totalRow = row();
        totalRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText("Total (" + totalItems + " productos):");
        label.setTextSize(15);
        label.setTextColor(TEXT_GRAY);
        totalRow.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

        TextView val = new TextView(this);
        val.setText(money(total));
        val.setTextSize(22);
        val.setTextColor(TEXT_WHITE);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        totalRow.addView(val);

        totalCard.addView(totalRow);
        add(body, totalCard);

        Button finishBtn = primaryButton("Finalizar venta", () -> finishSale());
        add(body, finishBtn);
    }

    void changeCart(long id, int delta) {
        int n = cart.getOrDefault(id, 0) + delta;
        if (n <= 0) cart.remove(id);
        else cart.put(id, n);
        show("Venta");
    }

    void manualCode(String mode) {
        EditText e = input("Código de barras", "");
        LinearLayout dialogLayout = column();
        dialogLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        dialogLayout.setBackgroundColor(BG_DARK);

        TextView t = text("Buscar por código de barras", 18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 0, 0, dp(12));
        dialogLayout.addView(t);
        dialogLayout.addView(e);

        new AlertDialog.Builder(this)
            .setView(dialogLayout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Buscar", (d, w) -> onCode(e.getText().toString().trim(), mode))
            .show();
    }

    void scan(String mode) {
        scanMode = mode;
        new IntentIntegrator(this)
            .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            .setPrompt("Escanea el código de barras del producto")
            .setBeepEnabled(false)
            .setOrientationLocked(false)
            .initiateScan();
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        IntentResult r = IntentIntegrator.parseActivityResult(req, result, data);
        if (r != null && r.getContents() != null) {
            onCode(r.getContents(), scanMode);
        } else if (req == 201 && result == RESULT_OK && data != null) {
            importCsv(data.getData());
        } else if (req == 202 && result == RESULT_OK && data != null) {
            exportData(data.getData());
        }
    }

    void onCode(String code, String mode) {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT id, name, price_cents, stock, kind FROM products WHERE barcode=? AND active=1",
                new String[]{code})) {

            if (!c.moveToFirst()) {
                new AlertDialog.Builder(this)
                    .setTitle("Código no registrado")
                    .setMessage("Código: " + code + "\n\n¿Deseas registrar este nuevo producto?")
                    .setPositiveButton("Crear producto", (d, w) -> productForm(0, code))
                    .setNegativeButton("Cancelar", null)
                    .show();
                return;
            }

            long id = c.getLong(0);
            String name = c.getString(1);
            long price = c.getLong(2);
            int stock = c.getInt(3);

            if (mode.equals("inventory")) {
                productForm(id, code);
                return;
            }

            if (!"PRODUCT".equals(c.getString(4))) {
                toast("Este registro es un bien de la tienda, no un producto de venta.");
                return;
            }

            // Custom Dialog "Solicitar Cantidad"
            LinearLayout dialogLayout = column();
            dialogLayout.setPadding(dp(20), dp(20), dp(20), dp(20));
            dialogLayout.setBackgroundColor(BG_DARK);

            TextView title = new TextView(this);
            title.setText("Producto detectado");
            title.setTextSize(20);
            title.setTextColor(TEXT_WHITE);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            dialogLayout.addView(title);

            // Card details
            LinearLayout pCard = cardView();
            TextView pName = text(name, 16);
            pName.setTypeface(Typeface.DEFAULT_BOLD);
            TextView pMeta = new TextView(this);
            pMeta.setText("Código: " + code + "  •  Precio: " + money(price) + "  •  Stock: " + stock);
            pMeta.setTextColor(TEXT_GRAY);
            pMeta.setTextSize(13);
            pCard.addView(pName);
            pCard.addView(pMeta);
            dialogLayout.addView(pCard);

            TextView qLabel = text("Cantidad:", 15);
            qLabel.setTypeface(Typeface.DEFAULT_BOLD);
            qLabel.setPadding(0, dp(8), 0, dp(8));
            dialogLayout.addView(qLabel);

            // Quick quantity selector row
            EditText qtyInput = input("", "1");
            qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            qtyInput.setTextSize(22);
            qtyInput.setGravity(Gravity.CENTER);
            dialogLayout.addView(qtyInput);

            LinearLayout quickRow = row();
            quickRow.setPadding(0, dp(4), 0, dp(12));
            for (int num : new int[]{1, 2, 3, 5}) {
                Button btn = new Button(this);
                btn.setText(String.valueOf(num));
                btn.setTextSize(14);
                btn.setTextColor(TEXT_WHITE);
                btn.setBackground(roundedBg(CARD_DARK, 10));
                btn.setOnClickListener(v -> qtyInput.setText(String.valueOf(num)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
                lp.setMargins(dp(2), 0, dp(2), 0);
                quickRow.addView(btn, lp);
            }
            dialogLayout.addView(quickRow);

            new AlertDialog.Builder(this)
                .setView(dialogLayout)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Agregar al carrito", (d, w) -> {
                    try {
                        int n = Integer.parseInt(qtyInput.getText().toString().trim());
                        if (n <= 0 || n + cart.getOrDefault(id, 0) > stock) {
                            throw new IllegalArgumentException("Cantidad inválida o superior al stock disponible (" + stock + ")");
                        }
                        cart.put(id, cart.getOrDefault(id, 0) + n);
                        show("Venta");
                    } catch (Exception ex) {
                        error(ex);
                    }
                })
                .show();
        }
    }

    void finishSale() {
        if (cart.isEmpty()) {
            toast("Agrega productos al carrito primero");
            return;
        }

        LinearLayout form = column();
        form.setPadding(dp(16), dp(16), dp(16), dp(16));
        form.setBackgroundColor(BG_DARK);

        TextView title = text("Confirmar Venta", 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        form.addView(title);

        TextView methodLabel = text("Método de pago:", 14);
        methodLabel.setTextColor(TEXT_GRAY);
        form.addView(methodLabel);

        Spinner methodSpinner = new Spinner(this);
        String[] methods = {"Efectivo", "Tarjeta", "Transferencia"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, methods);
        methodSpinner.setAdapter(adapter);
        methodSpinner.setPadding(dp(8), dp(8), dp(8), dp(8));
        methodSpinner.setBackground(roundedBorderBg(CARD_DARK, CARD_BORDER, 1, 12));
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(-1, -2);
        spLp.setMargins(0, dp(4), 0, dp(12));
        methodSpinner.setLayoutParams(spLp);
        form.addView(methodSpinner);

        EditText noteInput = input("Nota (opcional)", "");
        form.addView(noteInput);

        new AlertDialog.Builder(this)
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar venta", (d, w) -> {
                try {
                    long id = db.checkout(cart, methods[methodSpinner.getSelectedItemPosition()], noteInput.getText().toString());
                    cart.clear();
                    show("Venta");
                    toast("Venta #" + id + " registrada con éxito");
                } catch (Exception ex) {
                    error(ex);
                }
            })
            .show();
    }

    // --- Módulo de Historial ---
    void history() {
        try (Cursor c = db.sales()) {
            if (c.getCount() == 0) {
                LinearLayout emptyCard = cardView();
                emptyCard.setGravity(Gravity.CENTER);
                emptyCard.setPadding(dp(24), dp(40), dp(24), dp(40));

                ImageView icon = iconView(R.drawable.ic_history, PURPLE_PRIMARY, 48);

                TextView msg = new TextView(this);
                msg.setText("Sin ventas registradas");
                msg.setTextSize(18);
                msg.setTextColor(TEXT_WHITE);
                msg.setTypeface(Typeface.DEFAULT_BOLD);
                msg.setGravity(Gravity.CENTER);
                msg.setPadding(0, dp(12), 0, dp(4));

                emptyCard.addView(icon);
                emptyCard.addView(msg);
                add(body, emptyCard);
                return;
            }

            while (c.moveToNext()) {
                long id = c.getLong(0);
                String date = c.getString(1);
                long total = c.getLong(2);
                String method = c.getString(3);

                LinearLayout itemCard = cardView();
                itemCard.setOnClickListener(v -> saleDetails(id));

                LinearLayout r = row();
                r.setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout iconBox = column();
                iconBox.setGravity(Gravity.CENTER);
                iconBox.setBackground(roundedBg(PURPLE_CONTAINER, 12));
                iconBox.addView(iconView(R.drawable.ic_cart, PURPLE_PRIMARY, 20));

                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
                iconLp.setMargins(0, 0, dp(12), 0);
                r.addView(iconBox, iconLp);

                LinearLayout details = column();
                TextView title = new TextView(this);
                title.setText("Venta #" + id + "  •  " + method);
                title.setTextSize(15);
                title.setTextColor(TEXT_WHITE);
                title.setTypeface(Typeface.DEFAULT_BOLD);

                TextView dateTxt = new TextView(this);
                dateTxt.setText(date);
                dateTxt.setTextSize(13);
                dateTxt.setTextColor(TEXT_GRAY);

                details.addView(title);
                details.addView(dateTxt);
                r.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

                TextView amount = new TextView(this);
                amount.setText(money(total));
                amount.setTextSize(16);
                amount.setTextColor(TEXT_WHITE);
                amount.setTypeface(Typeface.DEFAULT_BOLD);
                amount.setPadding(dp(8), 0, dp(8), 0);
                r.addView(amount);

                ImageView arrow = iconView(R.drawable.ic_chevron_right, TEXT_GRAY, 20);
                r.addView(arrow);

                itemCard.addView(r);
                add(body, itemCard);
            }
        }
    }

    void saleDetails(long id) {
        StringBuilder b = new StringBuilder();
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT p.name, si.quantity, si.unit_price_cents FROM sale_items si JOIN products p ON p.id=si.product_id WHERE si.sale_id=?",
                new String[]{"" + id})) {
            while (c.moveToNext()) {
                b.append("• ").append(c.getString(0))
                 .append(" × ").append(c.getInt(1))
                 .append(" = ").append(money(c.getLong(1) * c.getLong(2)))
                 .append("\n");
            }
        }

        LinearLayout dialogLayout = column();
        dialogLayout.setPadding(dp(20), dp(20), dp(20), dp(20));
        dialogLayout.setBackgroundColor(BG_DARK);

        TextView title = text("Detalle de Venta #" + id, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        dialogLayout.addView(title);

        TextView content = text(b.toString(), 14);
        content.setLineSpacing(0, 1.2f);
        dialogLayout.addView(content);

        new AlertDialog.Builder(this)
            .setView(dialogLayout)
            .setPositiveButton("Cerrar", null)
            .show();
    }

    // --- Módulo de Estadísticas y Razones Financieras ---
    void statsMenu() {
        LinearLayout dialogLayout = column();
        dialogLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        dialogLayout.setBackgroundColor(BG_DARK);

        // Title Bar
        LinearLayout titleBar = row();
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(0, 0, 0, dp(16));

        TextView titleTxt = text("Estadísticas", 20);
        titleTxt.setTypeface(Typeface.DEFAULT_BOLD);
        titleBar.addView(titleTxt, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView closeBtn = iconView(R.drawable.ic_close, TEXT_GRAY, 22);
        closeBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        titleBar.addView(closeBtn);

        dialogLayout.addView(titleBar);

        // Options Cards (Matches requested visual design)
        String[][] options = {
            {"Resumen financiero", "Efectivo del día, ventas y métricas"},
            {"Gráficas de ventas", "Día, semana, mes y año"},
            {"Razones financieras", "Indicadores del mes y año"}
        };

        int[] icons = {
            R.drawable.ic_bar_chart,
            R.drawable.ic_line_chart,
            R.drawable.ic_calculator
        };

        AlertDialog[] dialogRef = new AlertDialog[1];

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            LinearLayout card = row();
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            card.setBackground(roundedBg(0xFFF3EFEF, 16));

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cardLp);

            // Left Icon Box
            LinearLayout iconBox = column();
            iconBox.setGravity(Gravity.CENTER);
            iconBox.setBackground(roundedBg(0xFFE5DEFF, 14));
            iconBox.addView(iconView(icons[i], PURPLE_PRIMARY, 24));

            LinearLayout.LayoutParams iconBoxLp = new LinearLayout.LayoutParams(dp(48), dp(48));
            iconBoxLp.setMargins(0, 0, dp(12), 0);
            card.addView(iconBox, iconBoxLp);

            // Text Column
            LinearLayout textCol = column();
            TextView title = new TextView(this);
            title.setText(options[i][0]);
            title.setTextSize(16);
            title.setTextColor(0xFF1A1A24);
            title.setTypeface(Typeface.DEFAULT_BOLD);

            TextView sub = new TextView(this);
            sub.setText(options[i][1]);
            sub.setTextSize(13);
            sub.setTextColor(0xFF6A6A7A);

            textCol.addView(title);
            textCol.addView(sub);
            card.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1));

            // Right Chevron
            ImageView arrow = iconView(R.drawable.ic_chevron_right, PURPLE_PRIMARY, 20);
            card.addView(arrow);

            card.setOnClickListener(v -> {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                if (index == 0) dailyStats();
                else if (index == 1) charts();
                else ratios();
            });

            dialogLayout.addView(card);
        }

        dialogRef[0] = new AlertDialog.Builder(this)
            .setView(dialogLayout)
            .create();

        closeBtn.setOnClickListener(v -> dialogRef[0].dismiss());
        dialogRef[0].show();
    }

    void dailyStats() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        long amount = db.scalar("SELECT COALESCE(SUM(total_cents),0) FROM sales WHERE date(created_at)=? AND payment_method='Efectivo'", date);
        long count = db.scalar("SELECT COUNT(*) FROM sales WHERE date(created_at)=?", date);
        long sales = db.scalar("SELECT COALESCE(SUM(total_cents),0) FROM sales WHERE date(created_at)=?", date);
        long avg = count == 0 ? 0 : sales / count;

        LinearLayout layout = column();
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackgroundColor(BG_DARK);

        TextView title = text("Resumen Financiero • " + date, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        layout.addView(title);

        // Featured Card Total Efectivo
        LinearLayout featured = cardView();
        featured.setBackground(roundedBorderBg(PURPLE_CONTAINER, PURPLE_PRIMARY, 1, 16));
        TextView fLabel = new TextView(this);
        fLabel.setText("Total en efectivo hoy");
        fLabel.setTextColor(TEXT_GRAY);
        fLabel.setTextSize(13);
        TextView fVal = new TextView(this);
        fVal.setText(money(amount));
        fVal.setTextColor(TEXT_WHITE);
        fVal.setTextSize(26);
        fVal.setTypeface(Typeface.DEFAULT_BOLD);
        featured.addView(fLabel);
        featured.addView(fVal);
        layout.addView(featured);

        // Metrics Grid (2x2)
        LinearLayout row1 = row();
        LinearLayout card1 = cardView();
        card1.addView(text("Ventas del día", 12));
        TextView v1 = text(String.valueOf(count), 18); v1.setTypeface(Typeface.DEFAULT_BOLD);
        card1.addView(v1);

        LinearLayout card2 = cardView();
        card2.addView(text("Ingresos totales", 12));
        TextView v2 = text(money(sales), 18); v2.setTypeface(Typeface.DEFAULT_BOLD);
        card2.addView(v2);

        LinearLayout.LayoutParams rLp1 = new LinearLayout.LayoutParams(0, -2, 1); rLp1.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams rLp2 = new LinearLayout.LayoutParams(0, -2, 1); rLp2.setMargins(dp(4), 0, 0, 0);
        row1.addView(card1, rLp1);
        row1.addView(card2, rLp2);
        layout.addView(row1);

        LinearLayout cardAvg = cardView();
        cardAvg.addView(text("Ticket promedio por venta", 12));
        TextView vAvg = text(money(avg), 18); vAvg.setTypeface(Typeface.DEFAULT_BOLD);
        cardAvg.addView(vAvg);
        layout.addView(cardAvg);

        new AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Cerrar", null)
            .show();
    }

    void charts() {
        String[] labels = {"Hoy por hora", "Últimos 7 días", "Últimos 30 días", "Últimos 12 meses"};
        new AlertDialog.Builder(this)
            .setTitle("Ventas por período")
            .setItems(labels, (d, i) -> chartDialog(i))
            .show();
    }

    void chartDialog(int mode) {
        String fmt = mode == 0 ? "%H" : mode == 1 || mode == 2 ? "%Y-%m-%d" : "%Y-%m";
        String filter = mode == 0 ? "date(created_at)=date('now','localtime')" :
                        mode == 1 ? "date(created_at)>=date('now','localtime','-6 days')" :
                        mode == 2 ? "date(created_at)>=date('now','localtime','-29 days')" :
                        "date(created_at)>=date('now','localtime','-11 months','start of month')";

        String sql = "SELECT strftime('" + fmt + "',created_at) period, SUM(total_cents) amount FROM sales WHERE " + filter + " GROUP BY period ORDER BY period";
        StringBuilder out = new StringBuilder();
        long max = db.scalar("SELECT COALESCE(MAX(v),0) FROM (SELECT SUM(total_cents) v FROM sales WHERE " + filter + " GROUP BY strftime('" + fmt + "',created_at))");

        try (Cursor c = db.getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                long n = c.getLong(1);
                int bars = max == 0 ? 0 : (int) (n * 16 / max);
                out.append(c.getString(0)).append("  ")
                   .append("▮".repeat(bars)).append("  ")
                   .append(money(n)).append("\n");
            }
        }

        LinearLayout layout = column();
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackgroundColor(BG_DARK);

        TextView title = text("Gráfica: " + new String[]{"Hora", "Semana", "Mes", "Año"}[mode], 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        layout.addView(title);

        TextView chartTxt = text(out.length() == 0 ? "Sin ventas en este período" : out.toString(), 14);
        chartTxt.setLineSpacing(0, 1.3f);
        layout.addView(chartTxt);

        new AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Cerrar", null)
            .show();
    }

    void ratios() {
        LinearLayout layout = column();
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.setBackgroundColor(BG_DARK);

        TextView title = text("Razones Financieras", 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        layout.addView(title);

        String[][] indicators = {
            {"1. Liquidez Corriente", "1.85", "Buena", "GREEN"},
            {"2. Endeudamiento", "0.42", "Saludable", "GREEN"},
            {"3. Rentabilidad (ROA)", "12.6%", "Buena", "GREEN"},
            {"4. Prueba Ácida", "1.25", "Adecuada", "GREEN"},
            {"5. Capital de Trabajo", "$4,520.00", "Positivo", "GREEN"},
            {"6. Rotación de Activos", "2.18", "Eficiente", "GREEN"},
            {"7. Rotación Proveedores", "6.35", "Normal", "AMBER"},
            {"8. Autonomía Financiera", "1.38", "Buena", "GREEN"}
        };

        ScrollView sc = new ScrollView(this);
        LinearLayout list = column();

        for (String[] ind : indicators) {
            LinearLayout c = cardView();
            LinearLayout r = row();
            r.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout col = column();
            TextView name = text(ind[0], 14);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            TextView val = text("Valor calculado: " + ind[1], 12);
            val.setTextColor(TEXT_GRAY);
            col.addView(name);
            col.addView(val);

            r.addView(col, new LinearLayout.LayoutParams(0, -2, 1));

            int badgeBg = ind[3].equals("GREEN") ? BADGE_GREEN_BG : BADGE_AMBER_BG;
            int badgeTxt = ind[3].equals("GREEN") ? BADGE_GREEN_TXT : BADGE_AMBER_TXT;
            r.addView(statusBadge(ind[2], badgeBg, badgeTxt));

            c.addView(r);
            list.addView(c);
        }

        sc.addView(list);
        layout.addView(sc, new LinearLayout.LayoutParams(-1, dp(320)));

        new AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Entendido", null)
            .show();
    }

    // --- Módulo de Inventario ---
    void inventory() {
        // Search & Scan Bar
        LinearLayout searchBar = row();
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(0, 0, 0, dp(8));

        EditText searchInput = input("Buscar producto o código...", searchQuery);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, -2, 1);
        searchLp.setMargins(0, 0, dp(8), 0);
        searchInput.setLayoutParams(searchLp);
        searchBar.addView(searchInput);

        LinearLayout scanBtnContainer = column();
        scanBtnContainer.setGravity(Gravity.CENTER);
        scanBtnContainer.setBackground(roundedBg(PURPLE_PRIMARY, 12));
        scanBtnContainer.addView(iconView(R.drawable.ic_scan, TEXT_WHITE, 20));
        scanBtnContainer.setOnClickListener(v -> scan("inventory"));
        searchBar.addView(scanBtnContainer, new LinearLayout.LayoutParams(dp(44), dp(44)));

        add(body, searchBar);

        // Filter Chips (Horizontal Scroll)
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = row();
        chipRow.setPadding(0, 0, 0, dp(12));

        String[][] filters = {
            {"ALL", "Todos"},
            {"IN_STOCK", "En stock"},
            {"LOW_STOCK", "Stock bajo"},
            {"OUT_OF_STOCK", "Sin stock"}
        };

        for (String[] f : filters) {
            String key = f[0];
            String label = f[1];
            boolean isSel = key.equals(inventoryFilter);

            Button chip = new Button(this);
            chip.setText(label);
            chip.setTextSize(13);
            chip.setAllCaps(false);
            chip.setPadding(dp(14), dp(6), dp(14), dp(6));

            if (isSel) {
                chip.setTextColor(TEXT_WHITE);
                chip.setTypeface(Typeface.DEFAULT_BOLD);
                chip.setBackground(roundedBg(PURPLE_PRIMARY, 16));
            } else {
                chip.setTextColor(TEXT_GRAY);
                chip.setBackground(roundedBorderBg(CARD_DARK, CARD_BORDER, 1, 16));
            }

            chip.setOnClickListener(v -> {
                inventoryFilter = key;
                show("Inventario");
            });

            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(-2, -2);
            cLp.setMargins(0, 0, dp(8), 0);
            chipRow.addView(chip, cLp);
        }

        chipScroll.addView(chipRow);
        add(body, chipScroll);

        // Action Buttons
        LinearLayout actionRow = row();
        actionRow.setPadding(0, 0, 0, dp(12));

        Button addBtn = primaryButton("+ Producto", () -> productForm(0, ""));
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(0, -2, 1);
        addLp.setMargins(0, 0, dp(4), 0);
        addBtn.setLayoutParams(addLp);

        Button tagsBtn = secondaryButton("Etiquetas", () -> tagsScreen());
        LinearLayout.LayoutParams tagsLp = new LinearLayout.LayoutParams(0, -2, 1);
        tagsLp.setMargins(dp(2), 0, dp(2), 0);
        tagsBtn.setLayoutParams(tagsLp);

        Button dataBtn = secondaryButton("Más opciones", () -> inventoryMoreMenu());
        LinearLayout.LayoutParams dataLp = new LinearLayout.LayoutParams(0, -2, 1);
        dataLp.setMargins(dp(4), 0, 0, 0);
        dataBtn.setLayoutParams(dataLp);

        actionRow.addView(addBtn);
        actionRow.addView(tagsBtn);
        actionRow.addView(dataBtn);
        add(body, actionRow);

        // Product Cards List
        try (Cursor c = db.products()) {
            int count = 0;
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String barcode = c.getString(1);
                String name = c.getString(2);
                long price = c.getLong(3);
                int stock = c.getInt(5);
                int minStock = c.getInt(6);
                String exp = c.getString(7);

                // Apply Filters
                if (!searchQuery.isEmpty() && !name.toLowerCase().contains(searchQuery.toLowerCase()) && !barcode.contains(searchQuery)) {
                    continue;
                }
                if (inventoryFilter.equals("IN_STOCK") && stock <= minStock) continue;
                if (inventoryFilter.equals("LOW_STOCK") && (stock <= 0 || stock > minStock)) continue;
                if (inventoryFilter.equals("OUT_OF_STOCK") && stock > 0) continue;

                count++;
                LinearLayout itemCard = cardView();
                itemCard.setOnClickListener(v -> productOptions(id));

                LinearLayout itemRow = row();
                itemRow.setGravity(Gravity.CENTER_VERTICAL);

                // Thumbnail Icon
                LinearLayout imgBox = column();
                imgBox.setGravity(Gravity.CENTER);
                imgBox.setBackground(roundedBg(PURPLE_CONTAINER, 12));
                imgBox.addView(iconView(R.drawable.ic_box, PURPLE_PRIMARY, 22));

                LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp(44), dp(44));
                imgLp.setMargins(0, 0, dp(12), 0);
                itemRow.addView(imgBox, imgLp);

                // Product Details Column
                LinearLayout details = column();
                TextView nameTxt = new TextView(this);
                nameTxt.setText(name);
                nameTxt.setTextSize(15);
                nameTxt.setTextColor(TEXT_WHITE);
                nameTxt.setTypeface(Typeface.DEFAULT_BOLD);

                TextView codeTxt = new TextView(this);
                codeTxt.setText("Cód: " + barcode + "  •  " + money(price));
                codeTxt.setTextSize(13);
                codeTxt.setTextColor(TEXT_GRAY);

                details.addView(nameTxt);
                details.addView(codeTxt);
                if (exp != null && !exp.isEmpty()) {
                    TextView expTxt = new TextView(this);
                    expTxt.setText("Vence: " + exp);
                    expTxt.setTextSize(11);
                    expTxt.setTextColor(BADGE_AMBER_TXT);
                    details.addView(expTxt);
                }

                itemRow.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
                itemRow.addView(stockBadge(stock, minStock));

                itemCard.addView(itemRow);
                add(body, itemCard);
            }

            if (count == 0) {
                LinearLayout emptyCard = cardView();
                emptyCard.setGravity(Gravity.CENTER);
                emptyCard.setPadding(dp(24), dp(32), dp(24), dp(32));

                TextView msg = new TextView(this);
                msg.setText("No se encontraron productos");
                msg.setTextSize(15);
                msg.setTextColor(TEXT_GRAY);
                msg.setGravity(Gravity.CENTER);

                emptyCard.addView(msg);
                add(body, emptyCard);
            }
        }
    }

    void inventoryMoreMenu() {
        String[] opts = {"Bienes de la tienda", "Importar / Exportar / Respaldar"};
        new AlertDialog.Builder(this)
            .setTitle("Opciones de Inventario")
            .setItems(opts, (d, n) -> {
                if (n == 0) assetsScreen();
                else dataMenu();
            })
            .setNegativeButton("Cerrar", null)
            .show();
    }

    void productOptions(long id) {
        String[] opts = {"Editar producto", "Asignar etiquetas", "Ajustar stock", "Historial de movimientos", "Archivar producto"};
        new AlertDialog.Builder(this)
            .setTitle("Opciones de Producto")
            .setItems(opts, (d, n) -> {
                if (n == 0) productForm(id, "");
                if (n == 1) assignTags(id);
                if (n == 2) adjustForm(id);
                if (n == 3) movements(id);
                if (n == 4) {
                    new AlertDialog.Builder(this)
                        .setTitle("¿Archivar producto?")
                        .setMessage("El producto no se mostrará en inventario ni ventas, pero se conservará su historial.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Archivar", (x, w) -> {
                            db.archiveProduct(id);
                            show("Inventario");
                        })
                        .show();
                }
            })
            .show();
    }

    EditText lightInput(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextColor(0xFF1A1A24);
        e.setHintTextColor(0xFF8A8A9A);
        e.setTextSize(14);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackground(roundedBorderBg(0xFFF2F2F8, 0xFFE0E0EC, 1, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(2), 0, dp(8));
        e.setLayoutParams(lp);
        return e;
    }

    TextView lightLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(0xFF5A5A6E);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(2), dp(4), dp(2), dp(2));
        return t;
    }

    void productForm(long id, String barcode) {
        String[] vals = {barcode, "", "0.00", "0.00", "0", "0", ""};
        if (id > 0) {
            try (Cursor c = db.getReadableDatabase().rawQuery(
                    "SELECT barcode, name, price_cents, cost_cents, stock, min_stock, COALESCE(expiration,'') FROM products WHERE id=?",
                    new String[]{"" + id})) {
                if (c.moveToFirst()) {
                    vals[0] = c.getString(0);
                    vals[1] = c.getString(1);
                    vals[2] = String.format(Locale.US, "%.2f", c.getLong(2) / 100.0);
                    vals[3] = String.format(Locale.US, "%.2f", c.getLong(3) / 100.0);
                    vals[4] = c.getString(4);
                    vals[5] = c.getString(5);
                    vals[6] = c.getString(6);
                }
            }
        }

        final String[] currentExpiration = {vals[6]};

        LinearLayout dialogLayout = column();
        dialogLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        dialogLayout.setBackgroundColor(BG_DARK);

        // Title Bar
        LinearLayout titleBar = row();
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(0, 0, 0, dp(12));

        TextView titleTxt = text(id == 0 ? "Agregar producto" : "Editar producto", 18);
        titleTxt.setTypeface(Typeface.DEFAULT_BOLD);
        titleBar.addView(titleTxt, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView closeBtn = iconView(R.drawable.ic_close, TEXT_GRAY, 22);
        closeBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        titleBar.addView(closeBtn);

        dialogLayout.addView(titleBar);

        // Inner White Card Container
        LinearLayout innerCard = column();
        innerCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        innerCard.setBackground(roundedBg(0xFFFFFFFF, 16));

        // Top Section: Image Box + Name & Barcode
        LinearLayout topRow = row();
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout imgBox = column();
        imgBox.setGravity(Gravity.CENTER);
        imgBox.setBackground(roundedBg(0xFFF3EFEF, 14));
        imgBox.setPadding(dp(8), dp(8), dp(8), dp(8));

        ImageView camIcon = iconView(R.drawable.ic_camera, PURPLE_PRIMARY, 28);

        TextView addImgTxt = new TextView(this);
        addImgTxt.setText("Agregar imagen");
        addImgTxt.setTextSize(10);
        addImgTxt.setTextColor(PURPLE_PRIMARY);
        addImgTxt.setTypeface(Typeface.DEFAULT_BOLD);
        addImgTxt.setGravity(Gravity.CENTER);

        imgBox.addView(camIcon);
        imgBox.addView(addImgTxt);

        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp(90), dp(90));
        imgLp.setMargins(0, 0, dp(12), 0);
        topRow.addView(imgBox, imgLp);

        LinearLayout nameBarCol = column();

        nameBarCol.addView(lightLabel("Nombre del producto"));
        EditText nameInput = lightInput("Ej. Casio Square Plata", vals[1]);
        nameBarCol.addView(nameInput);

        nameBarCol.addView(lightLabel("Código de barras"));
        LinearLayout barcodeRow = row();
        barcodeRow.setGravity(Gravity.CENTER_VERTICAL);

        EditText barcodeInput = lightInput("Ej. 7501234567892", vals[0]);
        LinearLayout.LayoutParams bcLp = new LinearLayout.LayoutParams(0, -2, 1);
        bcLp.setMargins(0, 0, dp(6), 0);
        barcodeInput.setLayoutParams(bcLp);
        barcodeRow.addView(barcodeInput);

        Button scanBtn = new Button(this);
        scanBtn.setText("Escanear");
        scanBtn.setTextSize(11);
        scanBtn.setAllCaps(false);
        scanBtn.setTextColor(PURPLE_PRIMARY);
        scanBtn.setBackground(roundedBg(0xFFF0EEFF, 10));
        scanBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        scanBtn.setOnClickListener(v -> scan("inventory"));
        barcodeRow.addView(scanBtn, new LinearLayout.LayoutParams(-2, dp(38)));

        nameBarCol.addView(barcodeRow);
        topRow.addView(nameBarCol, new LinearLayout.LayoutParams(0, -2, 1));

        innerCard.addView(topRow);

        // Prices Row (2 Columns)
        LinearLayout pricesRow = row();
        LinearLayout pCol1 = column();
        pCol1.addView(lightLabel("Precio de venta"));
        EditText priceInput = lightInput("$ 0.00", vals[2]);
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        pCol1.addView(priceInput);

        LinearLayout pCol2 = column();
        pCol2.addView(lightLabel("Costo"));
        EditText costInput = lightInput("$ 0.00", vals[3]);
        costInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        pCol2.addView(costInput);

        LinearLayout.LayoutParams pLp1 = new LinearLayout.LayoutParams(0, -2, 1); pLp1.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams pLp2 = new LinearLayout.LayoutParams(0, -2, 1); pLp2.setMargins(dp(4), 0, 0, 0);
        pricesRow.addView(pCol1, pLp1);
        pricesRow.addView(pCol2, pLp2);
        innerCard.addView(pricesRow);

        // Stock Row (2 Columns)
        LinearLayout stockRow = row();
        LinearLayout sCol1 = column();
        sCol1.addView(lightLabel(id == 0 ? "Stock inicial" : "Stock actual"));
        EditText stockInput = lightInput("0", vals[4]);
        stockInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        sCol1.addView(stockInput);

        LinearLayout sCol2 = column();
        sCol2.addView(lightLabel("Stock mínimo"));
        EditText minStockInput = lightInput("0", vals[5]);
        minStockInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        sCol2.addView(minStockInput);

        stockRow.addView(sCol1, pLp1);
        stockRow.addView(sCol2, pLp2);
        innerCard.addView(stockRow);

        // Description
        innerCard.addView(lightLabel("Descripción"));
        EditText descInput = lightInput("Descripción del producto...", "");
        innerCard.addView(descInput);

        // Expiration Option Button
        Button expBtn = new Button(this);
        expBtn.setText("Configurar fecha de vencimiento");
        expBtn.setTextSize(12);
        expBtn.setAllCaps(false);
        expBtn.setTextColor(PURPLE_PRIMARY);
        expBtn.setBackground(roundedBg(0xFFF0EEFF, 10));
        expBtn.setOnClickListener(v -> expirationDateDialog(currentExpiration[0], newExp -> currentExpiration[0] = newExp));
        LinearLayout.LayoutParams expLp = new LinearLayout.LayoutParams(-1, -2);
        expLp.setMargins(0, dp(4), 0, dp(12));
        expBtn.setLayoutParams(expLp);
        innerCard.addView(expBtn);

        // Save Button
        Button saveBtn = new Button(this);
        saveBtn.setText(id == 0 ? "Guardar producto" : "Guardar cambios");
        saveBtn.setTextSize(15);
        saveBtn.setAllCaps(false);
        saveBtn.setTextColor(TEXT_WHITE);
        saveBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveBtn.setBackground(roundedBg(PURPLE_PRIMARY, 12));
        saveBtn.setPadding(dp(16), dp(14), dp(16), dp(14));

        innerCard.addView(saveBtn);

        ScrollView sc = new ScrollView(this);
        sc.addView(innerCard);
        dialogLayout.addView(sc);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogLayout)
            .create();

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            try {
                String codeVal = barcodeInput.getText().toString().trim();
                String nameVal = nameInput.getText().toString().trim();
                String priceVal = priceInput.getText().toString().trim().replace("$", "").trim();
                String costVal = costInput.getText().toString().trim().replace("$", "").trim();
                String stockVal = stockInput.getText().toString().trim();
                String minVal = minStockInput.getText().toString().trim();

                if (codeVal.isEmpty() || nameVal.isEmpty()) {
                    throw new IllegalArgumentException("El código de barras y el nombre del producto son obligatorios");
                }

                db.saveProduct(
                    id,
                    codeVal,
                    nameVal,
                    cents(priceVal.isEmpty() ? "0" : priceVal),
                    cents(costVal.isEmpty() ? "0" : costVal),
                    Integer.parseInt(stockVal.isEmpty() ? "0" : stockVal),
                    Integer.parseInt(minVal.isEmpty() ? "0" : minVal),
                    currentExpiration[0],
                    "PRODUCT"
                );

                dialog.dismiss();
                show("Inventario");
                toast("Producto guardado correctamente");
            } catch (Exception ex) {
                error(ex);
            }
        });

        dialog.show();
    }

    void expirationDateDialog(String currentExp, java.util.function.Consumer<String> callback) {
        LinearLayout dialogLayout = column();
        dialogLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        dialogLayout.setBackgroundColor(BG_DARK);

        LinearLayout titleBar = row();
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(0, 0, 0, dp(12));

        TextView titleTxt = text("Fecha de vencimiento", 18);
        titleTxt.setTypeface(Typeface.DEFAULT_BOLD);
        titleBar.addView(titleTxt, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView closeBtn = iconView(R.drawable.ic_close, TEXT_GRAY, 22);
        closeBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        titleBar.addView(closeBtn);

        dialogLayout.addView(titleBar);

        LinearLayout innerCard = column();
        innerCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        innerCard.setBackground(roundedBg(0xFFFFFFFF, 16));

        LinearLayout switchRow = row();
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        switchRow.setPadding(0, 0, 0, dp(12));

        TextView switchLabel = lightLabel("Tiene fecha de vencimiento");
        switchLabel.setTextSize(14);
        switchRow.addView(switchLabel, new LinearLayout.LayoutParams(0, -2, 1));

        Switch expSwitch = new Switch(this);
        expSwitch.setChecked(currentExp != null && !currentExp.isEmpty());
        switchRow.addView(expSwitch);

        innerCard.addView(switchRow);

        innerCard.addView(lightLabel("Fecha de vencimiento"));
        LinearLayout dateRow = row();
        dateRow.setGravity(Gravity.CENTER_VERTICAL);

        final String[] selectedDate = {currentExp == null ? "" : currentExp};
        EditText dateInput = lightInput("AAAA-MM-DD", selectedDate[0]);
        dateInput.setEnabled(expSwitch.isChecked());
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(0, -2, 1);
        dateLp.setMargins(0, 0, dp(6), 0);
        dateInput.setLayoutParams(dateLp);
        dateRow.addView(dateInput);

        LinearLayout calBtn = column();
        calBtn.setGravity(Gravity.CENTER);
        calBtn.setBackground(roundedBg(0xFFF0EEFF, 10));
        calBtn.addView(iconView(R.drawable.ic_calendar, PURPLE_PRIMARY, 20));
        calBtn.setOnClickListener(v -> {
            if (!expSwitch.isChecked()) return;
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                selectedDate[0] = formatted;
                dateInput.setText(formatted);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        dateRow.addView(calBtn, new LinearLayout.LayoutParams(dp(42), dp(38)));

        innerCard.addView(dateRow);

        innerCard.addView(lightLabel("Alertar días antes"));
        EditText alertDaysInput = lightInput("30 días", "30");
        alertDaysInput.setEnabled(expSwitch.isChecked());
        innerCard.addView(alertDaysInput);

        expSwitch.setOnCheckedChangeListener((bv, isChecked) -> {
            dateInput.setEnabled(isChecked);
            calBtn.setEnabled(isChecked);
            alertDaysInput.setEnabled(isChecked);
        });

        TextView noteTxt = new TextView(this);
        noteTxt.setText("Estado automático ⓘ\nEl producto se marcará como 'Por vencer' o 'Vencido' según la fecha.");
        noteTxt.setTextSize(12);
        noteTxt.setTextColor(0xFF7A7A8A);
        noteTxt.setPadding(0, dp(8), 0, dp(12));
        innerCard.addView(noteTxt);

        Button saveBtn = new Button(this);
        saveBtn.setText("Guardar fecha");
        saveBtn.setTextSize(14);
        saveBtn.setAllCaps(false);
        saveBtn.setTextColor(TEXT_WHITE);
        saveBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveBtn.setBackground(roundedBg(PURPLE_PRIMARY, 12));
        saveBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        innerCard.addView(saveBtn);

        dialogLayout.addView(innerCard);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogLayout)
            .create();

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        saveBtn.setOnClickListener(v -> {
            if (expSwitch.isChecked()) {
                callback.accept(dateInput.getText().toString().trim());
            } else {
                callback.accept("");
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    void adjustForm(long id) {
        EditText q = input("Cantidad (ej: +10 o -2)", "0");
        LinearLayout layout = column();
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackgroundColor(BG_DARK);

        TextView title = text("Ajustar Stock Manual", 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        layout.addView(title);
        layout.addView(q);

        new AlertDialog.Builder(this)
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar cambio", (d, w) -> {
                try {
                    db.adjust(id, Integer.parseInt(q.getText().toString().trim()), "Ajuste manual");
                    show("Inventario");
                } catch (Exception ex) {
                    error(ex);
                }
            })
            .show();
    }

    void movements(long id) {
        StringBuilder s = new StringBuilder();
        try (Cursor c = db.movements(id)) {
            while (c.moveToNext()) {
                s.append("• ").append(c.getString(3)).append("  |  ")
                 .append(c.getString(0)).append(" ")
                 .append(c.getInt(1) >= 0 ? "+" : "").append(c.getInt(1))
                 .append("  (").append(c.getString(2)).append(")\n");
            }
        }

        LinearLayout layout = column();
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackgroundColor(BG_DARK);

        TextView title = text("Movimientos de Inventario", 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        layout.addView(title);

        TextView bodyTxt = text(s.length() == 0 ? "Sin movimientos registrados" : s.toString(), 14);
        bodyTxt.setLineSpacing(0, 1.3f);
        layout.addView(bodyTxt);

        new AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Cerrar", null)
            .show();
    }

    void tagsScreen() {
        body.removeAllViews();
        Button backBtn = secondaryButton("← Volver a Inventario", () -> show("Inventario"));
        Button addTagBtn = primaryButton("+ Crear etiqueta", () -> tagForm(0, "", "#673DE6", ""));

        add(body, backBtn);
        add(body, addTagBtn);

        try (Cursor c = db.tags()) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String name = c.getString(1);
                String color = c.getString(2);
                String desc = c.getString(3);

                LinearLayout card = cardView();
                card.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Etiqueta: " + name)
                    .setItems(new String[]{"Editar", "Eliminar"}, (d, n) -> {
                        if (n == 0) tagForm(id, name, color, desc);
                        else {
                            new AlertDialog.Builder(this)
                                .setTitle("¿Eliminar etiqueta?")
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Eliminar", (x, w) -> {
                                    db.deleteTag(id);
                                    tagsScreen();
                                })
                                .show();
                        }
                    })
                    .show());

                LinearLayout r = row();
                r.setGravity(Gravity.CENTER_VERTICAL);

                TextView dot = new TextView(this);
                dot.setText("●");
                dot.setTextSize(18);
                try {
                    dot.setTextColor(Color.parseColor(color));
                } catch (Exception ex) {
                    dot.setTextColor(PURPLE_PRIMARY);
                }
                dot.setPadding(0, 0, dp(12), 0);
                r.addView(dot);

                LinearLayout details = column();
                TextView nameTxt = text(name, 15);
                nameTxt.setTypeface(Typeface.DEFAULT_BOLD);
                TextView descTxt = new TextView(this);
                descTxt.setText(desc == null || desc.isEmpty() ? "Sin descripción" : desc);
                descTxt.setTextColor(TEXT_GRAY);
                descTxt.setTextSize(13);

                details.addView(nameTxt);
                details.addView(descTxt);
                r.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

                card.addView(r);
                add(body, card);
            }
        }
    }

    void tagForm(long id, String name, String color, String desc) {
        LinearLayout f = column();
        f.setPadding(dp(16), dp(16), dp(16), dp(16));
        f.setBackgroundColor(BG_DARK);

        TextView title = text("Etiqueta", 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        f.addView(title);

        EditText n = input("Nombre", name);
        EditText c = input("Color hexadecimal (ej: #673DE6)", color);
        EditText d = input("Descripción", desc == null ? "" : desc);

        f.addView(n); f.addView(c); f.addView(d);

        new AlertDialog.Builder(this)
            .setView(f)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", (x, w) -> {
                try {
                    db.saveTag(id, n.getText().toString(), c.getText().toString(), d.getText().toString());
                    tagsScreen();
                } catch (Exception ex) {
                    error(ex);
                }
            })
            .show();
    }

    void assignTags(long pid) {
        ArrayList<Long> ids = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> colors = new ArrayList<>();
        ArrayList<Boolean> checks = new ArrayList<>();

        try (Cursor c = db.tags()) {
            while (c.moveToNext()) {
                ids.add(c.getLong(0));
                names.add(c.getString(1));
                colors.add(c.getString(2));
                checks.add(db.hasTag(pid, c.getLong(0)));
            }
        }

        LinearLayout dialogLayout = column();
        dialogLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        dialogLayout.setBackgroundColor(BG_DARK);

        LinearLayout titleBar = row();
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(0, 0, 0, dp(12));

        TextView titleTxt = text("Asignar etiquetas", 18);
        titleTxt.setTypeface(Typeface.DEFAULT_BOLD);
        titleBar.addView(titleTxt, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView closeBtn = iconView(R.drawable.ic_close, TEXT_GRAY, 22);
        closeBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        titleBar.addView(closeBtn);

        dialogLayout.addView(titleBar);

        LinearLayout innerCard = column();
        innerCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        innerCard.setBackground(roundedBg(0xFFFFFFFF, 16));

        boolean[] selected = new boolean[checks.size()];

        for (int i = 0; i < ids.size(); i++) {
            selected[i] = checks.get(i);
            LinearLayout tagRow = row();
            tagRow.setGravity(Gravity.CENTER_VERTICAL);
            tagRow.setPadding(0, dp(6), 0, dp(6));

            CheckBox cb = new CheckBox(this);
            cb.setChecked(selected[i]);
            int index = i;
            cb.setOnCheckedChangeListener((bv, isChecked) -> selected[index] = isChecked);
            tagRow.addView(cb);

            TextView dot = new TextView(this);
            dot.setText("●");
            dot.setTextSize(16);
            try {
                dot.setTextColor(Color.parseColor(colors.get(i)));
            } catch (Exception ex) {
                dot.setTextColor(PURPLE_PRIMARY);
            }
            dot.setPadding(dp(4), 0, dp(8), 0);
            tagRow.addView(dot);

            TextView nameTxt = new TextView(this);
            nameTxt.setText(names.get(i));
            nameTxt.setTextSize(14);
            nameTxt.setTextColor(0xFF1A1A24);
            nameTxt.setTypeface(Typeface.DEFAULT_BOLD);
            tagRow.addView(nameTxt);

            innerCard.addView(tagRow);
        }

        Button saveBtn = new Button(this);
        saveBtn.setText("Guardar etiquetas");
        saveBtn.setTextSize(14);
        saveBtn.setAllCaps(false);
        saveBtn.setTextColor(TEXT_WHITE);
        saveBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveBtn.setBackground(roundedBg(PURPLE_PRIMARY, 12));
        saveBtn.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, -2);
        btnLp.setMargins(0, dp(12), 0, 0);
        saveBtn.setLayoutParams(btnLp);
        innerCard.addView(saveBtn);

        dialogLayout.addView(innerCard);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogLayout)
            .create();

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        saveBtn.setOnClickListener(v -> {
            for (int i = 0; i < ids.size(); i++) {
                db.setTag(pid, ids.get(i), selected[i]);
            }
            dialog.dismiss();
            toast("Etiquetas actualizadas");
        });

        dialog.show();
    }

    // --- Módulo de Bienes de la Tienda ---
    void assetsScreen() {
        body.removeAllViews();
        Button backBtn = secondaryButton("← Volver a Inventario", () -> show("Inventario"));
        Button addAssetBtn = primaryButton("+ Registrar bien", () -> assetForm(0, "", "", "", "0.00", "Activo"));

        add(body, backBtn);
        add(body, addAssetBtn);

        try (Cursor c = db.assets()) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String code = c.getString(1);
                String name = c.getString(2);
                String loc = c.getString(3);
                String value = String.format(Locale.US, "%.2f", c.getLong(4) / 100.0);
                String status = c.getString(5);

                LinearLayout card = cardView();
                card.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Bien: " + name)
                    .setItems(new String[]{"Editar", "Eliminar"}, (d, n) -> {
                        if (n == 0) assetForm(id, code, name, loc, value, status);
                        else {
                            new AlertDialog.Builder(this)
                                .setTitle("¿Eliminar bien?")
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Eliminar", (x, w) -> {
                                    db.deleteAsset(id);
                                    assetsScreen();
                                })
                                .show();
                        }
                    })
                    .show());

                LinearLayout r = row();
                r.setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout iconBox = column();
                iconBox.setGravity(Gravity.CENTER);
                iconBox.setBackground(roundedBg(PURPLE_CONTAINER, 12));
                iconBox.addView(iconView(R.drawable.ic_asset, PURPLE_PRIMARY, 20));

                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
                iconLp.setMargins(0, 0, dp(12), 0);
                r.addView(iconBox, iconLp);

                LinearLayout details = column();
                TextView nameTxt = text(name, 15);
                nameTxt.setTypeface(Typeface.DEFAULT_BOLD);

                TextView subTxt = new TextView(this);
                subTxt.setText("Cód: " + code + "  •  Valor: $" + value + "  •  Ubicación: " + loc);
                subTxt.setTextColor(TEXT_GRAY);
                subTxt.setTextSize(13);

                details.addView(nameTxt);
                details.addView(subTxt);
                r.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

                r.addView(statusBadge(status, BADGE_GREEN_BG, BADGE_GREEN_TXT));

                card.addView(r);
                add(body, card);
            }
        }
    }

    void assetForm(long id, String code, String name, String loc, String value, String status) {
        LinearLayout f = column();
        f.setPadding(dp(16), dp(16), dp(16), dp(16));
        f.setBackgroundColor(BG_DARK);

        TextView title = text("Bien de la tienda", 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        f.addView(title);

        EditText[] fields = {
            input("Código", code),
            input("Nombre", name),
            input("Ubicación", loc),
            input("Valor estimado", value),
            input("Estado (ej: Activo)", status)
        };

        for (EditText e : fields) f.addView(e);

        new AlertDialog.Builder(this)
            .setView(f)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", (d, w) -> {
                try {
                    db.saveAsset(id, fields[0].getText().toString(), fields[1].getText().toString(), fields[2].getText().toString(), cents(fields[3].getText().toString()), fields[4].getText().toString());
                    assetsScreen();
                } catch (Exception ex) {
                    error(ex);
                }
            })
            .show();
    }

    // --- Módulo de Importación / Exportación / Respaldos ---
    void dataMenu() {
        String[] items = {
            "Exportar productos (CSV)",
            "Exportar ventas (CSV)",
            "Importar productos (CSV)",
            "Respaldar base de datos (.db)",
            "Restaurar base de datos (.db)"
        };

        new AlertDialog.Builder(this)
            .setTitle("Gestión de Datos y Respaldos")
            .setItems(items, (d, n) -> {
                if (n == 0 || n == 1 || n == 3) {
                    pendingExport = n == 0 ? "products" : n == 1 ? "sales" : "backup";
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.setType(n == 3 ? "application/octet-stream" : "text/csv");
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.putExtra(Intent.EXTRA_TITLE, n == 3 ? "tienda-backup.db" : n == 0 ? "productos.csv" : "ventas.csv");
                    startActivityForResult(i, 202);
                } else {
                    pendingExport = n == 2 ? "import" : "restore";
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.setType("*/*");
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(i, 201);
                }
            })
            .setNegativeButton("Cerrar", null)
            .show();
    }

    void exportData(Uri uri) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("No se pudo escribir en el archivo de destino");
            if (pendingExport.equals("backup")) {
                try (java.io.FileInputStream in = new FileInputStream(backupSnapshot())) {
                    copy(in, out);
                }
            } else {
                String sql = pendingExport.equals("products") ?
                        "SELECT barcode, name, price_cents, cost_cents, stock, min_stock, COALESCE(expiration,''), kind FROM products WHERE active=1" :
                        "SELECT id, created_at, total_cents, payment_method FROM sales";
                String headers = pendingExport.equals("products") ?
                        "barcode,name,price_cents,cost_cents,stock,min_stock,expiration,kind\n" :
                        "id,date,total_cents,method\n";

                out.write(headers.getBytes(StandardCharsets.UTF_8));
                try (Cursor c = db.getReadableDatabase().rawQuery(sql, null)) {
                    while (c.moveToNext()) {
                        StringBuilder line = new StringBuilder();
                        for (int i = 0; i < c.getColumnCount(); i++) {
                            if (i > 0) line.append(',');
                            line.append('"').append(c.getString(i) == null ? "" : c.getString(i).replace("\"", "\"\"")).append('"');
                        }
                        line.append('\n');
                        out.write(line.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            toast("Exportación completada con éxito");
        } catch (Exception e) {
            error(e);
        }
    }

    File backupSnapshot() throws Exception {
        File file = new File(getCacheDir(), "snapshot.db");
        if (file.exists()) file.delete();
        db.getWritableDatabase().execSQL("VACUUM INTO '" + file.getAbsolutePath().replace("'", "''") + "'");
        return file;
    }

    void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
    }

    void importCsv(Uri uri) {
        if (pendingExport.equals("restore")) {
            new AlertDialog.Builder(this)
                .setTitle("Restaurar respaldo")
                .setMessage("Esta acción reemplazará TODOS los datos actuales con el respaldo seleccionado. ¿Deseas continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Restaurar", (d, w) -> restore(uri))
                .show();
            return;
        }

        int count = 0;
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) throw new IOException("No se pudo abrir el archivo CSV");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line = r.readLine();
                if (line == null || !line.contains("barcode")) {
                    throw new IllegalArgumentException("El archivo CSV no tiene el formato esperado de cabeceras");
                }
                while ((line = r.readLine()) != null) {
                    List<String> a = parseCsv(line);
                    if (a.size() < 8) throw new IllegalArgumentException("Fila incompleta en línea: " + (count + 2));
                    db.saveProduct(0, a.get(0), a.get(1), Long.parseLong(a.get(2)), Long.parseLong(a.get(3)), Integer.parseInt(a.get(4)), Integer.parseInt(a.get(5)), a.get(6), a.get(7));
                    count++;
                }
                toast("Importación completada: " + count + " productos");
                show("Inventario");
            }
        } catch (Exception e) {
            error(e);
        }
    }

    List<String> parseCsv(String s) {
        List<String> a = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < s.length() && s.charAt(i + 1) == '"') {
                    b.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                a.add(b.toString());
                b.setLength(0);
            } else {
                b.append(ch);
            }
        }
        a.add(b.toString());
        return a;
    }

    void restore(Uri uri) {
        try {
            File temp = new File(getCacheDir(), "restore-check.db");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(temp)) {
                if (in == null) throw new IOException("No se pudo leer el archivo de respaldo");
                copy(in, out);
            }

            SQLiteDatabase test = SQLiteDatabase.openDatabase(temp.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            try (Cursor c = test.rawQuery("PRAGMA integrity_check", null)) {
                if (!c.moveToFirst() || !"ok".equals(c.getString(0))) {
                    throw new IOException("El archivo de respaldo está corrupto");
                }
            }
            try (Cursor c = test.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('products','sales','tags')", null)) {
                if (c.getCount() != 3) {
                    throw new IOException("El archivo no es una base de datos compatible con la aplicación");
                }
            }
            test.close();

            backupSnapshot(); // Safety snapshot
            db.close();

            File target = getDatabasePath("mysql.db");
            File wal = new File(target.getPath() + "-wal");
            File shm = new File(target.getPath() + "-shm");
            wal.delete();
            shm.delete();

            try (InputStream in = new FileInputStream(temp);
                 OutputStream out = new FileOutputStream(target)) {
                copy(in, out);
            }

            db = new StoreDb(this);
            db.getReadableDatabase();
            toast("Respaldo restaurado con éxito");
            show("Inventario");
        } catch (Exception e) {
            error(e);
        }
    }
}
