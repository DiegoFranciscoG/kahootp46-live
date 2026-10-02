import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AdminSessionService } from './admin-session.service';

const ADMIN_API = /^\/api\/(quizzes|host)(\/|$)/;

/** Adds the admin bearer token to admin API calls only; viewer calls carry their own token. */
export const adminAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(AdminSessionService);
  const router = inject(Router);
  const token = session.token();
  if (!ADMIN_API.test(request.url) || !token) {
    return next(request);
  }
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        session.logout();
        void router.navigate(['/admin/login']);
      }
      return throwError(() => error);
    }),
  );
};

export const adminGuard: CanActivateFn = () => {
  const session = inject(AdminSessionService);
  return session.isAuthenticated() ? true : inject(Router).createUrlTree(['/admin/login']);
};
