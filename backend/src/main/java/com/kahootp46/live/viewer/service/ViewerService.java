package com.kahootp46.live.viewer.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.kahootp46.live.auth.AccessGrant;
import com.kahootp46.live.auth.IssuedToken;
import com.kahootp46.live.auth.TokenService;
import com.kahootp46.live.game.GameViews.GameSnapshot;
import com.kahootp46.live.game.GameViews.HistoryItem;
import com.kahootp46.live.game.GameViews.LiveQuestion;
import com.kahootp46.live.game.GameViews.RevealedAnswer;
import com.kahootp46.live.shared.error.ResourceNotFoundException;
import com.kahootp46.live.shared.ratelimit.RateLimitProperties;
import com.kahootp46.live.shared.ratelimit.RateLimiter;
import com.kahootp46.live.viewer.dto.ConnectResponse;
import com.kahootp46.live.viewer.dto.CurrentQuestionResponse;
import com.kahootp46.live.viewer.dto.ShownQuestion;
import com.kahootp46.live.viewer.provider.QuizProvider;

/**
 * Viewer use cases. Read-only by design: a viewer can never send an answer or change the game.
 */
@Service
public class ViewerService {

    private static final Duration CONNECT_WINDOW = Duration.ofMinutes(1);

    private final QuizProvider provider;
    private final TokenService tokens;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties limits;

    ViewerService(QuizProvider provider, TokenService tokens, RateLimiter rateLimiter, RateLimitProperties limits) {
        this.provider = provider;
        this.tokens = tokens;
        this.rateLimiter = rateLimiter;
        this.limits = limits;
    }

    /** Rate limited per IP: the 6-digit PIN space must not be enumerable (OWASP API4/API6). */
    public ConnectResponse connect(String pin, String clientIp) {
        rateLimiter.acquire("connect:" + clientIp, limits.connectPerMinute(), CONNECT_WINDOW);
        GameSnapshot snapshot = provider.connect(pin).orElseThrow(ViewerService::gameNotFound);
        IssuedToken token = tokens.issueViewerToken(pin);
        return new ConnectResponse(snapshot.gameId(), pin, token.value(), token.expiresAt(), snapshot);
    }

    public GameSnapshot snapshot(Jwt jwt, String pin) {
        authorize(jwt, pin);
        return provider.connect(pin).orElseThrow(ViewerService::gameNotFound);
    }

    public CurrentQuestionResponse current(Jwt jwt, String pin) {
        GameSnapshot snapshot = snapshot(jwt, pin);
        LiveQuestion question = provider.getCurrentQuestion(pin).orElse(null);
        RevealedAnswer answer = provider.getAnswer(pin).orElse(null);
        return new CurrentQuestionResponse(snapshot.status(), question, answer);
    }

    public List<ShownQuestion> questions(Jwt jwt, String pin) {
        GameSnapshot snapshot = snapshot(jwt, pin);
        List<ShownQuestion> shown = new ArrayList<>();
        for (HistoryItem item : snapshot.history()) {
            shown.add(new ShownQuestion(item.questionNumber(), item.question(), item.choices(), true, item.correctIndex()));
        }
        LiveQuestion current = snapshot.currentQuestion();
        if (current != null && snapshot.currentAnswer() == null) {
            shown.add(new ShownQuestion(current.questionNumber(), current.question(), current.choices(), false, null));
        }
        return shown;
    }

    public List<HistoryItem> history(Jwt jwt, String pin) {
        authorize(jwt, pin);
        return provider.getHistory(pin);
    }

    /** Object-level authorization (OWASP API1): a viewer token only opens the game of its own PIN. */
    private void authorize(Jwt jwt, String pin) {
        AccessGrant grant = tokens.toGrant(jwt);
        if (!grant.canView(pin)) {
            throw new AccessDeniedException("Token not valid for this game");
        }
    }

    private static ResourceNotFoundException gameNotFound() {
        return new ResourceNotFoundException("No existe una partida con ese PIN");
    }
}
