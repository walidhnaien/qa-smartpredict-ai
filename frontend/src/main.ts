import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { initKeycloak } from './app/core/auth/keycloak.service';

initKeycloak()
  .then(() => {
    bootstrapApplication(App, appConfig)
      .catch(err => console.error(err));
  })
  .catch(err => console.error('Keycloak init failed', err));