package com.widgetstudio;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

/**
 * Built-in quote collection for the Quote widget: English quotes and two-line shayari.
 * The shown quote changes every 6 hours, every day, or only on tap; tapping skips ahead.
 */
final class Quotes {
    static final String JAUN = "Jaun Elia";

    static final String[][] EN = {
            {"The only way to do great work is to love what you do.", "Steve Jobs"},
            {"Stay hungry, stay foolish.", "Steve Jobs"},
            {"The best way out is always through.", "Robert Frost"},
            {"Not all those who wander are lost.", "J.R.R. Tolkien"},
            {"You miss 100% of the shots you don't take.", "Wayne Gretzky"},
            {"Do what you can, with what you have, where you are.", "Theodore Roosevelt"},
            {"It always seems impossible until it's done.", "Nelson Mandela"},
            {"The journey of a thousand miles begins with one step.", "Lao Tzu"},
            {"Arise, awake, and stop not till the goal is reached.", "Swami Vivekananda"},
            {"Strength does not come from physical capacity. It comes from an indomitable will.", "Mahatma Gandhi"},
            {"Dream is not that which you see while sleeping, it is something that does not let you sleep.", "A.P.J. Abdul Kalam"},
            {"If you want to shine like a sun, first burn like a sun.", "A.P.J. Abdul Kalam"},
            {"Happiness is not something ready made. It comes from your own actions.", "Dalai Lama"},
            {"Life is what happens when you're busy making other plans.", "John Lennon"},
            {"Imagination is more important than knowledge.", "Albert Einstein"},
            {"Life is like riding a bicycle. To keep your balance, you must keep moving.", "Albert Einstein"},
            {"Well done is better than well said.", "Benjamin Franklin"},
            {"Turn your wounds into wisdom.", "Oprah Winfrey"},
            {"Everything you've ever wanted is on the other side of fear.", "George Addair"},
            {"Hope is a good thing, maybe the best of things.", "Stephen King"},
            {"Done is better than perfect.", "Sheryl Sandberg"},
            {"Fall seven times, stand up eight.", "Japanese proverb"},
            {"This too shall pass.", "Persian proverb"},
            {"Waste no more time arguing about what a good man should be. Be one.", "Marcus Aurelius"},
            {"What stands in the way becomes the way.", "Marcus Aurelius"},
            {"He who has a why to live can bear almost any how.", "Friedrich Nietzsche"},
            {"The wound is the place where the light enters you.", "Rumi"},
            {"Yesterday I was clever, so I wanted to change the world. Today I am wise, so I am changing myself.", "Rumi"},
            {"The unexamined life is not worth living.", "Socrates"},
            {"We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Will Durant"},
            {"Talk is cheap. Show me the code.", "Linus Torvalds"},
    };

    static final String[][] SHER = {
            // Jaun Elia
            {"Kitne aish se rehte honge kitne itraate honge\nJaane kaise log woh honge jo us ko bhaate honge", JAUN},
            {"Main bhi bahut ajeeb hoon, itna ajeeb hoon ki bas\nKhud ko tabaah kar liya aur malaal bhi nahin", JAUN},
            {"Kaun is ghar ki dekh-bhaal kare\nRoz ik cheez toot jaati hai", JAUN},
            {"Zindagi kis tarah basar hogi\nDil nahin lag raha mohabbat mein", JAUN},
            {"Ab nahin koi baat khatre ki\nAb sabhi ko sabhi se khatra hai", JAUN},
            {"Kya sitam hai ki ab teri soorat\nGhaur karne pe yaad aati hai", JAUN},
            {"Shayad mujhe kisi se mohabbat nahin hui\nLekin yaqeen sab ko dilaata raha hoon main", JAUN},
            {"Hum ko yaaron ne yaad bhi na rakha\n'Jaun' yaaron ke yaar the hum to", JAUN},
            {"Ek hi haadsa to hai aur woh ye ki aaj tak\nBaat nahin kahi gayi, baat nahin suni gayi", JAUN},
            {"Kis liye dekhti ho aaina\nTum to khud se bhi khoobsurat ho", JAUN},
            // Mirza Ghalib
            {"Hazaaron khwahishen aisi ki har khwahish pe dam nikle\nBahut nikle mere armaan lekin phir bhi kam nikle", "Mirza Ghalib"},
            {"Dil-e-naadaan tujhe hua kya hai\nAakhir is dard ki dawa kya hai", "Mirza Ghalib"},
            {"Hum ko maaloom hai jannat ki haqeeqat lekin\nDil ke khush rakhne ko 'Ghalib' ye khayal achha hai", "Mirza Ghalib"},
            {"Ishq ne 'Ghalib' nikamma kar diya\nVarna hum bhi aadmi the kaam ke", "Mirza Ghalib"},
            {"Un ke dekhe se jo aa jaati hai munh par raunaq\nWoh samajhte hain ki beemaar ka haal achha hai", "Mirza Ghalib"},
            // Mir Taqi Mir
            {"Patta patta boota boota haal hamaara jaane hai\nJaane na jaane gul hi na jaane, baagh to saara jaane hai", "Mir Taqi Mir"},
            // Allama Iqbal
            {"Khudi ko kar buland itna ki har taqdeer se pehle\nKhuda bande se khud pooche, bata teri raza kya hai", "Allama Iqbal"},
            {"Sitaaron se aage jahaan aur bhi hain\nAbhi ishq ke imtihaan aur bhi hain", "Allama Iqbal"},
            // Faiz Ahmad Faiz
            {"Aur bhi dukh hain zamaane mein mohabbat ke siva\nRaahaten aur bhi hain vasl ki raahat ke siva", "Faiz Ahmad Faiz"},
            {"Dil na-umeed to nahin, naakaam hi to hai\nLambi hai gham ki shaam, magar shaam hi to hai", "Faiz Ahmad Faiz"},
            // Ahmad Faraz
            {"Ranjish hi sahi dil hi dukhaane ke liye aa\nAa phir se mujhe chhod ke jaane ke liye aa", "Ahmad Faraz"},
            {"Ab ke hum bichhde to shaayad kabhi khwaabon mein milen\nJis tarah sookhe hue phool kitaabon mein milen", "Ahmad Faraz"},
            // Bashir Badr
            {"Ujaale apni yaadon ke hamaare saath rehne do\nNa jaane kis gali mein zindagi ki shaam ho jaaye", "Bashir Badr"},
            {"Koi haath bhi na milaayega jo gale miloge tapaak se\nYe naye mizaaj ka shehar hai, zara faasle se mila karo", "Bashir Badr"},
            // Nida Fazli
            {"Kabhi kisi ko mukammal jahaan nahin milta\nKahin zameen kahin aasmaan nahin milta", "Nida Fazli"},
            {"Ghar se masjid hai bahut door chalo yun kar lein\nKisi rote hue bachche ko hansaaya jaaye", "Nida Fazli"},
            // Rahat Indori
            {"Sabhi ka khoon hai shaamil yahaan ki mitti mein\nKisi ke baap ka Hindustan thodi hai", "Rahat Indori"},
            // Gulzar
            {"Haath chhootein bhi to rishte nahin chhoda karte\nWaqt ki shaakh se lamhe nahin toda karte", "Gulzar"},
            // Dushyant Kumar
            {"Kaun kehta hai aasmaan mein suraakh nahin ho sakta\nEk patthar to tabiyat se uchhaalo yaaro", "Dushyant Kumar"},
    };

