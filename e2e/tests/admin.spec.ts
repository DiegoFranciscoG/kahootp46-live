import type { Page } from '@playwright/test';

import { expect, test } from './fixtures';

/** Opens the admin panel with the host's session, without typing credentials in the form. */
async function openAdmin(page: Page, session: { token: string; expiresAt: string }) {
  await page.addInitScript(
    ([key, value]) => sessionStorage.setItem(key, value),
    ['kp46:admin', JSON.stringify({ token: session.token, expiresAt: session.expiresAt })],
  );
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Mis quizzes' })).toBeVisible();
}

test.describe('panel de administración', () => {
  test('sin sesión redirige al login', async ({ page }) => {
    await page.goto('/admin');

    await expect(page).toHaveURL(/\/admin\/login$/);
    await expect(page.getByRole('button', { name: 'ENTRAR' })).toBeVisible();
  });

  test('crea un quiz, inicia la partida y el viewer recibe pregunta y respuesta', async ({ page, context, host }) => {
    const title = `E2E admin ${Date.now()}`;
    await openAdmin(page, host);

    await page.getByRole('link', { name: '+ Crear quiz' }).click();
    await page.getByLabel('Título').fill(title);
    await page.getByPlaceholder('¿Cuál es la capital del Ecuador?').fill('¿Cuál es la capital del Ecuador?');
    const options = ['Guayaquil', 'Quito', 'Cuenca', 'Loja'];
    for (const [index, text] of options.entries()) {
      await page.getByLabel(`Opción ${'ABCD'[index]}`).fill(text);
    }
    await page.getByLabel('Marcar B como correcta').check();
    // Regression: the time selector must show the real default (20 s), not the first option.
    await expect(page.getByLabel('Tiempo')).toHaveValue('20');
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page).toHaveURL(/\/admin$/);

    try {
      await page.locator('li', { hasText: title }).getByRole('button', { name: 'Iniciar partida' }).click();
      await expect(page).toHaveURL(/\/admin\/games\/[0-9a-f-]{36}$/);
      const pin = (await page.getByTestId('host-pin').innerText()).trim();
      expect(pin).toMatch(/^\d{6}$/);

      const viewer = await context.newPage();
      await viewer.goto('/');
      await viewer.locator('#pin').fill(pin);
      await viewer.getByRole('button', { name: 'CONECTAR' }).click();
      await expect(viewer.getByTestId('waiting')).toBeVisible();

      await page.getByRole('button', { name: 'Iniciar partida' }).click();
      await expect(viewer.getByTestId('question-text')).toHaveText('¿Cuál es la capital del Ecuador?');
      await expect(viewer.getByTestId('answer')).toHaveCount(0);
      // The host sees the correct option from the start; viewers do not.
      await expect(page.locator('li.ring-emerald-400')).toContainText('Quito');

      await page.getByRole('button', { name: 'Revelar respuesta' }).click();
      await expect(viewer.getByTestId('answer')).toHaveText('✓ B — Quito');

      await page.getByRole('button', { name: 'Terminar partida' }).click();
      await expect(viewer.getByText('Partida finalizada').first()).toBeVisible();
      await expect(page.getByText('Partida finalizada.')).toBeVisible();
    } finally {
      await host.removeQuizzesTitled(title);
    }
  });

  test('importa un quiz desde CSV', async ({ page, host }) => {
    const title = `E2E import ${Date.now()}`;
    await openAdmin(page, host);

    await page.getByText('Importar JSON o CSV').click();
    await page.getByPlaceholder('Título (obligatorio para CSV)').fill(title);
    await page.locator('input[type="file"]').setInputFiles({
      name: 'quiz.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(
        'question,choice_a,choice_b,choice_c,choice_d,correct,time_limit\n' +
          '¿Cuál es la capital del Ecuador?,Guayaquil,Quito,Cuenca,Loja,B,20\n',
        'utf-8',
      ),
    });

    try {
      const card = page.locator('li', { hasText: title });
      await expect(card).toBeVisible();
      await expect(card).toContainText('1 preguntas');
    } finally {
      await host.removeQuizzesTitled(title);
    }
  });
});
