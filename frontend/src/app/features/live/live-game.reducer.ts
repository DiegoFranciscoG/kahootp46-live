import {
  ConnectionStatus,
  GameSnapshot,
  GameStatus,
  HistoryItem,
  LiveEvent,
  LiveQuestion,
  RevealedAnswer,
} from '../../core/models/live.models';

/** States shown to the user (badge + layout). */
export type ViewerStatus =
  | 'CONNECTED'
  | 'WAITING'
  | 'QUESTION'
  | 'REVEAL'
  | 'FINISHED'
  | 'ERROR'
  | 'RECONNECTING';

export interface LiveGameState {
  pin: string;
  gameId: string | null;
  quizTitle: string | null;
  status: GameStatus | null;
  totalQuestions: number;
  current: LiveQuestion | null;
  answer: RevealedAnswer | null;
  /** Revealed questions, ascending by number. */
  history: HistoryItem[];
  lastSeq: number;
  /** Client time (ISO) of the last applied event. */
  lastUpdate: string | null;
  /** serverTime - clientTime, used to keep the countdown in sync with the server. */
  clockOffsetMs: number;
  error: string | null;
  /** A gap in `seq` was detected: the client must ask for a fresh snapshot. */
  needsResync: boolean;
}

export function initialState(pin: string): LiveGameState {
  return {
    pin,
    gameId: null,
    quizTitle: null,
    status: null,
    totalQuestions: 0,
    current: null,
    answer: null,
    history: [],
    lastSeq: 0,
    lastUpdate: null,
    clockOffsetMs: 0,
    error: null,
    needsResync: false,
  };
}

export function fromSnapshot(state: LiveGameState, snapshot: GameSnapshot, receivedAt: number): LiveGameState {
  const sameGame = state.gameId === null || state.gameId === snapshot.gameId;
  // An older snapshot (e.g. a slow cache read) must never roll the screen back.
  if (sameGame && snapshot.lastSeq < state.lastSeq) {
    return { ...state, needsResync: false };
  }
  return {
    ...state,
    gameId: snapshot.gameId,
    quizTitle: snapshot.quizTitle,
    status: snapshot.status,
    totalQuestions: snapshot.totalQuestions,
    current: snapshot.currentQuestion,
    answer: snapshot.currentAnswer,
    history: [...snapshot.history].sort((a, b) => a.questionNumber - b.questionNumber),
    lastSeq: snapshot.lastSeq,
    lastUpdate: new Date(receivedAt).toISOString(),
    clockOffsetMs: Date.parse(snapshot.serverTime) - receivedAt,
    error: null,
    needsResync: false,
  };
}

/**
 * Pure state transition for one protocol event. Duplicated or old events (seq <= lastSeq) are ignored,
 * and a jump in seq flags `needsResync` so the client requests the snapshot again.
 */
export function applyEvent(state: LiveGameState, event: LiveEvent, receivedAt: number): LiveGameState {
  if (event.type === 'GAME_CONNECTED') {
    return fromSnapshot(state, event.snapshot, receivedAt);
  }
  if (event.type === 'GAME_ERROR') {
    return { ...state, error: event.message };
  }
  if (state.gameId !== null && event.gameId !== state.gameId) {
    return state;
  }
  if (event.seq <= state.lastSeq) {
    return state;
  }
  const gap = state.gameId !== null && event.seq > state.lastSeq + 1;
  const base: LiveGameState = {
    ...state,
    gameId: event.gameId,
    lastSeq: event.seq,
    lastUpdate: new Date(receivedAt).toISOString(),
    needsResync: state.needsResync || gap,
    error: null,
  };

  switch (event.type) {
    case 'GAME_STARTED':
      return { ...base, quizTitle: event.quizTitle, totalQuestions: event.totalQuestions };
    case 'QUESTION_STARTED':
      return {
        ...base,
        status: 'QUESTION_OPEN',
        totalQuestions: event.totalQuestions,
        current: {
          questionNumber: event.questionNumber,
          totalQuestions: event.totalQuestions,
          question: event.question,
          choices: event.choices,
          timeLimitSeconds: event.timeLimitSeconds,
          startedAt: event.startedAt,
          endsAt: event.endsAt,
        },
        answer: null,
      };
    case 'ANSWER_REVEALED': {
      const answer: RevealedAnswer = {
        questionNumber: event.questionNumber,
        correctIndex: event.correctIndex,
        correctText: event.correctText,
      };
      return {
        ...base,
        status: 'ANSWER_REVEALED',
        answer,
        history: addToHistory(base.history, base.current, answer),
      };
    }
    case 'QUESTION_FINISHED':
      // The revealed answer stays on screen until the next question arrives.
      return base;
    case 'GAME_FINISHED':
      return { ...base, status: 'FINISHED' };
  }
}

function addToHistory(
  history: HistoryItem[],
  question: LiveQuestion | null,
  answer: RevealedAnswer,
): HistoryItem[] {
  if (!question || question.questionNumber !== answer.questionNumber) {
    return history;
  }
  const item: HistoryItem = {
    questionNumber: question.questionNumber,
    question: question.question,
    choices: question.choices,
    correctIndex: answer.correctIndex,
    correctText: answer.correctText,
  };
  return [...history.filter((h) => h.questionNumber !== item.questionNumber), item].sort(
    (a, b) => a.questionNumber - b.questionNumber,
  );
}

export function viewerStatus(state: LiveGameState, connection: ConnectionStatus): ViewerStatus {
  if (connection === 'ERROR' || state.error) {
    return 'ERROR';
  }
  if (connection === 'RECONNECTING' || (connection === 'CONNECTING' && state.gameId !== null)) {
    return 'RECONNECTING';
  }
  switch (state.status) {
    case 'LOBBY':
      return 'WAITING';
    case 'QUESTION_OPEN':
      return 'QUESTION';
    case 'ANSWER_REVEALED':
      return 'REVEAL';
    case 'FINISHED':
      return 'FINISHED';
    default:
      return 'CONNECTED';
  }
}

/** Seconds left for the current question, using the server clock. */
export function secondsLeft(state: LiveGameState, now: number): number {
  if (!state.current || state.status !== 'QUESTION_OPEN') {
    return 0;
  }
  const remainingMs = Date.parse(state.current.endsAt) - (now + state.clockOffsetMs);
  // The offset is estimated from one round trip, so it can be off by a fraction of a second:
  // never show more time than the question has.
  return Math.min(state.current.timeLimitSeconds, Math.max(0, Math.ceil(remainingMs / 1000)));
}
