package com.kahootp46.live.viewer.provider;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kahootp46.live.game.GameViews.GameSnapshot;
import com.kahootp46.live.game.GameViews.HistoryItem;
import com.kahootp46.live.game.GameViews.LiveQuestion;
import com.kahootp46.live.game.GameViews.RevealedAnswer;
import com.kahootp46.live.game.LiveGameQueries;

/**
 * Provider backed by our own game engine (QuizLive).
 */
@Component
class QuizLiveProvider implements QuizProvider {

    private final LiveGameQueries games;

    QuizLiveProvider(LiveGameQueries games) {
        this.games = games;
    }

    @Override
    public Optional<GameSnapshot> connect(String pin) {
        return games.findSnapshot(pin);
    }

    @Override
    public Optional<LiveQuestion> getCurrentQuestion(String pin) {
        return games.findSnapshot(pin).map(GameSnapshot::currentQuestion);
    }

    @Override
    public Optional<RevealedAnswer> getAnswer(String pin) {
        return games.findSnapshot(pin).map(GameSnapshot::currentAnswer);
    }

    @Override
    public List<HistoryItem> getHistory(String pin) {
        return games.findSnapshot(pin).map(GameSnapshot::history).orElseGet(List::of);
    }
}
