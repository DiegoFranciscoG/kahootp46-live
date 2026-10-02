package com.kahootp46.live.viewer.provider;

import java.util.List;
import java.util.Optional;

import com.kahootp46.live.game.GameViews.GameSnapshot;
import com.kahootp46.live.game.GameViews.HistoryItem;
import com.kahootp46.live.game.GameViews.LiveQuestion;
import com.kahootp46.live.game.GameViews.RevealedAnswer;

/**
 * Source of live game data for the viewer. {@link QuizLiveProvider} reads our own QuizLive engine.
 * A third-party provider can only be added through an official, authorized integration (see
 * docs/arquitectura.md#proveedores): never by scraping, reusing private tokens or joining games as a bot.
 */
public interface QuizProvider {

    /** Resolves a PIN to the current state of its game. */
    Optional<GameSnapshot> connect(String pin);

    Optional<LiveQuestion> getCurrentQuestion(String pin);

    /** Empty until the provider's server has revealed the answer of the current question. */
    Optional<RevealedAnswer> getAnswer(String pin);

    /** Questions already closed, with the answer the server revealed. */
    List<HistoryItem> getHistory(String pin);
}
