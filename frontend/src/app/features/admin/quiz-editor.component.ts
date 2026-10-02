import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AdminApiService, apiErrorMessage } from '../../core/api/admin-api.service';
import { QuestionForm, QuestionType, QuizForm, TIME_LIMITS } from '../../core/models/admin.models';
import { choiceAccent, choiceLetter } from '../../shared/choice-style';

const MAX_CHOICES = 6;

function emptyQuestion(): QuestionForm {
  return {
    text: '',
    type: 'SINGLE_CHOICE',
    timeLimit: 20,
    choices: [
      { text: '', correct: true },
      { text: '', correct: false },
      { text: '', correct: false },
      { text: '', correct: false },
    ],
  };
}

function trueFalseChoices(): QuestionForm['choices'] {
  return [
    { text: 'Verdadero', correct: true },
    { text: 'Falso', correct: false },
  ];
}

/** Create and edit a quiz: questions, choices, correct answer and time per question. */
@Component({
  selector: 'app-quiz-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  templateUrl: './quiz-editor.component.html',
})
export class QuizEditorComponent implements OnInit {
  /** Route param; absent when creating. */
  readonly id = input<string>();

  private readonly api = inject(AdminApiService);
  private readonly router = inject(Router);

  protected readonly timeLimits = TIME_LIMITS;
  protected readonly letter = choiceLetter;
  protected readonly accent = choiceAccent;
  protected readonly maxChoices = MAX_CHOICES;

  protected readonly quiz = signal<QuizForm>({ title: '', description: null, questions: [emptyQuestion()] });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    const id = this.id();
    if (!id) {
      return;
    }
    try {
      const quiz = await this.api.getQuiz(id);
      this.quiz.set({ title: quiz.title, description: quiz.description, questions: quiz.questions });
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    }
  }

  protected text(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected setTitle(title: string): void {
    this.quiz.update((q) => ({ ...q, title }));
  }

  protected setDescription(description: string): void {
    this.quiz.update((q) => ({ ...q, description: description || null }));
  }

  protected updateQuestion(index: number, change: Partial<QuestionForm>): void {
    this.quiz.update((q) => ({
      ...q,
      questions: q.questions.map((question, i) => (i === index ? { ...question, ...change } : question)),
    }));
  }

  protected setType(index: number, type: QuestionType): void {
    const question = this.quiz().questions[index];
    const choices = type === 'TRUE_FALSE' ? trueFalseChoices() : question.choices.length >= 2 ? question.choices : emptyQuestion().choices;
    this.updateQuestion(index, { type, choices });
  }

  protected setChoiceText(questionIndex: number, choiceIndex: number, value: string): void {
    const choices = this.quiz().questions[questionIndex].choices.map((choice, i) =>
      i === choiceIndex ? { ...choice, text: value } : choice,
    );
    this.updateQuestion(questionIndex, { choices });
  }

  protected markCorrect(questionIndex: number, choiceIndex: number): void {
    const choices = this.quiz().questions[questionIndex].choices.map((choice, i) => ({
      ...choice,
      correct: i === choiceIndex,
    }));
    this.updateQuestion(questionIndex, { choices });
  }

  protected addChoice(questionIndex: number): void {
    const question = this.quiz().questions[questionIndex];
    if (question.choices.length < MAX_CHOICES) {
      this.updateQuestion(questionIndex, { choices: [...question.choices, { text: '', correct: false }] });
    }
  }

  protected removeChoice(questionIndex: number, choiceIndex: number): void {
    const question = this.quiz().questions[questionIndex];
    if (question.choices.length <= 2) {
      return;
    }
    const choices = question.choices.filter((_, i) => i !== choiceIndex);
    if (!choices.some((choice) => choice.correct)) {
      choices[0] = { ...choices[0], correct: true };
    }
    this.updateQuestion(questionIndex, { choices });
  }

  protected addQuestion(): void {
    this.quiz.update((q) => ({ ...q, questions: [...q.questions, emptyQuestion()] }));
  }

  protected removeQuestion(index: number): void {
    this.quiz.update((q) => ({ ...q, questions: q.questions.filter((_, i) => i !== index) }));
  }

  protected moveQuestion(index: number, delta: -1 | 1): void {
    const target = index + delta;
    this.quiz.update((q) => {
      if (target < 0 || target >= q.questions.length) {
        return q;
      }
      const questions = [...q.questions];
      [questions[index], questions[target]] = [questions[target], questions[index]];
      return { ...q, questions };
    });
  }

  protected async save(event: Event): Promise<void> {
    event.preventDefault();
    this.saving.set(true);
    this.error.set(null);
    try {
      const id = this.id();
      if (id) {
        await this.api.updateQuiz(id, this.quiz());
      } else {
        await this.api.createQuiz(this.quiz());
      }
      await this.router.navigate(['/admin']);
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }
}