    static final String[] SETS = {"mix", "en", "shayari", "jaun"};
    static final String[] SET_LABELS = {"English + Shayari", "English quotes", "Shayari", "Only Jaun Elia"};
    static final String[] EVERY = {"6h", "day", "tap"};
    static final String[] EVERY_LABELS = {"Every 6 hours", "Every day", "Only when I tap"};

    /** True when the widget shows the built-in collection (new widgets, or untouched old ones). */
    static boolean on(Cfg cfg) {
        if (cfg.o.has("mode")) return cfg.s("mode", "collection").equals("collection");
        String t = cfg.s("text", "").trim();
        return t.isEmpty() || t.equals("+Small steps\nevery single day.");
    }

    static String[][] pool(String set) {
        List<String[]> out = new ArrayList<>();
        switch (set) {
            case "en": for (String[] q : EN) out.add(q); break;
            case "shayari": for (String[] q : SHER) out.add(q); break;
            case "jaun": for (String[] q : SHER) if (q[1].equals(JAUN)) out.add(q); break;
            default:
                // alternate English and shayari so the mix stays balanced
                for (int i = 0; i < Math.max(EN.length, SHER.length); i++) {
                    if (i < EN.length) out.add(EN[i]);
                    if (i < SHER.length) out.add(SHER[i]);
                }
        }
        return out.toArray(new String[0][]);
    }

    static long slot(String every, long now) {
        if (every.equals("tap")) return 0;
        long local = now + TimeZone.getDefault().getOffset(now);
        return every.equals("day") ? Math.floorDiv(local, 86400000L) : Math.floorDiv(local, 6 * 3600000L);
    }

    /** {text, author} to show now. */
    static String[] current(Context c, Cfg cfg) {
        String set = cfg.s("set", "mix");
        String[][] p = pool(set);
        if (Render.DEMO) return p[0];
        int n = p.length;
        // fixed shuffled order per widget, so every quote shows once before any repeats
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Random r = new Random(cfg.id * 7919L + set.hashCode());
        for (int i = n - 1; i > 0; i--) { int j = r.nextInt(i + 1); int t = order[i]; order[i] = order[j]; order[j] = t; }
        long k = slot(cfg.s("every", "6h"), System.currentTimeMillis()) + Cfg.prefs(c).getInt("q" + cfg.id, 0);
        return p[order[(int) Math.floorMod(k, (long) n)]];
    }

    /** Skip to the next quote. */
    static void next(Context c, Cfg cfg) {
        android.content.SharedPreferences sp = Cfg.prefs(c);
        sp.edit().putInt("q" + cfg.id, sp.getInt("q" + cfg.id, 0) + 1).apply();
    }

    /** When the quote next changes by itself (for the refresh alarm). */
    static long nextChange(Cfg cfg, long now) {
        String every = cfg.s("every", "6h");
        if (every.equals("tap")) return Long.MAX_VALUE;
        long len = every.equals("day") ? 86400000L : 6 * 3600000L;
        long off = TimeZone.getDefault().getOffset(now);
        return (Math.floorDiv(now + off, len) + 1) * len - off + 5000;
    }
}
