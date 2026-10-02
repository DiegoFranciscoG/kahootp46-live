// Load test: many simultaneous viewers on one game, driven by a host.
//   docker run --rm --network kahootp46-live_default \
//     -e BASE_URL=http://frontend:8080 -e ADMIN_EMAIL -e ADMIN_PASSWORD -e VIEWERS=300 \
//     -v "$PWD/loadtest:/scripts:ro" grafana/k6:2.3.0 run /scripts/viewers.js
// Each viewer speaks STOMP over a real WebSocket, subscribes to the game topic and measures the time between
// the server creating an event (`sentAt`) and receiving it. Clock skew is nil: every container shares the host clock.
import http from 'k6/http';
import { sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { WebSocket } from 'k6/websockets';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:4200';
const VIEWERS = Number(__ENV.VIEWERS || 100);
const ORIGIN = __ENV.ORIGIN || 'http://localhost:4200';
const HOLD_SECONDS = 45;

const eventLatency = new Trend('event_latency_ms', true);
const connected = new Counter('viewers_connected');
const gotQuestion = new Counter('viewers_got_question');
const gotAnswer = new Counter('viewers_got_answer');
const socketErrors = new Counter('socket_errors');

export const options = {
  scenarios: {
    viewers: {
      executor: 'per-vu-iterations',
      vus: VIEWERS,
      iterations: 1,
      maxDuration: '120s',
    },
    host: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      startTime: '20s',
      exec: 'host',
      maxDuration: '60s',
    },
  },
  thresholds: {
    viewers_connected: [`count>=${VIEWERS}`],
    viewers_got_question: [`count>=${VIEWERS}`],
    viewers_got_answer: [`count>=${VIEWERS}`],
    socket_errors: ['count==0'],
    event_latency_ms: ['p(95)<1000'],
  },
};

function json(response) {
  if (response.status < 200 || response.status >= 300) {
    throw new Error(`HTTP ${response.status} ${response.request.method} ${response.request.url}: ${response.body}`);
  }
  return response.json();
}

function auth(token) {
  return { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } };
}

export function setup() {
  if (!__ENV.ADMIN_EMAIL || !__ENV.ADMIN_PASSWORD) {
    throw new Error('Define ADMIN_EMAIL y ADMIN_PASSWORD');
  }
  const login = json(http.post(`${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: __ENV.ADMIN_EMAIL, password: __ENV.ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }));
  const adminToken = login.accessToken;

  const question = (n) => ({
    text: `Pregunta ${n} de carga`,
    timeLimit: 60,
    choices: [0, 1, 2, 3].map((i) => ({ text: `Opción ${i}`, correct: i === 1 })),
  });
  const quiz = json(http.post(`${BASE_URL}/api/quizzes`,
    JSON.stringify({ title: `Carga ${Date.now()}`, questions: [question(1), question(2)] }), auth(adminToken)));
  const game = json(http.post(`${BASE_URL}/api/host/games`, JSON.stringify({ quizId: quiz.id }), auth(adminToken)));
  // One token is enough: viewer tokens are reusable, and /connect is rate limited per IP.
  const connect = json(http.post(`${BASE_URL}/api/games/connect`, JSON.stringify({ pin: game.pin }),
    { headers: { 'Content-Type': 'application/json' } }));
  return { adminToken, gameId: game.id, quizId: quiz.id, pin: game.pin, viewerToken: connect.viewerToken };
}

function stompFrame(command, headers, body = '') {
  const lines = Object.entries(headers).map(([k, v]) => `${k}:${v}`);
  return `${command}\n${lines.join('\n')}\n\n${body}\0`;
}

export default function (data) {
  sleep(Math.random() * 8); // spread the connection storm over a few seconds
  const url = `${BASE_URL.replace(/^http/, 'ws')}/ws`;
  const ws = new WebSocket(url, null, { headers: { Origin: ORIGIN } });
  let sawQuestion = false;
  let sawAnswer = false;

  ws.onopen = () => {
    ws.send(stompFrame('CONNECT', {
      'accept-version': '1.2',
      host: 'kahootp46',
      'heart-beat': '0,0',
      Authorization: `Bearer ${data.viewerToken}`,
    }));
  };

  ws.onmessage = (message) => {
    for (const frame of String(message.data).split('\0')) {
      if (frame.startsWith('CONNECTED')) {
        connected.add(1);
        ws.send(stompFrame('SUBSCRIBE', { id: 'sub-0', destination: `/topic/games/${data.pin}`, ack: 'auto' }));
      } else if (frame.startsWith('MESSAGE')) {
        const body = frame.substring(frame.indexOf('\n\n') + 2);
        const event = JSON.parse(body);
        eventLatency.add(Date.now() - Date.parse(event.sentAt));
        if (event.type === 'QUESTION_STARTED' && !sawQuestion) {
          sawQuestion = true;
          gotQuestion.add(1);
        }
        if (event.type === 'ANSWER_REVEALED' && !sawAnswer) {
          sawAnswer = true;
          gotAnswer.add(1);
        }
      } else if (frame.startsWith('ERROR')) {
        socketErrors.add(1);
      }
    }
  };

  ws.onerror = () => socketErrors.add(1);
  setTimeout(() => ws.close(), HOLD_SECONDS * 1000);
}

export function host(data) {
  const act = (action) => json(http.post(`${BASE_URL}/api/host/games/${data.gameId}/${action}`, '{}', auth(data.adminToken)));
  act('start');   // GAME_STARTED + QUESTION_STARTED
  sleep(4);
  act('reveal');  // ANSWER_REVEALED
  sleep(4);
  act('next');    // QUESTION_FINISHED + QUESTION_STARTED
  sleep(4);
  act('finish');  // ANSWER_REVEALED + QUESTION_FINISHED + GAME_FINISHED
}

export function teardown(data) {
  http.del(`${BASE_URL}/api/quizzes/${data.quizId}`, null, auth(data.adminToken));
}
