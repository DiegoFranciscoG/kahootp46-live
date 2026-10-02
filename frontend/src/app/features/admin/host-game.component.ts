import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { AdminApiService, apiErrorMessage } from '../../core/api/admin-api.service';
import { AdminSessionService } from '../../core/auth/admin-session.service';
import { HostAction, HostGame } from '../../core/models/admin.models';
import { LiveSocket } from '../../core/realtime/live-socket';
import { choiceAccent, choiceLetter } from '../../shared/choice-style';

/**
 * Host control panel. It listens to the same live topic as the viewers (admin tokens may watch any game),
 * so a server-side timeout reveal shows up here without polling.
 */
@Component({
  selector: 'app-host-game',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  templateUrl: './host-game.component.html',
})
export class HostGameComponent implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(AdminApiService);
  private readonly session = inject(AdminSessionService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly letter = choiceLetter;
  protected readonly accent = choiceAccent;
  protected readonly game = signal<HostGame | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly now = signal(Date.now());

  protected readonly secondsLeft = computed(() => {
    const game = this.game();
    if (!game?.question || game.status !== 'QUESTION_OPEN') {
      return 0;
    }
    return Math.max(0, Math.ceil((Date.parse(game.question.endsAt) - this.now()) / 1000));
  });

  protected readonly viewerUrl = computed(() => {
    const pin = this.game()?.pin;
    return pin ? `${location.origin}/live/${pin}` : '';
  });

  async ngOnInit(): Promise<void> {
    const ticker = setInterval(() => this.now.set(Date.now()), 250);
    this.destroyRef.onDestroy(() => clearInterval(ticker));
    await this.refresh();
    const pin = this.game()?.pin;
    const token = this.session.token();
    if (pin && token) {
      const socket = new LiveSocket({ pin, token: async () => this.session.token() ?? '' });
      socket.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
        if (event.type !== 'GAME_CONNECTED' && event.seq > (this.game()?.lastSeq ?? 0)) {
          void this.refresh();
        }
      });
      socket.connect();
      this.destroyRef.onDestroy(() => void socket.disconnect());
    }
  }

  protected async act(action: HostAction): Promise<void> {
    if (action === 'finish' && !confirm('¿Cerrar la partida para todos los viewers?')) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      this.game.set(await this.api.hostAction(this.id(), action));
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private async refresh(): Promise<void> {
    try {
      this.game.set(await this.api.getGame(this.id()));
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    }
  }
}
