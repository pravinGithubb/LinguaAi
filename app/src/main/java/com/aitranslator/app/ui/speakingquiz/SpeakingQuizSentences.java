package com.aitranslator.app.ui.speakingquiz;

/**
 * Static, bundled English sentence corpus for the Speaking Quiz feature.
 *
 * Why bundled (not Gemini-generated)?
 *   - Zero network cost, instant load, fully offline.
 *   - Deterministic difficulty curves per level.
 *   - No JSON parsing risk.
 *
 * Three levels mirror the Lernix UX (Beginner / Intermediate / Advanced).
 * Each level holds exactly 50 sentences. They are intentionally varied in
 * structure (statements, questions, requests, narratives) to exercise
 * different prosody and rhythm patterns.
 *
 * NOTE: Sentences are English-only by design. The app's TTS still pronounces
 * them in the user's target-language locale, which means non-English target
 * languages will produce phonetically rough output. That is acceptable for
 * v1 — a future iteration can swap this corpus for a per-language pack
 * without touching the rest of the speaking-quiz code.
 */
public final class SpeakingQuizSentences {

    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }

    private SpeakingQuizSentences() { /* no instances */ }

    /** ~3-5 word everyday phrases. */
    public static final String[] BEGINNER = {
            "Hello, how are you",
            "My name is John",
            "I am a student",
            "Where is the bathroom",
            "Thank you very much",
            "I am very hungry",
            "What time is it",
            "I love this music",
            "She is my sister",
            "He works at home",
            "The cat is sleeping",
            "I want some water",
            "It is a sunny day",
            "We are going home",
            "How much does it cost",
            "I do not understand",
            "Please speak slowly",
            "Can you help me",
            "I am from India",
            "The food is delicious",
            "I wake up early",
            "I drink coffee daily",
            "She reads many books",
            "We play in the park",
            "He drives to work",
            "I eat breakfast at eight",
            "The sky is blue today",
            "My phone is on the table",
            "Open the window please",
            "Close the door behind you",
            "I need a new pen",
            "The store is closed now",
            "She speaks three languages",
            "Children are playing outside",
            "I am tired and sleepy",
            "The weather is very cold",
            "Please pass me the salt",
            "I will call you tomorrow",
            "We watched a great movie",
            "He is my best friend",
            "The bus arrives at noon",
            "I lost my house keys",
            "She has long brown hair",
            "We live near the beach",
            "I want to learn music",
            "The baby is crying loudly",
            "Please turn off the lights",
            "I forgot my umbrella again",
            "He bought a new bicycle",
            "Let us meet at six"
    };

    /** ~7-12 word conversational sentences with subordinate clauses. */
    public static final String[] INTERMEDIATE = {
            "I usually wake up at six in the morning",
            "Could you please tell me where the train station is",
            "If it rains tomorrow, we will stay at home",
            "She has been working at this company for five years",
            "The book that I read last weekend was very interesting",
            "I would like to order a coffee and a sandwich please",
            "When I finish my work, I will go for a long walk",
            "He told me that he was planning to move to another city",
            "Even though it was raining, we decided to go hiking anyway",
            "Learning a new language takes time, patience and a lot of practice",
            "The restaurant we visited last night had amazing Italian food",
            "I think we should leave early to avoid the heavy traffic",
            "She was so tired that she fell asleep during the movie",
            "My brother is studying medicine at one of the best universities",
            "Could you remind me to send that email before five o'clock",
            "Despite the cold weather, the children kept playing in the garden",
            "I have never been to Japan, but I really want to visit someday",
            "The teacher explained the lesson clearly so everyone could understand",
            "We need to buy some groceries before the store closes tonight",
            "He apologized for being late and promised it would not happen again",
            "If I had more free time, I would definitely take up painting",
            "The meeting has been postponed until next Wednesday morning",
            "She speaks four languages fluently and is currently learning a fifth",
            "Although the project was difficult, the team finished it on schedule",
            "I am thinking about taking a short vacation next month",
            "The doctor said I should drink more water and exercise regularly",
            "He has a habit of reading the newspaper while drinking his morning tea",
            "We were surprised to see how much the city had changed in ten years",
            "If you press this button, the machine will start automatically",
            "My grandmother used to tell us wonderful stories every night",
            "The film was so emotional that almost everyone in the theater cried",
            "I am sorry, but I will not be able to attend the party tomorrow",
            "She convinced her parents to let her study abroad next semester",
            "The chef prepared a special dish using fresh seasonal ingredients",
            "We should consider all the options before making a final decision",
            "He has been jogging every morning ever since the doctor advised him to",
            "The kids built a huge sandcastle on the beach this afternoon",
            "I had no idea that this small town had such a fascinating history",
            "The flight was delayed by three hours because of bad weather",
            "She managed to fix the broken laptop using only a screwdriver",
            "Most people prefer coffee in the morning and tea in the evening",
            "He works as a software engineer at a large international company",
            "The package should arrive within three to five business days",
            "I really enjoyed the concert, especially the second half of the show",
            "We need to find a quieter place if we want to have a serious conversation",
            "The garden looks beautiful, especially when all the flowers are blooming",
            "She told me that she had finally finished writing her first novel",
            "If you keep practicing every day, you will improve much faster",
            "The hotel staff were extremely friendly and helped us with everything",
            "I cannot believe how quickly this year has gone by"
    };

    /** ~15-25 word complex sentences with idiom, abstraction, formal register. */
    public static final String[] ADVANCED = {
            "Notwithstanding the unprecedented challenges of the past year, our organization has continued to demonstrate remarkable resilience and adaptability",
            "The committee unanimously concluded that the proposed legislation would have far-reaching implications for both consumers and small business owners",
            "Although the experiment yielded results that contradicted the prevailing hypothesis, the researchers maintained that further investigation was warranted",
            "It is imperative that we address the underlying causes of inequality rather than simply treating its most visible symptoms",
            "Her acceptance speech, characterized by its eloquence and humility, struck a chord with audiences across the political spectrum",
            "The novel explores the complex interplay between memory, identity and the passage of time in a deeply moving narrative",
            "Critics argue that the new policy, however well-intentioned, fails to take into account the practical realities faced by frontline workers",
            "Despite the overwhelming evidence presented during the trial, the jury reached a verdict that surprised many legal commentators",
            "The author skillfully weaves together historical fact and personal reminiscence to create a portrait of a vanishing way of life",
            "Reconciling our commitment to economic growth with the urgent need to protect the environment remains one of the defining challenges of our era",
            "The conductor's interpretation of the symphony was so nuanced that even seasoned listeners discovered new dimensions in the familiar score",
            "Few would dispute that technological innovation has transformed virtually every aspect of contemporary life, for better and for worse",
            "The professor's lecture, though dense with theoretical jargon, was peppered with anecdotes that kept the audience thoroughly engaged",
            "It is precisely in moments of profound uncertainty that the strength of our institutions and the integrity of our leaders are truly tested",
            "The architectural firm's award-winning design seamlessly integrates traditional craftsmanship with cutting-edge sustainable materials",
            "Although she had spent decades perfecting her craft, she remained convinced that there was always something new to learn from her students",
            "The diplomatic negotiations, conducted under the strictest confidentiality, ultimately produced an agreement that satisfied all the major stakeholders",
            "Reading widely across disciplines cultivates the kind of intellectual flexibility that is increasingly valued in today's rapidly changing economy",
            "The documentary offers an unflinching look at the human cost of policies that, on paper, were intended to alleviate suffering",
            "Her ability to synthesize disparate ideas into a coherent vision is what truly distinguishes her work from that of her contemporaries",
            "The court's ruling, while narrow in its specific application, sets a precedent that legal scholars will be analyzing for years to come",
            "Such was the magnitude of the achievement that even his most persistent critics felt compelled to offer their grudging admiration",
            "The exhibition juxtaposes ancient artifacts with contemporary installations to provoke a meditation on continuity and rupture in human experience",
            "It would be a serious mistake to underestimate the long-term consequences of decisions made hastily and without adequate consultation",
            "The biography paints a portrait of a man whose public persona masked a private life riddled with contradictions and quiet sorrows",
            "Throughout the protracted negotiations, both sides exhibited a willingness to compromise that had been notably absent in earlier rounds",
            "The implications of artificial intelligence for employment, ethics and personal autonomy demand our most careful and sustained attention",
            "Her novels are remarkable for the way they illuminate the inner lives of characters who might otherwise remain invisible to history",
            "The conference brought together leading thinkers whose perspectives, though sometimes diametrically opposed, enriched the overall discussion considerably",
            "While the proposal has merit in principle, its implementation will require substantial resources that the organization simply does not currently possess",
            "The orchestra's performance was a masterclass in restraint, allowing the composer's emotional architecture to emerge with crystalline clarity",
            "It is a testament to her vision that what once seemed impossibly ambitious is now widely regarded as the industry standard",
            "The investigation uncovered a pattern of systematic negligence that had persisted for years despite repeated warnings from internal auditors",
            "Few writers possess the rare ability to render complex philosophical ideas accessible without sacrificing intellectual rigor or nuance",
            "The treaty represents the culmination of more than a decade of patient diplomacy conducted largely beyond the glare of public attention",
            "Even the most carefully designed regulatory framework will prove ineffective without robust enforcement mechanisms and genuine political will",
            "His memoir is at once a vivid social history and an unsparing self-examination conducted with the detachment of a seasoned observer",
            "The phenomenon of misinformation spreading at unprecedented speed presents democracies with challenges that traditional remedies are ill-equipped to address",
            "She has the rare gift of making everyone in the room feel that their contribution genuinely matters to the outcome of the discussion",
            "The restoration project sought not merely to preserve the building but to reanimate the cultural traditions that had once flourished within its walls",
            "What distinguishes truly exceptional teachers is not the breadth of their knowledge but their capacity to inspire genuine curiosity in others",
            "The analyst's report, dense with statistical detail, ultimately reached conclusions that would have profound implications for the entire industry",
            "Although his early work was largely ignored, posterity has been considerably more generous in its assessment of his artistic legacy",
            "The decision to relocate the headquarters reflects a broader strategic pivot that the leadership has been quietly preparing for several quarters",
            "Her translation captures not only the literal meaning of the original text but also its cadence, irony and underlying emotional currents",
            "The museum's new wing, an architectural triumph in its own right, provides a fittingly grand setting for the masterpieces it houses",
            "It is increasingly difficult to deny that the patterns of extreme weather observed in recent years are consistent with long-standing scientific predictions",
            "The candidate's measured response to a clearly hostile question won her admiration even from those who disagreed with her policy positions",
            "The collaboration between scientists and artists has yielded insights that neither community would likely have arrived at working in isolation",
            "Such reforms, however necessary, will inevitably encounter resistance from constituencies whose interests are bound up with the existing arrangements"
    };

    // ─────────────────────────────────────────────────────────────────────────
    //  Hindi corpus (देवनागरी)
    //
    //  Same difficulty curve as English: short everyday phrases → conversational
    //  sentences → complex/literary sentences. The on-device Hindi STT engine
    //  (when installed) returns Devanagari text, which is what the Levenshtein
    //  scorer compares against — no transliteration step needed.
    //  TTS will pronounce these correctly on devices with a Hindi voice pack.
    // ─────────────────────────────────────────────────────────────────────────

    /** Short Hindi everyday phrases. */
    public static final String[] BEGINNER_HI = {
            "नमस्ते आप कैसे हैं",
            "मेरा नाम राहुल है",
            "मैं एक छात्र हूँ",
            "बाथरूम कहाँ है",
            "बहुत बहुत धन्यवाद",
            "मुझे बहुत भूख लगी है",
            "अभी क्या समय हुआ है",
            "मुझे यह संगीत पसंद है",
            "वह मेरी बहन है",
            "वह घर पर काम करता है",
            "बिल्ली सो रही है",
            "मुझे थोड़ा पानी चाहिए",
            "आज मौसम बहुत अच्छा है",
            "हम घर जा रहे हैं",
            "यह कितने का है",
            "मुझे समझ नहीं आया",
            "कृपया धीरे बोलिए",
            "क्या आप मेरी मदद कर सकते हैं",
            "मैं भारत से हूँ",
            "खाना बहुत स्वादिष्ट है",
            "मैं सुबह जल्दी उठता हूँ",
            "मैं रोज चाय पीता हूँ",
            "वह बहुत किताबें पढ़ती है",
            "हम पार्क में खेलते हैं",
            "वह कार से जाता है",
            "मैं आठ बजे नाश्ता करता हूँ",
            "आज आसमान नीला है",
            "मेरा फोन मेज पर है",
            "खिड़की खोल दीजिए",
            "दरवाजा बंद कर दीजिए",
            "मुझे एक नया पेन चाहिए",
            "दुकान अभी बंद है",
            "वह तीन भाषाएँ बोलती है",
            "बच्चे बाहर खेल रहे हैं",
            "मैं थका हुआ हूँ",
            "आज बहुत ठंड है",
            "कृपया मुझे नमक दीजिए",
            "मैं कल आपको फोन करूँगा",
            "हमने एक अच्छी फिल्म देखी",
            "वह मेरा सबसे अच्छा दोस्त है",
            "बस बारह बजे आती है",
            "मैंने अपनी चाबी खो दी",
            "उसके लंबे काले बाल हैं",
            "हम समुद्र के पास रहते हैं",
            "मुझे संगीत सीखना है",
            "बच्चा जोर से रो रहा है",
            "कृपया लाइट बंद कर दीजिए",
            "मैं अपना छाता भूल गया",
            "उसने नई साइकिल खरीदी",
            "हम छह बजे मिलते हैं"
    };

    /** Conversational Hindi sentences. */
    public static final String[] INTERMEDIATE_HI = {
            "मैं आमतौर पर सुबह छह बजे उठ जाता हूँ",
            "क्या आप मुझे बता सकते हैं कि रेलवे स्टेशन कहाँ है",
            "अगर कल बारिश हुई तो हम घर पर ही रहेंगे",
            "वह इस कंपनी में पाँच साल से काम कर रही है",
            "पिछले हफ्ते मैंने जो किताब पढ़ी वह बहुत दिलचस्प थी",
            "मुझे एक कॉफी और एक सैंडविच चाहिए",
            "जब मैं अपना काम खत्म कर लूँगा तो मैं घूमने जाऊँगा",
            "उसने मुझे बताया कि वह दूसरे शहर जाने की योजना बना रहा है",
            "बारिश होने के बावजूद हमने पहाड़ों पर जाने का फैसला किया",
            "नई भाषा सीखने में समय धैर्य और बहुत अभ्यास लगता है",
            "जिस रेस्तराँ में हम कल रात गए थे वहाँ बहुत स्वादिष्ट खाना मिलता है",
            "मुझे लगता है हमें भारी ट्रैफिक से बचने के लिए जल्दी निकलना चाहिए",
            "वह इतनी थकी हुई थी कि फिल्म के दौरान सो गई",
            "मेरा भाई एक प्रसिद्ध विश्वविद्यालय में चिकित्सा की पढ़ाई कर रहा है",
            "क्या आप मुझे पाँच बजे से पहले वह ईमेल भेजने की याद दिला सकते हैं",
            "ठंडे मौसम के बावजूद बच्चे बगीचे में खेलते रहे",
            "मैं कभी जापान नहीं गया लेकिन मैं वहाँ जाना चाहता हूँ",
            "शिक्षक ने पाठ इतनी स्पष्टता से समझाया कि सब समझ गए",
            "हमें आज रात दुकान बंद होने से पहले कुछ सामान खरीदना है",
            "उसने देर से आने के लिए माफी माँगी और कहा फिर ऐसा नहीं होगा",
            "अगर मेरे पास और समय होता तो मैं चित्रकला सीखता",
            "मीटिंग अगले बुधवार सुबह तक टाल दी गई है",
            "वह चार भाषाएँ धाराप्रवाह बोलती है और पाँचवीं सीख रही है",
            "हालाँकि प्रोजेक्ट कठिन था टीम ने उसे समय पर पूरा किया",
            "मैं अगले महीने एक छोटी छुट्टी लेने के बारे में सोच रहा हूँ",
            "डॉक्टर ने कहा मुझे ज्यादा पानी पीना चाहिए और नियमित व्यायाम करना चाहिए",
            "उसकी आदत है कि वह सुबह की चाय के साथ अखबार पढ़ता है",
            "हम यह देखकर हैरान थे कि शहर दस सालों में कितना बदल गया",
            "अगर आप यह बटन दबाएँगे तो मशीन अपने आप शुरू हो जाएगी",
            "मेरी दादी हमें हर रात अद्भुत कहानियाँ सुनाया करती थीं",
            "फिल्म इतनी भावुक थी कि सिनेमा हॉल में लगभग सभी रो पड़े",
            "मुझे माफ करना मैं कल पार्टी में नहीं आ पाऊँगा",
            "उसने अपने माता पिता को विदेश पढ़ने जाने के लिए मना लिया",
            "रसोइए ने ताजी सामग्री से एक खास व्यंजन बनाया",
            "अंतिम फैसला लेने से पहले हमें सभी विकल्पों पर विचार करना चाहिए",
            "जब से डॉक्टर ने सलाह दी है वह रोज सुबह दौड़ता है",
            "बच्चों ने आज दोपहर समुद्र तट पर एक बड़ा रेत का किला बनाया",
            "मुझे नहीं पता था कि इस छोटे शहर का इतिहास इतना दिलचस्प है",
            "खराब मौसम के कारण उड़ान तीन घंटे देरी से चली",
            "उसने केवल एक पेचकस से टूटा हुआ लैपटॉप ठीक कर दिया",
            "ज्यादातर लोग सुबह कॉफी और शाम को चाय पीना पसंद करते हैं",
            "वह एक बड़ी अंतरराष्ट्रीय कंपनी में सॉफ्टवेयर इंजीनियर का काम करता है",
            "पैकेज तीन से पाँच कार्य दिवसों के भीतर पहुँच जाएगा",
            "मुझे संगीत समारोह बहुत पसंद आया खासकर दूसरा भाग",
            "अगर हमें गंभीर बातचीत करनी है तो हमें शांत जगह चाहिए",
            "बगीचा बहुत सुंदर लगता है खासकर जब सारे फूल खिलते हैं",
            "उसने मुझे बताया कि उसने आखिरकार अपना पहला उपन्यास लिख लिया",
            "अगर आप रोज अभ्यास करते रहेंगे तो आप तेजी से सुधार करेंगे",
            "होटल के कर्मचारी बहुत मिलनसार थे और हमारी हर मदद करते रहे",
            "मुझे विश्वास नहीं होता कि यह साल कितनी जल्दी बीत गया"
    };

    /** Complex/literary Hindi sentences. */
    public static final String[] ADVANCED_HI = {
            "पिछले वर्ष की अभूतपूर्व चुनौतियों के बावजूद हमारे संगठन ने उल्लेखनीय लचीलापन दिखाया है",
            "समिति ने सर्वसम्मति से निष्कर्ष निकाला कि प्रस्तावित कानून के दूरगामी परिणाम होंगे",
            "हालाँकि प्रयोग के परिणाम मूल परिकल्पना के विपरीत थे शोधकर्ताओं ने आगे जाँच करने का निर्णय लिया",
            "यह आवश्यक है कि हम असमानता के मूल कारणों का समाधान करें न कि केवल लक्षणों का",
            "उसका स्वीकृति भाषण जो वाक्पटुता और विनम्रता से भरा था सभी को बहुत भाया",
            "उपन्यास स्मृति पहचान और समय के बीच के जटिल संबंधों को गहराई से दर्शाता है",
            "आलोचकों का कहना है कि नई नीति चाहे जितनी अच्छी मंशा से बनी हो व्यावहारिक नहीं है",
            "मुकदमे में पेश किए गए साक्ष्यों के बावजूद जूरी ने ऐसा फैसला सुनाया जिसने सबको चौंकाया",
            "लेखक ऐतिहासिक तथ्य और व्यक्तिगत स्मरण को कुशलता से बुनकर एक मार्मिक चित्र प्रस्तुत करता है",
            "आर्थिक विकास के साथ पर्यावरण की रक्षा का संतुलन हमारे युग की प्रमुख चुनौती है",
            "संचालक की सिम्फनी की व्याख्या इतनी सूक्ष्म थी कि श्रोताओं ने उसमें नए आयाम खोजे",
            "कम लोग यह कहेंगे कि तकनीकी नवाचार ने जीवन के हर पहलू को नहीं बदला है",
            "प्रोफेसर का व्याख्यान सैद्धांतिक शब्दों से भरा होने पर भी रोचक उदाहरणों से सजा था",
            "गहरी अनिश्चितता के समय में ही हमारे संस्थानों और नेताओं की वास्तविक परीक्षा होती है",
            "वास्तुकला फर्म का पुरस्कार विजेता डिज़ाइन पारंपरिक शिल्प और आधुनिक सामग्री को जोड़ता है",
            "हालाँकि उसने दशकों अपनी कला को निखारने में बिताए वह हमेशा कुछ नया सीखने को तैयार रहती",
            "गोपनीयता से चली राजनयिक वार्ता ने अंततः सभी पक्षों को संतुष्ट करने वाला समझौता दिया",
            "विभिन्न विषयों में व्यापक अध्ययन वह बौद्धिक लचीलापन देता है जो आज की दुनिया में मूल्यवान है",
            "वृत्तचित्र उन नीतियों की मानवीय कीमत पर बेबाक नज़र डालता है जो दुख कम करने के लिए बनी थीं",
            "अलग अलग विचारों को एक स्पष्ट दृष्टि में पिरोने की उसकी क्षमता उसे विशिष्ट बनाती है",
            "अदालत का फैसला अपने दायरे में सीमित होते हुए भी वर्षों तक अध्ययन का विषय बनेगा",
            "उपलब्धि इतनी बड़ी थी कि उसके आलोचक भी अनिच्छा से प्रशंसा करने को मजबूर हो गए",
            "प्रदर्शनी प्राचीन वस्तुओं को आधुनिक कलाकृतियों के साथ रखकर निरंतरता और बदलाव पर सोचने को कहती है",
            "जल्दबाजी और बिना उचित परामर्श के लिए गए फैसलों के दीर्घकालिक परिणामों को कम आँकना भारी भूल होगी",
            "जीवनी एक ऐसे व्यक्ति का चित्रण करती है जिसका सार्वजनिक रूप उसके निजी जीवन के विरोधाभासों को छुपाता था",
            "लंबी वार्ताओं के दौरान दोनों पक्षों ने समझौते की वह तत्परता दिखाई जो पहले नहीं थी",
            "रोजगार नैतिकता और व्यक्तिगत स्वतंत्रता पर कृत्रिम बुद्धिमत्ता के प्रभाव गंभीर ध्यान माँगते हैं",
            "उसके उपन्यास इस तरह उल्लेखनीय हैं कि वे अदृश्य पात्रों के भीतरी जीवन को रोशन करते हैं",
            "सम्मेलन ने ऐसे विचारकों को एकत्रित किया जिनके दृष्टिकोण भिन्न होते हुए भी चर्चा को समृद्ध बनाते थे",
            "हालाँकि प्रस्ताव सिद्धांत में अच्छा है उसके क्रियान्वयन के लिए संगठन के पास संसाधन नहीं हैं",
            "ऑर्केस्ट्रा का प्रदर्शन संयम का एक उत्कृष्ट उदाहरण था जिसने रचनाकार की भावनाओं को स्पष्ट किया",
            "यह उसकी दूरदृष्टि का प्रमाण है कि जो कभी असंभव लगता था अब उद्योग का मानक माना जाता है",
            "जाँच ने वर्षों से चली आ रही व्यवस्थित लापरवाही के पैटर्न का खुलासा किया",
            "बहुत कम लेखक ऐसे हैं जो कठिन दार्शनिक विचारों को सरलता और गहराई दोनों के साथ प्रस्तुत कर सकें",
            "संधि एक दशक से अधिक की धैर्यपूर्ण कूटनीति का परिणाम है जो जनता की नज़रों से दूर चलती रही",
            "सावधानी से बनाया गया कोई भी नियामक ढाँचा बिना मजबूत क्रियान्वयन और इच्छाशक्ति के बेअसर रहेगा",
            "उसका संस्मरण एक जीवंत सामाजिक इतिहास और बेबाक आत्मनिरीक्षण दोनों है",
            "अभूतपूर्व गति से फैलती गलत सूचना की घटना लोकतंत्रों के सामने नई चुनौतियाँ खड़ी कर रही है",
            "उसमें यह दुर्लभ क्षमता है कि कक्ष में मौजूद हर व्यक्ति को लगे कि उसकी बात मायने रखती है",
            "पुनर्स्थापना परियोजना का उद्देश्य भवन को बचाना ही नहीं उसकी सांस्कृतिक परंपराओं को पुनर्जीवित करना भी था",
            "असाधारण शिक्षकों की पहचान उनके ज्ञान से नहीं बल्कि छात्रों में जिज्ञासा जगाने की क्षमता से होती है",
            "विश्लेषक की रिपोर्ट आँकड़ों से भरी होने पर भी ऐसे निष्कर्षों पर पहुँची जो उद्योग के लिए महत्वपूर्ण हैं",
            "हालाँकि उसका प्रारंभिक काम अनदेखा रहा बाद की पीढ़ियों ने उसकी कलात्मक विरासत को मान्यता दी",
            "मुख्यालय बदलने का निर्णय उस व्यापक रणनीति का हिस्सा है जिसे नेतृत्व चुपचाप तैयार कर रहा था",
            "उसका अनुवाद मूल पाठ के अर्थ ही नहीं उसकी लय व्यंग्य और भावनाओं को भी पकड़ता है",
            "संग्रहालय का नया भाग जो स्वयं एक वास्तुशिल्प उपलब्धि है उसमें रखी कृतियों के योग्य है",
            "हाल के वर्षों में देखे गए असामान्य मौसम के पैटर्न वैज्ञानिक भविष्यवाणियों के अनुरूप हैं",
            "उम्मीदवार ने एक स्पष्ट रूप से शत्रुतापूर्ण प्रश्न का संतुलित उत्तर देकर विरोधियों का भी सम्मान जीता",
            "वैज्ञानिकों और कलाकारों के सहयोग ने वे अंतर्दृष्टियाँ दीं जो अकेले काम करते हुए नहीं मिलतीं",
            "ऐसे सुधार चाहे कितने भी आवश्यक हों मौजूदा हितधारकों के विरोध का सामना अवश्य करेंगे"
    };

    /**
     * Returns the corpus for the given level and language code.
     *
     * @param level     difficulty band; null defaults to BEGINNER
     * @param langCode  ISO-ish language code from {@code PrefsManager.getTargetLanguageCode()}.
     *                  Currently "hi" returns the Hindi corpus; anything else
     *                  (including null) falls back to English.
     */
    public static String[] forLevel(Level level, String langCode) {
        boolean hindi = "hi".equalsIgnoreCase(langCode);
        if (level == null) return hindi ? BEGINNER_HI : BEGINNER;
        switch (level) {
            case ADVANCED:     return hindi ? ADVANCED_HI     : ADVANCED;
            case INTERMEDIATE: return hindi ? INTERMEDIATE_HI : INTERMEDIATE;
            case BEGINNER:
            default:           return hindi ? BEGINNER_HI     : BEGINNER;
        }
    }

    /** Back-compat: defaults to English. Prefer the (Level, langCode) overload. */
    public static String[] forLevel(Level level) {
        return forLevel(level, "en");
    }
}
