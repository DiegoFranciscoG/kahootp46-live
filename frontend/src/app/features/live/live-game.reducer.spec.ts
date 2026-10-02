import { GameSnapshot, LiveEvent } from '../../core/models/live.models';
import { applyEvent, fromSnapshot, initialState, LiveGameState, secondsLeft, viewerStatus } from './live-game.reducer';

const GAME = 'game-1';
const T0 = Date.parse('2026-10-01T12:00:00Z');

const choices = [
  { index: 0, text: 'Guayaquil' },
  { index: 1, text: 'Quito' },
  { index: 2, text: 'Cuenca' },
  { index: 3, text: 'Loja' },
];

function envelope<T extends LiveEvent['type']>(type: T, seq: number) {
  return { type, protocolVersion: 1, gameId: GAME, pin: '582941', seq, sentAt: '2026-10-01T12:00:00Z' };
}

function questionStarted(seq: number, questionNumber: number): LiveEvent {
  return {
    ...envelope('QUESTION_STARTED', seq),
    questionNumber,
    totalQuestions: 20,
    question: '¿Cuál es la capital del Ecuador?',
    choices,
    timeLimitSeconds: 20,
    startedAt: '2026-10-01T12:00:00Z',
    endsAt: '2026-10-01T12:00:20Z',
  };
}

function answerRevealed(seq: number, questionNumber: number): LiveEvent {
  return {
    ...envelope('ANSWER_REVEALED', seq),
    questionNumber,
    correctIndex: 1,
    correctText: 'Quito',
    reason: 'HOST',
  };
}

function snapshot(overrides: Partial<GameSnapshot> = {}): GameSnapshot {
  return {
    gameId: GAME,
    pin: '582941',
    status: 'LOBBY',
    quizTitle: 'Geografía',
    totalQuestions: 20,
    currentQuestion: null,
    currentAnswer: null,
    history: [],
    lastSeq: 0,
    serverTime: '2026-10-01T12:00:00Z',
    ...overrides,
  };
}

function connected(): LiveGameState {
  return fromSnapshot(initialState('582941'), snapshot(), T0);
}

describe('live game reducer', () => {
  it('shows a question as soon as QUESTION_STARTED arrives, without an answer', () => {
    const state = applyEvent(connected(), questionStarted(1, 4), T0);

    expect(state.status).toBe('QUESTION_OPEN');
    expect(state.current?.questionNumber).toBe(4);
    expect(state.current?.choices).toHaveLength(4);
    expect(state.answer).toBeNull();
    expect(viewerStatus(state, 'CONNECTED')).toBe('QUESTION');
  });

  it('shows the answer and adds the question to the history on ANSWER_REVEALED', () => {
    let state = applyEvent(connected(), questionStarted(1, 4), T0);
    state = applyEvent(state, answerRevealed(2, 4), T0);

    expect(state.answer).toEqual({ questionNumber: 4, correctIndex: 1, correctText: 'Quito' });
    expect(state.history).toHaveLength(1);
    expect(state.history[0].correctText).toBe('Quito');
    expect(viewerStatus(state, 'CONNECTED')).toBe('REVEAL');
  });

  it('moves to the next question automatically', () => {
    let state = applyEvent(connected(), questionStarted(1, 1), T0);
    state = applyEvent(state, answerRevealed(2, 1), T0);
    state = applyEvent(state, { ...envelope('QUESTION_FINISHED', 3), questionNumber: 1 }, T0);
    state = applyEvent(state, questionStarted(4, 2), T0);

    expect(state.current?.questionNumber).toBe(2);
    expect(state.answer).toBeNull();
    expect(state.history.map((h) => h.questionNumber)).toEqual([1]);
  });

  it('ignores duplicated events and flags a resync when seq jumps', () => {
    let state = applyEvent(connected(), questionStarted(1, 1), T0);
    const duplicated = applyEvent(state, questionStarted(1, 1), T0);
    expect(duplicated).toBe(state);

    state = applyEvent(state, questionStarted(5, 3), T0);
    expect(state.needsResync).toBe(true);
    expect(state.current?.questionNumber).toBe(3);
  });

  it('keeps question and history while reconnecting and never rolls back to an older snapshot', () => {
    let state = applyEvent(connected(), questionStarted(1, 1), T0);
    state = applyEvent(state, answerRevealed(2, 1), T0);

    expect(viewerStatus(state, 'RECONNECTING')).toBe('RECONNECTING');
    expect(state.history).toHaveLength(1);

    const stale = fromSnapshot(state, snapshot({ lastSeq: 1, status: 'QUESTION_OPEN' }), T0);
    expect(stale.status).toBe('ANSWER_REVEALED');
    expect(stale.lastSeq).toBe(2);
  });

  it('marks the game as finished', () => {
    const state = applyEvent(connected(), {
      ...envelope('GAME_FINISHED', 1),
      questionsPlayed: 20,
      totalQuestions: 20,
      reason: 'COMPLETED',
    }, T0);

    expect(viewerStatus(state, 'CONNECTED')).toBe('FINISHED');
  });

  it('computes the countdown with the server clock offset', () => {
    const state = applyEvent(connected(), questionStarted(1, 1), T0);

    expect(secondsLeft(state, T0)).toBe(20);
    expect(secondsLeft(state, T0 + 15_500)).toBe(5);
    expect(secondsLeft({ ...state, clockOffsetMs: 10_000 }, T0)).toBe(10);
    expect(secondsLeft(state, T0 + 60_000)).toBe(0);
  });

  it('reports GAME_ERROR as an error state', () => {
    const state = applyEvent(connected(), {
      ...envelope('GAME_ERROR', 0),
      code: 'GAME_NOT_FOUND',
      message: 'No existe una partida con ese PIN',
    }, T0);

    expect(viewerStatus(state, 'CONNECTED')).toBe('ERROR');
  });
});
