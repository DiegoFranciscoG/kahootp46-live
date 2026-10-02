import { Routes } from '@angular/router';

import { adminGuard } from './core/auth/admin-auth';
import { HomeComponent } from './features/home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'KahootP46 Live' },
  {
    path: 'live/:pin',
    title: 'KahootP46 Live · Visor',
    loadComponent: () =>
      import('./features/live/live-answer-viewer.component').then((m) => m.LiveAnswerViewerComponent),
  },
  {
    path: 'admin/login',
    title: 'KahootP46 Live · Admin',
    loadComponent: () => import('./features/admin/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin-shell.component').then((m) => m.AdminShellComponent),
    children: [
      {
        path: '',
        title: 'KahootP46 Live · Mis quizzes',
        loadComponent: () => import('./features/admin/quiz-list.component').then((m) => m.QuizListComponent),
      },
      {
        path: 'quizzes/new',
        title: 'KahootP46 Live · Crear quiz',
        loadComponent: () => import('./features/admin/quiz-editor.component').then((m) => m.QuizEditorComponent),
      },
      {
        path: 'quizzes/:id',
        title: 'KahootP46 Live · Editar quiz',
        loadComponent: () => import('./features/admin/quiz-editor.component').then((m) => m.QuizEditorComponent),
      },
      {
        path: 'games/:id',
        title: 'KahootP46 Live · Partida',
        loadComponent: () => import('./features/admin/host-game.component').then((m) => m.HostGameComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
