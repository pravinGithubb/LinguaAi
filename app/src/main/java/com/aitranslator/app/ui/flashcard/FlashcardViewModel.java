package com.aitranslator.app.ui.flashcard;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.local.entity.FlashcardSession;
import com.aitranslator.app.data.local.entity.VocabularyWord;
import com.aitranslator.app.data.repository.AppRepository;
import java.util.ArrayList;
import java.util.List;

public class FlashcardViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final MutableLiveData<VocabularyWord> currentCard = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFlipped = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isDeckEmpty = new MutableLiveData<>(false);
    private final MutableLiveData<FlashcardStats> stats = new MutableLiveData<>();

    private List<VocabularyWord> deck = new ArrayList<>();
    private int currentIndex = 0;
    private int knownCount = 0;
    private int unknownCount = 0;
    private long sessionStart;

    public static class FlashcardStats {
        public int total, known, unknown, remaining;
        public FlashcardStats(int total, int known, int unknown, int remaining) {
            this.total = total; this.known = known;
            this.unknown = unknown; this.remaining = remaining;
        }
    }

    public FlashcardViewModel(@NonNull Application app) {
        super(app);
        repository = new AppRepository(app);
        sessionStart = System.currentTimeMillis();
    }

    public LiveData<VocabularyWord> getCurrentCard() { return currentCard; }
    public LiveData<Boolean> getIsFlipped() { return isFlipped; }
    public LiveData<Boolean> getIsDeckEmpty() { return isDeckEmpty; }
    public LiveData<FlashcardStats> getStats() { return stats; }

    public void loadDeck() {
        new Thread(() -> {
            deck = repository.getDueWordsSync(20);
            if (deck.isEmpty()) {
                isDeckEmpty.postValue(true);
            } else {
                currentIndex = 0;
                currentCard.postValue(deck.get(0));
                updateStats();
            }
        }).start();
    }

    public void flipCard() { isFlipped.setValue(!Boolean.TRUE.equals(isFlipped.getValue())); }

    public void markKnown() {
        VocabularyWord word = currentCard.getValue();
        if (word == null) return;
        knownCount++;
        repository.updateMastery(word, true);
        nextCard();
    }

    public void markUnknown() {
        VocabularyWord word = currentCard.getValue();
        if (word == null) return;
        unknownCount++;
        repository.updateMastery(word, false);
        nextCard();
    }

    private void nextCard() {
        isFlipped.setValue(false);
        currentIndex++;
        if (currentIndex >= deck.size()) {
            // Session complete — save stats
            int duration = (int)((System.currentTimeMillis() - sessionStart) / 1000);
            repository.saveFlashcardSession(new FlashcardSession(
                    System.currentTimeMillis(), deck.size(), knownCount, unknownCount, duration));
            isDeckEmpty.setValue(true);
        } else {
            currentCard.setValue(deck.get(currentIndex));
            updateStats();
        }
    }

    private void updateStats() {
        int remaining = deck.size() - currentIndex;
        stats.postValue(new FlashcardStats(deck.size(), knownCount, unknownCount, remaining));
    }

    public int getKnownCount() { return knownCount; }
    public int getUnknownCount() { return unknownCount; }
    public int getDeckSize() { return deck.size(); }
}