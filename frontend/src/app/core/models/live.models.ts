/**
 * KahootP46 Live protocol v1 (server -> client). Mirrors docs/protocolo-websocket.md.
 */
export type GameStatus = 'LOBBY' | 'QUESTION_OPEN' | 'ANSWER_REVEALED' | 'FINISHED';

/** State of the WebSocket connection, independent from the game phase. */
export type ConnectionStatus = 'CONNECTING' | 'CONNECTED' | 'RECONNECTING' | 'ERROR';

export interface ChoiceView {
  index: number;
  text: string;
}

export interface LiveQuestion {
  questionNumber: number;
  totalQuestions: number;
  question: string;
  choices: ChoiceView[];
  timeLimitSeconds: number;
  startedAt: string;
  endsAt: string;
}

export interface RevealedAnswer {
  questionNumber: number;
  correctIndex: number;
  correctText: string;
}

export interface HistoryItem {
  questionNumber: number;
  question: string;
  choices: ChoiceView[];
  correctIndex: number;
  correctText: string;
}

export interface GameSnapshot {
  gameId: string;
  pin: string;
  status: GameStatus;
  quizTitle: string;
  totalQuestions: number;
  currentQuestion: LiveQuestion | null;
  currentAnswer: RevealedAnswer | null;
  history: HistoryItem[];
  lastSeq: number;
  serverTime: string;
}

interface EventEnvelope<T extends string> {
  type: T;
  protocolVersion: number;
  gameId: string | null;
  pin: string;
  seq: number;
  sentAt: string;
}

export interface GameConnectedEvent extends EventEnvelope<'GAME_CONNECTED'> {
  snapshot: GameSnapshot;
}

export interface GameStartedEvent extends EventEnvelope<'GAME_STARTED'> {
  quizTitle: string;
  totalQuestions: number;
}

export interface QuestionStartedEvent extends EventEnvelope<'QUESTION_STARTED'>, LiveQuestion {}

export interface AnswerRevealedEvent extends EventEnvelope<'ANSWER_REVEALED'>, RevealedAnswer {
  reason: 'TIME_UP' | 'HOST';
}

export interface QuestionFinishedEvent extends EventEnvelope<'QUESTION_FINISHED'> {
  questionNumber: number;
}

export interface GameFinishedEvent extends EventEnvelope<'GAME_FINISHED'> {
  questionsPlayed: number;
  totalQuestions: number;
  reason: 'COMPLETED' | 'CLOSED_BY_HOST';
}

export interface GameErrorEvent extends EventEnvelope<'GAME_ERROR'> {
  code: string;
  message: string;
}

export type LiveEvent =
  | GameConnectedEvent
  | GameStartedEvent
  | QuestionStartedEvent
  | AnswerRevealedEvent
  | QuestionFinishedEvent
  | GameFinishedEvent
  | GameErrorEvent;

export interface ConnectResponse {
  gameId: string;
  pin: string;
  viewerToken: string;
  expiresAt: string;
  snapshot: GameSnapshot;
}
