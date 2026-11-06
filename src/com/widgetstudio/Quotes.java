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
            {"The secret of getting ahead is getting started.", "Mark Twain"},
            {"Simplicity is the ultimate sophistication.", "Leonardo da Vinci"},
            {"Be yourself; everyone else is already taken.", "Oscar Wilde"},
            {"Whether you think you can, or you think you can't, you're right.", "Henry Ford"},
            {"The future depends on what you do today.", "Mahatma Gandhi"},
            {"Live as if you were to die tomorrow. Learn as if you were to live forever.", "Mahatma Gandhi"},
            {"Act as if what you do makes a difference. It does.", "William James"},
            {"Knowing is not enough; we must apply.", "Johann Wolfgang von Goethe"},
            {"Nothing in life is to be feared, it is only to be understood.", "Marie Curie"},
            {"The best time to plant a tree was 20 years ago. The second best time is now.", "Chinese proverb"},
            {"Wherever you go, go with all your heart.", "Confucius"},
            {"It does not matter how slowly you go as long as you do not stop.", "Confucius"},
            {"We suffer more often in imagination than in reality.", "Seneca"},
            {"Luck is what happens when preparation meets opportunity.", "Seneca"},
            {"You have power over your mind, not outside events. Realize this, and you will find strength.", "Marcus Aurelius"},
            {"The happiness of your life depends upon the quality of your thoughts.", "Marcus Aurelius"},
            {"First say to yourself what you would be; and then do what you have to do.", "Epictetus"},
            {"No man ever steps in the same river twice.", "Heraclitus"},
            {"Knowing yourself is the beginning of all wisdom.", "Aristotle"},
            {"Nature does not hurry, yet everything is accomplished.", "Lao Tzu"},
            {"I have not failed. I've just found 10,000 ways that won't work.", "Thomas Edison"},
            {"Genius is one percent inspiration and ninety-nine percent perspiration.", "Thomas Edison"},
            {"Start where you are. Use what you have. Do what you can.", "Arthur Ashe"},
            {"Courage is grace under pressure.", "Ernest Hemingway"},
            {"You may encounter many defeats, but you must not be defeated.", "Maya Angelou"},
            {"There is no greater agony than bearing an untold story inside you.", "Maya Angelou"},
            {"Keep your face always toward the sunshine, and shadows will fall behind you.", "Walt Whitman"},
            {"What you seek is seeking you.", "Rumi"},
            {"Take up one idea. Make that one idea your life.", "Swami Vivekananda"},
            {"You cannot believe in God until you believe in yourself.", "Swami Vivekananda"},
            {"Excellence is a continuous process and not an accident.", "A.P.J. Abdul Kalam"},
            {"Believe you can and you're halfway there.", "Theodore Roosevelt"},
            {"Comparison is the thief of joy.", "Theodore Roosevelt"},
            {"In three words I can sum up everything I've learned about life: it goes on.", "Robert Frost"},
            {"Talent wins games, but teamwork and intelligence win championships.", "Michael Jordan"},
            {"The best way to predict the future is to invent it.", "Alan Kay"},
            {"Make it work, make it right, make it fast.", "Kent Beck"},
            {"Programs must be written for people to read, and only incidentally for machines to execute.", "Harold Abelson"},
            {"The man who moves a mountain begins by carrying away small stones.", "Confucius"},
            {"A ship in harbor is safe, but that is not what ships are built for.", "John A. Shedd"},
            {"Little by little, one travels far.", "J.R.R. Tolkien"},
            {"All we have to decide is what to do with the time that is given us.", "J.R.R. Tolkien"},
            {"The only impossible journey is the one you never begin.", "Tony Robbins"},
            {"Do one thing every day that scares you.", "Mary Schmich"},
            {"If you are going through hell, keep going.", "Winston Churchill"},
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
            {"Ye na thi hamaari qismat ki visaal-e-yaar hota\nAgar aur jeete rehte yahi intezaar hota", "Mirza Ghalib"},
            {"Hum ne maana ki taghaful na karoge lekin\nKhaak ho jaayenge hum tum ko khabar hone tak", "Mirza Ghalib"},
            {"Rekhte ke tumhi ustaad nahin ho 'Ghalib'\nKehte hain agle zamaane mein koi Mir bhi tha", "Mirza Ghalib"},
            {"Ishq par zor nahin hai ye woh aatish 'Ghalib'\nKi lagaaye na lage aur bujhaaye na bane", "Mirza Ghalib"},
            {"Koi umeed bar nahin aati\nKoi soorat nazar nahin aati", "Mirza Ghalib"},
            {"Mohabbat mein nahin hai farq jeene aur marne ka\nUsi ko dekh kar jeete hain jis kaafir pe dam nikle", "Mirza Ghalib"},
            {"Na tha kuchh to Khuda tha, kuchh na hota to Khuda hota\nDuboya mujh ko hone ne, na hota main to kya hota", "Mirza Ghalib"},
            {"Ibtida-e-ishq hai rota hai kya\nAage aage dekhiye hota hai kya", "Mir Taqi Mir"},
            {"Naazuki us ke lab ki kya kahiye\nPankhudi ik gulaab ki si hai", "Mir Taqi Mir"},
            {"Nahin hai na-umeed 'Iqbal' apni kisht-e-veeraan se\nZara nam ho to ye mitti bahut zarkhez hai saaqi", "Allama Iqbal"},
            {"Tu shaheen hai parwaaz hai kaam tera\nTere saamne aasmaan aur bhi hain", "Allama Iqbal"},
            {"Ghazab kiya tere waade pe aitbaar kiya\nTamaam raat qayaamat ka intezaar kiya", "Daagh Dehlvi"},
            {"Tum mere paas hote ho goya\nJab koi doosra nahin hota", "Momin Khan Momin"},
            {"Umr-e-daraaz maang ke laaye the chaar din\nDo aarzoo mein kat gaye do intezaar mein", "Bahadur Shah Zafar"},
            {"Hum aah bhi karte hain to ho jaate hain badnaam\nWoh qatl bhi karte hain to charcha nahin hota", "Akbar Allahabadi"},
            {"Jo guzaari na ja saki hum se\nHum ne woh zindagi guzaari hai", JAUN},
            {"Bahut nazdeek aati ja rahi ho\nBichhadne ka iraada kar liya kya", JAUN},
            {"Ab to har baat yaad rehti hai\nGhaaliban main kisi ko bhool gaya", JAUN},
            {"Main jo hoon 'Jaun Elia' hoon janaab\nIs ka behad lihaaz kijiyega", JAUN},
            {"Ek muddat se teri yaad bhi aayi na hamein\nAur hum bhool gaye hon tujhe aisa bhi nahin", "Firaq Gorakhpuri"},
            {"Main akela hi chala tha jaanib-e-manzil magar\nLog saath aate gaye aur kaarvaan banta gaya", "Majrooh Sultanpuri"},
            {"Woh to khushboo hai hawaaon mein bikhar jaayega\nMasla phool ka hai phool kidhar jaayega", "Parveen Shakir"},
            {"Suna hai log use aankh bhar ke dekhte hain\nSo us ke shehar mein kuchh din thehar ke dekhte hain", "Ahmad Faraz"},
            {"Mujh se pehli si mohabbat meri mehboob na maang\nMain ne samjha tha ki tu hai to darakhshaan hai hayaat", "Faiz Ahmad Faiz"},
            {"Woh jhooth bol raha tha bade saleeqe se\nMain aitbaar na karta to aur kya karta", "Wasim Barelvi"},
            {"Bulaati hai magar jaane ka nahin\nYe duniya hai idhar jaane ka nahin", "Rahat Indori"},
            {"Dushmani jam kar karo lekin ye gunjaaish rahe\nJab kabhi hum dost ho jaayen to sharminda na hon", "Bashir Badr"},
            {"Kisi ko ghar mila hisse mein ya koi dukaan aayi\nMain ghar mein sab se chhota tha mere hisse mein maa aayi", "Munawwar Rana"},
            {"Har aadmi mein hote hain das bees aadmi\nJis ko bhi dekhna ho kayi baar dekhna", "Nida Fazli"},
            {"Sirf hungaama khada karna mera maqsad nahin\nMeri koshish hai ki ye soorat badalni chahiye", "Dushyant Kumar"},
    };

    /** Dohe by Kabir and Rahim. */
    static final String[][] DOHA = {
            {"Bura jo dekhan main chala, bura na milya koye\nJo dil khoja aapna, mujhse bura na koye", "Kabir"},
            {"Kaal kare so aaj kar, aaj kare so ab\nPal mein pralay hoyegi, bahuri karega kab", "Kabir"},
            {"Dheere dheere re mana, dheere sab kuchh hoye\nMaali seenche sau ghada, ritu aaye phal hoye", "Kabir"},
            {"Pothi padh padh jag mua, pandit bhaya na koye\nDhai aakhar prem ka, padhe so pandit hoye", "Kabir"},
            {"Aisi vaani boliye, man ka aapa khoye\nAuran ko sheetal kare, aapahu sheetal hoye", "Kabir"},
            {"Dukh mein sumiran sab kare, sukh mein kare na koye\nJo sukh mein sumiran kare, to dukh kaahe ko hoye", "Kabir"},
            {"Bada hua to kya hua, jaise ped khajoor\nPanthi ko chhaaya nahin, phal laage ati door", "Kabir"},
            {"Rahiman dhaaga prem ka, mat todo chatkaaye\nToote se phir na jude, jude gaanth pad jaaye", "Rahim"},
            {"Rahiman dekh badan ko, laghu na deejiye daari\nJahaan kaam aave sui, kaha kare talwaari", "Rahim"},
            {"Jo Rahim uttam prakriti, ka kari sakat kusang\nChandan vish vyaapat nahin, lipte rahat bhujang", "Rahim"},
    };

    static final String[] SETS = {"mix", "en", "shayari", "doha", "jaun"};
    static final String[] SET_LABELS = {"Everything mixed", "English quotes", "Shayari", "Kabir & Rahim dohe", "Only Jaun Elia"};
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
            case "doha": for (String[] q : DOHA) out.add(q); break;
            case "jaun": for (String[] q : SHER) if (q[1].equals(JAUN)) out.add(q); break;
            default:
                // English, shayari and dohe in turn so the mix stays balanced
                for (int i = 0; i < Math.max(EN.length, Math.max(SHER.length, DOHA.length)); i++) {
                    if (i < EN.length) out.add(EN[i]);
                    if (i < SHER.length) out.add(SHER[i]);
                    if (i < DOHA.length && i % 3 == 0) out.add(DOHA[i]);
                }
                for (int i = 0; i < DOHA.length; i++) if (i % 3 != 0) out.add(DOHA[i]);
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
