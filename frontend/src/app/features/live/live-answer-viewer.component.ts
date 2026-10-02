import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ChoiceView, GameSnapshot, HistoryItem } from '../../core/models/live.models';
import { BrandLogoComponent } from '../../shared/brand-logo.component';
import { choiceAccent, choiceLetter } from '../../shared/choice-style';
import { StatusBadgeComponent } from '../../shared/status-badge.component';
import { LiveGameStore } from './live-game.store';

/** What the main card shows: the live question or a closed one picked from the history. */
interface QuestionView {
  number: number;
  total: number;
  text: string;
  choices: ChoiceView[];
  correctIndex: number | null;
  correctText: string | null;
  live: boolean;
}

const AUTO_NEXT_KEY = 'kp46:auto-next';

@Component({
  selector: 'app-live-answer-viewer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, BrandLogoComponent, StatusBadgeComponent],
  providers: [LiveGameStore],
  templateUrl: './live-answer-viewer.component.html',
})
export class LiveAnswerViewerComponent implements OnInit {
  /** Bound from the route /live/:pin. */
  readonly pin = input.required<string>();

  protected readonly store = inject(LiveGameStore);
  protected readonly letter = choiceLetter;
  protected readonly accent = choiceAccent;

  protected readonly autoNext = signal(readAutoNext());
  protected readonly selected = signal<HistoryItem | null>(null);

  protected readonly state = this.store.state;
  protected readonly status = this.store.status;
  protected readonly secondsLeft = this.store.secondsLeft;
  protected readonly connected = computed(() => this.store.connection() === 'CONNECTED');

  protected readonly liveView = computed<QuestionView | null>(() => {
    const { current, answer, totalQuestions } = this.state();
    if (!current) {
      return null;
    }
    const revealed = answer?.questionNumber === current.questionNumber ? answer : null;
    return {
      number: current.questionNumber,
      total: totalQuestions || current.totalQuestions,
      text: current.question,
      choices: current.choices,
      correctIndex: revealed?.correctIndex ?? null,
      correctText: revealed?.correctText ?? null,
      live: true,
    };
  });

  protected readonly view = computed<QuestionView | null>(() => {
    const picked = this.selected();
    if (!picked) {
      return this.liveView();
    }
    return {
      number: picked.questionNumber,
      total: this.state().totalQuestions,
      text: picked.question,
      choices: picked.choices,
      correctIndex: picked.correctIndex,
      correctText: picked.correctText,
      live: false,
    };
  });

  /** Closed questions, newest first, without the one currently live on screen. */
  protected readonly previous = computed(() => {
    const liveNumber = this.state().status === 'FINISHED' ? null : this.state().current?.questionNumber;
    return [...this.state().history].filter((item) => item.questionNumber !== liveNumber).reverse();
  });

  protected readonly progress = computed(() => {
    const current = this.state().current;
    if (!current || this.status() !== 'QUESTION') {
      return 0;
    }
    return Math.min(100, (this.secondsLeft() / current.timeLimitSeconds) * 100);
  });

  /** A new question arrived while the user is reading an old one with AUTO NEXT off. */
  protected readonly newQuestionWaiting = computed(
    () => this.selected() !== null && this.liveView() !== null && this.status() === 'QUESTION',
  );

  constructor() {
    // AUTO NEXT: every new question replaces whatever the user was looking at.
    effect(() => {
      const liveNumber = this.state().current?.questionNumber;
      if (liveNumber !== undefined && this.autoNext()) {
        this.selected.set(null);
      }
    });
  }

  ngOnInit(): void {
    const handed = (history.state as { snapshot?: GameSnapshot } | null)?.snapshot ?? null;
    void this.store.start(this.pin(), handed?.pin === this.pin() ? handed : null);
  }

  protected toggleAutoNext(): void {
    this.autoNext.update((value) => !value);
    try {
      localStorage.setItem(AUTO_NEXT_KEY, String(this.autoNext()));
    } catch {
      // Preference only; ignore blocked storage.
    }
  }

  protected open(item: HistoryItem): void {
    this.selected.set(item);
  }

  protected backToLive(): void {
    this.selected.set(null);
  }
}

function readAutoNext(): boolean {
  try {
    return localStorage.getItem(AUTO_NEXT_KEY) !== 'false';
  } catch {
    return true;
  }
}
