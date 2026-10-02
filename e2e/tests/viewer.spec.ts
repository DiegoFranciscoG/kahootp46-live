import type { Page, WebSocketRoute } from '@playwright/test';

import { expect, GEOGRAPHY, test } from './fixtures';

async function connect(page: Page, code: string) {
  await page.goto('/');
  await page.locator('#pin').fill(code);
  await page.getByRole('button', { name: 'CONECTAR' }).click();
}

test.describe('visor: PIN → WebSocket → pregunta → respuesta', () => {
  test('sigue una partida completa y consulta el historial', async ({ page, host }) => {
    const game = await host.createGame('E2E partida completa', GEOGRAPHY);

    await connect(page, game.pin);
    await expect(page).toHaveURL(new RegExp(`/live/${game.pin}$`));
    await expect(page.getByTestId('pin')).toHaveText(game.pin);
    await expect(page.getByTestId('live-indicator')).toBeVisible();
    await expect(page.getByTestId('waiting')).toContainText('Esperando que el anfitrión inicie la partida');

    // QUESTION_STARTED: question, four options and no answer yet.
    await host.act(game, 'start');
    await expect(page.getByTestId('question-number')).toContainText('PREGUNTA 1 / 3');
    await expect(page.getByTestId('question-text')).toHaveText('¿Cuál es la capital del Ecuador?');
    await expect(page.getByTestId('question-card').locator('li')).toHaveCount(4);
    await expect(page.getByTestId('countdown')).toBeVisible();
    await expect(page.getByTestId('answer-pending')).toBeVisible();
    await expect(page.getByTestId('answer')).toHaveCount(0);
    await expect(page.locator('[data-correct="true"]')).toHaveCount(0);

    // ANSWER_REVEALED: the correct option is highlighted and spelled out.
    await host.act(game, 'reveal');
    await expect(page.getByTestId('answer')).toHaveText('✓ B — Quito');
    await expect(page.locator('li[data-correct="true"]')).toContainText('Quito');
    await expect(page.getByTestId('countdown')).toHaveCount(0);

    // AUTO NEXT: the next question replaces the screen and question 1 moves to the history.
    await host.act(game, 'next');
    await expect(page.getByTestId('question-number')).toContainText('PREGUNTA 2 / 3');
    await expect(page.getByTestId('question-text')).toContainText('Islas Galápagos');
    await expect(page.getByTestId('answer')).toHaveCount(0);
    const history = page.getByTestId('history').locator('li');
    await expect(history).toHaveCount(1);
    await expect(history.first()).toContainText('Correcta: B — Quito');

    // A closed question can be consulted again and the user can go back to the live one.
    await history.first().getByRole('button').click();
    await expect(page.getByText('Estás consultando la pregunta 1 (cerrada).')).toBeVisible();
    await expect(page.getByTestId('answer')).toHaveText('✓ B — Quito');
    await page.getByRole('button', { name: 'Volver al vivo' }).first().click();
    await expect(page.getByTestId('question-number')).toContainText('PREGUNTA 2 / 3');

    await host.act(game, 'reveal');
    await expect(page.getByTestId('answer')).toHaveText('✓ C — Pacífico');
    await host.act(game, 'next');
    await expect(page.getByTestId('question-text')).toContainText('montaña más alta');
    await host.act(game, 'reveal');
    await expect(page.getByTestId('answer')).toHaveText('✓ D — Chimborazo');

    // GAME_FINISHED: every question is in the history.
    await host.act(game, 'next');
    await expect(page.getByText('Partida finalizada').first()).toBeVisible();
    await expect(page.getByTestId('history').locator('li')).toHaveCount(3);
  });

  test('el servidor revela la respuesta cuando se acaba el tiempo', async ({ page, host }) => {
    const game = await host.createGame('E2E tiempo', [{ ...GEOGRAPHY[0], timeLimit: 5 }]);

    await connect(page, game.pin);
    await expect(page.getByTestId('waiting')).toBeVisible();
    await host.act(game, 'start');

    await expect(page.getByTestId('countdown')).toBeVisible();
    await expect(page.getByTestId('answer')).toHaveText('✓ B — Quito', { timeout: 20_000 });
  });

  test('reconexión: conserva la pantalla y recupera lo que pasó sin conexión', async ({ page, host }) => {
    const game = await host.createGame('E2E reconexión', GEOGRAPHY);
    let blocked = false;
    let socket: WebSocketRoute | null = null;
    let upstream: WebSocketRoute | null = null;
    await page.routeWebSocket(/\/ws$/, (ws) => {
      if (blocked) {
        void ws.close();
        return;
      }
      upstream = ws.connectToServer();
      socket = ws;
    });

    await connect(page, game.pin);
    await expect(page.getByTestId('waiting')).toBeVisible();
    await host.act(game, 'start');
    await expect(page.getByTestId('question-text')).toContainText('capital del Ecuador');

    // The connection drops: the screen keeps the question and shows the reconnecting state.
    blocked = true;
    await upstream!.close();
    await socket!.close();
    await expect(page.getByText('Reconnecting…')).toBeVisible();
    await expect(page.getByTestId('question-text')).toContainText('capital del Ecuador');
    await expect(page.locator('app-status-badge')).toContainText('RECONNECTING');

    // ANSWER_REVEALED is published while the viewer is offline...
    await host.act(game, 'reveal');

    // ...and the snapshot delivered after reconnecting brings it back.
    blocked = false;
    await expect(page.getByTestId('answer')).toHaveText('✓ B — Quito', { timeout: 40_000 });
    await expect(page.getByTestId('live-indicator')).toBeVisible();
    await expect(page.getByText('Reconnecting…')).toHaveCount(0);
    await expect(page.getByTestId('question-number')).toContainText('PREGUNTA 1 / 3');
  });

  test('recargar la página no pierde la pregunta ni el historial', async ({ page, host }) => {
    const game = await host.createGame('E2E recarga', GEOGRAPHY);
    await connect(page, game.pin);
    await host.act(game, 'start');
    await host.act(game, 'reveal');
    await host.act(game, 'next');
    await expect(page.getByTestId('question-number')).toContainText('PREGUNTA 2 / 3');

    await page.reload();

    await expect(page.getByTestId('question-number')).toContainText('PREGUNTA 2 / 3');
    await expect(page.getByTestId('history').locator('li')).toHaveCount(1);
  });
});

test.describe('ingreso del código', () => {
  test('acepta el enlace de una partida QuizLive', async ({ page, host, baseURL }) => {
    const game = await host.createGame('E2E enlace', GEOGRAPHY);

    await connect(page, `${baseURL}/live/${game.pin}`);

    await expect(page).toHaveURL(new RegExp(`/live/${game.pin}$`));
    await expect(page.getByTestId('waiting')).toBeVisible();
  });

  test('explica por qué no acepta PIN de otro largo ni enlaces de Kahoot!', async ({ page }) => {
    await connect(page, '09399511');
    await expect(page.getByRole('alert')).toContainText('6 dígitos (este tiene 8)');
    await expect(page).toHaveURL(/\/$/);

    await page.locator('#pin').fill('https://kahoot.it/challenge/09399511?challenge-id=abc');
    await page.getByRole('button', { name: 'CONECTAR' }).click();
    await expect(page.getByRole('alert')).toContainText('no se conecta a partidas de Kahoot!');
    await expect(page).toHaveURL(/\/$/);
  });

  test('avisa cuando el PIN no existe', async ({ page }) => {
    await connect(page, '000001');

    await expect(page.getByRole('alert')).toHaveText('No existe una partida con ese PIN.');
  });
});
