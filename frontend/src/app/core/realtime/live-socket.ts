import { RxStomp, RxStompState } from '@stomp/rx-stomp';
import { ReconnectionTimeMode } from '@stomp/stompjs';
import { BehaviorSubject, map, merge, Observable, switchMap } from 'rxjs';

import { ConnectionStatus, LiveEvent } from '../models/live.models';

export interface LiveSocketOptions {
  pin: string;
  /** Called before every (re)connection, so an expired token is renewed transparently. */
  token: () => Promise<string>;
}

/**
 * STOMP connection to one game. Reconnects with exponential backoff (1 s -> 15 s) and, after every
 * reconnection, subscribes again to /app/games/{pin}/state, which answers with GAME_CONNECTED + snapshot.
 */
export class LiveSocket {
  private readonly stomp = new RxStomp();
  private readonly resync$ = new BehaviorSubject<void>(undefined);
  private everConnected = false;

  readonly events$: Observable<LiveEvent>;
  readonly status$: Observable<ConnectionStatus>;

  constructor(options: LiveSocketOptions) {
    this.stomp.configure({
      brokerURL: socketUrl(),
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      reconnectDelay: 1_000,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      maxReconnectDelay: 15_000,
      beforeConnect: async (client: RxStomp) => {
        try {
          const token = await options.token();
          client.configure({ connectHeaders: { Authorization: `Bearer ${token}` } });
        } catch {
          // The API is unreachable: connect without token, the server rejects it and we retry with backoff.
          client.configure({ connectHeaders: {} });
        }
      },
    });

    const topic$ = this.stomp
      .watch({ destination: `/topic/games/${options.pin}` })
      .pipe(map((message) => JSON.parse(message.body) as LiveEvent));
    const state$ = this.resync$.pipe(
      switchMap(() => this.stomp.watch({ destination: `/app/games/${options.pin}/state` })),
      map((message) => JSON.parse(message.body) as LiveEvent),
    );
    this.events$ = merge(topic$, state$);

    this.status$ = this.stomp.connectionState$.pipe(
      map((state): ConnectionStatus => {
        if (state === RxStompState.OPEN) {
          this.everConnected = true;
          return 'CONNECTED';
        }
        return this.everConnected ? 'RECONNECTING' : 'CONNECTING';
      }),
    );
  }

  connect(): void {
    this.stomp.activate();
  }

  /** Requests a fresh snapshot (after a seq gap). */
  resync(): void {
    this.resync$.next();
  }

  async disconnect(): Promise<void> {
    await this.stomp.deactivate();
  }
}

function socketUrl(): string {
  const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
  return `${scheme}://${location.host}/ws`;
}
