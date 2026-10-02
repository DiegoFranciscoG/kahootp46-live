import { computed, DestroyRef, effect, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ViewerApiService, viewerErrorMessage } from '../../core/api/viewer-api.service';
import { ConnectionStatus, GameSnapshot } from '../../core/models/live.models';
import { LiveSocket } from '../../core/realtime/live-socket';
import {
  applyEvent,
  fromSnapshot,
  initialState,
  LiveGameState,
  secondsLeft,
  viewerStatus,
} from './live-game.reducer';

const STATE_PREFIX = 'kp46:live:';

/**
 * State of one viewer screen. Survives reconnections (the socket resubscribes and the snapshot is merged
 * by seq) and page reloads (state persisted in sessionStorage per PIN).
 */
@Injectable()
export class LiveGameStore {
  private readonly api = inject(ViewerApiService);
  private readonly destroyRef = inject(DestroyRef);
  private socket: LiveSocket | null = null;

  readonly state = signal<LiveGameState>(initialState(''));
  readonly connection = signal<ConnectionStatus>('CONNECTING');
  readonly connectError = signal<string | null>(null);
  private readonly now = signal(Date.now());

  readonly status = computed(() => viewerStatus(this.state(), this.connection()));
  readonly secondsLeft = computed(() => secondsLeft(this.state(), this.now()));

  constructor() {
    effect(() => persist(this.state()));
    const ticker = setInterval(() => this.now.set(Date.now()), 250);
    this.destroyRef.onDestroy(() => {
      clearInterval(ticker);
      void this.socket?.disconnect();
    });
  }

  async start(pin: string, handedSnapshot: GameSnapshot | null): Promise<void> {
    this.state.set(restore(pin) ?? initialState(pin));
    if (handedSnapshot) {
      this.state.update((s) => fromSnapshot(s, handedSnapshot, Date.now()));
    } else if (!this.api.session(pin)) {
      // Deep link without a previous connect: validate the PIN before opening the socket.
      try {
        const response = await this.api.connect(pin);
        this.state.update((s) => fromSnapshot(s, response.snapshot, Date.now()));
      } catch (error) {
        this.connection.set('ERROR');
        this.connectError.set(viewerErrorMessage(error));
        return;
      }
    }

    const socket = new LiveSocket({ pin, token: () => this.api.token(pin) });
    this.socket = socket;
    socket.status$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((s) => this.connection.set(s));
    socket.events$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((event) => {
      this.state.update((s) => applyEvent(s, event, Date.now()));
      if (this.state().needsResync) {
        socket.resync();
      }
    });
    socket.connect();
  }
}

function persist(state: LiveGameState): void {
  if (!state.pin) {
    return;
  }
  try {
    sessionStorage.setItem(STATE_PREFIX + state.pin, JSON.stringify(state));
  } catch {
    // Storage full or blocked: the screen keeps working, it just will not survive a reload.
  }
}

function restore(pin: string): LiveGameState | null {
  try {
    const raw = sessionStorage.getItem(STATE_PREFIX + pin);
    return raw ? { ...(JSON.parse(raw) as LiveGameState), needsResync: false } : null;
  } catch {
    return null;
  }
}
