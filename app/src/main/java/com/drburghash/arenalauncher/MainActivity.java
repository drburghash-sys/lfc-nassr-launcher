package com.drburghash.arenalauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.icu.util.IslamicCalendar;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PAGE_HOME = 0;
    private static final int PAGE_WORK = 1;
    private static final int PAGE_FAMILY = 2;
    private static final int PAGE_ME = 3;
    private static final int THEME_FUSION = 0;
    private static final int THEME_ROYAL = 1;
    private static final int THEME_RED = 2;

    private final ArrayList<AppEntry> allApps = new ArrayList<>();
    private SharedPreferences prefs;
    private FrameLayout root;
    private LinearLayout pageHost;
    private LinearLayout navBar;
    private int currentPage = PAGE_HOME;
    private int themeIndex = THEME_FUSION;
    private float touchStartX;
    private float touchStartY;
    private long touchStartTime;

    private int accentGold = Color.rgb(216, 179, 74);
    private int accentRed = Color.rgb(218, 32, 53);
    private int accentBlue = Color.rgb(55, 139, 255);
    private int textPrimary = Color.WHITE;
    private int textSecondary = 0xCCFFFFFF;
    private int cardFill = 0xB0141820;
    private int cardStroke = 0x667FA8D6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(4, 7, 11));
        getWindow().setNavigationBarColor(Color.rgb(4, 7, 11));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        prefs = getSharedPreferences("arena_launcher", MODE_PRIVATE);
        themeIndex = clamp(prefs.getInt("theme", THEME_FUSION), 0, 2);
        currentPage = clamp(prefs.getInt("page", PAGE_HOME), 0, 3);
        loadApps();
        buildRoot();
        renderPage(currentPage);
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void buildRoot() {
        root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(new ArenaBackground(themeIndex));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(column, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        pageHost = new LinearLayout(this);
        pageHost.setOrientation(LinearLayout.VERTICAL);
        pageHost.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        column.addView(pageHost, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        navBar = buildBottomNav();
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72));
        navLp.setMargins(dp(12), 0, dp(12), dp(8));
        column.addView(navBar, navLp);
        setContentView(root);
    }

    private LinearLayout buildBottomNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(4), dp(6), dp(4), dp(6));
        bar.setBackground(roundRect(0xE613151B, 26, 0x668E7341, 1));
        String[] icons = {"⌂", "▦", "♥", "◉"};
        String[] labels = {"الرئيسية", "العمل", "العائلة", "أنا"};
        int[] pages = {PAGE_HOME, PAGE_WORK, PAGE_FAMILY, PAGE_ME};
        for (int i = 0; i < pages.length; i++) {
            final int page = pages[i];
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(4), dp(4), dp(4));
            TextView icon = new TextView(this);
            icon.setText(icons[i]);
            icon.setTextSize(24);
            icon.setGravity(Gravity.CENTER);
            icon.setTag("navIcon" + page);
            item.addView(icon, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(28)));
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setTag("navLabel" + page);
            item.addView(label, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)));
            item.setOnClickListener(v -> switchPage(page));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            bar.addView(item, lp);
        }
        updateNavStyles();
        return bar;
    }

    private void updateNavStyles() {
        if (navBar == null) return;
        for (int page = 0; page < 4; page++) {
            TextView icon = navBar.findViewWithTag("navIcon" + page);
            TextView label = navBar.findViewWithTag("navLabel" + page);
            boolean active = page == currentPage;
            int color = active ? accentGold : 0xD0FFFFFF;
            if (icon != null) icon.setTextColor(color);
            if (label != null) {
                label.setTextColor(color);
                label.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void switchPage(int page) {
        currentPage = clamp(page, 0, 3);
        prefs.edit().putInt("page", currentPage).apply();
        renderPage(currentPage);
    }

    private void renderPage(int page) {
        applyThemeColors();
        root.setBackground(new ArenaBackground(themeIndex));
        pageHost.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        body.setPadding(dp(14), dp(14), dp(14), dp(20));
        scroll.addView(body, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        pageHost.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        body.addView(buildHeroHeader(page));
        body.addView(spacer(10));
        if (page == PAGE_HOME) buildHome(body);
        else if (page == PAGE_WORK) buildWork(body);
        else if (page == PAGE_FAMILY) buildFamily(body);
        else buildMe(body);
        updateNavStyles();
    }

    private void applyThemeColors() {
        if (themeIndex == THEME_ROYAL) {
            cardFill = 0xC10A1425;
            cardStroke = 0x779E8337;
            accentBlue = Color.rgb(47, 132, 255);
            accentRed = Color.rgb(150, 25, 42);
        } else if (themeIndex == THEME_RED) {
            cardFill = 0xC1180B10;
            cardStroke = 0x77CC2D42;
            accentBlue = Color.rgb(60, 95, 150);
            accentRed = Color.rgb(235, 38, 61);
        } else {
            cardFill = 0xB7141820;
            cardStroke = 0x667FA8D6;
            accentBlue = Color.rgb(55, 139, 255);
            accentRed = Color.rgb(218, 32, 53);
        }
    }

    private View buildHeroHeader(int page) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(12), dp(10), dp(12), dp(12));
        hero.setBackground(roundRect(0xB0090D13, 24, 0x668E7341, 1));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        hero.addView(top, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView left = new TextView(this);
        left.setText("L.F.C.\nYOU'LL NEVER WALK ALONE");
        left.setTextColor(accentRed);
        left.setTextSize(11);
        left.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        left.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(left, new LinearLayout.LayoutParams(0, dp(54), 1f));
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        top.addView(center, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.6f));
        TextView time = new TextView(this);
        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        time.setTextColor(Color.WHITE);
        time.setTextSize(34);
        time.setGravity(Gravity.CENTER);
        time.setTypeface(null, android.graphics.Typeface.BOLD);
        center.addView(time);
        TextView pageTitle = new TextView(this);
        pageTitle.setText(pageTitle(page));
        pageTitle.setTextColor(accentGold);
        pageTitle.setTextSize(17);
        pageTitle.setGravity(Gravity.CENTER);
        center.addView(pageTitle);
        TextView right = new TextView(this);
        right.setText("AL NASSR\nMORE THAN A CLUB");
        right.setTextColor(accentGold);
        right.setTextSize(11);
        right.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        right.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(right, new LinearLayout.LayoutParams(0, dp(54), 1f));
        TextView date = new TextView(this);
        date.setText(gregorianDate() + "   •   " + hijriDate());
        date.setTextColor(textSecondary);
        date.setGravity(Gravity.CENTER);
        date.setTextSize(12);
        date.setPadding(0, dp(7), 0, 0);
        hero.addView(date);
        TextView motto = new TextView(this);
        motto.setText(pageMotto(page));
        motto.setTextColor(0xF0FFFFFF);
        motto.setGravity(Gravity.CENTER);
        motto.setTextSize(13);
        motto.setPadding(dp(4), dp(8), dp(4), 0);
        hero.addView(motto);
        return hero;
    }

    private String pageTitle(int page) {
        if (page == PAGE_WORK) return "العمل";
        if (page == PAGE_FAMILY) return "العائلة";
        if (page == PAGE_ME) return "أنا";
        return "الرئيسية";
    }

    private String pageMotto(int page) {
        if (page == PAGE_WORK) return "بالعلم والخدمة نصنع فرقًا";
        if (page == PAGE_FAMILY) return "العائلة أولاً • Family Always First";
        if (page == PAGE_ME) return "كل يوم خطوة نحو نسخة أفضل";
        return "الشغف لا يعرف حدودًا • Passion Has No Limits";
    }

    private String gregorianDate() { return new SimpleDateFormat("EEEE d MMMM yyyy", new Locale("ar", "SA")).format(new Date()); }

    private String hijriDate() {
        IslamicCalendar cal = new IslamicCalendar();
        cal.setTime(new Date());
        String[] months = {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};
        int d = cal.get(IslamicCalendar.DAY_OF_MONTH);
        int m = cal.get(IslamicCalendar.MONTH);
        int y = cal.get(IslamicCalendar.YEAR);
        return d + " " + months[Math.max(0, Math.min(11, m))] + " " + y + " هـ";
    }

    private void buildHome(LinearLayout body) {
        body.addView(sectionTitle("اليوم"));
        body.addView(buildThreeInfoRow());
        body.addView(spacer(12));
        body.addView(sectionTitle("التطبيقات السريعة"));
        List<SlotSpec> quick = new ArrayList<>();
        quick.add(new SlotSpec("home_whatsapp", "●", "واتساب", "whatsapp", "واتساب"));
        quick.add(new SlotSpec("home_phone", "☎", "الاتصال", "dialer", "phone", "هاتف", "اتصال"));
        quick.add(new SlotSpec("home_chatgpt", "✦", "ChatGPT", "openai", "chatgpt"));
        quick.add(new SlotSpec("home_notes", "▤", "ملاحظات", "notes", "keep", "ملاحظات"));
        quick.add(new SlotSpec("home_camera", "◉", "الكاميرا", "camera", "كاميرا"));
        quick.add(new SlotSpec("home_gallery", "▣", "الصور", "gallery", "photos", "صور"));
        body.addView(buildSlotGrid(quick, 3));
        body.addView(spacer(12));
        body.addView(sectionTitle("لوحاتي"));
        LinearLayout gateways = new LinearLayout(this);
        gateways.setOrientation(LinearLayout.HORIZONTAL);
        gateways.setGravity(Gravity.CENTER);
        gateways.addView(actionTile("⚕", "العمل", "المستشفى والمهام", v -> switchPage(PAGE_WORK)), weightedTileParams());
        gateways.addView(actionTile("♥", "العائلة", "الدراسة والمنزل", v -> switchPage(PAGE_FAMILY)), weightedTileParams());
        gateways.addView(actionTile("✈", "السفر", "الرحلات والحجوزات", v -> openOrAssign("home_travel", "السفر", "maps", "booking", "travel")), weightedTileParams());
        gateways.addView(actionTile("▰", "السيارة", "خرائط ووثائق", v -> openOrAssign("home_car", "السيارة", "maps", "car", "سيارة")), weightedTileParams());
        body.addView(gateways);
        body.addView(spacer(12));
        body.addView(createEditableNoteCard("ملاحظة اليوم", "note_home", "اضغط هنا لإضافة ملاحظة سريعة لليوم."));
        body.addView(spacer(10));
        Button apps = premiumButton("جميع التطبيقات");
        apps.setOnClickListener(v -> showAppDrawer());
        body.addView(apps, fullButtonParams());
    }

    private View buildThreeInfoRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.addView(infoCard("البطارية", batteryPercent() + "%", "حالة الهاتف", accentGold), weightedTileParams());
        row.addView(actionTile("☾", "الطقس • تبوك", "افتح تطبيق الطقس", v -> openOrAssign("tool_weather", "الطقس", "weather", "طقس")), weightedTileParams());
        row.addView(actionTile("◈", "مواقيت الصلاة", "اربط تطبيق الصلاة", v -> openOrAssign("tool_prayer", "الصلاة", "prayer", "اذان", "أذان", "صلاتي")), weightedTileParams());
        return row;
    }

    private void buildWork(LinearLayout body) {
        body.addView(createEditableNoteCard("المناوبة اليوم", "note_work_shift", "اضغط لتسجيل المناوبة أو برنامج العمل اليومي."));
        body.addView(spacer(12));
        body.addView(sectionTitle("تطبيقات العمل"));
        List<SlotSpec> slots = new ArrayList<>();
        slots.add(new SlotSpec("work_arcus", "A", "ARCUS", "arcus"));
        slots.add(new SlotSpec("work_or", "▦", "OR Schedule", "calendar", "schedule"));
        slots.add(new SlotSpec("work_dsu", "✚", "DSU", "dsu"));
        slots.add(new SlotSpec("work_files", "▰", "الملفات", "files", "drive", "ملفات"));
        slots.add(new SlotSpec("work_mail", "✉", "البريد", "gmail", "outlook", "mail", "بريد"));
        slots.add(new SlotSpec("work_chatgpt", "✦", "ChatGPT", "openai", "chatgpt"));
        slots.add(new SlotSpec("work_whatsapp", "●", "واتساب العمل", "whatsapp"));
        slots.add(new SlotSpec("work_pubmed", "P", "PubMed", "chrome", "browser"));
        slots.add(new SlotSpec("work_calc", "÷", "الحاسبة", "calculator", "حاسبة"));
        body.addView(buildSlotGrid(slots, 3));
        body.addView(spacer(12));
        body.addView(createEditableNoteCard("ملاحظات سريعة", "note_work_quick", "نتائج، حالات، اتصالات أو نقاط تحتاج متابعة."));
        body.addView(spacer(10));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(actionTile("⚙", "إعداد Home", "اختر اللانشر الافتراضي", v -> openHomeSettings()), weightedTileParams());
        row.addView(actionTile("▦", "كل التطبيقات", "بحث وفتح أي تطبيق", v -> showAppDrawer()), weightedTileParams());
        body.addView(row);
    }

    private void buildFamily(LinearLayout body) {
        TextView quote = new TextView(this);
        quote.setText("أهلي سندي وقوتي");
        quote.setTextColor(accentGold);
        quote.setTextSize(20);
        quote.setGravity(Gravity.CENTER);
        quote.setPadding(0, dp(6), 0, dp(10));
        quote.setTypeface(null, android.graphics.Typeface.BOLD);
        body.addView(quote);
        body.addView(sectionTitle("العائلة والمنزل"));
        List<SlotSpec> slots = new ArrayList<>();
        slots.add(new SlotSpec("family_school", "⌂", "جدول الدراسة", "school", "classroom", "مدرستي"));
        slots.add(new SlotSpec("family_exams", "✓", "الاختبارات", "exams", "اختبار"));
        slots.add(new SlotSpec("family_children", "♥", "تطبيقات البنات", "student", "تعليم"));
        slots.add(new SlotSpec("family_magazine", "▤", "المجلة العائلية", "magazine", "مجلة"));
        slots.add(new SlotSpec("family_photos", "▣", "الصور", "gallery", "photos", "صور"));
        slots.add(new SlotSpec("family_food", "♨", "جدول الوجبات", "family-food", "food", "وجبات"));
        slots.add(new SlotSpec("family_money", "$", "المصاريف", "budget", "money", "مصروف"));
        slots.add(new SlotSpec("family_notes", "✎", "الملاحظات", "notes", "keep", "ملاحظات"));
        body.addView(buildSlotGrid(slots, 3));
        body.addView(spacer(12));
        body.addView(createEditableNoteCard("رسالة عائلية", "note_family", "اكتب هنا موعدًا أو مهمة عائلية أو تذكيرًا مهمًا."));
        body.addView(spacer(10));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(actionTile("▦", "جميع التطبيقات", "بحث سريع", v -> showAppDrawer()), weightedTileParams());
        row.addView(actionTile("✦", "ChatGPT", "مساعد الأسرة", v -> openOrAssign("family_chatgpt", "ChatGPT", "openai", "chatgpt")), weightedTileParams());
        body.addView(row);
    }

    private void buildMe(LinearLayout body) {
        body.addView(sectionTitle("حالتي"));
        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.addView(infoCard("البطارية", batteryPercent() + "%", "الآن", accentGold), weightedTileParams());
        status.addView(infoCard("الثيم", themeName(), "نشط", themeIndex == THEME_RED ? accentRed : accentBlue), weightedTileParams());
        status.addView(infoCard("التطبيقات", String.valueOf(allApps.size()), "مثبتة", accentGold), weightedTileParams());
        body.addView(status);
        body.addView(spacer(12));
        body.addView(sectionTitle("تخصيص الثيم"));
        LinearLayout themes = new LinearLayout(this);
        themes.setOrientation(LinearLayout.HORIZONTAL);
        themes.addView(themeButton("Fusion", THEME_FUSION), weightedTileParams());
        themes.addView(themeButton("Royal Blue", THEME_ROYAL), weightedTileParams());
        themes.addView(themeButton("Red Arena", THEME_RED), weightedTileParams());
        body.addView(themes);
        body.addView(spacer(12));
        body.addView(sectionTitle("أنا"));
        List<SlotSpec> slots = new ArrayList<>();
        slots.add(new SlotSpec("me_quran", "☰", "القرآن", "quran", "قرآن"));
        slots.add(new SlotSpec("me_health", "♥", "الصحة", "health", "صحتي"));
        slots.add(new SlotSpec("me_money", "$", "المال", "budget", "bank", "بنك"));
        slots.add(new SlotSpec("me_travel", "✈", "السفر", "booking", "travel", "maps"));
        slots.add(new SlotSpec("me_car", "▰", "السيارة", "maps", "car", "سيارة"));
        slots.add(new SlotSpec("me_chatgpt", "✦", "ChatGPT", "openai", "chatgpt"));
        body.addView(buildSlotGrid(slots, 3));
        body.addView(spacer(12));
        LinearLayout actions1 = new LinearLayout(this);
        actions1.setOrientation(LinearLayout.HORIZONTAL);
        actions1.addView(actionTile("▦", "جميع التطبيقات", "درج التطبيقات", v -> showAppDrawer()), weightedTileParams());
        actions1.addView(actionTile("⌂", "الشاشة الرئيسية", "تعيين Arena Launcher", v -> openHomeSettings()), weightedTileParams());
        body.addView(actions1);
        body.addView(spacer(8));
        LinearLayout actions2 = new LinearLayout(this);
        actions2.setOrientation(LinearLayout.HORIZONTAL);
        actions2.addView(actionTile("⚙", "إعدادات أندرويد", "إعدادات الجهاز", v -> startActivity(new Intent(Settings.ACTION_SETTINGS))), weightedTileParams());
        actions2.addView(actionTile("↺", "إعادة تعيين الاختصارات", "مسح التخصيصات فقط", v -> confirmResetSlots()), weightedTileParams());
        body.addView(actions2);
        body.addView(spacer(10));
        body.addView(createEditableNoteCard("هدفي", "note_me", "كل يوم خطوة نحو نسخة أفضل."));
    }

    private String themeName() {
        if (themeIndex == THEME_ROYAL) return "Royal Blue";
        if (themeIndex == THEME_RED) return "Red Arena";
        return "Fusion";
    }

    private View themeButton(String name, int index) {
        LinearLayout tile = (LinearLayout) actionTile("◆", name, index == themeIndex ? "مفعّل" : "اضغط للتفعيل", v -> {
            themeIndex = index;
            prefs.edit().putInt("theme", index).apply();
            renderPage(currentPage);
        });
        if (index == themeIndex) tile.setBackground(roundRect(0xD01C2028, 20, accentGold, 2));
        return tile;
    }

    private View sectionTitle(String title) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(accentGold);
        t.setTextSize(18);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.RIGHT);
        t.setPadding(dp(4), dp(2), dp(4), dp(8));
        return t;
    }

    private View buildSlotGrid(List<SlotSpec> specs, int columns) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        for (int i = 0; i < specs.size(); i += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            for (int j = 0; j < columns; j++) {
                int idx = i + j;
                if (idx < specs.size()) row.addView(slotTile(specs.get(idx)), weightedTileParams());
                else row.addView(new View(this), weightedTileParams());
            }
            wrapper.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(108)));
            if (i + columns < specs.size()) wrapper.addView(spacer(6));
        }
        return wrapper;
    }

    private View slotTile(SlotSpec spec) {
        AppEntry app = resolveSlot(spec);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(5), dp(7), dp(5), dp(7));
        tile.setBackground(roundRect(cardFill, 20, app != null ? 0x8859A5FF : cardStroke, 1));
        if (app != null) {
            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(app.info.loadIcon(getPackageManager())); }
            catch (Exception ignored) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            tile.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
        } else {
            TextView glyph = new TextView(this);
            glyph.setText(spec.glyph);
            glyph.setTextSize(28);
            glyph.setTextColor(accentGold);
            glyph.setGravity(Gravity.CENTER);
            tile.addView(glyph, new LinearLayout.LayoutParams(dp(48), dp(44)));
        }
        TextView name = new TextView(this);
        name.setText(app != null ? app.label : spec.label);
        name.setTextColor(textPrimary);
        name.setTextSize(11);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);
        name.setPadding(dp(2), dp(4), dp(2), 0);
        tile.addView(name, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(26)));
        tile.setOnClickListener(v -> {
            AppEntry resolved = resolveSlot(spec);
            if (resolved != null) launch(resolved); else chooseApp(spec);
        });
        tile.setOnLongClickListener(v -> { chooseApp(spec); return true; });
        return tile;
    }

    private View actionTile(String glyph, String title, String subtitle, View.OnClickListener listener) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(8), dp(6), dp(8));
        tile.setBackground(roundRect(cardFill, 20, cardStroke, 1));
        TextView g = new TextView(this);
        g.setText(glyph);
        g.setTextColor(accentGold);
        g.setTextSize(24);
        g.setGravity(Gravity.CENTER);
        tile.addView(g, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(34)));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(textPrimary);
        t.setTextSize(12);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        tile.addView(t, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)));
        TextView s = new TextView(this);
        s.setText(subtitle);
        s.setTextColor(textSecondary);
        s.setTextSize(9);
        s.setGravity(Gravity.CENTER);
        s.setMaxLines(2);
        tile.addView(s, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(30)));
        tile.setOnClickListener(listener);
        return tile;
    }

    private View infoCard(String title, String value, String subtitle, int valueColor) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(8), dp(6), dp(8));
        tile.setBackground(roundRect(cardFill, 20, cardStroke, 1));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(textSecondary);
        t.setTextSize(10);
        t.setGravity(Gravity.CENTER);
        tile.addView(t, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(valueColor);
        v.setTextSize(24);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        tile.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));
        TextView s = new TextView(this);
        s.setText(subtitle);
        s.setTextColor(textSecondary);
        s.setTextSize(9);
        s.setGravity(Gravity.CENTER);
        tile.addView(s, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)));
        return tile;
    }

    private View createEditableNoteCard(String title, String key, String defaultText) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(roundRect(0xC00D1118, 20, 0x557B95B8, 1));
        TextView h = new TextView(this);
        h.setText(title + "   ✎");
        h.setTextColor(accentGold);
        h.setTextSize(14);
        h.setTypeface(null, android.graphics.Typeface.BOLD);
        h.setGravity(Gravity.RIGHT);
        card.addView(h);
        TextView body = new TextView(this);
        body.setText(prefs.getString(key, defaultText));
        body.setTextColor(0xF0FFFFFF);
        body.setTextSize(13);
        body.setGravity(Gravity.RIGHT);
        body.setPadding(0, dp(8), 0, 0);
        body.setMinHeight(dp(44));
        card.addView(body, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.setOnClickListener(v -> editNote(title, key, body, defaultText));
        return card;
    }

    private void editNote(String title, String key, TextView target, String defaultText) {
        EditText input = new EditText(this);
        input.setText(prefs.getString(key, defaultText));
        input.setSelection(input.getText().length());
        input.setGravity(Gravity.RIGHT | Gravity.TOP);
        input.setMinLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        new AlertDialog.Builder(this).setTitle(title).setView(input)
                .setPositiveButton("حفظ", (d, w) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) value = defaultText;
                    prefs.edit().putString(key, value).apply();
                    target.setText(value);
                }).setNegativeButton("إلغاء", null).show();
    }

    private Button premiumButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.rgb(8, 12, 18));
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setBackground(roundRect(accentGold, 20, Color.WHITE, 1));
        return b;
    }

    private LinearLayout.LayoutParams fullButtonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, dp(4), 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams weightedTileParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(100), 1f);
        lp.setMargins(dp(3), dp(2), dp(3), dp(2));
        return lp;
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(heightDp)));
        return v;
    }

    private GradientDrawable roundRect(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), strokeColor);
        return g;
    }

    private int batteryPercent() {
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) return 0;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) return 0;
        return Math.round(level * 100f / scale);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        allApps.clear();
        for (ResolveInfo info : list) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            CharSequence labelCs = info.loadLabel(pm);
            String label = labelCs == null ? pkg : labelCs.toString();
            allApps.add(new AppEntry(label, pkg, info.activityInfo.name, info));
        }
        Collections.sort(allApps, Comparator.comparing(a -> a.label.toLowerCase(Locale.getDefault())));
    }

    private AppEntry resolveSlot(SlotSpec spec) {
        String pkg = prefs.getString("slot_pkg_" + spec.key, "");
        String cls = prefs.getString("slot_cls_" + spec.key, "");
        if (!pkg.isEmpty()) {
            for (AppEntry app : allApps) if (app.pkg.equals(pkg) && (cls.isEmpty() || app.cls.equals(cls))) return app;
        }
        return findPreferred(spec.tokens);
    }

    private AppEntry findPreferred(String... tokens) {
        if (tokens == null) return null;
        for (String token : tokens) {
            if (token == null || token.trim().isEmpty()) continue;
            String needle = token.toLowerCase(Locale.getDefault());
            for (AppEntry app : allApps) {
                String hay = (app.label + " " + app.pkg).toLowerCase(Locale.getDefault());
                if (hay.contains(needle)) return app;
            }
        }
        return null;
    }

    private void chooseApp(SlotSpec spec) {
        String[] names = new String[allApps.size() + 1];
        names[0] = "— إزالة التعيين —";
        for (int i = 0; i < allApps.size(); i++) names[i + 1] = allApps.get(i).label;
        new AlertDialog.Builder(this).setTitle("اختر تطبيقًا لـ " + spec.label).setItems(names, (dialog, which) -> {
            if (which == 0) {
                prefs.edit().remove("slot_pkg_" + spec.key).remove("slot_cls_" + spec.key).apply();
            } else {
                AppEntry app = allApps.get(which - 1);
                prefs.edit().putString("slot_pkg_" + spec.key, app.pkg).putString("slot_cls_" + spec.key, app.cls).apply();
            }
            renderPage(currentPage);
        }).show();
    }

    private void openOrAssign(String key, String label, String... tokens) {
        SlotSpec spec = new SlotSpec(key, "◆", label, tokens);
        AppEntry app = resolveSlot(spec);
        if (app != null) launch(app); else chooseApp(spec);
    }

    private void launch(AppEntry app) {
        try {
            Intent in = new Intent(Intent.ACTION_MAIN);
            in.addCategory(Intent.CATEGORY_LAUNCHER);
            in.setComponent(new ComponentName(app.pkg, app.cls));
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(in);
        } catch (Exception ex) {
            try {
                Intent fallback = getPackageManager().getLaunchIntentForPackage(app.pkg);
                if (fallback != null) startActivity(fallback); else throw ex;
            } catch (Exception e) { Toast.makeText(this, "تعذر فتح التطبيق", Toast.LENGTH_SHORT).show(); }
        }
    }

    private void showAppDrawer() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        wrap.setPadding(dp(12), dp(14), dp(12), dp(12));
        wrap.setBackground(new ArenaBackground(themeIndex));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        wrap.addView(head, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        TextView title = new TextView(this);
        title.setText("جميع التطبيقات");
        title.setTextColor(accentGold);
        title.setTextSize(21);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        head.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        Button close = new Button(this);
        close.setText("×");
        close.setTextSize(22);
        close.setTextColor(Color.WHITE);
        close.setBackground(roundRect(0x66111111, 20, 0x55FFFFFF, 1));
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close, new LinearLayout.LayoutParams(dp(50), dp(44)));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("ابحث في التطبيقات...");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(0xAAFFFFFF);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(roundRect(0xA0121620, 20, 0x556F89A8, 1));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        searchLp.setMargins(0, dp(4), 0, dp(10));
        wrap.addView(search, searchLp);
        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setHorizontalSpacing(dp(4));
        grid.setVerticalSpacing(dp(8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setPadding(0, dp(4), 0, dp(8));
        grid.setClipToPadding(false);
        ArrayList<AppEntry> shown = new ArrayList<>(allApps);
        AppDrawerAdapter adapter = new AppDrawerAdapter(shown);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) -> { launch(shown.get(pos)); dialog.dismiss(); });
        grid.setOnItemLongClickListener((p, v, pos, id) -> { openAppDetails(shown.get(pos)); return true; });
        wrap.addView(grid, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim().toLowerCase(Locale.getDefault());
                shown.clear();
                if (q.isEmpty()) shown.addAll(allApps);
                else for (AppEntry app : allApps) if ((app.label + " " + app.pkg).toLowerCase(Locale.getDefault()).contains(q)) shown.add(app);
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        dialog.setContentView(wrap);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        });
        dialog.show();
    }

    private void openAppDetails(AppEntry app) {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.pkg))); }
        catch (Exception ignored) {}
    }

    private void openHomeSettings() {
        try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
        catch (ActivityNotFoundException ex) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void confirmResetSlots() {
        new AlertDialog.Builder(this).setTitle("إعادة تعيين الاختصارات")
                .setMessage("سيتم حذف تعيينات التطبيقات المخصصة فقط، ولن تُحذف ملاحظاتك أو الثيم.")
                .setPositiveButton("إعادة تعيين", (d, w) -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    for (String key : prefs.getAll().keySet()) if (key.startsWith("slot_pkg_") || key.startsWith("slot_cls_")) editor.remove(key);
                    editor.apply();
                    renderPage(currentPage);
                }).setNegativeButton("إلغاء", null).show();
    }

    @Override public void onBackPressed() { if (currentPage != PAGE_HOME) switchPage(PAGE_HOME); }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            touchStartX = ev.getX();
            touchStartY = ev.getY();
            touchStartTime = System.currentTimeMillis();
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            float dx = ev.getX() - touchStartX;
            float dy = ev.getY() - touchStartY;
            long dt = System.currentTimeMillis() - touchStartTime;
            if (dt < 450 && Math.abs(dx) > dp(90) && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                if (dx < 0 && currentPage < PAGE_ME) switchPage(currentPage + 1);
                else if (dx > 0 && currentPage > PAGE_HOME) switchPage(currentPage - 1);
            } else if (dt < 450 && dy < -dp(120) && Math.abs(dy) > Math.abs(dx) * 1.5f) showAppDrawer();
        }
        return super.dispatchTouchEvent(ev);
    }

    private class AppDrawerAdapter extends BaseAdapter {
        private final List<AppEntry> data;
        AppDrawerAdapter(List<AppEntry> data) { this.data = data; }
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (convertView == null) {
                cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(2), dp(4), dp(2), dp(4));
                cell.setBackground(roundRect(0x66101620, 18, 0x445F7FA8, 1));
                icon = new ImageView(MainActivity.this);
                icon.setTag("icon");
                icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                cell.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(54)));
                label = new TextView(MainActivity.this);
                label.setTag("label");
                label.setTextColor(Color.WHITE);
                label.setTextSize(10);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(1);
                label.setPadding(0, dp(3), 0, 0);
                cell.addView(label, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)));
            } else {
                cell = (LinearLayout) convertView;
                icon = (ImageView) cell.findViewWithTag("icon");
                label = (TextView) cell.findViewWithTag("label");
            }
            AppEntry app = data.get(position);
            label.setText(app.label);
            try { icon.setImageDrawable(app.info.loadIcon(getPackageManager())); }
            catch (Exception ex) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
            return cell;
        }
    }

    private class ArenaBackground extends Drawable {
        private final int mode;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ArenaBackground(int mode) { this.mode = mode; }
        @Override
        public void draw(Canvas canvas) {
            int w = getBounds().width();
            int h = getBounds().height();
            canvas.drawColor(Color.rgb(3, 6, 10));
            if (mode == THEME_RED) {
                paint.setShader(new RadialGradient(w * 0.22f, h * 0.28f, w * 0.95f, new int[]{0xCC8C0D1E, 0x991B0710, 0x00000000}, new float[]{0f, 0.46f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w * 0.85f, h * 0.75f, w * 0.65f, new int[]{0x553C2E10, 0x00000000}, null, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
            } else if (mode == THEME_ROYAL) {
                paint.setShader(new RadialGradient(w * 0.72f, h * 0.23f, w * 0.95f, new int[]{0xCC073B78, 0x9907132B, 0x00000000}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w * 0.25f, h * 0.8f, w * 0.65f, new int[]{0x55482F0A, 0x00000000}, null, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
            } else {
                paint.setShader(new RadialGradient(w * 0.15f, h * 0.3f, w * 0.78f, new int[]{0xCC990D21, 0x772A0710, 0x00000000}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w * 0.86f, h * 0.32f, w * 0.86f, new int[]{0xCC074B92, 0x7710223D, 0x00000000}, new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
            }
            paint.setShader(new LinearGradient(0, h * 0.72f, 0, h, new int[]{0x00000000, 0xA005070B, 0xF005070B}, new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, w / 700f));
            paint.setColor(0x226F86A0);
            for (int i = 0; i < 8; i++) {
                float y = h * (0.60f + i * 0.045f);
                canvas.drawOval(new RectF(-w * 0.15f, y, w * 1.15f, y + h * 0.09f), paint);
            }
            paint.setColor(0x337B642F);
            canvas.drawLine(0, h * 0.78f, w, h * 0.78f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) {}
        @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
    }

    private static class SlotSpec {
        final String key;
        final String glyph;
        final String label;
        final String[] tokens;
        SlotSpec(String key, String glyph, String label, String... tokens) { this.key = key; this.glyph = glyph; this.label = label; this.tokens = tokens; }
    }

    private static class AppEntry {
        final String label;
        final String pkg;
        final String cls;
        final ResolveInfo info;
        AppEntry(String label, String pkg, String cls, ResolveInfo info) { this.label = label; this.pkg = pkg; this.cls = cls; this.info = info; }
    }
}
