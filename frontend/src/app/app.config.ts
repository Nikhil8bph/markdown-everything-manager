import { provideHttpClient, withFetch } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter([{ path: '', loadComponent: () => import('./features/workspace/workspace/workspace.component').then(m => m.WorkspaceComponent) }]),
    provideHttpClient(withFetch()),
  ],
};
