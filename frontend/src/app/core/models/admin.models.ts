import { ChoiceView, GameStatus } from './live.models';

export type QuestionType = 'SINGLE_CHOICE' | 'TRUE_FALSE';

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
}

export interface QuizSummary {
  id: string;
  title: string;
  description: string | null;
  questionCount: number;
  updatedAt: string;
}

export interface ChoiceForm {
  text: string;
  correct: boolean;
}

export interface QuestionForm {
  text: string;
  type: QuestionType;
  timeLimit: number;
  choices: ChoiceForm[];
}

export interface QuizForm {
  title: string;
  description: string | null;
  questions: QuestionForm[];
}

export interface Quiz extends QuizForm {
  id: string;
  createdAt: string;
  updatedAt: string;
}

export interface HostQuestion {
  number: number;
  text: string;
  choices: ChoiceView[];
  correctIndex: number;
  timeLimitSeconds: number;
  startedAt: string;
  endsAt: string;
  revealed: boolean;
}

export interface HostGame {
  id: string;
  pin: string;
  status: GameStatus;
  quizId: string;
  quizTitle: string;
  totalQuestions: number;
  currentQuestion: number | null;
  question: HostQuestion | null;
  lastSeq: number;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}

export type HostAction = 'start' | 'reveal' | 'next' | 'finish';

/** Time presets accepted by the backend (QuizRules.TIME_LIMITS). */
export const TIME_LIMITS = [5, 10, 20, 30, 60, 90, 120, 240] as const;
