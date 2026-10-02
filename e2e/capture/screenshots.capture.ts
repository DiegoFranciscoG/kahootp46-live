import { devices, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { join } from 'node:path';

import { expect, test } from '../tests/fixtures';

const OUT = process.env.CAPTURE_DIR ?? '../docs/img';
const FRAMES = process.env.FRAMES_DIR ?? 'capture-frames';

/**
 * Produces the README screenshots and the frames of docs/img/demo.gif from the real stack,
 * playing the demo quiz "Geografía del Ecuador" with a phone viewer, a desktop viewer and the admin panel.
 */
test('captura las pantallas del README', async ({ browser, host }) => {
  mkdirSync(OUT, { recursive: true });
  mkdirSync(FRAMES, { recursive: true });
  const game = await host.gameForQuizTitled('Geografía del Ecuador');

  const common = { locale: 'es-EC', colorScheme: 'dark' } as const;
  const phone = await browser.newContext({ ...devices['Pixel 7'], deviceScaleFactor: 2, ...common });
  const desktop = await browser.newContext({ viewport: { width: 1280, height: 580 }, ...common });
  const admin = await browser.newContext({ viewport: { width: 1280, height: 620 }, ...common });

  const shot = (page: Page, name: string) => page.screenshot({ path: join(OUT, `${name}.png`) });
  const frame = (page: Page, name: string) => page.screenshot({ path: join(FRAMES, `${name}.png`) });
  const watch = async (page: Page) => {
    await page.goto('/');
    await page.locator('#pin').fill(game.pin);
    return page;
  };

  // Phone: PIN screen → waiting → question → answer → next question.
  const m = await watch(await phone.newPage());
  await shot(m, 'inicio-movil');
  await frame(m, '01-inicio');
  await m.getByRole('button', { name: 'CONECTAR' }).click();
  await expect(m.getByTestId('waiting')).toBeVisible();
  await expect(m.getByTestId('live-indicator')).toBeVisible();
  await frame(m, '02-espera');

  const d = await watch(await desktop.newPage());
  await d.getByRole('button', { name: 'CONECTAR' }).click();
  await expect(d.getByTestId('waiting')).toBeVisible();

  await host.act(game, 'start');
  await expect(m.getByTestId('question-text')).toHaveText('¿Cuál es la capital del Ecuador?');
  await expect(m.getByTestId('countdown')).toBeVisible();
  await shot(m, 'visor-pregunta-movil');
  await frame(m, '03-pregunta');

  await host.act(game, 'reveal');
  await expect(m.getByTestId('answer')).toHaveText('✓ B — Quito');
  await shot(m, 'visor-respuesta-movil');
  await frame(m, '04-respuesta');

  await host.act(game, 'next');
  await expect(m.getByTestId('question-number')).toContainText('PREGUNTA 2 / 6');
  await expect(m.getByTestId('countdown')).toBeVisible();
  await frame(m, '05-siguiente');
  await host.act(game, 'reveal');
  await expect(m.getByTestId('answer')).toHaveText('✓ C — Pacífico');
  await frame(m, '06-respuesta-2');

  // Desktop: play until question 4 so the history panel has content.
  await host.act(game, 'next');
  await expect(d.getByTestId('question-number')).toContainText('PREGUNTA 3 / 6');
  await host.act(game, 'reveal');
  await host.act(game, 'next');
  await expect(d.getByTestId('question-number')).toContainText('PREGUNTA 4 / 6');
  await expect(d.getByTestId('history').locator('li')).toHaveCount(3);
  await expect(d.getByTestId('countdown')).toBeVisible();
  await shot(d, 'visor-escritorio-pregunta');

  // Host panel while question 4 is open (its answer is visible to the host only).
  await admin.addInitScript(
    ([key, value]) => sessionStorage.setItem(key, value),
    ['kp46:admin', JSON.stringify({ token: host.token, expiresAt: host.expiresAt })],
  );
  const a = await admin.newPage();
  await a.goto(`/admin/games/${game.id}`);
  await expect(a.getByTestId('host-pin')).toHaveText(game.pin);
  await expect(a.getByText('PREGUNTA 4 / 6')).toBeVisible();
  // The screenshots are taken from inside the docker network; show the address a local user sees.
  await a.evaluate(([from, to]) => {
    for (const p of document.querySelectorAll('p')) {
      p.textContent = (p.textContent ?? '').replace(from, to);
    }
  }, [new URL(a.url()).origin, 'http://localhost:4200']);
  await shot(a, 'admin-anfitrion');

  await host.act(game, 'reveal');
  await expect(d.getByTestId('answer')).toHaveText('✓ B — Dólar estadounidense');
  await shot(d, 'visor-escritorio-respuesta');

  await a.goto('/admin');
  await expect(a.getByRole('heading', { name: 'Mis quizzes' })).toBeVisible();
  await expect(a.locator('li', { hasText: 'Geografía del Ecuador' }).first()).toBeVisible();
  await shot(a, 'admin-quizzes');

  await a.setViewportSize({ width: 1280, height: 820 });
  await a.locator('li', { hasText: 'Geografía del Ecuador' }).getByRole('link', { name: 'Editar' }).click();
  await expect(a.getByLabel('Título')).toHaveValue('Geografía del Ecuador');
  await expect(a.getByText('PREGUNTA 1')).toBeVisible();
  await shot(a, 'admin-editor');
});
